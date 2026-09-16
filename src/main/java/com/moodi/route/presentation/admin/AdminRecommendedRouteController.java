package com.moodi.route.presentation.admin;

import com.moodi.route.application.RecommendedRouteService;
import com.moodi.shared.auth.AdminRequired;
import com.moodi.shared.response.SuccessResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@AdminRequired
@RestController
@RequestMapping("/api/admin/recommended-routes")
public class AdminRecommendedRouteController {

    private final RecommendedRouteService service;

    public AdminRecommendedRouteController(RecommendedRouteService service) {
        this.service = service;
    }

    public record IdResponse(Long id) {}

    public record OrderRequest(@NotNull List<Long> ids) {}
    @GetMapping
    public SuccessResponse<List<RecommendedRouteService.View>> list() {
        return SuccessResponse.of(service.getAll());
    }
    @GetMapping("/{id}")
    public SuccessResponse<RecommendedRouteService.View> get(@PathVariable Long id) {
        return SuccessResponse.of(service.get(id));
    }
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SuccessResponse<IdResponse> create(@RequestBody RecommendedRouteService.Command request) {
        return SuccessResponse.of(new IdResponse(service.create(request)));
    }
    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void update(@PathVariable Long id, @RequestBody RecommendedRouteService.Command request) {
        service.update(id, request);
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

    public record VisibilityRequest(@NotNull Boolean visible) {}
    @PatchMapping("/{id}/visibility")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void visibility(@PathVariable Long id, @Valid @RequestBody VisibilityRequest request) {
        service.changeVisibility(id, request.visible());
    }
}
