package com.moodi.route.presentation;

import com.moodi.route.application.LegCalculateService;
import com.moodi.route.application.LegCalculateService.LegCalculateResult;
import com.moodi.route.application.LegCalculateService.SpotPairCommand;
import com.moodi.route.application.RouteDeleteService;
import com.moodi.route.application.RouteGenerateCommand;
import com.moodi.route.application.RouteGenerateResult;
import com.moodi.route.application.RouteGenerateService;
import com.moodi.route.application.RouteListRow;
import com.moodi.route.application.RouteQueryService;
import com.moodi.route.application.RouteSaveCommand;
import com.moodi.route.application.RouteSaveCommand.DayCommand;
import com.moodi.route.application.RouteSaveResult;
import com.moodi.route.application.RouteSaveService;
import com.moodi.route.application.RouteCopyService;
import com.moodi.route.application.RouteShareLinkBuilder;
import com.moodi.route.application.RouteShareService;
import com.moodi.route.domain.Route;
import com.moodi.route.presentation.dto.LegCalculateRequest;
import com.moodi.route.presentation.dto.LegCalculateResponse;
import com.moodi.route.presentation.dto.LegCalculateResponse.LegInfo;
import com.moodi.route.presentation.dto.RouteAddSpotRequest;
import com.moodi.route.presentation.dto.RouteCopyResponse;
import com.moodi.route.presentation.dto.RouteGenerateRequest;
import com.moodi.route.presentation.dto.RouteGenerateResponse;
import com.moodi.route.presentation.dto.RouteGenerateResponse.DayPlan;
import com.moodi.route.presentation.dto.RouteGenerateResponse.LegPlan;
import com.moodi.route.presentation.dto.RouteGenerateResponse.SpotPlan;
import com.moodi.route.application.RouteDetail;
import com.moodi.route.presentation.dto.RouteListResponse;
import com.moodi.route.presentation.dto.RouteSaveRequest;
import com.moodi.route.presentation.dto.RouteSaveResponse;
import com.moodi.route.presentation.dto.RouteShareResponse;
import com.moodi.shared.auth.AuthMember;
import com.moodi.shared.auth.LoginRequired;
import com.moodi.shared.response.CursorResponse;
import com.moodi.shared.response.SuccessResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
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
import java.util.UUID;

@LoginRequired
@RestController
@RequestMapping("/api/routes")
public class RouteController {

    private final RouteGenerateService routeGenerateService;
    private final RouteSaveService routeSaveService;
    private final RouteQueryService routeQueryService;
    private final RouteDeleteService routeDeleteService;
    private final RouteShareService routeShareService;
    private final RouteCopyService routeCopyService;
    private final LegCalculateService legCalculateService;
    private final RouteShareLinkBuilder shareLinkBuilder;

    public RouteController(RouteGenerateService routeGenerateService,
                           RouteSaveService routeSaveService,
                           RouteQueryService routeQueryService,
                           RouteDeleteService routeDeleteService,
                           RouteShareService routeShareService,
                           RouteCopyService routeCopyService,
                           LegCalculateService legCalculateService,
                           RouteShareLinkBuilder shareLinkBuilder) {
        this.routeGenerateService = routeGenerateService;
        this.routeSaveService = routeSaveService;
        this.routeQueryService = routeQueryService;
        this.routeDeleteService = routeDeleteService;
        this.routeShareService = routeShareService;
        this.routeCopyService = routeCopyService;
        this.legCalculateService = legCalculateService;
        this.shareLinkBuilder = shareLinkBuilder;
    }

    @GetMapping
    public SuccessResponse<CursorResponse<RouteListResponse>> getList(
            @AuthMember UUID memberId,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int size) {
        CursorResponse<RouteListRow> result = routeQueryService.getList(memberId, cursor, size);
        CursorResponse<RouteListResponse> response = result.map(RouteController::toListResponse);
        return SuccessResponse.of(response);
    }

