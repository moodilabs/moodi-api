package com.moodi.member.presentation;

import com.moodi.member.application.MemberOnboardingService;
import com.moodi.member.application.MemberQueryService;
import com.moodi.member.application.dto.MemberInfo;
import com.moodi.member.presentation.dto.AgreementRequest;
import com.moodi.member.presentation.dto.MemberMeResponse;
import com.moodi.member.presentation.dto.NicknameAvailabilityResponse;
import com.moodi.member.presentation.dto.PreferredMoodRequest;
import com.moodi.member.presentation.dto.ProfileRequest;
import com.moodi.shared.auth.AuthMember;
import com.moodi.shared.auth.LoginRequired;
import com.moodi.shared.response.SuccessResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
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

    public MemberController(
            MemberOnboardingService memberOnboardingService,
            MemberQueryService memberQueryService
    ) {
        this.memberOnboardingService = memberOnboardingService;
        this.memberQueryService = memberQueryService;
    }

    @GetMapping("/me")
    public SuccessResponse<MemberMeResponse> getMe(@AuthMember UUID memberId) {
        MemberInfo info = memberQueryService.getMe(memberId);
        return SuccessResponse.of(MemberMeResponse.from(info));
    }

    @GetMapping("/nickname-availability")
    public SuccessResponse<NicknameAvailabilityResponse> checkNicknameAvailability(@RequestParam String nickname) {
        boolean available = memberOnboardingService.isNicknameAvailable(nickname);
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
}
