package com.moodi.route.infrastructure.kakao;

import com.moodi.route.application.LegClient;
import com.moodi.route.application.LegResult;
import com.moodi.route.domain.TravelMode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Comparator;
import java.util.Optional;

@Slf4j
@Component
public class KakaoLegClient implements LegClient {

    private static final String BASE_URL = "https://dapi.kakao.com";
    private static final String TRANSIT_PATH = "/v2/routing/publictraffic";
    private static final String WALK_PATH = "/v2/routing/walk";

    private final RestClient restClient;

    public KakaoLegClient(@Value("${moodi.kakao.rest-api-key}") String restApiKey) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(10));

        this.restClient = RestClient.builder()
                .baseUrl(BASE_URL)
                .defaultHeader("Authorization", "KakaoAK " + restApiKey)
                .requestFactory(factory)
                .build();
    }

    @Override
    public Optional<LegResult> findTransitRoute(double startLongitude, double startLatitude,
                                                 double endLongitude, double endLatitude) {
        try {
            KakaoTransitResponse response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(TRANSIT_PATH)
                            .queryParam("start_x", startLongitude)
                            .queryParam("start_y", startLatitude)
                            .queryParam("end_x", endLongitude)
                            .queryParam("end_y", endLatitude)
                            .build())
                    .retrieve()
                    .body(KakaoTransitResponse.class);

            String landingUrl = buildLandingUrl(startLatitude, startLongitude, endLatitude, endLongitude);
            return parseTransitResponse(response, landingUrl);
        } catch (Exception e) {
            log.warn("카카오 대중교통 경로 조회 실패: start=({}, {}), end=({}, {}), error={}",
                    startLongitude, startLatitude, endLongitude, endLatitude, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public Optional<LegResult> findWalkRoute(double startLongitude, double startLatitude,
                                              double endLongitude, double endLatitude) {
        try {
            KakaoWalkResponse response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(WALK_PATH)
                            .queryParam("start_x", startLongitude)
                            .queryParam("start_y", startLatitude)
                            .queryParam("end_x", endLongitude)
                            .queryParam("end_y", endLatitude)
                            .queryParam("route_mode", "SHORTEST")
                            .build())
                    .retrieve()
                    .body(KakaoWalkResponse.class);

            String landingUrl = buildLandingUrl(startLatitude, startLongitude, endLatitude, endLongitude);
            return parseWalkResponse(response, landingUrl);
        } catch (Exception e) {
            log.warn("카카오 도보 경로 조회 실패: start=({}, {}), end=({}, {}), error={}",
                    startLongitude, startLatitude, endLongitude, endLatitude, e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<LegResult> parseTransitResponse(KakaoTransitResponse response, String landingUrl) {
        if (response == null || response.routes() == null || response.routes().isEmpty()) {
            return Optional.empty();
        }

        return response.routes().stream()
                .min(Comparator.comparingInt(r -> r.properties().totalTime()))
                .map(best -> new LegResult(
                        TravelMode.PUBLIC_TRANSIT,
                        best.properties().totalTime(),
                        best.properties().totalDistance(),
                        landingUrl
                ));
    }

    private Optional<LegResult> parseWalkResponse(KakaoWalkResponse response, String landingUrl) {
        if (response == null || response.route() == null
                || response.route().legs() == null || response.route().legs().isEmpty()) {
            return Optional.empty();
        }

        KakaoWalkResponse.Properties properties = response.route().legs().getFirst().properties();
        return Optional.of(new LegResult(
                TravelMode.WALK,
                properties.time(),
                properties.distance(),
                landingUrl
        ));
    }

    private String buildLandingUrl(double startLatitude, double startLongitude,
                                    double endLatitude, double endLongitude) {
        return "https://map.kakao.com/link/from/출발," + startLatitude + "," + startLongitude
                + "/to/도착," + endLatitude + "," + endLongitude;
    }
}