    @PostMapping("/legs/calculate")
    public SuccessResponse<LegCalculateResponse> calculateLegs(
            @AuthMember UUID memberId,
            @Valid @RequestBody LegCalculateRequest request) {
        List<SpotPairCommand> commands = request.pairs().stream()
                .map(p -> new SpotPairCommand(p.fromSpotId(), p.toSpotId()))
                .toList();
        List<LegCalculateResult> results = legCalculateService.calculate(commands);
        LegCalculateResponse response = new LegCalculateResponse(
                results.stream()
                        .map(r -> new LegInfo(
                                r.fromSpotId(), r.toSpotId(),
                                r.travelMode(), r.durationSeconds(),
                                r.distanceMeters(), r.landingUrl()))
                        .toList()
        );
        return SuccessResponse.of(response);
    }

    @PostMapping("/generate")
    public SuccessResponse<RouteGenerateResponse> generate(
            @AuthMember UUID memberId,
            @Valid @RequestBody RouteGenerateRequest request) {
        RouteGenerateCommand command = new RouteGenerateCommand(
                request.spotIds(), request.toAreaConditions(), request.startDate(), request.endDate());
        RouteGenerateResult result = routeGenerateService.generate(command);
        return SuccessResponse.of(toResponse(result));
    }

    @PostMapping
    public SuccessResponse<RouteSaveResponse> save(
            @AuthMember UUID memberId,
            @Valid @RequestBody RouteSaveRequest request) {
        RouteSaveCommand command = toSaveCommand(memberId, request);
        RouteSaveResult result = routeSaveService.save(command);
        return SuccessResponse.of(RouteSaveResponse.from(result));
    }

    @PutMapping("/{publicId}")
    public SuccessResponse<RouteSaveResponse> update(
            @AuthMember UUID memberId,
            @PathVariable UUID publicId,
            @Valid @RequestBody RouteSaveRequest request) {
        RouteSaveCommand command = toSaveCommand(memberId, request);
        RouteSaveResult result = routeSaveService.update(publicId, command);
        return SuccessResponse.of(RouteSaveResponse.from(result));
    }

    @GetMapping("/{publicId}")
    public SuccessResponse<RouteDetail> getDetail(
            @AuthMember UUID memberId,
            @PathVariable UUID publicId) {
        RouteDetail detail = routeQueryService.getDetail(publicId, memberId);
        return SuccessResponse.of(detail);
    }

    @PostMapping("/{publicId}/copy")
    @ResponseStatus(HttpStatus.CREATED)
    public SuccessResponse<RouteCopyResponse> copy(
            @AuthMember UUID memberId,
            @PathVariable UUID publicId) {
        Route copied = routeCopyService.copy(publicId, memberId);
        return SuccessResponse.of(new RouteCopyResponse(copied.getPublicId()));
    }

    @PostMapping("/{publicId}/spots")
    public SuccessResponse<RouteSaveResponse> addSpot(
            @AuthMember UUID memberId,
            @PathVariable UUID publicId,
            @Valid @RequestBody RouteAddSpotRequest request) {
        RouteSaveResult result = routeSaveService.addSpotToLastDay(publicId, memberId, request.spotId());
        return SuccessResponse.of(RouteSaveResponse.from(result));
    }

    /**
     * 공유를 켜고 단축 링크를 돌려준다. 예전 앱(204 + 본문 무시)과도 호환된다 — 2xx 이기만 하면 됐다.
     */
    @PostMapping("/{publicId}/share")
    public SuccessResponse<RouteShareResponse> share(
            @AuthMember UUID memberId,
            @PathVariable UUID publicId,
            HttpServletRequest request) {
        Route route = routeShareService.share(publicId, memberId);
        String shareUrl = shareLinkBuilder.shareUrl(RequestOrigin.of(request), route.getPublicId(), route.getShortCode());
        return SuccessResponse.of(new RouteShareResponse(route.getPublicId(), route.getShortCode(), shareUrl));
    }

    @DeleteMapping("/{publicId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @AuthMember UUID memberId,
            @PathVariable UUID publicId) {
        routeDeleteService.delete(publicId, memberId);
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

    private static RouteListResponse toListResponse(RouteListRow row) {
        return new RouteListResponse(
                row.publicId(), row.title(),
                row.startDate(), row.endDate(),
                (int) (row.endDate().toEpochDay() - row.startDate().toEpochDay()) + 1,
                row.spotCount(),
                row.updatedAt()
        );
    }
}
