package com.moodi.spot.infrastructure.openai;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * {@code moodi.llm.protection.mode=redis}일 때만 Lettuce Redis 연결을 생성한다.
 *
 * <p>Lettuce를 직접 사용하는 이유: {@code spring-data-redis}를 classpath에 두면
 * Spring Data의 multi-store 감지가 orm.xml 기반 JPA 리포지토리 스캔을 깨뜨린다.
 */
@Configuration
@ConditionalOnProperty(name = "moodi.llm.protection.mode", havingValue = "redis")
public class LettuceRedisConfig {

    @Bean(destroyMethod = "shutdown")
    public RedisClient redisClient(@Value("${moodi.llm.redis.url}") String redisUrl) {
        return RedisClient.create(RedisURI.create(redisUrl));
    }

    @Bean(destroyMethod = "close")
    public StatefulRedisConnection<String, String> redisConnection(RedisClient redisClient) {
        return redisClient.connect();
    }

    @Bean
    public RedisCommands<String, String> redisCommands(
            StatefulRedisConnection<String, String> redisConnection) {
        return redisConnection.sync();
    }
}
