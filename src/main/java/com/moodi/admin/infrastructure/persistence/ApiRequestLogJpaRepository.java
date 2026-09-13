package com.moodi.admin.infrastructure.persistence;

import com.moodi.admin.domain.ApiRequestLog;
import com.moodi.admin.domain.ApiRequestLogRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import java.time.LocalDateTime;

public interface ApiRequestLogJpaRepository extends ApiRequestLogRepository, Repository<ApiRequestLog, Long> {

    /** 파생 delete는 행을 하나씩 로드해 지우므로 벌크 JPQL로 한 번에 지운다. */
    @Override
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM ApiRequestLog l WHERE l.createdAt < :threshold")
    long deleteByCreatedAtBefore(LocalDateTime threshold);
}
