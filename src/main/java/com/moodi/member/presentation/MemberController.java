package com.moodi.member.presentation;

import com.moodi.member.application.MemberOnboardingService;
import com.moodi.member.application.MemberProfileService;
import com.moodi.member.application.MemberQueryService;
import com.moodi.member.application.MemberWithdrawService;
import com.moodi.member.application.dto.MemberInfo;
import com.moodi.member.application.dto.MemberSummary;
import com.moodi.member.presentation.dto.AgreementRequest;
import com.moodi.member.presentation.dto.CountryChangeRequest;
import com.moodi.member.presentation.dto.MemberMeResponse;
import com.moodi.member.presentation.dto.MemberSummaryResponse;
import com.moodi.member.presentation.dto.NicknameChangeRequest;
import com.moodi.member.presentation.dto.NicknameAvailabilityResponse;
import com.moodi.member.presentation.dto.PreferredMoodRequest;
import com.moodi.member.presentation.dto.ProfileRequest;
import com.moodi.member.presentation.dto.WithdrawalRequest;
import com.moodi.shared.auth.AuthMember;
import com.moodi.shared.auth.LoginRequired;
import com.moodi.shared.response.SuccessResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@LoginRequired
@RestController
@RequestMapping("/api/v1/members")
public class MemberController {

    private final MemberOnboardingService memberOnboardingService;
    private final MemberQueryService memberQueryService;
    private final MemberProfileService memberProfileService;
    private final MemberWithdrawService memberWithdrawService;

    public MemberController(
            MemberOnboardingService memberOnboardingService,
            MemberQueryService memberQueryService,
            MemberProfileService memberProfileService,
            MemberWithdrawService memberWithdrawService
    ) {
        this.memberOnboardingService = memberOnboardingService;
        this.memberQueryService = memberQueryService;
        this.memberProfileService = memberProfileService;
        this.memberWithdrawService = memberWithdrawService;
    }

    @GetMapping("/me")
    public SuccessResponse<MemberMeResponse> getMe(@AuthMember UUID memberId) {
        MemberInfo info = memberQueryService.getMe(memberId);
        return SuccessResponse.of(MemberMeResponse.from(info));
    }

    @GetMapping("/me/summary")
    public SuccessResponse<MemberSummaryResponse> getSummary(@AuthMember UUID memberId) {
        MemberSummary summary = memberQueryService.getSummary(memberId);
        return SuccessResponse.of(MemberSummaryResponse.from(summary));
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PatchMapping("/me/nickname")
    public void changeNickname(@AuthMember UUID memberId, @Valid @RequestBody NicknameChangeRequest request) {
        memberProfileService.changeNickname(memberId, request.nickname());
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PatchMapping("/me/country")
    public void changeCountry(@AuthMember UUID memberId, @Valid @RequestBody CountryChangeRequest request) {
        memberProfileService.changeCountry(memberId, request.country());
    }

    @GetMapping("/nickname-availability")
    public SuccessResponse<NicknameAvailabilityResponse> checkNicknameAvailability(
            @AuthMember UUID memberId,
            @RequestParam String nickname
    ) {
        boolean available = memberOnboardingService.isNicknameAvailable(memberId, nickname);
        return SuccessResponse.of(new NicknameAvailabilityResponse(available));
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PostMapping("/profile")
    public void updateProfile(@AuthMember UUID memberId, @Valid @RequestBody ProfileRequest request) {
        memberOnboardingService.updateProfile(memberId, request.toCommand());
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PostMapping("/agreements")
    public void agree(@AuthMember UUID memberId, @Valid @RequestBody AgreementRequest request) {
        memberOnboardingService.agree(memberId, request.toCommand());
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PostMapping("/me/preferred-moods")
    public void updatePreferredMoods(@AuthMember UUID memberId, @Valid @RequestBody PreferredMoodRequest request) {
        memberOnboardingService.updatePreferredMoods(memberId, request.moods());
    }

    /** 탈퇴 사유(`MY-03-02`)를 함께 받아야 해서 DELETE 대신 POST를 쓴다 — DELETE 본문을 버리는 클라이언트가 있다. */
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PostMapping("/me/withdrawal")
    public void withdraw(@AuthMember UUID memberId, @Valid @RequestBody WithdrawalRequest request) {
        memberWithdrawService.withdraw(memberId, request.toCommand());
    }
}
