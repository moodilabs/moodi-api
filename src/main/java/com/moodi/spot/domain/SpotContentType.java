package com.moodi.spot.domain;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum SpotContentType {

    TOURIST_ATTRACTION("관광지", false),
    CULTURAL_FACILITY("문화시설", false),
    FESTIVAL("축제공연행사", false),
    TRAVEL_COURSE("여행코스", false),
    LEISURE_SPORTS("레포츠", false),
    ACCOMMODATION("숙박", true),
    SHOPPING("쇼핑", false),
    RESTAURANT("음식점", true);

    private static final Map<String, SpotContentType> LABEL_MAP =
            Stream.of(values()).collect(Collectors.toMap(v -> v.label, v -> v));

    private final String label;
    private final boolean routeExcluded;

    SpotContentType(String label, boolean routeExcluded) {
        this.label = label;
        this.routeExcluded = routeExcluded;
    }

    public static SpotContentType fromLabel(String label) {
        SpotContentType type = LABEL_MAP.get(label);
        if (type == null) {
            throw new IllegalArgumentException("Unknown content type label: " + label);
        }
        return type;
    }

    public String getLabel() {
        return label;
    }

    public boolean isRouteExcluded() {
        return routeExcluded;
    }
}
