package com.moodi.spot.infrastructure.persistence;

import com.moodi.spot.domain.Bookmark;
import com.moodi.spot.domain.BookmarkRepository;
import org.springframework.data.repository.Repository;

public interface BookmarkJpaRepository extends BookmarkRepository, Repository<Bookmark, Long> {
}
