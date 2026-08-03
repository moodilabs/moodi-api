package com.moodi.member.domain;

import com.moodi.shared.error.BusinessException;
import com.moodi.shared.error.ErrorCode;
import com.moodi.shared.mood.MoodTag;

import java.util.List;

/**
 * 선호 무드 선택 규칙(`AUT-F06`)을 담는 값 객체.
 * 사전조사는 건너뛸 수 있으므로 "0개 또는 3개 이상"이 유효한 상태다.
 */
public record PreferredMoods(List<MoodTag> values) {

    private static final int MINIMUM_SELECTION = 3;

    public static PreferredMoods of(List<MoodTag> moods) {
        List<MoodTag> distinct = distinct(moods);
        if (!distinct.isEmpty() && distinct.size() < MINIMUM_SELECTION) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_MOOD_SELECTION);
        }
        return new PreferredMoods(distinct);
    }

    public boolean isEmpty() {
        return values.isEmpty();
    }

    private static List<MoodTag> distinct(List<MoodTag> moods) {
        if (moods == null) {
            return List.of();
        }
        return moods.stream().distinct().toList();
    }
}
