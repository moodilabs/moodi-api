package com.moodi.support.application.dto;

import com.moodi.support.domain.Faq;

public record FaqItem(Long id, String question, String answer, int sortOrder, boolean visible) {

    public static FaqItem from(Faq faq) {
        return new FaqItem(faq.getId(), faq.getQuestion(), faq.getAnswer(), faq.getSortOrder(), faq.isVisible());
    }
}
