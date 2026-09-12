package com.moodi.support.presentation;

import com.moodi.shared.auth.AuthMember;
import com.moodi.shared.auth.LoginRequired;
import com.moodi.shared.response.CursorResponse;
import com.moodi.shared.response.SuccessResponse;
import com.moodi.support.application.InquiryService;
import com.moodi.support.presentation.dto.InquiryCreateRequest;
import com.moodi.support.presentation.dto.InquiryDetailResponse;
import com.moodi.support.presentation.dto.InquiryIdResponse;
import com.moodi.support.presentation.dto.InquirySummaryResponse;
import com.moodi.support.presentation.dto.InquiryUploadUrlResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** 1:1 문의(`MY-06`). 비회원 진입 화면이 없어 로그인 필수. */
@LoginRequired
@RestController
@RequestMapping("/api/v1/inquiries")
public class InquiryController {

    private final InquiryService inquiryService;

    public InquiryController(InquiryService inquiryService) {
        this.inquiryService = inquiryService;
    }

    @GetMapping
    public SuccessResponse<CursorResponse<InquirySummaryResponse>> getInquiries(
            @AuthMember UUID memberId,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int size
    ) {
        return SuccessResponse.of(inquiryService.getMine(memberId, cursor, size).map(InquirySummaryResponse::from));
    }

    /** 첨부 업로드용 서명 URL. 클라이언트가 uploadUrl로 직접 PUT하고 attachmentKey를 등록 요청에 싣는다. */
    @GetMapping("/upload-url")
    public SuccessResponse<InquiryUploadUrlResponse> issueUploadUrl(
            @AuthMember UUID memberId,
            @RequestParam String contentType,
            @RequestParam long contentLength
    ) {
        return SuccessResponse.of(InquiryUploadUrlResponse.from(
                inquiryService.issueUploadUrl(memberId, contentType, contentLength)));
    }

    @GetMapping("/{inquiryId}")
    public SuccessResponse<InquiryDetailResponse> getInquiry(@AuthMember UUID memberId,
                                                             @PathVariable UUID inquiryId) {
        return SuccessResponse.of(InquiryDetailResponse.from(inquiryService.getMine(memberId, inquiryId)));
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public SuccessResponse<InquiryIdResponse> create(@AuthMember UUID memberId,
                                                     @Valid @RequestBody InquiryCreateRequest request) {
        return SuccessResponse.of(new InquiryIdResponse(inquiryService.create(memberId, request.toCommand())));
    }
}
