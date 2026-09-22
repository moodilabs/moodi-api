package com.moodi.member.presentation.admin;

import com.moodi.member.application.MemberAdminService;
import com.moodi.member.application.dto.MemberAdminFilter;
import com.moodi.member.application.dto.MemberAdminStatus;
import com.moodi.member.domain.MemberStatus;
import com.moodi.member.domain.OAuthProvider;
import com.moodi.member.presentation.dto.admin.AdminMemberDailyStatResponse;
import com.moodi.member.presentation.dto.admin.AdminMemberDetailResponse;
import com.moodi.member.presentation.dto.admin.AdminMemberStatusRequest;
import com.moodi.member.presentation.dto.admin.AdminMemberSummaryResponse;
import com.moodi.shared.auth.AdminRequired;
import com.moodi.shared.error.BusinessException;
import com.moodi.shared.error.ErrorCode;
import com.moodi.shared.response.CursorResponse;
import com.moodi.shared.response.SuccessResponse;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** 회원 관리. 정지·강제 탈퇴까지 모든 관리자가 할 수 있다. */
@AdminRequired
@RestController
@RequestMapping("/api/admin/members")
public class AdminMemberController {

    private final MemberAdminService memberAdminService;

    public AdminMemberController(MemberAdminService memberAdminService) {
        this.memberAdminService = memberAdminService;
    }

    @GetMapping
    public SuccessResponse<CursorResponse<AdminMemberSummaryResponse>> getMembers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) MemberAdminStatus status,
            @RequestParam(required = false) OAuthProvider provider,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int size
    ) {
        MemberAdminFilter filter = new MemberAdminFilter(keyword, status, provider);
        return SuccessResponse.of(memberAdminService.getMembers(filter, cursor, size)
                .map(AdminMemberSummaryResponse::from));
    }

    @GetMapping("/stats")
    public SuccessResponse<List<AdminMemberDailyStatResponse>> getDailyStats(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return SuccessResponse.of(memberAdminService.getDailyStats(from, to).stream()
                .map(AdminMemberDailyStatResponse::from).toList());
    }

    @GetMapping("/{memberId}")
    public SuccessResponse<AdminMemberDetailResponse> getMember(@PathVariable UUID memberId) {
        return SuccessResponse.of(AdminMemberDetailResponse.from(memberAdminService.getMember(memberId)));
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PatchMapping("/{memberId}/status")
    public void changeStatus(@PathVariable UUID memberId, @Valid @RequestBody AdminMemberStatusRequest request) {
        switch (request.status()) {
            case SUSPENDED -> memberAdminService.suspend(memberId, request.reason());
            case ACTIVE -> memberAdminService.unsuspend(memberId);
            default -> throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PostMapping("/{memberId}/withdrawal")
    public void withdraw(@PathVariable UUID memberId) {
        memberAdminService.withdraw(memberId);
    }
}
