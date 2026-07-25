package com.moodi.shared.mood;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MoodTagRuleEngineTest {

    private final MoodTagRuleEngine engine = new MoodTagRuleEngine();

    @Test
    @DisplayName("을지로 레트로 골목 벡터 → #Retro, #Local 태그가 부여된다")
    void retro_alley_vector_derives_retro_and_local() {
        // given — 무드-분류-체계.md §6.1 판정 예시
        MoodVector vector = new MoodVector(
                Map.of(Atmosphere.MOODY, 0.45, Atmosphere.COZY, 0.35, Atmosphere.LIVELY, 0.20),
                Map.of(Color.WARM, 0.70, Color.MONO, 0.20, Color.VIVID, 0.10),
                Map.of(Lighting.OVERCAST, 0.40, Lighting.INDOOR, 0.35, Lighting.DAYLIGHT, 0.25),
                Map.of(Space.ALLEY_LOCAL, 0.80, Space.URBAN, 0.20),
                Map.of(Structure.DENSE, 0.85, Structure.GEOMETRIC, 0.10, Structure.ORGANIC, 0.05),
                Map.of(Era.RETRO, 0.82, Era.TRADITIONAL, 0.10, Era.MODERN, 0.08)
        );

        // when
        List<MoodTag> tags = engine.deriveTags(vector);

        // then
        assertThat(tags).contains(MoodTag.RETRO, MoodTag.LOCAL);
    }

    @Test
    @DisplayName("바다 + 쿨톤 + 개방 벡터 → #Ocean 태그가 부여된다")
    void ocean_vector_derives_ocean() {
        // given
        MoodVector vector = new MoodVector(
                Map.of(Atmosphere.SERENE, 0.7, Atmosphere.ROMANTIC, 0.3),
                Map.of(Color.COOL, 0.8, Color.PASTEL, 0.2),
                Map.of(Lighting.DAYLIGHT, 0.9, Lighting.GOLDEN_HOUR, 0.1),
                Map.of(Space.OCEAN, 0.9, Space.NATURE, 0.1),
                Map.of(Structure.OPEN, 0.85, Structure.ORGANIC, 0.15),
                Map.of(Era.MODERN, 1.0)
        );

        // when
        List<MoodTag> tags = engine.deriveTags(vector);

        // then
        assertThat(tags).contains(MoodTag.OCEAN);
    }

    @Test
    @DisplayName("경복궁 벡터 → #Traditional 태그가 부여된다")
    void traditional_vector_derives_traditional() {
        // given
        MoodVector vector = new MoodVector(
                Map.of(Atmosphere.SERENE, 0.6, Atmosphere.COZY, 0.4),
                Map.of(Color.WARM, 0.7, Color.MONO, 0.3),
                Map.of(Lighting.DAYLIGHT, 0.8, Lighting.GOLDEN_HOUR, 0.2),
                Map.of(Space.INTERIOR, 0.5, Space.URBAN, 0.5),
                Map.of(Structure.GEOMETRIC, 0.7, Structure.DENSE, 0.3),
                Map.of(Era.TRADITIONAL, 0.9, Era.RETRO, 0.1)
        );

        // when
        List<MoodTag> tags = engine.deriveTags(vector);

        // then
        assertThat(tags).contains(MoodTag.TRADITIONAL);
    }

    @Test
    @DisplayName("임계값 미만인 태그는 부여되지 않는다")
    void below_threshold_tag_is_excluded() {
        // given — 모든 축 균등 분포 → 어떤 태그도 임계값 넘기 어려움
        MoodVector vector = new MoodVector(
                Map.of(Atmosphere.COZY, 0.2, Atmosphere.SERENE, 0.2, Atmosphere.LIVELY, 0.2,
                        Atmosphere.ROMANTIC, 0.2, Atmosphere.MOODY, 0.2),
                Map.of(Color.WARM, 0.2, Color.COOL, 0.2, Color.PASTEL, 0.2,
                        Color.MONO, 0.2, Color.VIVID, 0.2),
                Map.of(Lighting.DAYLIGHT, 0.2, Lighting.GOLDEN_HOUR, 0.2, Lighting.NIGHT_NEON, 0.2,
                        Lighting.OVERCAST, 0.2, Lighting.INDOOR, 0.2),
                Map.of(Space.NATURE, 0.2, Space.OCEAN, 0.2, Space.URBAN, 0.2,
                        Space.INTERIOR, 0.2, Space.ALLEY_LOCAL, 0.2),
                Map.of(Structure.OPEN, 0.2, Structure.MINIMAL, 0.2, Structure.DENSE, 0.2,
                        Structure.GEOMETRIC, 0.2, Structure.ORGANIC, 0.2),
                Map.of(Era.TRADITIONAL, 0.25, Era.RETRO, 0.25, Era.MODERN, 0.25, Era.FUTURISTIC, 0.25)
        );

        // when
        List<MoodTag> tags = engine.deriveTags(vector);

        // then
        assertThat(tags).isEmpty();
    }

    @Test
    @DisplayName("최대 태그 수를 초과하지 않는다")
    void tags_limited_to_max() {
        // given — 여러 태그가 높은 점수를 받도록 설계
        MoodTagRuleEngine strictEngine = new MoodTagRuleEngine(0.3, 2);

        MoodVector vector = new MoodVector(
                Map.of(Atmosphere.MOODY, 0.45, Atmosphere.COZY, 0.35, Atmosphere.LIVELY, 0.20),
                Map.of(Color.WARM, 0.70, Color.MONO, 0.20, Color.VIVID, 0.10),
                Map.of(Lighting.OVERCAST, 0.40, Lighting.INDOOR, 0.35, Lighting.DAYLIGHT, 0.25),
                Map.of(Space.ALLEY_LOCAL, 0.80, Space.URBAN, 0.20),
                Map.of(Structure.DENSE, 0.85, Structure.GEOMETRIC, 0.10, Structure.ORGANIC, 0.05),
                Map.of(Era.RETRO, 0.82, Era.TRADITIONAL, 0.10, Era.MODERN, 0.08)
        );

        // when
        List<MoodTag> tags = strictEngine.deriveTags(vector);

        // then
        assertThat(tags).hasSizeLessThanOrEqualTo(2);
    }
}
