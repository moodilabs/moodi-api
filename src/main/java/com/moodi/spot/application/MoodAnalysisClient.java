package com.moodi.spot.application;

import com.moodi.shared.mood.MoodVector;

import java.util.List;

public interface MoodAnalysisClient {

    MoodAnalysisResult analyze(List<String> imageUrls, String overview);

    default int getRetryCount() { return 0; }

    default int getRateLimitCount() { return 0; }

    default void resetCounters() {}

    record MoodAnalysisResult(MoodVector moodVector, double confidence, double seasonalScore) {}
}
