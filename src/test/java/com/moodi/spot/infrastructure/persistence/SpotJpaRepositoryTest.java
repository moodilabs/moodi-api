package com.moodi.spot.infrastructure.persistence;

import com.moodi.shared.support.RepositoryTestSupport;
import com.moodi.spot.domain.Spot;
import com.moodi.spot.domain.SpotContentType;
import com.moodi.spot.domain.SpotRepository;
import com.moodi.spot.domain.SpotStatus;
import com.moodi.spot.support.SpotFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class SpotJpaRepositoryTest extends RepositoryTestSupport {

    @Autowired
    private SpotRepository spotRepository;

    @Test
    @DisplayName("Spot을 저장하고 source와 contentId로 조회한다")
    void save_and_find_by_source_and_content_id() {
        // given
        Spot spot = SpotFixture.create();

        // when
        Spot saved = spotRepository.save(spot);

        // then
        Optional<Spot> found = spotRepository.findBySourceAndContentId("kor_service", "2733967");
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(saved.getId());
        assertThat(found.get().getStatus()).isEqualTo(SpotStatus.TAGGING_PENDING);
    }

    @Test
    @DisplayName("존재하지 않는 source와 contentId로 조회하면 빈 Optional을 반환한다")
    void find_by_source_and_content_id_returns_empty_when_not_found() {
        Optional<Spot> found = spotRepository.findBySourceAndContentId("kor_service", "nonexistent");

        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("source와 contentId 존재 여부를 확인한다")
    void exists_by_source_and_content_id() {
        // given
        Spot spot = SpotFixture.create(SpotContentType.SHOPPING);
        spotRepository.save(spot);

        // when & then
        assertThat(spotRepository.existsBySourceAndContentId("kor_service", "2733967")).isTrue();
        assertThat(spotRepository.existsBySourceAndContentId("kor_service", "9999999")).isFalse();
    }
}
