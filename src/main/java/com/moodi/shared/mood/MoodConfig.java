package com.moodi.shared.mood;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MoodConfig {

    @Bean
    public MoodTagRuleEngine moodTagRuleEngine() {
        return new MoodTagRuleEngine();
    }
}
