package com.moodi.spot.infrastructure.mood;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.moodi.shared.mood.Atmosphere;
import com.moodi.shared.mood.Color;
import com.moodi.shared.mood.Era;
import com.moodi.shared.mood.Lighting;
import com.moodi.shared.mood.MoodVector;
import com.moodi.shared.mood.Space;
import com.moodi.shared.mood.Structure;
import com.moodi.spot.application.MoodAnalysisClient;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
@Profile("llm")
public class VisionLlmMoodAnalysisClient implements MoodAnalysisClient {

    private static final String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final int MAX_RETRIES = 1;

    private final RestClient restClient;
    private final String model;

    public VisionLlmMoodAnalysisClient(
            @Value("${moodi.llm.api-key}") String apiKey,
            @Value("${moodi.llm.model:gpt-4o-mini}") String model
    ) {
        this.restClient = RestClient.builder()
                .baseUrl(OPENAI_API_URL)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
        this.model = model;
    }

    @Override
    public MoodAnalysisResult analyze(List<String> imageUrls, String overview) {
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of("role", "user", "content", buildContentParts(imageUrls, overview)));

        for (int attempt = 0; attempt <= MAX_RETRIES; attempt++) {
            String responseJson = callApi(messages);
            String content = extractContent(responseJson);

            try {
                MoodVector vector = parseVector(content);
                double confidence = parseConfidence(content);
                return new MoodAnalysisResult(vector, confidence);
            } catch (InvalidMoodResponseException e) {
                if (attempt < MAX_RETRIES) {
                    log.warn("LLM 응답 invalid (시도 {}): {} — 재요청합니다", attempt + 1, e.getMessage());
                    messages.add(Map.of("role", "assistant", "content", content));
                    messages.add(Map.of("role", "user", "content",
                            "응답에 허용되지 않는 키가 포함되어 있습니다: " + e.getMessage()
                                    + "\n각 축에 허용된 키만 사용하세요. 다시 JSON을 출력하세요."));
                } else {
                    throw new IllegalStateException(
                            "LLM 응답 검증 실패 (재시도 후에도 invalid): " + e.getMessage(), e);
                }
            }
        }

