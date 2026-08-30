package com.moodi.discovery.application;

import com.moodi.shared.mood.MoodVector;

/**
 * 사진 1장을 무드 벡터로 분석하는 포트 (DSC-04).
 *
 * <p>{@code spot}의 같은 이름 포트는 스팟 콘텐츠(이미지 여러 장 + 소개글)를 분석하는 배치용이라
 * 시그니처가 다르다. 여기서는 사용자 사진 한 장만 다룬다.
 */
public interface MoodAnalysisClient {

    MoodVector analyze(String imageUrl);
}
