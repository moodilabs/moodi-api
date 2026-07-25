package com.moodi.shared.mood;

import java.util.List;
import java.util.function.Function;

public class MoodTagRule {

    private final MoodTag tag;
    private final List<Function<MoodVector, Double>> axisScorers;

    private MoodTagRule(MoodTag tag, List<Function<MoodVector, Double>> axisScorers) {
        this.tag = tag;
        this.axisScorers = axisScorers;
    }

    @SafeVarargs
    public static MoodTagRule of(MoodTag tag, Function<MoodVector, Double>... scorers) {
        return new MoodTagRule(tag, List.of(scorers));
    }

    public MoodTag getTag() {
        return tag;
    }

    public double score(MoodVector vector) {
        return axisScorers.stream()
                .mapToDouble(scorer -> Math.min(scorer.apply(vector), 1.0))
                .average()
                .orElse(0.0);
    }
}
