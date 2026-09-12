package com.moodi.support.presentation.dto.admin;

import com.moodi.support.application.dto.FaqCategoryCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminFaqCategoryRequest(
        @NotBlank(message = "유형명은 필수입니다.")
        @Size(max = 50, message = "유형명은 50자 이하여야 합니다.")
        String name,

        boolean visible
) {

    public FaqCategoryCommand toCommand() {
        return new FaqCategoryCommand(name, visible);
    }
}
