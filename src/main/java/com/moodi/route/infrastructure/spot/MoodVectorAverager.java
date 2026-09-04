package com.moodi.route.infrastructure.spot;

import com.moodi.shared.mood.Atmosphere;
import com.moodi.shared.mood.Color;
import com.moodi.shared.mood.Era;
import com.moodi.shared.mood.Lighting;
import com.moodi.shared.mood.MoodVector;
import com.moodi.shared.mood.Space;
import com.moodi.shared.mood.Structure;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 여러 무드 벡터의 축별 가중 평균을 구한다.
 *
 * <p>기준 스팟이 2개 이상일 때, 각 스팟의 무드 벡터를 하나로 합쳐야
 * 유사도 비교의 기준점이 된다. 축별로 평균을 낸 뒤 정규화(합=1)한다.
 */
class MoodVectorAverager {

    private MoodVectorAverager() {
    }

    static MoodVector average(List<MoodVector> vectors) {
        int n = vectors.size();
        return new MoodVector(
                averageAxis(vectors, Atmosphere.class, MoodVector::getAtmosphere, n),
                averageAxis(vectors, Color.class, MoodVector::getColor, n),
                averageAxis(vectors, Lighting.class, MoodVector::getLighting, n),
                averageAxis(vectors, Space.class, MoodVector::getSpace, n),
                averageAxis(vectors, Structure.class, MoodVector::getStructure, n),
                averageAxis(vectors, Era.class, MoodVector::getEra, n)
        );
    }

    private static <E extends Enum<E>> Map<E, Double> averageAxis(
            List<MoodVector> vectors, Class<E> enumType,
            java.util.function.Function<MoodVector, Map<E, Double>> axisExtractor, int n) {

        EnumMap<E, Double> sumMap = new EnumMap<>(enumType);
        for (E key : enumType.getEnumConstants()) {
            sumMap.put(key, 0.0);
        }

        for (MoodVector vector : vectors) {
            Map<E, Double> axis = axisExtractor.apply(vector);
            for (E key : enumType.getEnumConstants()) {
                sumMap.merge(key, axis.getOrDefault(key, 0.0), Double::sum);
            }
        }

        // 평균 후 정규화 (합 = 1.0)
        double total = sumMap.values().stream().mapToDouble(Double::doubleValue).sum();
        EnumMap<E, Double> result = new EnumMap<>(enumType);
        if (total > 0) {
            for (E key : enumType.getEnumConstants()) {
                result.put(key, sumMap.get(key) / total);
            }
        } else {
            double uniform = 1.0 / enumType.getEnumConstants().length;
            for (E key : enumType.getEnumConstants()) {
                result.put(key, uniform);
            }
        }
        return result;
    }
}
