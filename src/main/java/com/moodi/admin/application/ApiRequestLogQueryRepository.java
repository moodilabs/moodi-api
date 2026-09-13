package com.moodi.admin.application;

import com.moodi.admin.application.dto.ApiRequestLogFilter;
import com.moodi.admin.application.dto.ApiRequestLogItem;

import java.util.List;

/** 회원 닉네임·이메일을 붙인 조회 전용 포트. 구현은 네이티브 SQL(admin/infrastructure). */
public interface ApiRequestLogQueryRepository {

    List<ApiRequestLogItem> findAll(ApiRequestLogFilter filter, Long cursorId, int limit);
}
