package com.moodi.member.domain;

import com.moodi.shared.error.BusinessException;
import com.moodi.shared.error.ErrorCode;
import com.moodi.shared.mood.MoodTag;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PreferredMoodsTest {

    @Test
    @DisplayName("3개 이상 선택하면 그대로 유지된다")
    void of_keeps_selection_when_three_or_more() {
        PreferredMoods moods = PreferredMoods.of(List.of(MoodTag.NATURE, MoodTag.OCEAN, MoodTag.COZY));

        assertThat(moods.values()).containsExactly(MoodTag.NATURE, MoodTag.OCEAN, MoodTag.COZY);
        assertThat(moods.isEmpty()).isFalse();
    }

    @Test
    @DisplayName("사전조사를 건너뛰면 빈 선택도 유효하다")
    void of_allows_empty_selection() {
        PreferredMoods moods = PreferredMoods.of(List.of());

        assertThat(moods.isEmpty()).isTrue();
    }

    @Test
    @DisplayName("null은 빈 선택으로 처리한다")
    void of_treats_null_as_empty() {
        PreferredMoods moods = PreferredMoods.of(null);

        assertThat(moods.isEmpty()).isTrue();
    }

    @Test
    @DisplayName("1~2개만 선택하면 예외가 발생한다")
    void of_with_fewer_than_minimum_throws() {
        assertThatThrownBy(() -> PreferredMoods.of(List.of(MoodTag.NATURE, MoodTag.OCEAN)))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.INSUFFICIENT_MOOD_SELECTION);
    }

    @Test
    @DisplayName("중복 무드는 제거된다")
    void of_removes_duplicates() {
        PreferredMoods moods = PreferredMoods.of(
                List.of(MoodTag.NATURE, MoodTag.NATURE, MoodTag.OCEAN, MoodTag.COZY));

        assertThat(moods.values()).containsExactly(MoodTag.NATURE, MoodTag.OCEAN, MoodTag.COZY);
    }

    @Test
    @DisplayName("중복 제거 후 3개 미만이면 예외가 발생한다")
    void of_with_duplicates_below_minimum_throws() {
        assertThatThrownBy(() -> PreferredMoods.of(List.of(MoodTag.NATURE, MoodTag.NATURE, MoodTag.NATURE)))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.INSUFFICIENT_MOOD_SELECTION);
    }
}
