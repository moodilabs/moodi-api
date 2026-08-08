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
    private final SpotDistributor spotDistributor;
    private final LegCalculator legCalculator;
    private final DayScheduleValidator dayScheduleValidator;
    private final RouteTitleGenerator titleGenerator;

    public RouteGenerateResult generate(RouteGenerateCommand command) {
        validateCommand(command);

        List<SpotSnapshot> snapshots = loadAndValidateSpots(command.spotIds());

        int totalDays = calculateTotalDays(command.startDate(), command.endDate());

        List<List<SpotSnapshot>> distribution = spotDistributor.distribute(snapshots, totalDays);

        List<List<LegResult>> legsByDay = calculateLegsForAllDays(distribution);

        List<List<SpotSnapshot>> finalDistribution =
                dayScheduleValidator.validateAndRebalance(distribution, legsByDay);
        if (finalDistribution.isEmpty()) {
            throw new BusinessException(ErrorCode.ROUTE_GENERATION_FAILED);
        }

        if (finalDistribution != distribution) {
            legsByDay = calculateLegsForAllDays(finalDistribution);
        }

        String title = titleGenerator.generate(command.areas(), totalDays);

        return buildResult(title, command.startDate(), command.endDate(),
                finalDistribution, legsByDay);
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
