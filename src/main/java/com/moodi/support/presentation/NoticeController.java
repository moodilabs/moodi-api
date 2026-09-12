package com.moodi.support.presentation;

import com.moodi.shared.response.CursorResponse;
import com.moodi.shared.response.SuccessResponse;
import com.moodi.support.application.NoticeQueryService;
import com.moodi.support.application.dto.NoticeDetail;
import com.moodi.support.application.dto.NoticeSummary;
import com.moodi.support.presentation.dto.NoticeDetailResponse;
import com.moodi.support.presentation.dto.NoticeSummaryResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 공지사항(`MY-04`). 비회원 마이 메인에서도 진입하므로 로그인 없이 조회한다.
 */
@RestController
@RequestMapping("/api/v1/notices")
public class NoticeController {

    private final NoticeQueryService noticeQueryService;

    public NoticeController(NoticeQueryService noticeQueryService) {
        this.noticeQueryService = noticeQueryService;
    }

    @GetMapping
    public SuccessResponse<CursorResponse<NoticeSummaryResponse>> getNotices(
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int size
    ) {
        CursorResponse<NoticeSummary> result = noticeQueryService.getVisibleNotices(cursor, size);
        return SuccessResponse.of(result.map(NoticeSummaryResponse::from));
    }

    @GetMapping("/{noticeId}")
    public SuccessResponse<NoticeDetailResponse> getNotice(@PathVariable Long noticeId) {
        NoticeDetail detail = noticeQueryService.getVisibleNotice(noticeId);
        return SuccessResponse.of(NoticeDetailResponse.from(detail));
    }
}
