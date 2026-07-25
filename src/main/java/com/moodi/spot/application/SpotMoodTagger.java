package com.moodi.spot.application;

import java.util.List;

import com.moodi.shared.mood.MoodTag;
import com.moodi.shared.mood.MoodTagRuleEngine;
import com.moodi.shared.mood.MoodVector;
import com.moodi.spot.application.MoodAnalysisClient.MoodAnalysisResult;
import com.moodi.spot.domain.Spot;
import com.moodi.spot.domain.SpotImage;
import com.moodi.spot.domain.SpotImageRepository;
import com.moodi.spot.domain.SpotMood;
import com.moodi.spot.domain.SpotMoodRepository;
import com.moodi.spot.domain.SpotRepository;
import com.moodi.spot.domain.SpotTranslation;
import com.moodi.spot.domain.SpotTranslationRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class SpotMoodTagger {

    private final SpotRepository spotRepository;
    private final SpotImageRepository spotImageRepository;
    private final SpotTranslationRepository spotTranslationRepository;
    private final SpotMoodRepository spotMoodRepository;
    private final MoodAnalysisClient moodAnalysisClient;
    private final MoodTagRuleEngine moodTagRuleEngine;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void tagSpot(Spot spot) {
        List<String> imageUrls = spotImageRepository.findBySpotId(spot.getId()).stream()
                .map(SpotImage::getImageUrl)
                .toList();

        String overview = spotTranslationRepository.findBySpotIdAndLocale(spot.getId(), "ko")
                .map(SpotTranslation::getOverview)
                .orElse("");

        MoodAnalysisResult result = moodAnalysisClient.analyze(imageUrls, overview);
        MoodVector vector = result.moodVector();
        List<MoodTag> tags = moodTagRuleEngine.deriveTags(vector, result.seasonalScore());

        SpotMood spotMood = SpotMood.create(spot.getId(), vector, tags, result.confidence());
        spotMoodRepository.save(spotMood);

        spot.publish();
        spotRepository.save(spot);
    }
}
