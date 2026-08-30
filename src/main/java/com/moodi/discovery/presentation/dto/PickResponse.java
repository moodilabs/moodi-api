package com.moodi.discovery.presentation.dto;

import com.moodi.discovery.application.PickResult;
import com.moodi.discovery.application.PickResultItem;

import java.util.List;
import java.util.UUID;

public record PickResponse(
        UUID pickId,
        List<PickSpotResponse> spots,
        List<PickSpotResponse> fallbackSpots
) {

    public static PickResponse from(PickResult result) {
        return new PickResponse(
                result.pickId(),
                toResponses(result.spots()),
                toResponses(result.fallbackSpots())
        );
    }

    private static List<PickSpotResponse> toResponses(List<PickResultItem> items) {
        return items.stream().map(PickSpotResponse::from).toList();
    }
}
