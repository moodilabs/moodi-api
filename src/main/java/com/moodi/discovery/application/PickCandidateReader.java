package com.moodi.discovery.application;

import com.moodi.discovery.domain.PickAreas;
import com.moodi.shared.mood.MoodTag;

import java.util.List;
import java.util.UUID;

public interface PickCandidateReader {

    /**
     * 선택 지역 안의 후보를 읽는다 (DSC-05-01).
     */
    List<PickCandidate> readByAreas(UUID memberId, PickAreas areas, int limit);

    /**
     * 지역 조건을 해제하고 무드 태그가 겹치는 후보만 읽는다 (DSC-05-02 대체 추천).
     *
     * <p>지역을 풀면 카탈로그 전체가 후보가 되므로 태그로 한 번 좁힌다.
     * {@code spot_mood.mood_tags}의 GIN 인덱스를 타는 조건이라 전체 스캔을 피할 수 있다.
     */
    List<PickCandidate> readByMoodTags(UUID memberId, List<MoodTag> moodTags, int limit);
}
