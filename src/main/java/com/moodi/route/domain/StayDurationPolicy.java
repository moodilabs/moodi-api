package com.moodi.route.domain;

import com.moodi.spot.domain.SpotContentType;

public class StayDurationPolicy {

    private static final int DEFAULT_MINUTES = 60;

    public static int getEstimatedMinutes(SpotContentType contentType) {
        return switch (contentType) {
            case TOURIST_ATTRACTION -> 120;
            case CULTURAL_FACILITY -> 90;
            case LEISURE_SPORTS -> 120;
            case FESTIVAL -> 90;
            case SHOPPING -> 60;
            case TRAVEL_COURSE -> 60;
            case ACCOMMODATION, RESTAURANT -> DEFAULT_MINUTES;
        };
    }

    private StayDurationPolicy() {
    }
}
