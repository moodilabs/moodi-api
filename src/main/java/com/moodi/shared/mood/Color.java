package com.moodi.shared.mood;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Color {

    WARM("warm", "웜톤"),
    COOL("cool", "쿨톤"),
    PASTEL("pastel", "파스텔"),
    MONO("mono", "모노톤"),
    VIVID("vivid", "비비드");

    private static final Map<String, Color> KEY_MAP =
            Stream.of(values()).collect(Collectors.toMap(v -> v.key, v -> v));

    private final String key;
    private final String label;

    public static Color fromKey(String key) {
        Color value = KEY_MAP.get(key);
        if (value == null) {
            throw new IllegalArgumentException("Unknown color key: " + key);
        }
        return value;
    }
}
