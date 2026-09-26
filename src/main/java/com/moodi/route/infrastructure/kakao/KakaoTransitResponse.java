package com.moodi.route.infrastructure.kakao;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record KakaoTransitResponse(List<Route> routes) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Route(Properties properties) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Properties(int totalDistance, int totalTime) {}
}
