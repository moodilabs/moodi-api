package com.moodi.support.presentation.admin;

import com.moodi.shared.auth.AdminRequired;
import com.moodi.shared.response.CursorResponse;
import com.moodi.shared.response.SuccessResponse;
import com.moodi.support.application.NoticeAdminService;
import com.moodi.support.domain.NoticeType;
import com.moodi.support.presentation.dto.admin.AdminNoticeRequest;
import com.moodi.support.presentation.dto.admin.AdminNoticeResponse;
import com.moodi.support.presentation.dto.admin.IdResponse;
import com.moodi.support.presentation.dto.admin.VisibilityRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.LocalDate;

@AdminRequired
@RestController
@RequestMapping("/api/admin/notices")
public class AdminNoticeController {

    private final NoticeAdminService noticeAdminService;
    private final Clock clock;

    public AdminNoticeController(NoticeAdminService noticeAdminService, Clock clock) {
        this.noticeAdminService = noticeAdminService;
        this.clock = clock;
    }

    @GetMapping
    public SuccessResponse<CursorResponse<AdminNoticeResponse>> getNotices(
            @RequestParam(required = false) NoticeType type,
            @RequestParam(required = false) Boolean visible,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int size
    ) {
        return SuccessResponse.of(noticeAdminService.getNotices(type, visible, cursor, size)
                .map(AdminNoticeResponse::from));
    }

    @GetMapping("/{noticeId}")
    public SuccessResponse<AdminNoticeResponse> getNotice(@PathVariable Long noticeId) {
        return SuccessResponse.of(AdminNoticeResponse.from(noticeAdminService.get(noticeId)));
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public SuccessResponse<IdResponse> create(@Valid @RequestBody AdminNoticeRequest request) {
        return SuccessResponse.of(new IdResponse(noticeAdminService.create(request.toCommand(LocalDate.now(clock)))));
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PutMapping("/{noticeId}")
    public void update(@PathVariable Long noticeId, @Valid @RequestBody AdminNoticeRequest request) {
        noticeAdminService.update(noticeId, request.toCommand(LocalDate.now(clock)));
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PatchMapping("/{noticeId}/visibility")
    public void changeVisibility(@PathVariable Long noticeId, @Valid @RequestBody VisibilityRequest request) {
        noticeAdminService.changeVisibility(noticeId, request.visible());
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{noticeId}")
    public void delete(@PathVariable Long noticeId) {
        noticeAdminService.delete(noticeId);
    }
}
