package com.moodi.member.presentation;

import com.moodi.member.application.SurveyImageService;
import com.moodi.member.presentation.dto.SurveyImageResponse;
import com.moodi.shared.response.SuccessResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class SurveyImageController {

    private final SurveyImageService service;

    public SurveyImageController(SurveyImageService service) {
        this.service = service;
    }

    @GetMapping("/api/v1/members/survey-images")
    public SuccessResponse<List<SurveyImageResponse>> list() {
        List<SurveyImageResponse> responses = service.getPublished().stream()
                .map(SurveyImageResponse::from)
                .toList();
        return SuccessResponse.of(responses);
    }
}
