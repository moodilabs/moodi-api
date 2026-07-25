package com.moodi.shared.mood;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Structure {

    OPEN("open", "개방·광활"),
    MINIMAL("minimal", "미니멀·여백"),
    DENSE("dense", "밀집·디테일"),
    GEOMETRIC("geometric", "기하·대칭"),
    ORGANIC("organic", "자연·불규칙");

    private static final Map<String, Structure> KEY_MAP =
            Stream.of(values()).collect(Collectors.toMap(v -> v.key, v -> v));

    private final String key;
    private final String label;

    public static Structure fromKey(String key) {
        Structure value = KEY_MAP.get(key);
        if (value == null) {
            throw new IllegalArgumentException("Unknown structure key: " + key);
        }
        return value;
    }
}
