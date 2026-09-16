package com.moodi.route.domain;

import com.moodi.shared.BaseEntity;
import com.moodi.shared.error.BusinessException;
import com.moodi.shared.error.ErrorCode;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecommendedRoute extends BaseEntity {

    private Long id;
    private String title;
    private String imageUrl;
    private String region;
    private boolean visible;
    private int sortOrder;
    private List<RecommendedRouteStop> stops = new ArrayList<>();

    public static RecommendedRoute create(String title, String imageUrl, String region, boolean visible, int sortOrder, List<RecommendedRouteStop> stops) {
        RecommendedRoute route = new RecommendedRoute();
        route.update(title, imageUrl, region, visible, sortOrder, stops);
        return route;
    }

    public void update(String title, String imageUrl, String region, boolean visible, int sortOrder, List<RecommendedRouteStop> stops) {
        if (title == null || title.isBlank() || title.length() > 100 || imageUrl == null || !imageUrl.matches("https?://[^\\s]+") || imageUrl.length() > 2000
                || region == null || region.isBlank() || region.length() > 100 || sortOrder < 0 || stops == null || stops.isEmpty() || stops.size() > 30) invalid();
        Set<Long> ids = new HashSet<>();
        Map<Integer, Set<Integer>> days = new TreeMap<>();
        for (RecommendedRouteStop stop : stops) {
            if (stop == null || stop.getSpotId() == null || stop.getSpotId() <= 0 || stop.getDay() < 1 || stop.getDay() > 5
                    || stop.getSequence() < 1 || stop.getSequence() > 6 || !ids.add(stop.getSpotId())
                    || !days.computeIfAbsent(stop.getDay(), k -> new HashSet<>()).add(stop.getSequence())) invalid();
        }
        if (days.keySet().stream().mapToInt(Integer::intValue).max().orElse(0) != days.size()) invalid();
        for (Set<Integer> sequences : days.values()) if (Collections.max(sequences) != sequences.size()) invalid();
        this.title = title;
        this.imageUrl = imageUrl;
        this.region = region;
        this.visible = visible;
        this.sortOrder = sortOrder;
        this.stops.clear();
        this.stops.addAll(stops.stream().sorted(Comparator.comparingInt(RecommendedRouteStop::getDay).thenComparingInt(RecommendedRouteStop::getSequence)).toList());
    }

    public void changeVisibility(boolean visible) {
        this.visible = visible;
    }

    public void reorder(int order) {
        this.sortOrder = order;
    }

    private static void invalid() {
        throw new BusinessException(ErrorCode.INVALID_REQUEST);
    }
}
