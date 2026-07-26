package com.moodi.spot.application;

import com.moodi.spot.application.dto.SpotDescriptionContext;
import com.moodi.spot.domain.SpotDescription;
import com.moodi.spot.domain.SpotDescriptionRepository;
import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SpotDescriptionGenerator {

    private static final String DEFAULT_LOCALE = "ko-KR";

    private final SpotDescriptionRepository descriptionRepository;
    private final SpotDescriptionClient descriptionClient;
    private final SpotDescriptionWriter descriptionWriter;

    public SpotDescriptionGenerator(SpotDescriptionRepository descriptionRepository,
                                    SpotDescriptionClient descriptionClient,
                                    SpotDescriptionWriter descriptionWriter) {
        this.descriptionRepository = descriptionRepository;
        this.descriptionClient = descriptionClient;
        this.descriptionWriter = descriptionWriter;
    }

    @Nullable
    public String getOrGenerate(SpotDescriptionContext context) {
        return descriptionRepository.findBySpotIdAndLocale(context.spotId(), DEFAULT_LOCALE)
                .map(SpotDescription::getContent)
                .orElseGet(() -> generate(context));
    }

    @Nullable
    private String generate(SpotDescriptionContext context) {
        try {
            String moodTags = String.join(", ", context.moodTags());

            String content = descriptionClient.generate(
                    context.title(),
                    context.contentType(),
                    context.area(),
                    moodTags,
                    context.overview()
            );

            SpotDescription saved = descriptionWriter.saveIfAbsent(
                    context.spotId(), DEFAULT_LOCALE, content);
            return saved.getContent();
        } catch (Exception e) {
            log.error("AI 스팟 설명 생성 실패 (spotId={}): {}", context.spotId(), e.getMessage(), e);
            return null;
        }
    }
}
