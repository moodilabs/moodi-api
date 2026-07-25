package com.moodi.shared.mood;

import java.util.Comparator;
import java.util.List;

public class MoodTagRuleEngine {

    private static final double DEFAULT_THRESHOLD = 0.5;
    private static final int DEFAULT_MAX_TAGS = 5;

    private final List<MoodTagRule> rules;
    private final double threshold;
    private final int maxTags;

    public MoodTagRuleEngine() {
        this(DEFAULT_THRESHOLD, DEFAULT_MAX_TAGS);
    }

    public MoodTagRuleEngine(double threshold, int maxTags) {
        this.rules = buildRules();
        this.threshold = threshold;
        this.maxTags = maxTags;
    }

    public List<MoodTag> deriveTags(MoodVector vector) {
        return deriveTags(vector, 0.0);
    }

    public List<MoodTag> deriveTags(MoodVector vector, double seasonalScore) {
        List<MoodTag> tags = new java.util.ArrayList<>(rules.stream()
                .map(rule -> new TagScore(rule.getTag(), rule.score(vector)))
                .filter(ts -> ts.score >= threshold)
                .sorted(Comparator.comparingDouble(TagScore::score).reversed())
                .limit(maxTags)
                .map(TagScore::tag)
                .toList());

        if (seasonalScore >= threshold && !tags.contains(MoodTag.SEASONAL)) {
            tags.add(MoodTag.SEASONAL);
        }

        return List.copyOf(tags);
    }

    private static List<MoodTagRule> buildRules() {
        return List.of(
                // #Nature: 공간:자연 + 구조:자연·불규칙/개방
                MoodTagRule.of(MoodTag.NATURE,
                        v -> v.getWeight(Space.NATURE),
                        v -> v.getWeight(Structure.ORGANIC) + v.getWeight(Structure.OPEN)
                ),

                // #Ocean: 공간:바다 + 구조:개방 + 색감:쿨
                MoodTagRule.of(MoodTag.OCEAN,
                        v -> v.getWeight(Space.OCEAN),
                        v -> v.getWeight(Structure.OPEN),
                        v -> v.getWeight(Color.COOL)
                ),

                // #Cityscape: 공간:도심 + 조명:야경
                MoodTagRule.of(MoodTag.CITYSCAPE,
                        v -> v.getWeight(Space.URBAN),
                        v -> v.getWeight(Lighting.NIGHT_NEON)
                ),

                // #Riverside: 공간:자연/도심 + 구조:개방 (텍스트 신호는 LLM이 벡터에 반영)
                MoodTagRule.of(MoodTag.RIVERSIDE,
                        v -> v.getWeight(Space.NATURE) + v.getWeight(Space.URBAN),
                        v -> v.getWeight(Structure.OPEN)
                ),

                // #Countryside: 공간:자연 + 구조:개방/자연·불규칙
                MoodTagRule.of(MoodTag.COUNTRYSIDE,
                        v -> v.getWeight(Space.NATURE),
                        v -> v.getWeight(Structure.OPEN) + v.getWeight(Structure.ORGANIC)
                ),

                // #Expansive: 구조:개방·광활
                MoodTagRule.of(MoodTag.EXPANSIVE,
                        v -> v.getWeight(Structure.OPEN)
                ),

                // #Traditional: 시대감:전통·고전 + 구조:기하
                MoodTagRule.of(MoodTag.TRADITIONAL,
                        v -> v.getWeight(Era.TRADITIONAL),
                        v -> v.getWeight(Structure.GEOMETRIC)
                ),

                // #Local: 공간:골목 + 구조:밀집
                MoodTagRule.of(MoodTag.LOCAL,
                        v -> v.getWeight(Space.ALLEY_LOCAL),
                        v -> v.getWeight(Structure.DENSE)
                ),

                // #Retro: 시대감:레트로 + 색감:웜/모노 + 구조:밀집
                MoodTagRule.of(MoodTag.RETRO,
                        v -> v.getWeight(Era.RETRO),
                        v -> v.getWeight(Color.WARM) + v.getWeight(Color.MONO),
                        v -> v.getWeight(Structure.DENSE)
                ),

                // #Industrial: 구조:기하 + 색감:쿨 (시각단서는 LLM이 벡터에 반영)
                MoodTagRule.of(MoodTag.INDUSTRIAL,
                        v -> v.getWeight(Structure.GEOMETRIC),
                        v -> v.getWeight(Color.COOL)
                ),

                // #Modern: 시대감:모던 + 구조:미니멀
                MoodTagRule.of(MoodTag.MODERN,
                        v -> v.getWeight(Era.MODERN),
                        v -> v.getWeight(Structure.MINIMAL)
                ),

                // #Cozy: 분위기:아늑 + 조명:실내 + 색감:웜
                MoodTagRule.of(MoodTag.COZY,
                        v -> v.getWeight(Atmosphere.COZY),
                        v -> v.getWeight(Lighting.INDOOR),
                        v -> v.getWeight(Color.WARM)
                ),

                // #Serene: 분위기:고요·힐링 + 구조:미니멀·여백
                MoodTagRule.of(MoodTag.SERENE,
                        v -> v.getWeight(Atmosphere.SERENE),
                        v -> v.getWeight(Structure.MINIMAL)
                ),

                // #Lively: 분위기:활기 + 구조:밀집
                MoodTagRule.of(MoodTag.LIVELY,
                        v -> v.getWeight(Atmosphere.LIVELY),
                        v -> v.getWeight(Structure.DENSE)
                ),

                // #Romantic: 분위기:낭만 + 조명:노을/야경
                MoodTagRule.of(MoodTag.ROMANTIC,
                        v -> v.getWeight(Atmosphere.ROMANTIC),
                        v -> v.getWeight(Lighting.GOLDEN_HOUR) + v.getWeight(Lighting.NIGHT_NEON)
                ),

                // #Moody: 분위기:짙은무드 + 색감:모노 + 조명:흐림
                MoodTagRule.of(MoodTag.MOODY,
                        v -> v.getWeight(Atmosphere.MOODY),
                        v -> v.getWeight(Color.MONO),
                        v -> v.getWeight(Lighting.OVERCAST)
                ),

                // #GoldenHour: 조명:골든아워 + 색감:웜
                MoodTagRule.of(MoodTag.GOLDEN_HOUR,
                        v -> v.getWeight(Lighting.GOLDEN_HOUR),
                        v -> v.getWeight(Color.WARM)
                ),

                // #Neon: 조명:야경 + 색감:비비드
                MoodTagRule.of(MoodTag.NEON,
                        v -> v.getWeight(Lighting.NIGHT_NEON),
                        v -> v.getWeight(Color.VIVID)
                ),

                // #Artsy: 공간:실내/골목 (텍스트 신호는 별도 처리)
                MoodTagRule.of(MoodTag.ARTSY,
                        v -> v.getWeight(Space.INTERIOR) + v.getWeight(Space.ALLEY_LOCAL)
                )
                // SEASONAL은 LLM seasonalScore로 별도 판정 — deriveTags(vector, seasonalScore)에서 처리
        );
    }

    private record TagScore(MoodTag tag, double score) {}
}
