package com.moodi.spot.application;

import com.moodi.shared.mood.MoodVector;

import java.util.List;

public interface MoodAnalysisClient {

    MoodAnalysisResult analyze(List<String> imageUrls, String overview);

    record MoodAnalysisResult(MoodVector moodVector, double confidence, double seasonalScore) {}
}
