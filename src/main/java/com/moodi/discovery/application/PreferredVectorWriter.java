package com.moodi.discovery.application;

import com.moodi.shared.mood.MoodVector;

import java.util.UUID;

/**
 * 회원 누적 선호 벡터 저장 포트. Pick 추천 완료 시 블렌딩된 벡터를 upsert한다.
 */
public interface PreferredVectorWriter {

    void save(UUID memberId, MoodVector vector);
}
