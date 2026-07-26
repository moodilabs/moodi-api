package com.moodi.spot.infrastructure.persistence.converter;

import java.util.List;

import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import com.moodi.shared.mood.MoodTag;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class MoodTagListConverter implements AttributeConverter<List<MoodTag>, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<List<String>> TYPE_REF = new TypeReference<>() {};

    @Override
    public String convertToDatabaseColumn(List<MoodTag> tags) {
        if (tags == null) {
            return null;
        }
        try {
            List<String> keys = tags.stream().map(MoodTag::getKey).toList();
            return MAPPER.writeValueAsString(keys);
        } catch (JacksonException e) {
            throw new IllegalStateException("MoodTag 목록 직렬화 실패", e);
        }
    }

    @Override
    public List<MoodTag> convertToEntityAttribute(String json) {
        if (json == null) {
            return null;
        }
        try {
            List<String> keys = MAPPER.readValue(json, TYPE_REF);
            return keys.stream().map(MoodTag::fromKey).toList();
        } catch (JacksonException e) {
            throw new IllegalStateException("MoodTag 목록 역직렬화 실패", e);
        }
    }
}
