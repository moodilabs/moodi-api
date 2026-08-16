package com.moodi.route.application;

import com.moodi.route.application.RouteSaveCommand.DayCommand;
import com.moodi.route.domain.Route;
import com.moodi.route.domain.RouteDay;
import com.moodi.route.domain.RouteLeg;
import com.moodi.route.domain.RouteRepository;
import com.moodi.route.domain.RouteSpot;
import com.moodi.route.domain.StayDurationPolicy;
import com.moodi.shared.error.BusinessException;
import com.moodi.shared.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RouteSaveService {

    private final RouteRepository routeRepository;
    private final SpotSnapshotReader spotSnapshotReader;
    private final LegCalculator legCalculator;

    @Transactional
    public Route save(RouteSaveCommand command) {
        List<Long> allSpotIds = extractAllSpotIds(command.days());
        Map<Long, SpotSnapshot> snapshotMap = loadSnapshots(allSpotIds);

        List<RouteDay> days = buildDays(command.days(), snapshotMap);

        Route route = Route.create(
                command.memberId(), command.title(),
                command.startDate(), command.endDate(), days
        );

        return routeRepository.save(route);
    }

    @Transactional
    public Route update(UUID publicId, RouteSaveCommand command) {
        Route route = routeRepository.findByPublicId(publicId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ROUTE_NOT_FOUND));

        route.validateOwner(command.memberId());

        Map<Integer, RouteDay> existingDayMap = buildExistingDayMap(route);
        List<Long> changedSpotIds = collectChangedSpotIds(command.days(), existingDayMap);
        Map<Long, SpotSnapshot> snapshotMap = loadSnapshots(changedSpotIds);

        List<RouteDay> newDays = buildDaysForUpdate(command.days(), existingDayMap, snapshotMap);

        route.update(command.title(), command.startDate(), command.endDate(), newDays);

        return route;
    }

    @Transactional
    public Route addSpotToLastDay(UUID publicId, UUID memberId, Long spotId) {
        Route route = routeRepository.findByPublicId(publicId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ROUTE_NOT_FOUND));

        route.validateOwner(memberId);

        RouteDay lastDay = route.getLastDay();
        List<RouteSpot> existingSpots = lastDay.getSpots();

        SpotSnapshot snapshot = loadSnapshots(List.of(spotId)).get(spotId);
        int newSequence = existingSpots.size() + 1;
        RouteSpot newSpot = RouteSpot.create(
                snapshot.spotId(), newSequence,
                StayDurationPolicy.getEstimatedMinutes(snapshot.contentType()),
                snapshot.title(), snapshot.imageUrl(),
                snapshot.area(), snapshot.district(),
                snapshot.latitude(), snapshot.longitude(),
                snapshot.contentType().getLabel(),
                snapshot.description()
        );

        List<RouteSpot> spots = new ArrayList<>(existingSpots);
        spots.add(newSpot);

        List<RouteLeg> legs = new ArrayList<>(lastDay.getLegs());
        if (!existingSpots.isEmpty()) {
            RouteSpot lastSpot = existingSpots.get(existingSpots.size() - 1);
            Optional<LegResult> result = legCalculator.calculate(
                    lastSpot.getSpotLongitude(), lastSpot.getSpotLatitude(),
                    snapshot.longitude(), snapshot.latitude()
            );
            LegResult leg = result.orElse(LegResult.unavailable());
            legs.add(RouteLeg.create(
                    lastSpot.getSequence(), newSequence,
                    leg.travelMode(), leg.durationSeconds(),
                    leg.distanceMeters(), leg.landingUrl()
            ));
        }

        RouteDay newLastDay = RouteDay.create(lastDay.getDayNumber(), lastDay.getDate(), spots, legs);
        route.replaceLastDay(newLastDay);

        return route;
    }

    private Map<Integer, RouteDay> buildExistingDayMap(Route route) {
        Map<Integer, RouteDay> map = new LinkedHashMap<>();
        for (RouteDay day : route.getDays()) {
            map.put(day.getDayNumber(), day);
        }
        return map;
    }

    private List<Long> collectChangedSpotIds(List<DayCommand> dayCommands,
                                              Map<Integer, RouteDay> existingDayMap) {
        List<Long> changedSpotIds = new ArrayList<>();
        for (DayCommand dayCommand : dayCommands) {
            if (isDayChanged(dayCommand, existingDayMap.get(dayCommand.dayNumber()))) {
                changedSpotIds.addAll(dayCommand.spotIds());
            }
        }
        return changedSpotIds.stream().distinct().toList();
    }

    private boolean isDayChanged(DayCommand dayCommand, RouteDay existingDay) {
        if (existingDay == null) {
            return true;
        }
        List<Long> existingSpotIds = existingDay.getSpots().stream()
                .map(RouteSpot::getSpotId)
                .toList();
        return !existingSpotIds.equals(dayCommand.spotIds());
    }

    private List<RouteDay> buildDaysForUpdate(List<DayCommand> dayCommands,
                                               Map<Integer, RouteDay> existingDayMap,
                                               Map<Long, SpotSnapshot> snapshotMap) {
        List<RouteDay> days = new ArrayList<>();
        for (DayCommand dayCommand : dayCommands) {
            RouteDay existingDay = existingDayMap.get(dayCommand.dayNumber());

            if (!isDayChanged(dayCommand, existingDay)) {
                days.add(existingDay);
            } else {
                List<RouteSpot> spots = buildSpots(dayCommand.spotIds(), snapshotMap);
                List<RouteLeg> legs = calculateLegs(dayCommand.spotIds(), snapshotMap);
                days.add(RouteDay.create(dayCommand.dayNumber(), dayCommand.date(), spots, legs));
            }
        }
        return days;
    }

    private List<Long> extractAllSpotIds(List<DayCommand> days) {
        return days.stream()
                .flatMap(day -> day.spotIds().stream())
                .distinct()
                .toList();
    }

    private Map<Long, SpotSnapshot> loadSnapshots(List<Long> spotIds) {
        if (spotIds.isEmpty()) {
            return Map.of();
        }

        List<SpotSnapshot> snapshots = spotSnapshotReader.readBySpotIds(spotIds);
        if (snapshots.size() != spotIds.size()) {
            throw new BusinessException(ErrorCode.SPOT_NOT_FOUND);
        }

        return snapshots.stream()
                .collect(Collectors.toMap(SpotSnapshot::spotId, Function.identity()));
    }

    private List<RouteDay> buildDays(List<DayCommand> dayCommands,
                                      Map<Long, SpotSnapshot> snapshotMap) {
        List<RouteDay> days = new ArrayList<>();
        for (DayCommand dayCommand : dayCommands) {
            List<RouteSpot> spots = buildSpots(dayCommand.spotIds(), snapshotMap);
            List<RouteLeg> legs = calculateLegs(dayCommand.spotIds(), snapshotMap);
            days.add(RouteDay.create(dayCommand.dayNumber(), dayCommand.date(), spots, legs));
        }
        return days;
    }

    private List<RouteSpot> buildSpots(List<Long> spotIds,
                                        Map<Long, SpotSnapshot> snapshotMap) {
        List<RouteSpot> spots = new ArrayList<>();
        for (int i = 0; i < spotIds.size(); i++) {
            SpotSnapshot snapshot = snapshotMap.get(spotIds.get(i));
            spots.add(RouteSpot.create(
                    snapshot.spotId(), i + 1,
                    StayDurationPolicy.getEstimatedMinutes(snapshot.contentType()),
                    snapshot.title(), snapshot.imageUrl(),
                    snapshot.area(), snapshot.district(),
                    snapshot.latitude(), snapshot.longitude(),
                    snapshot.contentType().getLabel(),
                    snapshot.description()
            ));
        }
        return spots;
    }

    private List<RouteLeg> calculateLegs(List<Long> spotIds,
                                          Map<Long, SpotSnapshot> snapshotMap) {
        List<RouteLeg> legs = new ArrayList<>();
        for (int i = 0; i < spotIds.size() - 1; i++) {
            SpotSnapshot from = snapshotMap.get(spotIds.get(i));
            SpotSnapshot to = snapshotMap.get(spotIds.get(i + 1));

            Optional<LegResult> result = legCalculator.calculate(
                    from.longitude(), from.latitude(),
                    to.longitude(), to.latitude()
            );

            LegResult leg = result.orElse(LegResult.unavailable());
            legs.add(RouteLeg.create(
                    i + 1, i + 2,
                    leg.travelMode(), leg.durationSeconds(),
                    leg.distanceMeters(), leg.landingUrl()
            ));
        }
        return legs;
    }
}
