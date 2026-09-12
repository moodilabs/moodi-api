package com.moodi.support.presentation.dto;

import com.moodi.support.application.dto.FaqCategoryView;

import java.util.List;

public record FaqListResponse(List<FaqCategoryResponse> categories) {

    public static FaqListResponse from(List<FaqCategoryView> views) {
        return new FaqListResponse(views.stream().map(FaqCategoryResponse::from).toList());
    }
}
