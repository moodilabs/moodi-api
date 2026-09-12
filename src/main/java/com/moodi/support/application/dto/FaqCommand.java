package com.moodi.support.application.dto;

public record FaqCommand(Long categoryId, String question, String answer, boolean visible) {
}
