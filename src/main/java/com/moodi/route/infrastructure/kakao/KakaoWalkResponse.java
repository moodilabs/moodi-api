package com.moodi.route.infrastructure.kakao;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record KakaoWalkResponse(Route route) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Route(List<Leg> legs) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Leg(Properties properties) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Properties(int distance, int time) {}
}
