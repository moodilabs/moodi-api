package com.moodi.discovery.application;

import com.moodi.shared.mood.Atmosphere;
import com.moodi.shared.mood.Color;
import com.moodi.shared.mood.Era;
import com.moodi.shared.mood.Lighting;
import com.moodi.shared.mood.MoodVector;
import com.moodi.shared.mood.Space;
import com.moodi.shared.mood.Structure;

import java.util.EnumMap;
import java.util.Map;

/**
 * 두 MoodVector를 EMA(지수 이동 평균)로 블렌딩한다.
 *
 * <p>{@code result = alpha * newVector + (1 - alpha) * existing}
 * <br>alpha가 클수록 최신 사진 반영 비율이 높다.
 */
public final class MoodVectorBlender {

    private MoodVectorBlender() {
    }

    public static MoodVector blend(MoodVector existing, MoodVector incoming, double alpha) {
        if (alpha < 0.0 || alpha > 1.0) {
            throw new IllegalArgumentException("alpha는 0.0~1.0 범위여야 합니다: " + alpha);
        }

        return new MoodVector(
                blendAxis(existing.getAtmosphere(), incoming.getAtmosphere(), alpha, Atmosphere.class),
                blendAxis(existing.getColor(), incoming.getColor(), alpha, Color.class),
                blendAxis(existing.getLighting(), incoming.getLighting(), alpha, Lighting.class),
                blendAxis(existing.getSpace(), incoming.getSpace(), alpha, Space.class),
                blendAxis(existing.getStructure(), incoming.getStructure(), alpha, Structure.class),
                blendAxis(existing.getEra(), incoming.getEra(), alpha, Era.class)
        );
    }

    private static <E extends Enum<E>> Map<E, Double> blendAxis(
            Map<E, Double> existing, Map<E, Double> incoming, double alpha, Class<E> enumType) {
        EnumMap<E, Double> result = new EnumMap<>(enumType);
        for (E key : enumType.getEnumConstants()) {
            double oldVal = existing.getOrDefault(key, 0.0);
            double newVal = incoming.getOrDefault(key, 0.0);
            result.put(key, alpha * newVal + (1.0 - alpha) * oldVal);
        }
        return result;
    }
}
