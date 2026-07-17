package com.moodi.spot.infrastructure.persistence;

import com.moodi.shared.support.RepositoryTestSupport;
import com.moodi.spot.domain.Spot;
import com.moodi.spot.domain.SpotRepository;
import com.moodi.spot.domain.SpotTranslation;
import com.moodi.spot.domain.SpotTranslationRepository;
import com.moodi.spot.support.SpotFixture;
import com.moodi.spot.support.SpotTranslationFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class SpotTranslationJpaRepositoryTest extends RepositoryTestSupport {

    @Autowired
    private SpotRepository spotRepository;

    @Autowired
    private SpotTranslationRepository spotTranslationRepository;

    @Test
    @DisplayName("SpotTranslation을 저장한다")
    void save_spot_translation() {
        // given
        Spot spot = spotRepository.save(SpotFixture.create());
        SpotTranslation translation = SpotTranslationFixture.create(spot.getId());

        // when
        SpotTranslation saved = spotTranslationRepository.save(translation);

        // then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getSpotId()).isEqualTo(spot.getId());
        assertThat(saved.getLocale()).isEqualTo("ko-KR");
    }
}
