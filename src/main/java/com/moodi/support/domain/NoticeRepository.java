package com.moodi.support.domain;

import java.util.Optional;

public interface NoticeRepository {

    Notice save(Notice notice);

    Optional<Notice> findById(Long id);

    void delete(Notice notice);
}