        throw new IllegalStateException("LLM 분석 실패: 최대 재시도 초과");
    }

    private List<Map<String, Object>> buildContentParts(List<String> imageUrls, String overview) {
        List<Map<String, Object>> parts = new ArrayList<>();

        parts.add(Map.of("type", "text", "text", buildPrompt(overview)));

        for (String url : imageUrls) {
            parts.add(Map.of(
                    "type", "image_url",
                    "image_url", Map.of("url", url, "detail", "low")
            ));
        }

        return parts;
    }

    private String buildPrompt(String overview) {
        return """
                당신은 관광 스팟의 무드를 분석하는 전문가입니다.
                아래 이미지와 설명 텍스트를 분석하여 6축 무드 벡터를 JSON으로 출력하세요.

                각 축에서 값별 가중치 분포(합 1.0)를 산출하세요. 0인 값은 생략 가능합니다.
                반드시 각 축에 정의된 키만 사용하세요. 다른 축의 키를 섞지 마세요.

                축과 허용된 값:
                - atmosphere: cozy, serene, lively, romantic, moody
                - color: warm, cool, pastel, mono, vivid
                - lighting: daylight, golden_hour, night_neon, overcast, indoor
                - space: nature, ocean, urban, interior, alley_local
                - structure: open, minimal, dense, geometric, organic
                - era: traditional, retro, modern, futuristic

                스팟 설명: %s

                JSON만 출력하세요. 마크다운이나 설명 없이 순수 JSON만:
                {
                  "atmosphere": {"cozy": 0.6, "serene": 0.4},
                  "color": {"warm": 0.8, "mono": 0.2},
                  "lighting": {"indoor": 0.7, "golden_hour": 0.3},
                  "space": {"interior": 0.9, "alley_local": 0.1},
                  "structure": {"dense": 0.8, "geometric": 0.2},
                  "era": {"retro": 0.9, "modern": 0.1},
                  "confidence": 0.85
                }
                """.formatted(overview != null && !overview.isBlank() ? overview : "(설명 없음)");
    }

    private String callApi(List<Map<String, Object>> messages) {
        Map<String, Object> requestBody = Map.of(
                "model", model,
                "messages", messages,
                "max_tokens", 500,
                "temperature", 0.2
        );

        return restClient.post()
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(String.class);
    }

    @SuppressWarnings("unchecked")
    private String extractContent(String responseJson) {
        try {
            Map<String, Object> response = MAPPER.readValue(responseJson, new TypeReference<Map<String, Object>>() {});
            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
            Map<String, Object> message = (Map<String, Object>) choices.getFirst().get("message");
            String content = ((String) message.get("content")).strip();

            if (content.startsWith("```")) {
                content = content.replaceAll("^```(?:json)?\\s*", "").replaceAll("\\s*```$", "");
            }
            return content;
        } catch (Exception e) {
            throw new IllegalStateException("LLM 응답 추출 실패: " + e.getMessage(), e);
        }
    }

    private MoodVector parseVector(String content) {
        try {
            Map<String, Object> parsed = MAPPER.readValue(content, new TypeReference<Map<String, Object>>() {});
            return new MoodVector(
                    parseAxisStrict(parsed, "atmosphere", Atmosphere.class, Atmosphere::fromKey),
                    parseAxisStrict(parsed, "color", Color.class, Color::fromKey),
                    parseAxisStrict(parsed, "lighting", Lighting.class, Lighting::fromKey),
                    parseAxisStrict(parsed, "space", Space.class, Space::fromKey),
                    parseAxisStrict(parsed, "structure", Structure.class, Structure::fromKey),
                    parseAxisStrict(parsed, "era", Era.class, Era::fromKey)
            );
        } catch (InvalidMoodResponseException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("LLM 응답 파싱 실패: " + e.getMessage(), e);
        }
    }

    private double parseConfidence(String content) {
        try {
            Map<String, Object> parsed = MAPPER.readValue(content, new TypeReference<Map<String, Object>>() {});
            return parsed.containsKey("confidence")
                    ? ((Number) parsed.get("confidence")).doubleValue()
                    : 0.7;
        } catch (Exception e) {
            return 0.7;
        }
    }

    @SuppressWarnings("unchecked")
    private <E extends Enum<E>> Map<E, Double> parseAxisStrict(
            Map<String, Object> parsed, String axisKey, Class<E> enumType,
            java.util.function.Function<String, E> fromKey) {
        Map<String, Number> raw = (Map<String, Number>) parsed.get(axisKey);
        if (raw == null || raw.isEmpty()) {
            throw new InvalidMoodResponseException("축 '" + axisKey + "'이 응답에 없습니다");
        }

        List<String> invalidKeys = new ArrayList<>();
        EnumMap<E, Double> result = new EnumMap<>(enumType);

        for (Map.Entry<String, Number> entry : raw.entrySet()) {
            try {
                result.put(fromKey.apply(entry.getKey()), entry.getValue().doubleValue());
            } catch (IllegalArgumentException e) {
                invalidKeys.add(entry.getKey());
            }
        }

        if (!invalidKeys.isEmpty()) {
            throw new InvalidMoodResponseException(
                    "축 '" + axisKey + "'에 허용되지 않는 키: " + invalidKeys);
        }

        // 합계 보정 (LLM이 정확히 1.0을 맞추지 못할 수 있음)
        double sum = result.values().stream().mapToDouble(Double::doubleValue).sum();
        if (Math.abs(sum - 1.0) > 0.05) {
            log.warn("축 '{}' 합계 {}으로 정규화", axisKey, sum);
            double finalSum = sum;
            result.replaceAll((k, v) -> Math.round(v / finalSum * 100.0) / 100.0);
            double newSum = result.values().stream().mapToDouble(Double::doubleValue).sum();
            E firstKey = result.keySet().iterator().next();
            result.merge(firstKey, 1.0 - newSum, Double::sum);
        }

        return result;
    }

    private static class InvalidMoodResponseException extends RuntimeException {
        InvalidMoodResponseException(String message) {
            super(message);
        }
    }
}
