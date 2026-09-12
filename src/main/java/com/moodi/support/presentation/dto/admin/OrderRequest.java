package com.moodi.support.presentation.dto.admin;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/** 전체 ID를 원하는 순서로. 누락·중복이 있으면 400. */
public record OrderRequest(
        @NotEmpty(message = "순서 목록은 비어 있을 수 없습니다.")
        List<Long> ids
) {
}
