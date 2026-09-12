package com.moodi.support.presentation.dto.admin;

import com.moodi.support.application.dto.FaqCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdminFaqRequest(
        @NotNull(message = "유형은 필수입니다.")
        Long categoryId,

        @NotBlank(message = "질문은 필수입니다.")
        @Size(max = 200, message = "질문은 200자 이하여야 합니다.")
        String question,

        @NotBlank(message = "답변은 필수입니다.")
        @Size(max = 10_000, message = "답변은 10,000자 이하여야 합니다.")
        String answer,

        boolean visible
) {

    public FaqCommand toCommand() {
        return new FaqCommand(categoryId, question, answer, visible);
    }
}
