package com.moodi.spot.infrastructure.openai;

import com.moodi.spot.application.LlmAdmissionController;
import com.moodi.spot.application.MoodAnalysisClient;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

/**
 * LLM 호출 보호 계층 Bean 조립.
 *
 * <p>{@code protection-mode}에 따라 {@link LocalAdmissionController} 또는
 * Redis 기반 구현체를 선택하고, {@link ProtectedMoodAnalysisClient}로
 * 실제 {@link MoodAnalysisClient}를 감싼다.
 */
@Configuration
@Profile("llm")
@EnableConfigurationProperties(LlmProtectionProperties.class)
public class LlmProtectionConfig {

    @Bean
    public LlmAdmissionController llmAdmissionController(LlmProtectionProperties properties) {
        return switch (properties.mode()) {
            case "local" -> new LocalAdmissionController(properties);
            case "redis" -> throw new IllegalStateException(
                    "protection-mode=redis는 아직 구현되지 않았습니다. "
                            + "다중 인스턴스 운영 전에 Redis 기반 AdmissionController를 구현하세요.");
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
