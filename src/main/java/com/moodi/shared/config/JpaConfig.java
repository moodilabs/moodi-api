package com.moodi.shared.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;

@EnableJpaAuditing(dateTimeProviderRef = "auditingDateTimeProvider")
@Configuration
public class JpaConfig {

    // 기본 DateTimeProvider는 JVM 기본 시간대(컨테이너는 UTC)를 써서 ClockConfig(KST)와 어긋난다.
    @Bean
    public DateTimeProvider auditingDateTimeProvider(Clock clock) {
        return () -> Optional.of(LocalDateTime.now(clock));
    }
}
