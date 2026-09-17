package com.moodi.route.application;

import com.moodi.route.domain.TravelMode;
import com.moodi.shared.error.BusinessException;
import com.moodi.shared.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class LegCalculateService {

    private final SpotSnapshotReader spotSnapshotReader;
    private final LegCalculator legCalculator;

    public List<LegCalculateResult> calculate(List<SpotPairCommand> pairs) {
        Map<Long, SpotSnapshot> snapshotMap = loadSnapshots(pairs);

        List<LegCalculateResult> results = new ArrayList<>();
        for (SpotPairCommand pair : pairs) {
            SpotSnapshot from = snapshotMap.get(pair.fromSpotId());
            SpotSnapshot to = snapshotMap.get(pair.toSpotId());

            LegResult leg = calculateLeg(
                    from.longitude(), from.latitude(),
                    to.longitude(), to.latitude()
            );

            results.add(new LegCalculateResult(
                    pair.fromSpotId(), pair.toSpotId(),
                    leg.travelMode() != TravelMode.UNAVAILABLE ? leg.travelMode().name() : null,
                    leg.durationSeconds(), leg.distanceMeters(), leg.landingUrl()
            ));
        }
        return results;
    }

    private LegResult calculateLeg(Double fromLongitude, Double fromLatitude,
                                   Double toLongitude, Double toLatitude) {
        if (fromLongitude == null || fromLatitude == null
                || toLongitude == null || toLatitude == null) {
            return LegResult.unavailable();
        }
        return legCalculator.calculate(fromLongitude, fromLatitude, toLongitude, toLatitude)
                .orElse(LegResult.unavailable());
    }

    private Map<Long, SpotSnapshot> loadSnapshots(List<SpotPairCommand> pairs) {
        Set<Long> spotIds = pairs.stream()
                .flatMap(p -> Stream.of(p.fromSpotId(), p.toSpotId()))
                .collect(Collectors.toSet());

        List<SpotSnapshot> snapshots = spotSnapshotReader.readBySpotIds(new ArrayList<>(spotIds));
        if (snapshots.size() != spotIds.size()) {
            throw new BusinessException(ErrorCode.SPOT_NOT_FOUND);
        }

        return snapshots.stream()
                .collect(Collectors.toMap(SpotSnapshot::spotId, Function.identity()));
    }

    public record SpotPairCommand(Long fromSpotId, Long toSpotId) {}

    public record LegCalculateResult(
            Long fromSpotId,
            Long toSpotId,
            String travelMode,
            int durationSeconds,
            int distanceMeters,
            String landingUrl
    ) {}
}
