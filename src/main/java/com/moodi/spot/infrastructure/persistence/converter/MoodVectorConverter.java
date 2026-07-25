package com.moodi.spot.infrastructure.persistence.converter;

import java.util.EnumMap;
import java.util.Map;

import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import com.moodi.shared.mood.Atmosphere;
import com.moodi.shared.mood.Color;
import com.moodi.shared.mood.Era;
import com.moodi.shared.mood.Lighting;
import com.moodi.shared.mood.MoodVector;
import com.moodi.shared.mood.Space;
import com.moodi.shared.mood.Structure;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class MoodVectorConverter implements AttributeConverter<MoodVector, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<Map<String, Map<String, Double>>> TYPE_REF = new TypeReference<>() {};

    @Override
    public String convertToDatabaseColumn(MoodVector vector) {
        if (vector == null) {
            return null;
        }
        try {
            Map<String, Map<String, Double>> raw = Map.of(
                    "atmosphere", toKeyMap(vector.getAtmosphere(), Atmosphere::getKey),
                    "color", toKeyMap(vector.getColor(), Color::getKey),
                    "lighting", toKeyMap(vector.getLighting(), Lighting::getKey),
                    "space", toKeyMap(vector.getSpace(), Space::getKey),
                    "structure", toKeyMap(vector.getStructure(), Structure::getKey),
                    "era", toKeyMap(vector.getEra(), Era::getKey)
            );
            return MAPPER.writeValueAsString(raw);
        } catch (JacksonException e) {
            throw new IllegalStateException("MoodVector 직렬화 실패", e);
        }
    }

    @Override
    public MoodVector convertToEntityAttribute(String json) {
        if (json == null) {
            return null;
        }
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
        } catch (JacksonException e) {
            throw new IllegalStateException("MoodVector 역직렬화 실패", e);
        }
    }

    private <E extends Enum<E>> Map<String, Double> toKeyMap(
            Map<E, Double> enumMap, java.util.function.Function<E, String> keyExtractor) {
        Map<String, Double> result = new java.util.LinkedHashMap<>();
        enumMap.forEach((key, value) -> result.put(keyExtractor.apply(key), value));
        return result;
    }

    private <E extends Enum<E>> Map<E, Double> toEnumMap(
            Map<String, Double> raw, Class<E> enumType, java.util.function.Function<String, E> fromKey) {
        EnumMap<E, Double> result = new EnumMap<>(enumType);
        raw.forEach((key, value) -> result.put(fromKey.apply(key), value));
        return result;
    }
}
