package com.moodi.discovery.infrastructure.mood;

import com.moodi.discovery.application.MoodAnalysisClient;
import com.moodi.shared.mood.MoodVector;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 사진 1장 분석을 {@code spot}의 무드 분석기에 위임한다.
 *
 * <p>분석 대상이 결국 같은 이미지라 모델과 프롬프트를 나눌 이유가 없다.
 * 다만 {@code discovery}의 유스케이스가 {@code spot}을 알면 안 되므로 이 어댑터에서만 참조한다.
 * 소개글은 사용자 사진에 없으므로 넘기지 않는다.
 */
@Component
public class SpotMoodAnalysisClientAdapter implements MoodAnalysisClient {

    private final com.moodi.spot.application.MoodAnalysisClient delegate;

    public SpotMoodAnalysisClientAdapter(com.moodi.spot.application.MoodAnalysisClient delegate) {
        this.delegate = delegate;
    }

    @Override
    public MoodVector analyze(String imageUrl) {
        return delegate.analyze(List.of(imageUrl), null).moodVector();
    }
}
