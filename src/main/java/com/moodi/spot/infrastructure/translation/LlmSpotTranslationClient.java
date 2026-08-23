package com.moodi.spot.infrastructure.translation;

import com.moodi.spot.application.RateLimitException;
import com.moodi.spot.application.SpotTranslationClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@Profile("llm")
public class LlmSpotTranslationClient implements SpotTranslationClient {

    private static final String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final int MAX_RETRIES = 3;
    private static final long BASE_DELAY_MS = 1000;

    private final RestClient restClient;
    private final String model;

    public LlmSpotTranslationClient(
            @Value("${moodi.llm.api-key}") String apiKey,
            @Value("${moodi.llm.model:gpt-4o-mini}") String model
    ) {
        this.restClient = RestClient.builder()
                .baseUrl(OPENAI_API_URL)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .requestFactory(createRequestFactory())
                .defaultStatusHandler(
                        status -> status.isSameCodeAs(HttpStatusCode.valueOf(429)),
                        (request, response) -> {
                            throw new RateLimitException(
                                    "OpenAI API rate limit (429): " + response.getStatusCode());
                        }
                )
                .build();
        this.model = model;
    }

    private static org.springframework.http.client.ClientHttpRequestFactory createRequestFactory() {
        var factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(java.time.Duration.ofSeconds(10));
        factory.setReadTimeout(java.time.Duration.ofSeconds(30));
        return factory;
    }

    @Override
    public TranslatedSpot translate(String title, String addr1, String addr2) {
        String prompt = buildPrompt(title, addr1, addr2);

        for (int attempt = 0; attempt <= MAX_RETRIES; attempt++) {
            try {
                String response = callApi(prompt);
                return parseResponse(response);
            } catch (RateLimitException e) {
                if (attempt == MAX_RETRIES) {
                    throw e;
                }
                long delay = BASE_DELAY_MS * (1L << attempt);
                log.warn("429 Rate Limit (시도 {}/{}), {}ms 후 재시도", attempt + 1, MAX_RETRIES, delay);
                sleep(delay);
            } catch (org.springframework.web.client.ResourceAccessException e) {
                if (attempt == MAX_RETRIES) {
                    throw e;
                }
                long delay = BASE_DELAY_MS * (1L << attempt);
                log.warn("LLM 연결/타임아웃 실패 (시도 {}/{}): {} — {}ms 후 재시도",
                        attempt + 1, MAX_RETRIES, e.getMessage(), delay);
                sleep(delay);
            }
        }

        throw new IllegalStateException("LLM 호출 실패: 최대 재시도 초과");
    }

    private String buildPrompt(String title, String addr1, String addr2) {
        return """
                You are a professional translator specializing in Korean tourism content.
                Translate the following Korean place information into English.

                ## Translation rules

                ### Title
                - If the place has a widely known official English name, use that name
                - If the official English name is uncertain, use Revised Romanization without over-translating
                - Do NOT add information that is not in the original

                ### Address
                - Convert Korean address format to English address format
                - Example: "서울특별시 종로구 사직로 161" → "161 Sajik-ro, Jongno-gu, Seoul"
                - Use Revised Romanization for street and district names
                - If the address is empty, return an empty string

                ## Input
                - title: %s
                - addr1: %s
                - addr2: %s

                ## Output format
                Respond ONLY with a valid JSON object. No markdown, no explanation.
                The JSON must have exactly these three fields: "title", "addr1", "addr2"

                Example:
                {"title": "Gyeongbokgung Palace", "addr1": "161 Sajik-ro, Jongno-gu, Seoul", "addr2": ""}
                """.formatted(
                title,
                addr1 != null ? addr1 : "",
                addr2 != null ? addr2 : ""
        );
    }

    private String callApi(String prompt) {
        Map<String, Object> requestBody = Map.of(
                "model", model,
                "messages", List.of(Map.of("role", "user", "content", prompt)),
                "max_tokens", 300,
                "temperature", 0.3
        );

        String responseJson = restClient.post()
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(String.class);

        return extractContent(responseJson);
    }

    @SuppressWarnings("unchecked")
    private TranslatedSpot parseResponse(String content) {
        try {
            String json = content.strip();
            if (json.startsWith("```")) {
                json = json.replaceAll("```json\\s*", "").replaceAll("```\\s*$", "").strip();
            }

            Map<String, String> result = MAPPER.readValue(json, new TypeReference<Map<String, String>>() {});

            String title = result.getOrDefault("title", "");
            String addr1 = result.getOrDefault("addr1", "");
            String addr2 = result.getOrDefault("addr2", "");

            if (title.isBlank()) {
                throw new IllegalStateException("번역 결과에 title이 비어있습니다");
            }

            return new TranslatedSpot(title, blankToNull(addr1), blankToNull(addr2));
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("LLM 번역 응답 파싱 실패: " + content, e);
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    @SuppressWarnings("unchecked")
    private String extractContent(String responseJson) {
        try {
            Map<String, Object> response = MAPPER.readValue(responseJson, new TypeReference<Map<String, Object>>() {});
            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
            Map<String, Object> message = (Map<String, Object>) choices.getFirst().get("message");
            return ((String) message.get("content")).strip();
        } catch (Exception e) {
            throw new IllegalStateException("LLM 응답 추출 실패: " + e.getMessage(), e);
        }
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Retry interrupted", e);
        }
    }
}
