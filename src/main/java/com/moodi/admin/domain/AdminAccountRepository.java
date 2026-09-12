package com.moodi.admin.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AdminAccountRepository {

    AdminAccount save(AdminAccount account);

    Optional<AdminAccount> findById(UUID id);

    Optional<AdminAccount> findByEmail(String email);

    boolean existsByEmail(String email);

    List<AdminAccount> findAllByOrderByCreatedAtAsc();

    long count();

    void flush();
}
