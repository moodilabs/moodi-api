package com.moodi.support.infrastructure.persistence;

import com.moodi.support.domain.Notice;
import com.moodi.support.domain.NoticeRepository;
import org.springframework.data.repository.Repository;

public interface NoticeJpaRepository extends NoticeRepository, Repository<Notice, Long> {
}
