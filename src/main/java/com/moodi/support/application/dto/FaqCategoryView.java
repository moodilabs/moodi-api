package com.moodi.support.application.dto;

import com.moodi.support.domain.FaqCategory;

import java.util.List;

public record FaqCategoryView(Long id, String name, int sortOrder, boolean visible, List<FaqItem> items) {

    public static FaqCategoryView of(FaqCategory category, List<FaqItem> items) {
        return new FaqCategoryView(category.getId(), category.getName(), category.getSortOrder(),
                category.isVisible(), items);
    }
}
