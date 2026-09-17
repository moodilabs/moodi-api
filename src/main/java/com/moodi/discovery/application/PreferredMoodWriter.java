package com.moodi.discovery.application;

import com.moodi.shared.mood.MoodTag;

import java.util.List;
import java.util.UUID;

/**
 * 회원 선호 무드 태그 갱신 포트. 누적 벡터에서 파생된 태그로 member_preferred_mood를 교체한다.
 */
public interface PreferredMoodWriter {

    void overwrite(UUID memberId, List<MoodTag> tags);
}
