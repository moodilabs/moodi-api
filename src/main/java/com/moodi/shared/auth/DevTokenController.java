package com.moodi.shared.auth;

import com.moodi.shared.response.SuccessResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@Profile("local")
@RestController
public class DevTokenController {

    private final JwtProvider jwtProvider;

    public DevTokenController(JwtProvider jwtProvider) {
        this.jwtProvider = jwtProvider;
    }

    @GetMapping("/api/dev/token")
    public SuccessResponse<Map<String, String>> issueDevToken(
            @RequestParam(required = false) UUID memberId) {
        UUID id = (memberId != null) ? memberId : UUID.randomUUID();
        String accessToken = jwtProvider.issueAccessToken(id);
        return SuccessResponse.of(Map.of(
                "memberId", id.toString(),
                "accessToken", accessToken
        ));
    }
}
