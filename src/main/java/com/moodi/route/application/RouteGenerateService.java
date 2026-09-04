package com.moodi.route.application;

import com.moodi.route.application.RouteGenerateResult.DayResult;
import com.moodi.route.application.RouteGenerateResult.LegResultItem;
import com.moodi.route.application.RouteGenerateResult.SpotResult;
import com.moodi.route.domain.StayDurationPolicy;
import com.moodi.route.domain.TravelMode;
import com.moodi.shared.error.BusinessException;
import com.moodi.shared.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RouteGenerateService {

    private static final int MAX_SPOTS_PER_DAY = 6;

    private final Clock clock;
    private final SpotSnapshotReader spotSnapshotReader;
    private final SpotRecommendationReader spotRecommendationReader;
    private final SpotDistributor spotDistributor;
    private final LegCalculator legCalculator;
    private final DayScheduleValidator dayScheduleValidator;
    private final RouteTitleGenerator titleGenerator;

    private static final int MAX_DAILY_MINUTES = 480;
    private static final int TRAVEL_BUFFER_MINUTES = 30;

    public RouteGenerateResult generate(RouteGenerateCommand command) {
        validateCommand(command);

        List<SpotSnapshot> baseSnapshots = loadAndValidateSpots(command.spotIds());

        int totalDays = calculateTotalDays(command.startDate(), command.endDate());
        int maxSpots = totalDays * MAX_SPOTS_PER_DAY;
        int neededCount = maxSpots - baseSnapshots.size();

        List<SpotSnapshot> recommended = List.of();
        if (neededCount > 0 && command.areas() != null && !command.areas().isEmpty()) {
            recommended = spotRecommendationReader.recommend(
                    command.spotIds(), command.areas(), neededCount)
                    .stream().map(SpotSnapshot::asOptional).toList();
        }

        // Stage 1: 임시 분배 후 체류시간+버퍼로 사전 트리밍
        List<SpotSnapshot> allSnapshots = new ArrayList<>(baseSnapshots);
        allSnapshots.addAll(recommended);
        List<List<SpotSnapshot>> distribution = spotDistributor.distribute(allSnapshots, totalDays);
        distribution = trimByEstimatedBudget(distribution);

        // Stage 2: Leg 계산 후 실제 초과 Day만 OPTIONAL 제거 + 해당 Day Leg 재계산
        List<List<LegResult>> legsByDay = calculateLegsForAllDays(distribution);
        trimByActualSchedule(distribution, legsByDay);

        String title = titleGenerator.generate(command.areas(), totalDays);
        return buildResult(title, command.startDate(), command.endDate(), distribution, legsByDay);
    }

    /**
     * Stage 1: Kakao API 호출 전 사전 트리밍.
     * 날짜별로 체류시간 + (스팟수-1) × 이동버퍼가 480분을 넘으면
     * OPTIONAL 스팟을 뒤에서부터 제거한다.
     */
    private List<List<SpotSnapshot>> trimByEstimatedBudget(List<List<SpotSnapshot>> distribution) {
        List<List<SpotSnapshot>> result = new ArrayList<>();
        for (List<SpotSnapshot> daySpots : distribution) {
            List<SpotSnapshot> trimmed = new ArrayList<>(daySpots);
            while (trimmed.size() > 1 && estimatedMinutes(trimmed) > MAX_DAILY_MINUTES) {
                int lastOptional = findLastOptionalIndex(trimmed);
                if (lastOptional == -1) {
                    break;
                }
                trimmed.remove(lastOptional);
            }
            result.add(trimmed);
        }
        return result;
    }

    private int estimatedMinutes(List<SpotSnapshot> spots) {
        int stay = spots.stream()
                .mapToInt(s -> StayDurationPolicy.getEstimatedMinutes(s.contentType()))
                .sum();
        int travelBuffer = Math.max(0, spots.size() - 1) * TRAVEL_BUFFER_MINUTES;
        return stay + travelBuffer;
    }

    /**
     * Stage 2: 실제 Leg 이동시간 반영 후 초과 Day에서 OPTIONAL 스팟 제거.
     * 제거된 Day의 순서(Nearest Neighbor)와 Leg만 재계산한다.
     */
    private void trimByActualSchedule(List<List<SpotSnapshot>> distribution,
                                       List<List<LegResult>> legsByDay) {
        for (int day = 0; day < distribution.size(); day++) {
            List<SpotSnapshot> daySpots = distribution.get(day);
            List<LegResult> dayLegs = day < legsByDay.size() ? legsByDay.get(day) : List.of();

            while (daySpots.size() > 1
                    && dayScheduleValidator.calculateDayMinutes(daySpots, dayLegs) > MAX_DAILY_MINUTES) {
                int lastOptional = findLastOptionalIndex(daySpots);
                if (lastOptional == -1) {
                    break;
                }
                daySpots.remove(lastOptional);
                List<SpotSnapshot> reordered = spotDistributor.orderByNearestNeighbor(daySpots);
                daySpots.clear();
                daySpots.addAll(reordered);
                dayLegs = calculateLegsForDay(daySpots);
                legsByDay.set(day, dayLegs);
            }
        }
    }

    private int findLastOptionalIndex(List<SpotSnapshot> spots) {
        for (int i = spots.size() - 1; i >= 0; i--) {
            if (!spots.get(i).required()) {
                return i;
            }
        }
        return -1;
    }

    private void validateCommand(RouteGenerateCommand command) {
        int totalDays = calculateTotalDays(command.startDate(), command.endDate());
        if (totalDays < 1 || totalDays > 5) {
            throw new BusinessException(ErrorCode.ROUTE_INVALID_DATE_RANGE);
        }
        if (command.startDate().isBefore(LocalDate.now(clock))) {
            throw new BusinessException(ErrorCode.ROUTE_PAST_START_DATE);
        }
        if (new HashSet<>(command.spotIds()).size() != command.spotIds().size()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        if (command.spotIds().size() > totalDays * MAX_SPOTS_PER_DAY) {
            throw new BusinessException(ErrorCode.ROUTE_TOO_MANY_SPOTS_FOR_DAYS);
        }
    }

    private List<SpotSnapshot> loadAndValidateSpots(List<Long> spotIds) {
        List<SpotSnapshot> snapshots = spotSnapshotReader.readBySpotIds(spotIds);
        if (snapshots.size() != spotIds.size()) {
            throw new BusinessException(ErrorCode.SPOT_NOT_FOUND);
        }
        return snapshots;
    }

    private List<List<LegResult>> calculateLegsForAllDays(List<List<SpotSnapshot>> distribution) {
        List<List<LegResult>> legsByDay = new ArrayList<>();
        for (List<SpotSnapshot> daySpots : distribution) {
            legsByDay.add(calculateLegsForDay(daySpots));
        }
        return legsByDay;
    }

    private List<LegResult> calculateLegsForDay(List<SpotSnapshot> spots) {
        List<LegResult> legs = new ArrayList<>();
        for (int i = 0; i < spots.size() - 1; i++) {
            SpotSnapshot from = spots.get(i);
            SpotSnapshot to = spots.get(i + 1);
            Optional<LegResult> legResult = legCalculator.calculate(
                    from.longitude(), from.latitude(),
                    to.longitude(), to.latitude()
            );
            legs.add(legResult.orElse(LegResult.unavailable()));
        }
        return legs;
    }

    private RouteGenerateResult buildResult(String title, LocalDate startDate, LocalDate endDate,
                                             List<List<SpotSnapshot>> distribution,
                                             List<List<LegResult>> legsByDay) {
        List<DayResult> dayResults = new ArrayList<>();
        for (int dayIndex = 0; dayIndex < distribution.size(); dayIndex++) {
            List<SpotSnapshot> daySpots = distribution.get(dayIndex);
            List<LegResult> dayLegs = dayIndex < legsByDay.size() ? legsByDay.get(dayIndex) : List.of();

            List<SpotResult> spotResults = new ArrayList<>();
            for (int seq = 0; seq < daySpots.size(); seq++) {
                SpotSnapshot snap = daySpots.get(seq);
                spotResults.add(new SpotResult(
                        snap.spotId(), seq + 1,
                        StayDurationPolicy.getEstimatedMinutes(snap.contentType()),
                        snap.title(), snap.imageUrl(),
                        snap.area(), snap.district(),
                        snap.latitude(), snap.longitude(),
                        snap.contentType().getLabel(),
                        snap.description()
                ));
            }

            List<LegResultItem> legItems = new ArrayList<>();
            for (int legIndex = 0; legIndex < dayLegs.size(); legIndex++) {
                LegResult leg = dayLegs.get(legIndex);
                legItems.add(new LegResultItem(
                        legIndex + 1, legIndex + 2,
                        leg.isAvailable() ? leg.travelMode().name() : TravelMode.UNAVAILABLE.name(),
                        leg.durationSeconds(), leg.distanceMeters(),
                        leg.landingUrl()
                ));
            }

            dayResults.add(new DayResult(
                    dayIndex + 1,
                    startDate.plusDays(dayIndex),
                    spotResults, legItems
            ));
        }

        return new RouteGenerateResult(title, startDate, endDate, dayResults);
    }

    private int calculateTotalDays(LocalDate startDate, LocalDate endDate) {
        return (int) (endDate.toEpochDay() - startDate.toEpochDay()) + 1;
    }
}
