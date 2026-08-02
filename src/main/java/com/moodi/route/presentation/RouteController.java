package com.moodi.route.presentation;

import com.moodi.route.application.RouteGenerateCommand;
import com.moodi.route.application.RouteGenerateResult;
import com.moodi.route.application.RouteGenerateService;
import com.moodi.route.application.RouteSaveCommand;
import com.moodi.route.application.RouteSaveCommand.DayCommand;
import com.moodi.route.application.RouteSaveService;
import com.moodi.route.domain.Route;
import com.moodi.route.presentation.dto.RouteGenerateRequest;
import com.moodi.route.presentation.dto.RouteGenerateResponse;
import com.moodi.route.presentation.dto.RouteGenerateResponse.DayPlan;
import com.moodi.route.presentation.dto.RouteGenerateResponse.LegPlan;
import com.moodi.route.presentation.dto.RouteGenerateResponse.SpotPlan;
import com.moodi.route.presentation.dto.RouteSaveRequest;
import com.moodi.route.presentation.dto.RouteSaveResponse;
import com.moodi.shared.auth.AuthMember;
import com.moodi.shared.auth.LoginRequired;
import com.moodi.shared.response.SuccessResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@LoginRequired
@RestController
@RequestMapping("/api/routes")
public class RouteController {

    private final RouteGenerateService routeGenerateService;
    private final RouteSaveService routeSaveService;

    public RouteController(RouteGenerateService routeGenerateService,
                           RouteSaveService routeSaveService) {
        this.routeGenerateService = routeGenerateService;
        this.routeSaveService = routeSaveService;
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

    @PostMapping
    public SuccessResponse<RouteSaveResponse> save(
            @AuthMember UUID memberId,
            @Valid @RequestBody RouteSaveRequest request) {
        RouteSaveCommand command = toSaveCommand(memberId, request);
        Route route = routeSaveService.save(command);
        return SuccessResponse.of(RouteSaveResponse.from(route));
    }

    @PutMapping("/{publicId}")
    public SuccessResponse<RouteSaveResponse> update(
            @AuthMember UUID memberId,
            @PathVariable UUID publicId,
            @Valid @RequestBody RouteSaveRequest request) {
        RouteSaveCommand command = toSaveCommand(memberId, request);
        Route route = routeSaveService.update(publicId, command);
        return SuccessResponse.of(RouteSaveResponse.from(route));
    }

    private RouteSaveCommand toSaveCommand(UUID memberId, RouteSaveRequest request) {
        List<DayCommand> dayCommands = request.days().stream()
                .map(d -> new DayCommand(d.dayNumber(), d.date(), d.spotIds()))
                .toList();
        return new RouteSaveCommand(
                memberId, request.title(),
                request.startDate(), request.endDate(), dayCommands
        );
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
                                                s.spotContentType(), s.spotDescription()))
                                        .toList(),
                                day.legs().stream()
                                        .map(l -> new LegPlan(
                                                l.fromSequence(), l.toSequence(),
                                                l.travelMode(), l.durationSeconds(), l.distanceMeters(),
                                                l.landingUrl()))
                                        .toList()))
                        .toList()
        );
    }
}
