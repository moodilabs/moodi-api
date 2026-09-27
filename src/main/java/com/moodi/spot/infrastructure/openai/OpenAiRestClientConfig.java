package com.moodi.spot.infrastructure.openai;

import java.net.http.HttpClient;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import com.moodi.spot.application.RateLimitException;

/**
 * OpenAI API용 RestClient 싱글턴 Bean.
 *
 * <p>JDK {@link HttpClient}를 하나만 생성해 연결 풀을 재사용하고,
 * connect 3초 / read 8초로 타임아웃을 단축한다.
 * 모든 LLM 클라이언트({@code VisionLlm}, {@code Translation}, {@code Description})가
 * 이 Bean을 공유한다.
 */
@Configuration
@Profile("llm")
public class OpenAiRestClientConfig {

    private static final String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";

    @Bean
    public RestClient openAiRestClient(@Value("${moodi.llm.api-key}") String apiKey) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(8));

        return RestClient.builder()
                .baseUrl(OPENAI_API_URL)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .requestFactory(requestFactory)
                .defaultStatusHandler(
                        status -> status.isSameCodeAs(HttpStatusCode.valueOf(429)),
                        (request, response) -> {
                            throw new RateLimitException(
                                    "OpenAI API rate limit (429): " + response.getStatusCode());
                        }
                )
                .build();
    }
}
