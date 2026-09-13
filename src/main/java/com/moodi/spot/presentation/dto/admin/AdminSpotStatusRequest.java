package com.moodi.spot.presentation.dto.admin;

import com.moodi.spot.domain.SpotStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** `PUBLISHED`(숨김 해제) · `HIDDEN`(숨김, reason 필수) · `DELETED`(삭제, reason 필수). */
public record AdminSpotStatusRequest(
        @NotNull(message = "상태는 필수입니다.")
        SpotStatus status,

        @Size(max = 200, message = "사유는 200자 이하여야 합니다.")
        String reason
) {
}
