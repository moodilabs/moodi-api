package com.moodi.spot.application;

import com.moodi.spot.domain.SpotContentType;
import com.moodi.spot.domain.SpotRepository;
import com.moodi.spot.domain.SpotStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Sql(statements = "DELETE FROM spot_image; DELETE FROM spot_translation; DELETE FROM spot;",
        executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class SpotDataLoaderIntegrationTest {

    @Autowired
    private SpotCsvReader spotCsvReader;

    @Autowired
    private SpotDataLoader spotDataLoader;

    @Autowired
    private SpotRepository spotRepository;

    @Test
    @DisplayName("테스트 CSV를 읽어 전체 적재 흐름을 검증한다")
    void load_from_test_csv() throws Exception {
        // given
        Reader reader = new InputStreamReader(
                getClass().getResourceAsStream("/pilot-spots-test.csv"), StandardCharsets.UTF_8);
        var readResult = spotCsvReader.read(reader);
        List<SpotCsvRow> rows = readResult.rows();

        // when
        SpotDataLoader.LoadResult result = spotDataLoader.load(rows);

        // then
        assertThat(result.saved()).isEqualTo(8);
        assertThat(result.skipped()).isZero();
        assertThat(result.failed()).isZero();
    }

    @Test
    @DisplayName("동일 CSV를 두 번 적재하면 두 번째는 모두 스킵된다")
    void load_twice_skips_all_on_second_run() throws Exception {
        // given
        Reader reader1 = new InputStreamReader(
                getClass().getResourceAsStream("/pilot-spots-test.csv"), StandardCharsets.UTF_8);
        List<SpotCsvRow> rows = spotCsvReader.read(reader1).rows();
        spotDataLoader.load(rows);

        Reader reader2 = new InputStreamReader(
                getClass().getResourceAsStream("/pilot-spots-test.csv"), StandardCharsets.UTF_8);
        List<SpotCsvRow> rowsAgain = spotCsvReader.read(reader2).rows();

        // when
        SpotDataLoader.LoadResult result = spotDataLoader.load(rowsAgain);

        // then
        assertThat(result.saved()).isZero();
        assertThat(result.skipped()).isEqualTo(8);
    }

    @Test
    @DisplayName("숙박과 음식점은 routeExcluded가 true로 저장된다")
    void load_sets_route_excluded_for_accommodation_and_restaurant() throws Exception {
        // given
        Reader reader = new InputStreamReader(
                getClass().getResourceAsStream("/pilot-spots-test.csv"), StandardCharsets.UTF_8);
        var readResult = spotCsvReader.read(reader);
        List<SpotCsvRow> rows = readResult.rows();
        spotDataLoader.load(rows);

        // when & then
        var accommodation = spotRepository.findBySourceAndContentId("kor_service", "2574118");
        assertThat(accommodation).isPresent();
        assertThat(accommodation.get().isRouteExcluded()).isTrue();
        assertThat(accommodation.get().getContentType()).isEqualTo(SpotContentType.ACCOMMODATION);

        var restaurant = spotRepository.findBySourceAndContentId("kor_service", "2871024");
        assertThat(restaurant).isPresent();
        assertThat(restaurant.get().isRouteExcluded()).isTrue();

        var touristAttraction = spotRepository.findBySourceAndContentId("kor_service", "2733967");
        assertThat(touristAttraction).isPresent();
        assertThat(touristAttraction.get().isRouteExcluded()).isFalse();
        assertThat(touristAttraction.get().getStatus()).isEqualTo(SpotStatus.TAGGING_PENDING);
    }
}
