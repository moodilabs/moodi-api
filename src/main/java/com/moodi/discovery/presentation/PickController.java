package com.moodi.discovery.presentation;

import com.moodi.discovery.application.ImageStorageClient;
import com.moodi.discovery.application.PickImageUploadService;
import com.moodi.discovery.application.PickResult;
import com.moodi.discovery.application.PickService;
import com.moodi.discovery.presentation.dto.PickRequestDto;
import com.moodi.discovery.presentation.dto.PickResponse;
import com.moodi.discovery.presentation.dto.PickUploadUrlResponse;
import com.moodi.shared.auth.AuthMember;
import com.moodi.shared.auth.LoginRequired;
import com.moodi.shared.response.SuccessResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@LoginRequired
@RestController
@RequestMapping("/api/v1/picks")
public class PickController {

    private final PickImageUploadService pickImageUploadService;
    private final PickService pickService;

    public PickController(PickImageUploadService pickImageUploadService, PickService pickService) {
        this.pickImageUploadService = pickImageUploadService;
        this.pickService = pickService;
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

    /**
     * 업로드한 사진과 선택 지역으로 스팟을 추천한다 (DSC-04 → DSC-05).
     *
     * <p>생성과 결과 조회를 겸한다. [BTN] Find spots 한 번에 결과 화면까지 이동하므로 왕복을 줄인다.
     * 조건에 맞는 결과가 없는 것은 오류가 아니라 빈 배열 + 200이며, 이때만 fallbackSpots가 채워진다.
     */
    @PostMapping
    public SuccessResponse<PickResponse> recommend(
            @AuthMember UUID memberId,
            @Valid @RequestBody PickRequestDto request
    ) {
        PickResult result = pickService.recommend(memberId, request.imageKey(), request.toAreas());
        return SuccessResponse.of(PickResponse.from(result));
    }
}
