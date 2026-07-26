package com.moodi.spot.infrastructure.mood;

import com.moodi.shared.mood.Atmosphere;
import com.moodi.shared.mood.Color;
import com.moodi.shared.mood.Era;
import com.moodi.shared.mood.Lighting;
import com.moodi.shared.mood.MoodVector;
import com.moodi.shared.mood.Space;
import com.moodi.shared.mood.Structure;
import com.moodi.spot.application.MoodAnalysisClient;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Random;

@Component
@Profile("!llm")
public class StubMoodAnalysisClient implements MoodAnalysisClient {

    private final Random random = new Random();

    @Override
    public MoodAnalysisResult analyze(List<String> imageUrls, String overview) {
        MoodVector vector = new MoodVector(
                randomDistribution(Atmosphere.values()),
                randomDistribution(Color.values()),
                randomDistribution(Lighting.values()),
                randomDistribution(Space.values()),
                randomDistribution(Structure.values()),
                randomDistribution(Era.values())
        );
        return new MoodAnalysisResult(vector, 0.5 + random.nextDouble() * 0.5, random.nextDouble());
    }

    private <E extends Enum<E>> Map<E, Double> randomDistribution(E[] values) {
        double[] weights = new double[values.length];
        double sum = 0;
        for (int i = 0; i < values.length; i++) {
            weights[i] = random.nextDouble();
            sum += weights[i];
        }

        Map<E, Double> distribution = new java.util.EnumMap<>((Class<E>) values[0].getDeclaringClass());
        for (int i = 0; i < values.length; i++) {
            double normalized = Math.round(weights[i] / sum * 100.0) / 100.0;
            distribution.put(values[i], normalized);
        }

        // 합계 보정
        double total = distribution.values().stream().mapToDouble(Double::doubleValue).sum();
        double diff = 1.0 - total;
        distribution.merge(values[0], diff, Double::sum);

        return distribution;
    }
}
