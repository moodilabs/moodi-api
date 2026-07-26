package com.moodi.spot.application;

import com.moodi.spot.presentation.dto.SpotDetailResponse;
import jakarta.annotation.Nullable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class SpotDetailService {

    private final SpotDetailReader spotDetailReader;

    public SpotDetailService(SpotDetailReader spotDetailReader) {
        this.spotDetailReader = spotDetailReader;
    }

    public SpotDetailResponse getDetail(Long spotId, @Nullable UUID memberId) {
        return spotDetailReader.read(spotId, memberId);
    }
}
