package com.moodi.admin.domain;

import java.util.List;
import java.util.UUID;

public interface AdminAuditLogRepository {

    AdminAuditLog save(AdminAuditLog log);

    List<AdminAuditLog> findTop100ByOrderByIdDesc();

    List<AdminAuditLog> findTop100ByAdminIdOrderByIdDesc(UUID adminId);

    List<AdminAuditLog> findTop100ByIdLessThanOrderByIdDesc(Long id);

    List<AdminAuditLog> findTop100ByAdminIdAndIdLessThanOrderByIdDesc(UUID adminId, Long id);
}
