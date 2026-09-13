package com.moodi.spot.application.dto;

import com.moodi.spot.domain.SpotStatus;

/** @param keyword 영문 제목(spot_translation en-US)·contentId 부분일치 */
public record SpotAdminFilter(String keyword, SpotStatus status, String area) {
}
