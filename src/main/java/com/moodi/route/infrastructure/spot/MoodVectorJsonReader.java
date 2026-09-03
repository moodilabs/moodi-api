package com.moodi.route.infrastructure.spot;

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
 * {@code spot_mood.mood_vector}(JSONB)를 {@link MoodVector}로 역직렬화한다.
 *
 * <p>Discovery에도 같은 역할의 리더가 있으나 패키지 프라이빗이고 컨텍스트 경계가 다르므로
 * Route 쪽에 별도로 둔다. 공유 커널로 올리는 것은 팀 합의 대상.
 */
class MoodVectorJsonReader {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<Map<String, Map<String, Double>>> TYPE_REF = new TypeReference<>() {};

    private MoodVectorJsonReader() {
    }

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
