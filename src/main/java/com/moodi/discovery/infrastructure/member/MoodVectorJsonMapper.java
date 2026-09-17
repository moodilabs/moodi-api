package com.moodi.discovery.infrastructure.member;

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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * MoodVector ↔ JSON 변환. spot 컨텍스트의 MoodVectorConverter와 같은 포맷을 쓰지만
 * 컨텍스트 경계를 넘지 않기 위해 독립적으로 둔다.
 */
class MoodVectorJsonMapper {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<Map<String, Map<String, Double>>> TYPE_REF = new TypeReference<>() {};

    String toJson(MoodVector vector) {
        Map<String, Map<String, Double>> raw = Map.of(
                "atmosphere", toKeyMap(vector.getAtmosphere(), Atmosphere::getKey),
                "color", toKeyMap(vector.getColor(), Color::getKey),
                "lighting", toKeyMap(vector.getLighting(), Lighting::getKey),
                "space", toKeyMap(vector.getSpace(), Space::getKey),
                "structure", toKeyMap(vector.getStructure(), Structure::getKey),
                "era", toKeyMap(vector.getEra(), Era::getKey)
        );
        try {
            return MAPPER.writeValueAsString(raw);
        } catch (Exception e) {
            throw new IllegalStateException("MoodVector 직렬화 실패", e);
        }
    }

    MoodVector fromJson(String json) {
        try {
            Map<String, Map<String, Double>> raw = MAPPER.readValue(json, TYPE_REF);
            return new MoodVector(
                    toEnumMap(raw.get("atmosphere"), Atmosphere.class, Atmosphere::fromKey),
                    toEnumMap(raw.get("color"), Color.class, Color::fromKey),
                    toEnumMap(raw.get("lighting"), Lighting.class, Lighting::fromKey),
                    toEnumMap(raw.get("space"), Space.class, Space::fromKey),
                    toEnumMap(raw.get("structure"), Structure.class, Structure::fromKey),
                    toEnumMap(raw.get("era"), Era.class, Era::fromKey)
            );
        } catch (Exception e) {
            throw new IllegalStateException("MoodVector 역직렬화 실패", e);
        }
    }

    private <E extends Enum<E>> Map<String, Double> toKeyMap(
            Map<E, Double> enumMap, Function<E, String> keyExtractor) {
        Map<String, Double> result = new LinkedHashMap<>();
        enumMap.forEach((key, value) -> result.put(keyExtractor.apply(key), value));
        return result;
    }

    private <E extends Enum<E>> Map<E, Double> toEnumMap(
            Map<String, Double> raw, Class<E> enumType, Function<String, E> fromKey) {
        EnumMap<E, Double> result = new EnumMap<>(enumType);
        raw.forEach((key, value) -> result.put(fromKey.apply(key), value));
        return result;
    }
}
