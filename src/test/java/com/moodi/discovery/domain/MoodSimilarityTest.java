package com.moodi.discovery.domain;

import com.moodi.discovery.support.MoodVectorFixture;
import com.moodi.shared.mood.Atmosphere;
import com.moodi.shared.mood.Color;
import com.moodi.shared.mood.Era;
import com.moodi.shared.mood.Lighting;
import com.moodi.shared.mood.MoodVector;
import com.moodi.shared.mood.Space;
import com.moodi.shared.mood.Structure;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MoodSimilarityTest {

    @Test
    @DisplayName("같은 벡터끼리는 유사도가 1이다")
    void between_identical_vectors_is_one() {
        MoodVector vector = MoodVectorFixture.serene();

        assertThat(MoodSimilarity.between(vector, vector)).isCloseTo(1.0, org.assertj.core.data.Offset.offset(1e-9));
    }

    @Test
    @DisplayName("겹치는 항목이 하나도 없으면 유사도가 0이다")
    void between_disjoint_vectors_is_zero() {
        double similarity = MoodSimilarity.between(MoodVectorFixture.serene(), MoodVectorFixture.lively());

        assertThat(similarity).isZero();
    }

    @Test
    @DisplayName("겹치는 축이 많을수록 유사도가 높다")
    void between_increases_with_shared_axes() {
        MoodVector uploaded = MoodVectorFixture.serene();
        MoodVector sharesFiveAxes = MoodVectorFixture.focused(Atmosphere.SERENE, Color.COOL,
                Lighting.DAYLIGHT, Space.NATURE, Structure.OPEN, Era.RETRO);
        MoodVector sharesOneAxis = MoodVectorFixture.focused(Atmosphere.SERENE, Color.VIVID,
                Lighting.NIGHT_NEON, Space.URBAN, Structure.DENSE, Era.FUTURISTIC);

        assertThat(MoodSimilarity.between(uploaded, sharesFiveAxes))
                .isGreaterThan(MoodSimilarity.between(uploaded, sharesOneAxis));
    }

    @Test
    @DisplayName("비교 방향이 바뀌어도 유사도는 같다")
    void between_is_symmetric() {
        MoodVector left = MoodVectorFixture.serene();
        MoodVector right = MoodVectorFixture.focused(Atmosphere.SERENE, Color.WARM,
                Lighting.DAYLIGHT, Space.OCEAN, Structure.OPEN, Era.MODERN);

        assertThat(MoodSimilarity.between(left, right))
                .isEqualTo(MoodSimilarity.between(right, left));
    }

    @Test
    @DisplayName("분포가 퍼진 벡터보다 같은 쪽으로 쏠린 벡터가 더 유사하다")
    void between_prefers_matching_distribution_shape() {
        MoodVector uploaded = MoodVectorFixture.serene();
        MoodVector spread = new MoodVector(
                Map.of(Atmosphere.SERENE, 0.5, Atmosphere.LIVELY, 0.5),
                Map.of(Color.COOL, 0.5, Color.VIVID, 0.5),
                Map.of(Lighting.DAYLIGHT, 0.5, Lighting.NIGHT_NEON, 0.5),
                Map.of(Space.NATURE, 0.5, Space.URBAN, 0.5),
                Map.of(Structure.OPEN, 0.5, Structure.DENSE, 0.5),
                Map.of(Era.MODERN, 0.5, Era.RETRO, 0.5)
        );

        assertThat(MoodSimilarity.between(uploaded, MoodVectorFixture.serene()))
                .isGreaterThan(MoodSimilarity.between(uploaded, spread));
    }
}
