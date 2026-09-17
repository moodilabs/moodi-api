package com.moodi.discovery.application;

import com.moodi.shared.mood.MoodVector;

import java.util.Optional;
import java.util.UUID;

/**
 * 회원 누적 선호 벡터 조회 포트. Pick 추천 시 기존 벡터를 읽어 EMA 블렌딩에 쓴다.
 */
public interface PreferredVectorReader {

    Optional<MoodVector> readByMemberId(UUID memberId);
}
