package com.moodi.discovery.presentation.admin;

import com.moodi.discovery.application.AreaSuggestService;
import com.moodi.discovery.application.RecommendedAreaService;
import com.moodi.discovery.presentation.dto.AreaSuggestResponse;
import com.moodi.discovery.presentation.dto.RecommendedAreaRequest;
import com.moodi.discovery.presentation.dto.RecommendedAreaResponse;
import com.moodi.shared.auth.AdminRequired;
import com.moodi.shared.response.SuccessResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@AdminRequired
@RestController
@RequestMapping("/api/admin/recommended-areas")
public class AdminRecommendedAreaController {

    /** 앱 자동완성(`/api/v1/picks/areas`)과 같은 한도 — 운영자도 같은 후보 목록에서 고른다. */
    private static final int AREA_SUGGEST_LIMIT = 20;

    private final RecommendedAreaService service;
    private final AreaSuggestService areaSuggestService;

    public AdminRecommendedAreaController(RecommendedAreaService service, AreaSuggestService areaSuggestService) {
        this.service = service;
        this.areaSuggestService = areaSuggestService;
    }

    public record IdResponse(Long id) {}

    public record OrderRequest(@NotNull List<Long> ids) {}

    @GetMapping
    public SuccessResponse<List<RecommendedAreaResponse>> list() {
        List<RecommendedAreaResponse> responses = service.getAll().stream()
                .map(RecommendedAreaResponse::from)
                .toList();
        return SuccessResponse.of(responses);
    }

    /**
     * 추천 지역 등록 폼의 자동완성. 관리자 웹은 `/api/admin/**`만 호출하므로
     * 앱용 `/api/v1/picks/areas`(회원 토큰 필요)를 그대로 쓸 수 없어 같은 후보를 여기서 내보낸다.
     */
    @GetMapping("/suggest")
    public SuccessResponse<List<AreaSuggestResponse>> suggest(@RequestParam(required = false) String keyword) {
        List<AreaSuggestResponse> responses = areaSuggestService.search(keyword, AREA_SUGGEST_LIMIT).stream()
                .map(AreaSuggestResponse::from)
                .toList();
        return SuccessResponse.of(responses);
    }

    @GetMapping("/{id}")
    public SuccessResponse<RecommendedAreaResponse> get(@PathVariable Long id) {
        return SuccessResponse.of(RecommendedAreaResponse.from(service.get(id)));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SuccessResponse<IdResponse> create(@RequestBody RecommendedAreaRequest request) {
        return SuccessResponse.of(new IdResponse(service.create(request.toCommand())));
    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void update(@PathVariable Long id, @RequestBody RecommendedAreaRequest request) {
        service.update(id, request.toCommand());
    }

    @PutMapping("/order")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reorder(@Valid @RequestBody OrderRequest request) {
        service.reorder(request.ids());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
