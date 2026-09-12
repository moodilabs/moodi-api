package com.moodi.support.presentation.dto;

import com.moodi.support.application.dto.FaqItem;

public record FaqItemResponse(Long id, String question, String answer) {

    public static FaqItemResponse from(FaqItem item) {
        return new FaqItemResponse(item.id(), item.question(), item.answer());
    }
}
