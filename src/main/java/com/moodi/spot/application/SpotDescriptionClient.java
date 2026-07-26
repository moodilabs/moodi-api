package com.moodi.spot.application;

public interface SpotDescriptionClient {

    String generate(String spotName, String contentType, String area,
                    String moodTags, String overview);
}
