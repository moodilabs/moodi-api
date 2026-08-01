package com.moodi.route.infrastructure.kakao;

import com.moodi.route.application.LegClient;
import com.moodi.route.application.LegResult;
import com.moodi.route.domain.TravelMode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
public class KakaoLegClient implements LegClient {

    private static final String BASE_URL = "https://dapi.kakao.com";
    private static final String TRANSIT_PATH = "/v2/routing/publictraffic";
    private static final String WALK_PATH = "/v2/routing/walk";
    private static final ObjectMapper MAPPER = new ObjectMapper();

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
            String response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(TRANSIT_PATH)
                            .queryParam("start_x", startLongitude)
                            .queryParam("start_y", startLatitude)
                            .queryParam("end_x", endLongitude)
                            .queryParam("end_y", endLatitude)
                            .build())
                    .retrieve()
                    .body(String.class);

            return parseTransitResponse(response);
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
            String response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(WALK_PATH)
                            .queryParam("start_x", startLongitude)
                            .queryParam("start_y", startLatitude)
                            .queryParam("end_x", endLongitude)
                            .queryParam("end_y", endLatitude)
                            .queryParam("route_mode", "SHORTEST")
                            .build())
                    .retrieve()
                    .body(String.class);

            return parseWalkResponse(response);
        } catch (Exception e) {
            log.warn("카카오 도보 경로 조회 실패: start=({}, {}), end=({}, {}), error={}",
                    startLongitude, startLatitude, endLongitude, endLatitude, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * 대중교통 응답 구조:
     * { "routes": [ { "properties": { "totalDistance": m, "totalTime": sec, "transfers": n, "fare": {...} }, "steps": [...] } ] }
     * routes가 여러 개 → totalTime 가장 짧은 경로 선택
     */
    @SuppressWarnings("unchecked")
    private Optional<LegResult> parseTransitResponse(String responseJson) {
        try {
            Map<String, Object> response = MAPPER.readValue(responseJson, new TypeReference<>() {});
            List<Map<String, Object>> routes = (List<Map<String, Object>>) response.get("routes");

            if (routes == null || routes.isEmpty()) {
                return Optional.empty();
            }

            Map<String, Object> bestRoute = routes.stream()
                    .min((a, b) -> {
                        int timeA = extractTransitTotalTime(a);
                        int timeB = extractTransitTotalTime(b);
                        return Integer.compare(timeA, timeB);
                    })
                    .orElse(null);

            if (bestRoute == null) {
                return Optional.empty();
            }

            Map<String, Object> properties = (Map<String, Object>) bestRoute.get("properties");
            int totalTime = ((Number) properties.get("totalTime")).intValue();
            int totalDistance = ((Number) properties.get("totalDistance")).intValue();

            return Optional.of(new LegResult(
                    TravelMode.PUBLIC_TRANSIT,
                    totalTime,
                    totalDistance,
                    null
            ));
        } catch (Exception e) {
            log.warn("카카오 대중교통 응답 파싱 실패: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * 도보 응답 구조:
     * { "route": { "legs": [ { "properties": { "distance": m, "time": sec }, "steps": [...] } ] } }
     */
    @SuppressWarnings("unchecked")
    private Optional<LegResult> parseWalkResponse(String responseJson) {
        try {
            Map<String, Object> response = MAPPER.readValue(responseJson, new TypeReference<>() {});
            Map<String, Object> route = (Map<String, Object>) response.get("route");

            if (route == null) {
                return Optional.empty();
            }

            List<Map<String, Object>> legs = (List<Map<String, Object>>) route.get("legs");
            if (legs == null || legs.isEmpty()) {
                return Optional.empty();
            }

            Map<String, Object> properties = (Map<String, Object>) legs.getFirst().get("properties");
            int totalTime = ((Number) properties.get("time")).intValue();
            int totalDistance = ((Number) properties.get("distance")).intValue();

            return Optional.of(new LegResult(
                    TravelMode.WALK,
                    totalTime,
                    totalDistance,
                    null
            ));
        } catch (Exception e) {
            log.warn("카카오 도보 응답 파싱 실패: {}", e.getMessage());
            return Optional.empty();
        }
    }

    @SuppressWarnings("unchecked")
    private int extractTransitTotalTime(Map<String, Object> route) {
        Map<String, Object> properties = (Map<String, Object>) route.get("properties");
        return ((Number) properties.get("totalTime")).intValue();
    }
}
