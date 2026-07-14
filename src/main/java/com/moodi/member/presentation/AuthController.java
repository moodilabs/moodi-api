package com.moodi.member.presentation;

import com.moodi.member.application.AuthService;
import com.moodi.member.application.dto.LoginResult;
import com.moodi.member.presentation.dto.LoginRequest;
import com.moodi.member.presentation.dto.ReissueRequest;
import com.moodi.member.presentation.dto.TokenResponse;
import com.moodi.shared.auth.AuthMember;
import com.moodi.shared.auth.LoginRequired;
import com.moodi.shared.response.SuccessResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public SuccessResponse<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResult result = authService.login(request.toProvider(), request.idToken());
        return SuccessResponse.of(TokenResponse.from(result));
    }

    @PostMapping("/reissue")
    public SuccessResponse<TokenResponse> reissue(@Valid @RequestBody ReissueRequest request) {
        LoginResult result = authService.reissue(request.refreshToken());
        return SuccessResponse.of(TokenResponse.from(result));
    }

    @LoginRequired
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PostMapping("/logout")
    public void logout(@AuthMember UUID memberId) {
        authService.logout(memberId);
    }
}
