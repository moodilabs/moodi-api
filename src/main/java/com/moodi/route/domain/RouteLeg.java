package com.moodi.route.domain;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RouteLeg {

    private Long id;
    private int fromSequence;
    private int toSequence;
    private TravelMode travelMode;
    private int durationSeconds;
    private int distanceMeters;
    private String landingUrl;

    private RouteLeg(int fromSequence, int toSequence, TravelMode travelMode,
                     int durationSeconds, int distanceMeters, String landingUrl) {
        this.fromSequence = fromSequence;
        this.toSequence = toSequence;
        this.travelMode = travelMode;
        this.durationSeconds = durationSeconds;
        this.distanceMeters = distanceMeters;
        this.landingUrl = landingUrl;
    }

    public RouteLeg copy() {
        return new RouteLeg(fromSequence, toSequence, travelMode, durationSeconds, distanceMeters, landingUrl);
    }

    public static RouteLeg create(int fromSequence, int toSequence, TravelMode travelMode,
                                   int durationSeconds, int distanceMeters, String landingUrl) {
        return new RouteLeg(fromSequence, toSequence, travelMode, durationSeconds, distanceMeters, landingUrl);
    }
}
