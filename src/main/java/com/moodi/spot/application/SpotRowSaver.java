package com.moodi.spot.application;

import com.moodi.spot.domain.Spot;
import com.moodi.spot.domain.SpotContentType;
import com.moodi.spot.domain.SpotImage;
import com.moodi.spot.domain.SpotImageRepository;
import com.moodi.spot.domain.SpotRepository;
import com.moodi.spot.domain.SpotTranslation;
import com.moodi.spot.domain.SpotTranslationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class SpotRowSaver {

    private static final String LOCALE_KO = "ko-KR";

    private final SpotRepository spotRepository;
    private final SpotTranslationRepository spotTranslationRepository;
    private final SpotImageRepository spotImageRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void save(SpotCsvRow row) {
        SpotContentType contentType = SpotContentType.fromLabel(row.getContentType());

        Spot spot = spotRepository.save(Spot.create(
                row.getContentId(),
                contentType,
                row.getArea(),
                row.getSource(),
                parseDouble(row.getLongitude()),
                parseDouble(row.getLatitude()),
                blankToNull(row.getTel()),
                blankToNull(row.getLclsSystm1()),
                blankToNull(row.getLclsSystm2()),
                blankToNull(row.getLclsSystm3()),
                blankToNull(row.getHomepage())
        ));

        spotTranslationRepository.save(SpotTranslation.create(
                spot.getId(),
                LOCALE_KO,
                row.getTitle(),
                blankToNull(row.getOverview()),
                blankToNull(row.getAddr1()),
                blankToNull(row.getAddr2())
        ));

        String imageUrl = blankToNull(row.getSpotImage());
        if (imageUrl != null) {
            spotImageRepository.save(SpotImage.createPrimary(spot.getId(), imageUrl));
        }
    }

    private Double parseDouble(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Double.parseDouble(value);
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }
}
