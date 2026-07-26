package com.moodi.shared.mood;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Atmosphere {

    COZY("cozy", "아늑"),
    SERENE("serene", "고요·힐링"),
    LIVELY("lively", "활기"),
    ROMANTIC("romantic", "낭만"),
    MOODY("moody", "짙은무드");

    private static final Map<String, Atmosphere> KEY_MAP =
            Stream.of(values()).collect(Collectors.toMap(v -> v.key, v -> v));

    private final String key;
    private final String label;

    public static Atmosphere fromKey(String key) {
        Atmosphere value = KEY_MAP.get(key);
        if (value == null) {
            throw new IllegalArgumentException("Unknown atmosphere key: " + key);
        }
        return value;
    }
}
