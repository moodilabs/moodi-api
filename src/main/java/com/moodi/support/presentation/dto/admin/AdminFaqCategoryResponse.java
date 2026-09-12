package com.moodi.support.presentation.dto.admin;

import com.moodi.support.application.dto.FaqCategoryView;

import java.util.List;

public record AdminFaqCategoryResponse(Long id, String name, int sortOrder, boolean visible,
                                       List<AdminFaqItemResponse> items) {

    public static AdminFaqCategoryResponse from(FaqCategoryView view) {
        return new AdminFaqCategoryResponse(view.id(), view.name(), view.sortOrder(), view.visible(),
                view.items().stream().map(AdminFaqItemResponse::from).toList());
    }
}
