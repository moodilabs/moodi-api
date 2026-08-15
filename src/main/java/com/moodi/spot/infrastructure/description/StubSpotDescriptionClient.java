package com.moodi.spot.infrastructure.description;

import com.moodi.spot.application.SpotDescriptionClient;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!llm")
public class StubSpotDescriptionClient implements SpotDescriptionClient {

    @Override
    public String generate(String spotName, String contentType, String area,
                           String moodTags, String overview, String locale) {
        if ("en-US".equals(locale)) {
            return "A %s space located in %s where you can enjoy a %s atmosphere."
                    .formatted(contentType, area, moodTags.isBlank() ? "unique" : moodTags);
        }
        return "%s은(는) %s 지역에 위치한 공간으로, %s 분위기를 느낄 수 있습니다."
                .formatted(spotName, area, moodTags.isBlank() ? "독특한" : moodTags);
    }
}
