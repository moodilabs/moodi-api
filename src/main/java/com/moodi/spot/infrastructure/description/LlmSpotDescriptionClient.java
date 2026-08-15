package com.moodi.spot.infrastructure.description;

import com.moodi.spot.application.RateLimitException;
import com.moodi.spot.application.SpotDescriptionClient;
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
public class LlmSpotDescriptionClient implements SpotDescriptionClient {

    private static final String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final int MAX_RETRIES = 3;
    private static final long BASE_DELAY_MS = 1000;

    private final RestClient restClient;
    private final String model;

    public LlmSpotDescriptionClient(
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
    public String generate(String spotName, String contentType, String area,
                           String moodTags, String overview, String locale) {
        String prompt = "en-US".equals(locale)
                ? buildEnPrompt(spotName, contentType, area, overview)
                : buildKoPrompt(spotName, contentType, area, moodTags, overview);

        for (int attempt = 0; attempt <= MAX_RETRIES; attempt++) {
            try {
                return callApi(prompt);
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

    private String callApi(String prompt) {
        Map<String, Object> requestBody = Map.of(
                "model", model,
                "messages", List.of(Map.of("role", "user", "content", prompt)),
                "max_tokens", 400,
                "temperature", 0.7
        );

        String responseJson = restClient.post()
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(String.class);

        return extractContent(responseJson);
    }

    private String buildKoPrompt(String spotName, String contentType, String area,
                                 String moodTags, String overview) {
        return """
                당신은 여행 큐레이터입니다. 아래 장소 정보를 바탕으로 방문자가 기대할 수 있는 분위기와 경험을 자연스러운 문장으로 설명해주세요.

                작성 가이드:
                - 장소가 어떤 공간인지 간략하게 소개
                - 공간의 분위기 및 특징 설명
                - 추천 방문 대상 또는 방문 상황 설명
                - 여행자가 기대할 수 있는 경험 설명
                - 운영시간, 주소 등 단순 정보는 제외
                - 정보 나열이 아닌 자연스러운 문장 형태로 작성
                - 3~5문장 이내, 최대 300자

                장소 정보:
                - 이름: %s
                - 유형: %s
                - 지역: %s
                - 무드: %s
                - 소개: %s

                설명만 출력하세요. 제목, 마크다운, 따옴표 없이 순수 텍스트만:
                """.formatted(
                spotName,
                contentType,
                area,
                moodTags.isBlank() ? "(없음)" : moodTags,
                overview != null && !overview.isBlank() ? overview : "(없음)"
        );
    }

    private String buildEnPrompt(String spotName, String contentType, String area,
                                 String overview) {
        return """
                You are a travel curator. Based on the Korean place information below, write a concise English description of the atmosphere and experience a visitor can expect.

                Writing guide:
                - Capture the essence of the place in 1-2 sentences
                - Focus on atmosphere and mood
                - Exclude operational details like hours or addresses
                - STRICTLY keep within 100 characters (English letters, spaces, punctuation all count)

                Place info:
                - Name: %s
                - Type: %s
                - Area: %s
                - Description (Korean): %s

                Output only the description. No title, no markdown, no quotes, plain text only:
                """.formatted(
                spotName,
                contentType,
                area,
                overview != null && !overview.isBlank() ? overview : "(none)"
        );
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Retry interrupted", e);
        }
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
}
