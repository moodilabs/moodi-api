package com.moodi.spot.infrastructure.translation;

import com.moodi.spot.application.SpotTranslationClient;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!llm")
public class StubSpotTranslationClient implements SpotTranslationClient {

    @Override
    public TranslatedSpot translate(String title, String addr1, String addr2) {
        return new TranslatedSpot(
                "[EN] " + title,
                addr1 != null ? "[EN] " + addr1 : null,
                addr2 != null ? "[EN] " + addr2 : null
        );
    }
}
