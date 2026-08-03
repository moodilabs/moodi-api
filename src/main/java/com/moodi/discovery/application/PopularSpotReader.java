package com.moodi.discovery.application;

import java.util.List;
import java.util.UUID;

/**
 * 인기 스팟 조회 포트 (FED-F02). 북마크 수 기준이며 개인화 조건은 없다.
 */
public interface PopularSpotReader {

    List<PopularSpotRow> readTopByBookmarkCount(UUID memberId, int limit);
}
