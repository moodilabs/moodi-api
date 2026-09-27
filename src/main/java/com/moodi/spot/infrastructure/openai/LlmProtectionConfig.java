package com.moodi.spot.infrastructure.openai;

import java.util.Optional;

import com.moodi.spot.application.LlmAdmissionController;
import com.moodi.spot.application.MoodAnalysisClient;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.sync.RedisCommands;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

/**
 * LLM 호출 보호 계층 Bean 조립.
 *
 * <p>{@code protection-mode}에 따라 구현체를 선택한다.
 * <ul>
 *   <li>{@code local} — in-memory. Cloud Run max instances=1에서만 사용</li>
 *   <li>{@code redis} — 분산. 다중 인스턴스 운영 시 필수 (Upstash 등)</li>
 * </ul>
 *
 * <p>Redis 연결은 Lettuce를 직접 사용한다. {@code spring-data-redis}를 classpath에 두면
 * Spring Data의 multi-store 감지가 활성화되어 orm.xml 기반 JPA 리포지토리 스캔이 깨지므로,
 * {@code lettuce-core}만 의존한다.
 */
@Configuration
@Profile("llm")
@EnableConfigurationProperties(LlmProtectionProperties.class)
public class LlmProtectionConfig {

    @Bean
    public LlmAdmissionController llmAdmissionController(
            LlmProtectionProperties properties,
            Optional<RedisCommands<String, String>> redisCommands) {
        return switch (properties.mode()) {
            case "local" -> new LocalAdmissionController(properties);
            case "redis" -> {
                RedisCommands<String, String> commands = redisCommands.orElseThrow(() ->
                        new IllegalStateException(
                                "protection-mode=redis인데 Redis 연결이 설정되지 않았습니다. "
                                        + "REDIS_URL 환경변수를 확인하세요."));
                yield new RedisAdmissionController(commands, properties);
            }
            default -> throw new IllegalStateException(
                    "알 수 없는 protection-mode: " + properties.mode() + " (local 또는 redis만 허용)");
        };
    }

    @Bean
    @Primary
    public MoodAnalysisClient protectedMoodAnalysisClient(
            @Qualifier("visionLlmMoodAnalysisClient") MoodAnalysisClient delegate,
            LlmAdmissionController admissionController,
            LlmProtectionProperties properties) {
        return new ProtectedMoodAnalysisClient(delegate, admissionController, properties.acquireTimeoutMillis());
    }
}
