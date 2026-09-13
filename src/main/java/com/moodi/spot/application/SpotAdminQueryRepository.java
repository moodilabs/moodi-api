package com.moodi.spot.application;

import com.moodi.spot.application.dto.SpotAdminFilter;
import com.moodi.spot.application.dto.SpotAdminRow;

import java.util.List;

/** 어드민 스팟 목록 포트. `id DESC` 정렬, 커서는 마지막 id. `limit`만큼 돌려준다. */
public interface SpotAdminQueryRepository {

    List<SpotAdminRow> findAll(SpotAdminFilter filter, Long cursorId, int limit);
}
