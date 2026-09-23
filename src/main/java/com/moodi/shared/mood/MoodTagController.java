package com.moodi.shared.mood;

import com.moodi.shared.response.SuccessResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RestController
public class MoodTagController {

    @GetMapping("/api/v1/moods")
    public SuccessResponse<List<MoodTagResponse>> list() {
        List<MoodTagResponse> moods = Arrays.stream(MoodTag.values())
                .map(MoodTagResponse::from)
                .toList();
        return SuccessResponse.of(moods);
    }
}
