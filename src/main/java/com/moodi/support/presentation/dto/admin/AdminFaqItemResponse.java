package com.moodi.support.presentation.dto.admin;

import com.moodi.support.application.dto.FaqItem;

public record AdminFaqItemResponse(Long id, String question, String answer, int sortOrder, boolean visible) {

    public static AdminFaqItemResponse from(FaqItem item) {
        return new AdminFaqItemResponse(item.id(), item.question(), item.answer(), item.sortOrder(), item.visible());
    }
}
