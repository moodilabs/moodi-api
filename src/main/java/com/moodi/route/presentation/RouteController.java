package com.moodi.route.presentation;

import com.moodi.route.application.RouteGenerateCommand;
import com.moodi.route.application.RouteGenerateResult;
import com.moodi.route.application.RouteGenerateService;
import com.moodi.route.presentation.dto.RouteGenerateRequest;
import com.moodi.route.presentation.dto.RouteGenerateResponse;
import com.moodi.route.presentation.dto.RouteGenerateResponse.DayPlan;
import com.moodi.route.presentation.dto.RouteGenerateResponse.LegPlan;
import com.moodi.route.presentation.dto.RouteGenerateResponse.SpotPlan;
import com.moodi.shared.auth.AuthMember;
import com.moodi.shared.auth.LoginRequired;
import com.moodi.shared.response.SuccessResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@LoginRequired
@RestController
@RequestMapping("/api/routes")
public class RouteController {

    private final RouteGenerateService routeGenerateService;

    public RouteController(RouteGenerateService routeGenerateService) {
        this.routeGenerateService = routeGenerateService;
    }

    @PostMapping("/generate")
    public SuccessResponse<RouteGenerateResponse> generate(
            @AuthMember UUID memberId,
            @Valid @RequestBody RouteGenerateRequest request) {
        RouteGenerateCommand command = new RouteGenerateCommand(
                request.spotIds(), request.areas(), request.startDate(), request.endDate());
        RouteGenerateResult result = routeGenerateService.generate(command);
        return SuccessResponse.of(toResponse(result));
    }

    private RouteGenerateResponse toResponse(RouteGenerateResult result) {
        return new RouteGenerateResponse(
                result.title(), result.startDate(), result.endDate(),
                result.days().stream()
                        .map(day -> new DayPlan(
                                day.dayNumber(), day.date(),
                                day.spots().stream()
                                        .map(s -> new SpotPlan(
                                                s.spotId(), s.sequence(), s.estimatedMinutes(),
                                                s.spotTitle(), s.spotImageUrl(),
                                                s.spotArea(), s.spotDistrict(),
                                                s.spotLatitude(), s.spotLongitude(),
                                                s.spotContentType()))
                                        .toList(),
                                day.legs().stream()
                                        .map(l -> new LegPlan(
                                                l.fromSequence(), l.toSequence(),
                                                l.travelMode(), l.durationSeconds(), l.distanceMeters()))
                                        .toList()))
                        .toList()
        );
    }
}
