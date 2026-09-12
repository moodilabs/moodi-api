package com.moodi.support.presentation;

import com.moodi.shared.response.SuccessResponse;
import com.moodi.support.application.FaqQueryService;
import com.moodi.support.presentation.dto.FaqListResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * FAQ(`MY-05-01`). 비회원 마이 메인에서도 진입하므로 로그인 없이 조회한다.
 */
@RestController
public class FaqController {

    private final FaqQueryService faqQueryService;

    public FaqController(FaqQueryService faqQueryService) {
        this.faqQueryService = faqQueryService;
    }

    @GetMapping("/api/v1/faqs")
    public SuccessResponse<FaqListResponse> getFaqs() {
        return SuccessResponse.of(FaqListResponse.from(faqQueryService.getVisibleFaqs()));
    }
}
