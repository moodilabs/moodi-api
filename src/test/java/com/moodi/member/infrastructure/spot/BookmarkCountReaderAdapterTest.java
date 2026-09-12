package com.moodi.member.infrastructure.spot;

import com.moodi.shared.support.RepositoryTestSupport;
import com.moodi.spot.support.BookmarkFixture;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class BookmarkCountReaderAdapterTest extends RepositoryTestSupport {

    @Autowired
    private EntityManager em;

    private BookmarkCountReaderAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new BookmarkCountReaderAdapter(em);
    }

    @Test
    @DisplayName("본인 북마크 수만 센다")
    void count_only_own_bookmarks() {
        UUID memberId = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        em.persist(BookmarkFixture.create(memberId, 1L));
        em.persist(BookmarkFixture.create(memberId, 2L));
        em.persist(BookmarkFixture.create(other, 1L));
        em.flush();

        assertThat(adapter.countByMemberId(memberId)).isEqualTo(2L);
    }

    @Test
    @DisplayName("북마크가 없으면 0이다")
    void count_returns_zero_when_none() {
        assertThat(adapter.countByMemberId(UUID.randomUUID())).isZero();
    }
}
