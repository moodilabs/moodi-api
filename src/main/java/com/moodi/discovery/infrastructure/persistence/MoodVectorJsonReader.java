package com.moodi.discovery.infrastructure.persistence;

import com.moodi.shared.mood.Atmosphere;
import com.moodi.shared.mood.Color;
import com.moodi.shared.mood.Era;
import com.moodi.shared.mood.Lighting;
import com.moodi.shared.mood.MoodVector;
import com.moodi.shared.mood.Space;
import com.moodi.shared.mood.Structure;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;

/**
 * {@code spot_mood.mood_vector}(JSONB)를 읽어 {@link MoodVector}로 만든다.
 *
 * <p>{@code spot}에도 같은 일을 하는 컨버터가 있지만 그쪽은 JPA {@code AttributeConverter}라
 * 엔티티 매핑에 묶여 있고, 여기서는 네이티브 쿼리 결과 문자열을 직접 다룬다.
 * 컨텍스트 경계를 넘지 않기 위해서도 여기에 따로 둔다.
 */
class MoodVectorJsonReader {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<Map<String, Map<String, Double>>> TYPE_REF = new TypeReference<>() {};

    private MoodVectorJsonReader() {
    }

    /**
     * 분석 이력이 없거나 형식이 깨진 스팟은 추천 후보에서 조용히 빠진다.
     * 한 건 때문에 추천 전체를 실패시키지 않는다.
     */
    static MoodVector read(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            Map<String, Map<String, Double>> raw = MAPPER.readValue(json, TYPE_REF);
            return new MoodVector(
                    axis(raw, "atmosphere", Atmosphere.class, Atmosphere::getKey),
                    axis(raw, "color", Color.class, Color::getKey),
                    axis(raw, "lighting", Lighting.class, Lighting::getKey),
                    axis(raw, "space", Space.class, Space::getKey),
                    axis(raw, "structure", Structure.class, Structure::getKey),
                    axis(raw, "era", Era.class, Era::getKey)
            );
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static <E extends Enum<E>> Map<E, Double> axis(Map<String, Map<String, Double>> raw, String axisName,
                                                           Class<E> enumType, Function<E, String> keyExtractor) {
        Map<String, Double> weights = raw.getOrDefault(axisName, Map.of());
        EnumMap<E, Double> result = new EnumMap<>(enumType);
        for (E value : enumType.getEnumConstants()) {
            Double weight = weights.get(keyExtractor.apply(value));
            if (weight != null) {
                result.put(value, weight);
            }
        }
        return result;
    }
}
