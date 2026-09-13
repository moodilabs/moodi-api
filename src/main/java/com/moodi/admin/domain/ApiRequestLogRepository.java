package com.moodi.admin.domain;

import java.time.LocalDateTime;

public interface ApiRequestLogRepository {

    ApiRequestLog save(ApiRequestLog log);

    /** 보관 기간이 지난 행을 지우고 지운 수를 돌려준다. */
    long deleteByCreatedAtBefore(LocalDateTime threshold);
}
