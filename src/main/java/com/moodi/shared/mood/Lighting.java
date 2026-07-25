package com.moodi.shared.mood;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Lighting {

    DAYLIGHT("daylight", "자연광(낮)"),
    GOLDEN_HOUR("golden_hour", "골든아워·노을"),
    NIGHT_NEON("night_neon", "야경·네온"),
    OVERCAST("overcast", "흐림·그레이"),
    INDOOR("indoor", "실내조명");

    private static final Map<String, Lighting> KEY_MAP =
            Stream.of(values()).collect(Collectors.toMap(v -> v.key, v -> v));

    private final String key;
    private final String label;

    public static Lighting fromKey(String key) {
        Lighting value = KEY_MAP.get(key);
        if (value == null) {
            throw new IllegalArgumentException("Unknown lighting key: " + key);
        }
        return value;
    }
}
