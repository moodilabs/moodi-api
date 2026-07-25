package com.moodi.shared.mood;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

import lombok.Getter;

@Getter
public class MoodVector {

    private static final double TOLERANCE = 0.01;

    private final Map<Atmosphere, Double> atmosphere;
    private final Map<Color, Double> color;
    private final Map<Lighting, Double> lighting;
    private final Map<Space, Double> space;
    private final Map<Structure, Double> structure;
    private final Map<Era, Double> era;

    public MoodVector(
            Map<Atmosphere, Double> atmosphere,
            Map<Color, Double> color,
            Map<Lighting, Double> lighting,
            Map<Space, Double> space,
            Map<Structure, Double> structure,
            Map<Era, Double> era
    ) {
        validateDistribution("atmosphere", atmosphere);
        validateDistribution("color", color);
        validateDistribution("lighting", lighting);
        validateDistribution("space", space);
        validateDistribution("structure", structure);
        validateDistribution("era", era);

        this.atmosphere = copyOf(atmosphere, Atmosphere.class);
        this.color = copyOf(color, Color.class);
        this.lighting = copyOf(lighting, Lighting.class);
        this.space = copyOf(space, Space.class);
        this.structure = copyOf(structure, Structure.class);
        this.era = copyOf(era, Era.class);
    }

    public double getWeight(Atmosphere key) {
        return atmosphere.getOrDefault(key, 0.0);
    }

    public double getWeight(Color key) {
        return color.getOrDefault(key, 0.0);
    }

    public double getWeight(Lighting key) {
        return lighting.getOrDefault(key, 0.0);
    }

    public double getWeight(Space key) {
        return space.getOrDefault(key, 0.0);
    }

    public double getWeight(Structure key) {
        return structure.getOrDefault(key, 0.0);
    }

    public double getWeight(Era key) {
        return era.getOrDefault(key, 0.0);
    }

    private <E extends Enum<E>> void validateDistribution(String axisName, Map<E, Double> distribution) {
        if (distribution == null || distribution.isEmpty()) {
            throw new IllegalArgumentException(axisName + " 분포가 비어있습니다");
        }

        for (Map.Entry<E, Double> entry : distribution.entrySet()) {
            if (entry.getValue() < 0.0 || entry.getValue() > 1.0) {
                throw new IllegalArgumentException(
                        axisName + "의 " + entry.getKey() + " 가중치가 범위를 벗어났습니다: " + entry.getValue()
                );
            }
        }

        double sum = distribution.values().stream().mapToDouble(Double::doubleValue).sum();
        if (Math.abs(sum - 1.0) > TOLERANCE) {
            throw new IllegalArgumentException(
                    axisName + " 분포의 합이 1.0이 아닙니다: " + sum
            );
        }
    }

    private <E extends Enum<E>> Map<E, Double> copyOf(Map<E, Double> source, Class<E> enumType) {
        EnumMap<E, Double> copy = new EnumMap<>(enumType);
        copy.putAll(source);
        return Collections.unmodifiableMap(copy);
    }
}
