package com.moodi.discovery.application;

import com.moodi.discovery.support.MoodVectorFixture;
import com.moodi.shared.mood.Atmosphere;
import com.moodi.shared.mood.MoodVector;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class MoodVectorBlenderTest {

    @Test
    @DisplayName("alpha=1.0이면 새 벡터가 그대로 반환된다")
    void blend_alpha_one_returns_incoming() {
        MoodVector existing = MoodVectorFixture.serene();
        MoodVector incoming = MoodVectorFixture.lively();

        MoodVector result = MoodVectorBlender.blend(existing, incoming, 1.0);

        assertThat(result.getWeight(Atmosphere.LIVELY)).isCloseTo(1.0, within(0.01));
        assertThat(result.getWeight(Atmosphere.SERENE)).isCloseTo(0.0, within(0.01));
    }

    @Test
    @DisplayName("alpha=0.0이면 기존 벡터가 그대로 유지된다")
    void blend_alpha_zero_returns_existing() {
        MoodVector existing = MoodVectorFixture.serene();
        MoodVector incoming = MoodVectorFixture.lively();

        MoodVector result = MoodVectorBlender.blend(existing, incoming, 0.0);

        assertThat(result.getWeight(Atmosphere.SERENE)).isCloseTo(1.0, within(0.01));
        assertThat(result.getWeight(Atmosphere.LIVELY)).isCloseTo(0.0, within(0.01));
    }

    @Test
    @DisplayName("alpha=0.4이면 기존 60% + 새 40% 비율로 블렌딩된다")
    void blend_alpha_point_four_mixes_proportionally() {
        MoodVector existing = MoodVectorFixture.serene();
        MoodVector incoming = MoodVectorFixture.lively();

        MoodVector result = MoodVectorBlender.blend(existing, incoming, 0.4);

        // existing: SERENE=1.0, LIVELY=0.0
        // incoming: SERENE=0.0, LIVELY=1.0
        // blended: SERENE = 0.4*0.0 + 0.6*1.0 = 0.6, LIVELY = 0.4*1.0 + 0.6*0.0 = 0.4
        assertThat(result.getWeight(Atmosphere.SERENE)).isCloseTo(0.6, within(0.01));
        assertThat(result.getWeight(Atmosphere.LIVELY)).isCloseTo(0.4, within(0.01));
    }

    @Test
    @DisplayName("alpha가 범위를 벗어나면 예외를 던진다")
    void blend_invalid_alpha_throws() {
        MoodVector v = MoodVectorFixture.serene();

        assertThatThrownBy(() -> MoodVectorBlender.blend(v, v, -0.1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> MoodVectorBlender.blend(v, v, 1.1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
