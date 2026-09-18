package com.moodi.route.application;

import com.moodi.route.domain.Route;
import com.moodi.route.domain.RouteDay;
import com.moodi.route.domain.RouteSpot;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 루트 공유 랜딩 페이지({@code GET /routes/shared/{publicId}}, {@code GET /s/{code}})에 그릴 값.
 *
 * <p>존재하지 않거나·비공개·삭제된 루트는 앱이 자체적으로 [RTE-04-03] 화면을 보여주므로
 * 랜딩 페이지는 여기서 에러를 내지 않고 기본 제목으로 조용히 폴백한다 — 카카오톡 크롤러가
 * 이 URL을 스크랩할 때도 항상 200 + 미리보기 카드가 나와야 한다.
 *
 * @param area     스팟이 가장 많이 속한 시/도. 스팟이 없으면 null
 * @param imageUrl 순서상 첫 번째로 대표 이미지가 있는 스팟의 이미지. 없으면 null(컨트롤러가 로고로 대체)
 */
public record RouteShareLandingView(
        boolean found,
        UUID publicId,
        String shortCode,
        String title,
        LocalDate startDate,
        LocalDate endDate,
        int totalDays,
        int spotCount,
        String area,
        String imageUrl,
        List<DayView> days
) {

    public static final String DEFAULT_TITLE = "Moodi";

    public record DayView(int dayNumber, LocalDate date, List<SpotView> spots) {
    }

    public record SpotView(int sequence, String title, String imageUrl, String area, String district) {
    }

    public static RouteShareLandingView from(Route route) {
        List<DayView> days = route.getDays().stream()
                .map(RouteShareLandingView::toDayView)
                .toList();
        List<RouteSpot> spots = route.getDays().stream()
                .flatMap(day -> day.getSpots().stream())
                .toList();

        return new RouteShareLandingView(
                true,
                route.getPublicId(),
                route.getShortCode(),
                route.getTitle(),
                route.getStartDate(),
                route.getEndDate(),
                route.getTotalDays(),
                spots.size(),
                dominantArea(spots),
                firstImageUrl(spots),
                days
        );
    }

    public static RouteShareLandingView notFound() {
        return new RouteShareLandingView(false, null, null, DEFAULT_TITLE, null, null, 0, 0, null, null, List.of());
    }

    private static DayView toDayView(RouteDay day) {
        List<SpotView> spots = day.getSpots().stream()
                .map(spot -> new SpotView(spot.getSequence(), spot.getSpotTitle(), spot.getSpotImageUrl(),
                        spot.getSpotArea(), spot.getSpotDistrict()))
                .toList();
        return new DayView(day.getDayNumber(), day.getDate(), spots);
    }

    /** 가장 많이 등장한 시/도. 동률이면 먼저 나온 쪽 — LinkedHashMap 으로 삽입 순서를 지킨다. */
    private static String dominantArea(List<RouteSpot> spots) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (RouteSpot spot : spots) {
            String area = spot.getSpotArea();
            if (area == null || area.isBlank()) {
                continue;
            }
            counts.merge(area, 1, Integer::sum);
        }
        String best = null;
        int bestCount = 0;
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            if (entry.getValue() > bestCount) {
                best = entry.getKey();
                bestCount = entry.getValue();
            }
        }
        return best;
    }

    private static String firstImageUrl(List<RouteSpot> spots) {
        return spots.stream()
                .map(RouteSpot::getSpotImageUrl)
                .filter(url -> url != null && !url.isBlank())
                .findFirst()
                .orElse(null);
    }
}
