package com.moodi.support.presentation.dto;

import com.moodi.support.application.dto.FaqCategoryView;

import java.util.List;

public record FaqCategoryResponse(Long id, String name, List<FaqItemResponse> items) {

    public static FaqCategoryResponse from(FaqCategoryView view) {
        return new FaqCategoryResponse(view.id(), view.name(),
                view.items().stream().map(FaqItemResponse::from).toList());
    }
}
