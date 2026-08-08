package com.moodi.discovery.presentation;

import com.moodi.discovery.application.FeedService;
import com.moodi.discovery.application.FeedSpotItem;
import com.moodi.discovery.presentation.dto.FeedSpotResponse;
import com.moodi.discovery.presentation.dto.PopularSpotResponse;
import com.moodi.shared.auth.AuthMember;
import com.moodi.shared.auth.LoginRequired;
import com.moodi.shared.auth.OptionalAuthMember;
import com.moodi.shared.response.CursorResponse;
import com.moodi.shared.response.SuccessResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
public class FeedController {

    private final FeedService feedService;

    public FeedController(FeedService feedService) {
        this.feedService = feedService;
    }

    /**
     * 회원이면 선호 무드 기반 개인화 피드, 비회원이면 개인화 없는 전체 스팟 피드.
     * 커서 형식이 양쪽 동일해 클라이언트는 로그인 여부와 무관하게 같은 엔드포인트를 부른다.
     */
    @GetMapping("/api/v1/feed")
    public SuccessResponse<CursorResponse<FeedSpotResponse>> getFeed(
            @OptionalAuthMember UUID memberId,
            @RequestParam(required = false) String cursor
    ) {
        CursorResponse<FeedSpotItem> result = feedService.getFeed(memberId, cursor);
        return SuccessResponse.of(result.map(FeedSpotResponse::from));
    }

    @LoginRequired
    @GetMapping("/api/v1/feed/popular-spots")
    public SuccessResponse<List<PopularSpotResponse>> getPopularSpots(@AuthMember UUID memberId) {
        List<PopularSpotResponse> responses = feedService.getPopularSpots(memberId).stream()
                .map(PopularSpotResponse::from)
                .toList();
        return SuccessResponse.of(responses);
    }
}
