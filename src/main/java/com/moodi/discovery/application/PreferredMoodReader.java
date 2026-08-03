package com.moodi.discovery.application;

import com.moodi.shared.mood.MoodTag;

import java.util.List;
import java.util.UUID;

/**
 * 회원 선호 무드 조회 포트. 회원 컨텍스트의 데이터를 읽지만 이 인터페이스는 member를 모른다.
 */
public interface PreferredMoodReader {

    List<MoodTag> readByMemberId(UUID memberId);
}
