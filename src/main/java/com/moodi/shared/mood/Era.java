package com.moodi.shared.mood;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Era {

    TRADITIONAL("traditional", "전통·고전"),
    RETRO("retro", "레트로·뉴트로"),
    MODERN("modern", "모던"),
    FUTURISTIC("futuristic", "미래적");

    private static final Map<String, Era> KEY_MAP =
            Stream.of(values()).collect(Collectors.toMap(v -> v.key, v -> v));

    private final String key;
    private final String label;

    public static Era fromKey(String key) {
        Era value = KEY_MAP.get(key);
        if (value == null) {
            throw new IllegalArgumentException("Unknown era key: " + key);
        }
        return value;
    }
}
