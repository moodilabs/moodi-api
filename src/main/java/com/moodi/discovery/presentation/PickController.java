package com.moodi.discovery.presentation;

import com.moodi.discovery.application.ImageStorageClient;
import com.moodi.discovery.application.PickImageUploadService;
import com.moodi.discovery.presentation.dto.PickUploadUrlResponse;
import com.moodi.shared.auth.AuthMember;
import com.moodi.shared.auth.LoginRequired;
import com.moodi.shared.response.SuccessResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@LoginRequired
@RestController
@RequestMapping("/api/v1/picks")
public class PickController {

    private final PickImageUploadService pickImageUploadService;

    public PickController(PickImageUploadService pickImageUploadService) {
        this.pickImageUploadService = pickImageUploadService;
    }

    /**
     * 사진 업로드용 서명 URL을 발급한다 (DSC-04).
     * 클라이언트는 발급받은 uploadUrl로 파일을 직접 PUT하고, imageKey를 추천 요청에 실어 보낸다.
     */
    @GetMapping("/upload-url")
    public SuccessResponse<PickUploadUrlResponse> issueUploadUrl(
            @AuthMember UUID memberId,
            @RequestParam String contentType,
            @RequestParam long contentLength
    ) {
        ImageStorageClient.UploadTarget target =
                pickImageUploadService.issueUploadUrl(memberId, contentType, contentLength);
        return SuccessResponse.of(PickUploadUrlResponse.from(target));
    }
}
