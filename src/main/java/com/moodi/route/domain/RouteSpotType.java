package com.moodi.route.domain;

public enum RouteSpotType {

    TOURIST_ATTRACTION("관광지"),
    CULTURAL_FACILITY("문화시설"),
    FESTIVAL("축제공연행사"),
    LEISURE_SPORTS("레포츠"),
    ACCOMMODATION("숙박"),
    SHOPPING("쇼핑"),
    RESTAURANT("음식점");

    private final String label;

    RouteSpotType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
