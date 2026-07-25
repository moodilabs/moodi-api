package com.moodi.shared.mood;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Space {

    NATURE("nature", "자연"),
    OCEAN("ocean", "바다"),
    URBAN("urban", "도심"),
    INTERIOR("interior", "실내"),
    ALLEY_LOCAL("alley_local", "골목·로컬");

    private static final Map<String, Space> KEY_MAP =
            Stream.of(values()).collect(Collectors.toMap(v -> v.key, v -> v));

    private final String key;
    private final String label;

    public static Space fromKey(String key) {
        Space value = KEY_MAP.get(key);
        if (value == null) {
            throw new IllegalArgumentException("Unknown space key: " + key);
        }
        return value;
    }
}
