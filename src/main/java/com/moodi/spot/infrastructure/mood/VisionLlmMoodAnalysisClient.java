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
        List<Map<String, Object>> contentParts = buildContentParts(imageUrls, overview);
        Map<String, Object> requestBody = buildRequest(contentParts);

        String responseJson = restClient.post()
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(String.class);

        return parseResponse(responseJson);
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

                축과 값:
                - atmosphere: cozy(아늑), serene(고요·힐링), lively(활기), romantic(낭만), moody(짙은무드)
                - color: warm(웜톤), cool(쿨톤), pastel(파스텔), mono(모노톤), vivid(비비드)
                - lighting: daylight(자연광), golden_hour(골든아워·노을), night_neon(야경·네온), overcast(흐림), indoor(실내조명)
                - space: nature(자연), ocean(바다), urban(도심), interior(실내), alley_local(골목·로컬)
                - structure: open(개방·광활), minimal(미니멀·여백), dense(밀집·디테일), geometric(기하·대칭), organic(자연·불규칙)
                - era: traditional(전통·고전), retro(레트로·뉴트로), modern(모던), futuristic(미래적)

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

    private Map<String, Object> buildRequest(List<Map<String, Object>> contentParts) {
        return Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "user", "content", contentParts)
                ),
                "max_tokens", 500,
                "temperature", 0.2
        );
    }

    @SuppressWarnings("unchecked")
    private MoodAnalysisResult parseResponse(String responseJson) {
        try {
            Map<String, Object> response = MAPPER.readValue(responseJson, new TypeReference<Map<String, Object>>() {});
            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
            Map<String, Object> message = (Map<String, Object>) choices.getFirst().get("message");
            String content = (String) message.get("content");

            // 마크다운 코드블록 제거
            content = content.strip();
            if (content.startsWith("```")) {
                content = content.replaceAll("^```(?:json)?\\s*", "").replaceAll("\\s*```$", "");
            }

            Map<String, Object> parsed = MAPPER.readValue(content, new TypeReference<Map<String, Object>>() {});

            double confidence = parsed.containsKey("confidence")
                    ? ((Number) parsed.get("confidence")).doubleValue()
                    : 0.7;

            MoodVector vector = new MoodVector(
                    parseAxis(parsed, "atmosphere", Atmosphere.class, Atmosphere::fromKey),
                    parseAxis(parsed, "color", Color.class, Color::fromKey),
                    parseAxis(parsed, "lighting", Lighting.class, Lighting::fromKey),
                    parseAxis(parsed, "space", Space.class, Space::fromKey),
                    parseAxis(parsed, "structure", Structure.class, Structure::fromKey),
                    parseAxis(parsed, "era", Era.class, Era::fromKey)
            );

            return new MoodAnalysisResult(vector, confidence);
        } catch (Exception e) {
            throw new IllegalStateException("LLM 응답 파싱 실패: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private <E extends Enum<E>> Map<E, Double> parseAxis(
            Map<String, Object> parsed, String axisKey, Class<E> enumType,
            java.util.function.Function<String, E> fromKey) {
        Map<String, Number> raw = (Map<String, Number>) parsed.get(axisKey);
        if (raw == null || raw.isEmpty()) {
            throw new IllegalStateException("축 '" + axisKey + "'이 응답에 없습니다");
        }

        EnumMap<E, Double> result = new EnumMap<>(enumType);
        for (Map.Entry<String, Number> entry : raw.entrySet()) {
            result.put(fromKey.apply(entry.getKey()), entry.getValue().doubleValue());
        }

        // 합계 보정 (LLM이 정확히 1.0을 맞추지 못할 수 있음)
        double sum = result.values().stream().mapToDouble(Double::doubleValue).sum();
        if (Math.abs(sum - 1.0) > 0.05) {
            log.warn("축 '{}' 합계 {}으로 정규화", axisKey, sum);
            double finalSum = sum;
            result.replaceAll((k, v) -> Math.round(v / finalSum * 100.0) / 100.0);
            // 반올림 오차 보정
            double newSum = result.values().stream().mapToDouble(Double::doubleValue).sum();
            E firstKey = result.keySet().iterator().next();
            result.merge(firstKey, 1.0 - newSum, Double::sum);
        }

        return result;
    }
}
