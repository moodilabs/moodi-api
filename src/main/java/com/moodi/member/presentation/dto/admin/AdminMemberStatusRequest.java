package com.moodi.member.presentation.dto.admin;

import com.moodi.member.domain.MemberStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** `SUSPENDED`(정지, reason 필수) 또는 `ACTIVE`(해제). */
public record AdminMemberStatusRequest(
        @NotNull(message = "상태는 필수입니다.")
        MemberStatus status,

        @Size(max = 200, message = "사유는 200자 이하여야 합니다.")
        String reason
) {
}
