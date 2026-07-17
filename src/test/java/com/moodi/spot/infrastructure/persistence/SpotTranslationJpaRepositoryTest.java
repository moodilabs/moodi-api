package com.moodi.spot.infrastructure.persistence;

import com.moodi.shared.support.RepositoryTestSupport;
import com.moodi.spot.domain.Spot;
import com.moodi.spot.domain.SpotContentType;
import com.moodi.spot.domain.SpotRepository;
import com.moodi.spot.domain.SpotTranslation;
import com.moodi.spot.domain.SpotTranslationRepository;
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
        Spot spot = spotRepository.save(
                Spot.create("123", SpotContentType.TOURIST_ATTRACTION, "서울", "kor_service",
                        126.98, 37.58, null, null, null, null));

        SpotTranslation translation = SpotTranslation.create(
                spot.getId(), "ko-KR", "가회동성당", "가회동성당은 종로구에 위치한 성당이다.",
                "서울특별시 종로구 북촌로 57", null);

        // when
        SpotTranslation saved = spotTranslationRepository.save(translation);

        // then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getSpotId()).isEqualTo(spot.getId());
        assertThat(saved.getLocale()).isEqualTo("ko-KR");
    }
}
