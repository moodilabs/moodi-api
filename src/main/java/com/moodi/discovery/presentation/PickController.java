package com.moodi.discovery.presentation;

import com.moodi.discovery.application.AreaSuggestService;
import com.moodi.discovery.application.ImageStorageClient;
import com.moodi.discovery.application.PickImageUploadService;
import com.moodi.discovery.application.PickResult;
import com.moodi.discovery.application.PickService;
import com.moodi.discovery.presentation.dto.AreaSuggestResponse;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@LoginRequired
@RestController
@RequestMapping("/api/v1/picks")
public class PickController {

    private static final int DEFAULT_AREA_SUGGEST_LIMIT = 20;

    private final PickImageUploadService pickImageUploadService;
    private final PickService pickService;
    private final AreaSuggestService areaSuggestService;

    public PickController(PickImageUploadService pickImageUploadService, PickService pickService,
                          AreaSuggestService areaSuggestService) {
        this.pickImageUploadService = pickImageUploadService;
        this.pickService = pickService;
        this.areaSuggestService = areaSuggestService;
    }

    /**
     * 지역 자동완성 (DSC-04).
     *
     * <p>스팟이 실제로 있는 지역만 돌려준다. 고를 수는 있는데 추천 결과가 0건인 지역을 노출하면
     * 사용자가 빈 결과 화면으로 떨어진다. 응답의 level·region·district·neighborhood를
     * 그대로 추천 요청의 지역 조건에 실어 보내면 된다.
     */
    @GetMapping("/areas")
    public SuccessResponse<List<AreaSuggestResponse>> suggestAreas(@RequestParam(required = false) String keyword) {
        List<AreaSuggestResponse> responses =
                areaSuggestService.search(keyword, DEFAULT_AREA_SUGGEST_LIMIT).stream()
                        .map(AreaSuggestResponse::from)
                        .toList();
        return SuccessResponse.of(responses);
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

    /**
     * 저장된 추천 결과를 다시 조회한다 (DSC-05).
     *
     * <p>사진을 다시 분석하지 않으므로 순서가 그대로다. 저장 여부와 스팟 내용만 최신으로 채워진다.
     */
    @GetMapping("/{pickId}")
    public SuccessResponse<PickResponse> getPick(@AuthMember UUID memberId, @PathVariable UUID pickId) {
        return SuccessResponse.of(PickResponse.from(pickService.getPick(memberId, pickId)));
    }
}
