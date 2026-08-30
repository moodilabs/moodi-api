package com.moodi.discovery.presentation;

import com.moodi.discovery.application.ImageStorageClient;
import com.moodi.discovery.application.PickImageUploadService;
import com.moodi.shared.error.BusinessException;
import com.moodi.shared.error.ErrorCode;
import com.moodi.shared.support.AuthenticatedRestDocsSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.restdocs.payload.JsonFieldType;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PickControllerDocsTest extends AuthenticatedRestDocsSupport {

    private PickImageUploadService pickImageUploadService;

    @Override
    protected Object initController() {
        pickImageUploadService = mock(PickImageUploadService.class);
        return new PickController(pickImageUploadService);
    }

    @Test
    @DisplayName("사진 업로드용 서명 URL 발급")
    void issue_upload_url() throws Exception {
        when(pickImageUploadService.issueUploadUrl(eq(memberId), anyString(), anyLong()))
                .thenReturn(new ImageStorageClient.UploadTarget(
                        "https://storage.googleapis.com/moodi-pick-uploads/picks/...?X-Goog-Signature=...",
                        "picks/3f1c.../8a2e....jpg",
                        300));

        mockMvc.perform(get("/api/v1/picks/upload-url")
                        .param("contentType", "image/jpeg")
                        .param("contentLength", "2097152"))
                .andExpect(status().isOk())
                .andDo(document("picks/upload-url",
                        queryParameters(
                                parameterWithName("contentType")
                                        .description("업로드할 사진의 MIME 타입. `image/jpeg` · `image/png` · `image/heic`만 허용"),
                                parameterWithName("contentLength")
                                        .description("업로드할 사진의 바이트 수. 최대 10485760 (10MB)")
                        ),
                        responseFields(
                                fieldWithPath("data").type(JsonFieldType.OBJECT).description("업로드 대상"),
                                fieldWithPath("data.uploadUrl").type(JsonFieldType.STRING)
                                        .description("이 URL로 사진을 직접 PUT한다. 요청 시 `Content-Type` 헤더를 발급 때와 동일하게 보내야 한다"),
                                fieldWithPath("data.imageKey").type(JsonFieldType.STRING)
                                        .description("업로드 완료 후 추천 요청에 실어 보낼 객체 키. 비공개 버킷이라 그대로 열람할 수 없다"),
                                fieldWithPath("data.expiresInSeconds").type(JsonFieldType.NUMBER)
                                        .description("서명 URL 유효 시간(초). 만료되면 다시 발급받아야 한다")
                        )
                ));
    }

    @Test
    @DisplayName("지원하지 않는 형식이면 400을 응답한다")
    void issue_upload_url_unsupported_type() throws Exception {
        when(pickImageUploadService.issueUploadUrl(eq(memberId), anyString(), anyLong()))
                .thenThrow(new BusinessException(ErrorCode.PICK_UNSUPPORTED_IMAGE_TYPE));

        mockMvc.perform(get("/api/v1/picks/upload-url")
                        .param("contentType", "image/gif")
                        .param("contentLength", "1024"))
                .andExpect(status().isBadRequest())
                .andDo(document("picks/upload-url-unsupported-type"));
    }

    @Test
    @DisplayName("용량이 상한을 넘으면 400을 응답한다")
    void issue_upload_url_too_large() throws Exception {
        when(pickImageUploadService.issueUploadUrl(eq(memberId), anyString(), anyLong()))
                .thenThrow(new BusinessException(ErrorCode.PICK_IMAGE_TOO_LARGE));

        mockMvc.perform(get("/api/v1/picks/upload-url")
                        .param("contentType", "image/jpeg")
                        .param("contentLength", "20971520"))
                .andExpect(status().isBadRequest())
                .andDo(document("picks/upload-url-too-large"));
    }
}
