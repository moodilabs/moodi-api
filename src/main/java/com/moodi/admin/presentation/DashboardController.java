package com.moodi.admin.presentation;

import com.moodi.admin.application.DashboardQueryService;
import com.moodi.admin.presentation.dto.DashboardResponse;
import com.moodi.shared.auth.AdminRequired;
import com.moodi.shared.response.SuccessResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@AdminRequired
@RestController
public class DashboardController {

    private final DashboardQueryService dashboardQueryService;

    public DashboardController(DashboardQueryService dashboardQueryService) {
        this.dashboardQueryService = dashboardQueryService;
    }

    @GetMapping("/api/admin/dashboard")
    public SuccessResponse<DashboardResponse> getDashboard() {
        return SuccessResponse.of(DashboardResponse.from(dashboardQueryService.getSummary()));
    }
}
