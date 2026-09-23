package com.moodi.shared.mood;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MoodTag {

    NATURE("nature", "자연", "#Nature", "Nature"),
    OCEAN("ocean", "바다", "#Ocean", "Ocean"),
    CITYSCAPE("cityscape", "도심·야경", "#Cityscape", "Cityscape"),
    RIVERSIDE("riverside", "강변·호수", "#Riverside", "Riverside"),
    COUNTRYSIDE("countryside", "전원·시골", "#Countryside", "Countryside"),
    EXPANSIVE("expansive", "광활·탁 트인", "#Expansive", "Expansive"),
    TRADITIONAL("traditional", "전통·한옥", "#Traditional", "Traditional"),
    LOCAL("local", "동네·생활감", "#Local", "Local"),
    RETRO("retro", "레트로·뉴트로", "#Retro", "Retro"),
    INDUSTRIAL("industrial", "인더스트리얼", "#Industrial", "Industrial"),
    MODERN("modern", "모던", "#Modern", "Modern"),
    COZY("cozy", "아늑", "#Cozy", "Cozy"),
    SERENE("serene", "힐링", "#Serene", "Serene"),
    LIVELY("lively", "활기", "#Lively", "Lively"),
    ROMANTIC("romantic", "낭만", "#Romantic", "Romantic"),
    MOODY("moody", "무드", "#Moody", "Moody"),
    GOLDEN_HOUR("golden_hour", "노을", "#GoldenHour", "Golden Hour"),
    NEON("neon", "네온·야경", "#Neon", "Neon"),
    ARTSY("artsy", "예술", "#Artsy", "Artsy"),
    SEASONAL("seasonal", "계절감", "#Seasonal", "Seasonal");

    private static final Map<String, MoodTag> KEY_MAP =
            Stream.of(values()).collect(Collectors.toMap(v -> v.key, v -> v));

    private final String key;
    private final String label;
    private final String displayTag;
    private final String displayName;

    public static MoodTag fromKey(String key) {
        MoodTag value = KEY_MAP.get(key);
        if (value == null) {
            throw new IllegalArgumentException("Unknown mood tag key: " + key);
        }
        return value;
    }
}
