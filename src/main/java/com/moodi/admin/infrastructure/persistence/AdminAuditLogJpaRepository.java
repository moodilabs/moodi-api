package com.moodi.admin.infrastructure.persistence;

import com.moodi.admin.domain.AdminAuditLog;
import com.moodi.admin.domain.AdminAuditLogRepository;
import org.springframework.data.repository.Repository;

public interface AdminAuditLogJpaRepository extends AdminAuditLogRepository, Repository<AdminAuditLog, Long> {
}
