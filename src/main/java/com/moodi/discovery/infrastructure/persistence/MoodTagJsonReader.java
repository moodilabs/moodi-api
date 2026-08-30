package com.moodi.discovery.infrastructure.persistence;

import com.moodi.shared.mood.MoodTag;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.Arrays;
import java.util.List;

class MoodTagJsonReader {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<List<String>> TYPE_REF = new TypeReference<>() {};

    private MoodTagJsonReader() {
    }

    /**
     * 알 수 없는 키는 건너뛴다. 태그 규칙이 바뀌어 옛 키가 남아 있어도 추천이 실패하지 않아야 한다.
     */
    static List<MoodTag> read(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return MAPPER.readValue(json, TYPE_REF).stream()
                    .map(MoodTagJsonReader::toMoodTag)
                    .filter(tag -> tag != null)
                    .toList();
        } catch (RuntimeException e) {
            return List.of();
        }
    }

    private static MoodTag toMoodTag(String key) {
        return Arrays.stream(MoodTag.values())
                .filter(tag -> tag.getKey().equals(key))
                .findFirst()
                .orElse(null);
    }
}
