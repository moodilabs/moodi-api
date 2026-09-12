package com.moodi.admin.infrastructure.persistence;

import com.moodi.admin.domain.AdminAccount;
import com.moodi.admin.domain.AdminAccountRepository;
import org.springframework.data.repository.Repository;

import java.util.UUID;

public interface AdminAccountJpaRepository extends AdminAccountRepository, Repository<AdminAccount, UUID> {
}
