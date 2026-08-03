package com.moodi.discovery.infrastructure.persistence;

import com.moodi.discovery.application.FeedSeedGenerator;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

@Component
public class RandomFeedSeedGenerator implements FeedSeedGenerator {

    @Override
    public String generate() {
        return Long.toHexString(ThreadLocalRandom.current().nextLong());
    }
}
