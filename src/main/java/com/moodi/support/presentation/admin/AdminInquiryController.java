package com.moodi.support.presentation.admin;

import com.moodi.shared.auth.AdminRequired;
import com.moodi.shared.auth.AuthAdmin;
import com.moodi.shared.response.CursorResponse;
import com.moodi.shared.response.SuccessResponse;
import com.moodi.support.application.InquiryAdminService;
import com.moodi.support.domain.InquiryStatus;
import com.moodi.support.domain.InquiryTopic;
import com.moodi.support.presentation.dto.admin.AdminInquiryAnswerRequest;
import com.moodi.support.presentation.dto.admin.AdminInquiryDetailResponse;
import com.moodi.support.presentation.dto.admin.AdminInquirySummaryResponse;
import com.moodi.support.presentation.dto.admin.CountResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@AdminRequired
@RestController
@RequestMapping("/api/admin/inquiries")
public class AdminInquiryController {

    private final InquiryAdminService inquiryAdminService;

    public AdminInquiryController(InquiryAdminService inquiryAdminService) {
        this.inquiryAdminService = inquiryAdminService;
    }

    @GetMapping
    public SuccessResponse<CursorResponse<AdminInquirySummaryResponse>> getInquiries(
            @RequestParam(required = false) InquiryStatus status,
            @RequestParam(required = false) InquiryTopic topic,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int size
    ) {
        return SuccessResponse.of(inquiryAdminService.getInquiries(status, topic, cursor, size)
                .map(AdminInquirySummaryResponse::from));
    }

    @GetMapping("/count")
    public SuccessResponse<CountResponse> countReceived() {
        return SuccessResponse.of(new CountResponse(inquiryAdminService.countReceived()));
    }

    @GetMapping("/{inquiryId}")
    public SuccessResponse<AdminInquiryDetailResponse> getInquiry(@PathVariable UUID inquiryId) {
        return SuccessResponse.of(AdminInquiryDetailResponse.from(inquiryAdminService.get(inquiryId)));
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PutMapping("/{inquiryId}/answer")
    public void answer(@AuthAdmin UUID adminId, @PathVariable UUID inquiryId,
                       @Valid @RequestBody AdminInquiryAnswerRequest request) {
        inquiryAdminService.answer(inquiryId, adminId, request.content());
    }
}
