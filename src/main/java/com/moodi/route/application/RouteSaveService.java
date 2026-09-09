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

        return initializeDays(routeRepository.save(route));
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

        return initializeDays(route);
    }

    @Transactional
    public Route addSpotToLastDay(UUID publicId, UUID memberId, Long spotId) {
        Route route = routeRepository.findByPublicId(publicId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ROUTE_NOT_FOUND));

        route.validateOwner(memberId);

        RouteDay lastDay = route.getLastDay();
        Optional<RouteSpot> lastSpot = lastDay.getLastSpot();

        SpotSnapshot snapshot = loadSnapshots(List.of(spotId)).get(spotId);
        int newSequence = lastDay.getNextSequence();
        RouteSpot newSpot = RouteSpot.create(
                snapshot.spotId(), newSequence,
                StayDurationPolicy.getEstimatedMinutes(snapshot.contentType()),
                snapshot.title(), snapshot.imageUrl(),
                snapshot.area(), snapshot.district(),
                snapshot.latitude(), snapshot.longitude(),
                snapshot.contentType().getLabel(),
                snapshot.description()
        );

        // 영속 상태의 마지막 Day에 그대로 덧붙인다. 새 Day로 갈아끼우면 orphan 삭제보다 삽입이 먼저 실행되어
        // uk_route_day_route_day_number 위반으로 실패한다.
        lastDay.addSpot(newSpot);

        if (lastSpot.isPresent()) {
            RouteSpot from = lastSpot.get();
            LegResult leg = calculateLeg(
                    from.getSpotLongitude(), from.getSpotLatitude(),
                    snapshot.longitude(), snapshot.latitude()
            );
            lastDay.addLeg(RouteLeg.create(
                    from.getSequence(), newSequence,
                    leg.travelMode(), leg.durationSeconds(),
                    leg.distanceMeters(), leg.landingUrl()
            ));
        }

        return initializeDays(route);
    }

    /**
     * 컨트롤러가 응답을 만들 때는 트랜잭션이 끝나 있어(open-in-view=false) 지연 로딩이 불가능하다.
     * 트랜잭션 안에서 days → spots/legs 컬렉션을 모두 초기화한 뒤 돌려준다.
     */
    private Route initializeDays(Route route) {
        for (RouteDay day : route.getDays()) {
            day.getSpots().size();
            day.getLegs().size();
        }
        return route;
    }

    /**
     * 좌표가 없는 스팟(원장에 위경도가 비어 있는 경우)은 이동정보를 계산하지 않고 '정보 없음'으로 둔다.
     * LegCalculator는 primitive double을 받으므로 null을 그대로 넘기면 언박싱 NPE가 난다.
     */
    private LegResult calculateLeg(Double fromLongitude, Double fromLatitude,
                                   Double toLongitude, Double toLatitude) {
        if (fromLongitude == null || fromLatitude == null
                || toLongitude == null || toLatitude == null) {
            return LegResult.unavailable();
        }
        return legCalculator.calculate(fromLongitude, fromLatitude, toLongitude, toLatitude)
                .orElse(LegResult.unavailable());
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

            LegResult leg = calculateLeg(
                    from.longitude(), from.latitude(),
                    to.longitude(), to.latitude()
            );
            legs.add(RouteLeg.create(
                    i + 1, i + 2,
                    leg.travelMode(), leg.durationSeconds(),
                    leg.distanceMeters(), leg.landingUrl()
            ));
        }
        return legs;
    }
}
