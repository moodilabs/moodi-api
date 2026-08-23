package com.moodi.spot.application;

public interface SpotTranslationClient {

    TranslatedSpot translate(String title, String addr1, String addr2);

    record TranslatedSpot(String title, String addr1, String addr2) {
    }
}
