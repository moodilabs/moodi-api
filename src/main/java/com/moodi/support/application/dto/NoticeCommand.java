package com.moodi.support.application.dto;

import com.moodi.support.domain.NoticeType;

import java.time.LocalDate;

public record NoticeCommand(NoticeType type, String title, String content, boolean visible, LocalDate publishedAt) {
}
