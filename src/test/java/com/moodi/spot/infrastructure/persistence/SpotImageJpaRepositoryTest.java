package com.moodi.spot.infrastructure.persistence;

import com.moodi.shared.support.RepositoryTestSupport;
import com.moodi.spot.domain.Spot;
import com.moodi.spot.domain.SpotContentType;
import com.moodi.spot.domain.SpotImage;
import com.moodi.spot.domain.SpotImageRepository;
import com.moodi.spot.domain.SpotRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class SpotImageJpaRepositoryTest extends RepositoryTestSupport {

    @Autowired
    private SpotRepository spotRepository;

    @Autowired
    private SpotImageRepository spotImageRepository;

    @Test
    @DisplayName("SpotImage를 저장한다")
    void save_spot_image() {
        // given
        Spot spot = spotRepository.save(
                Spot.create("123", SpotContentType.TOURIST_ATTRACTION, "서울", "kor_service",
                        126.98, 37.58, null, null, null, null));

        SpotImage image = SpotImage.createPrimary(spot.getId(), "https://example.com/image.jpg");

        // when
        SpotImage saved = spotImageRepository.save(image);

        // then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getSpotId()).isEqualTo(spot.getId());
        assertThat(saved.isPrimary()).isTrue();
    }
}
