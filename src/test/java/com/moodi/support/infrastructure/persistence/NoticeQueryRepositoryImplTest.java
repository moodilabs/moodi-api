package com.moodi.support.infrastructure.persistence;

import com.moodi.shared.support.RepositoryTestSupport;
import com.moodi.support.domain.Notice;
import com.moodi.support.domain.NoticeType;
import com.moodi.support.support.NoticeFixture;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class NoticeQueryRepositoryImplTest extends RepositoryTestSupport {

    private static final LocalDate AUG_1 = LocalDate.of(2026, 8, 1);
    private static final LocalDate AUG_3 = LocalDate.of(2026, 8, 3);
    private static final LocalDate AUG_10 = LocalDate.of(2026, 8, 10);

    @Autowired
    private EntityManager em;

    private NoticeQueryRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        repository = new NoticeQueryRepositoryImpl(em);
    }

    @Test
    @DisplayName("노출 공지만 등록일 최신순, 같은 날짜면 ID 내림차순으로 조회한다")
    void find_visible_orders_by_published_at_then_id_desc() {
        Notice oldest = persist(NoticeFixture.visible(AUG_1));
        Notice sameDayFirst = persist(NoticeFixture.visible(AUG_3));
        Notice sameDaySecond = persist(NoticeFixture.visible(AUG_3));
        persist(NoticeFixture.hidden());
        Notice newest = persist(NoticeFixture.visible(AUG_10));

        List<Notice> result = repository.findVisible(null, null, 10);

        assertThat(result).extracting(Notice::getId)
                .containsExactly(newest.getId(), sameDaySecond.getId(), sameDayFirst.getId(), oldest.getId());
    }

    @Test
    @DisplayName("커서 이후의 공지만 조회한다 (같은 날짜는 ID가 작은 것만)")
    void find_visible_applies_cursor() {
        Notice oldest = persist(NoticeFixture.visible(AUG_1));
        Notice sameDayFirst = persist(NoticeFixture.visible(AUG_3));
        Notice sameDaySecond = persist(NoticeFixture.visible(AUG_3));
        persist(NoticeFixture.visible(AUG_10));

        List<Notice> result = repository.findVisible(AUG_3, sameDaySecond.getId(), 10);

        assertThat(result).extracting(Notice::getId).containsExactly(sameDayFirst.getId(), oldest.getId());
    }

    @Test
    @DisplayName("limit만큼만 돌려준다")
    void find_visible_respects_limit() {
        persist(NoticeFixture.visible(AUG_1));
        persist(NoticeFixture.visible(AUG_3));
        persist(NoticeFixture.visible(AUG_10));

        assertThat(repository.findVisible(null, null, 2)).hasSize(2);
    }

    @Test
    @DisplayName("어드민 조회는 유형·노출 여부로 거를 수 있고 숨김 공지도 포함한다")
    void find_all_filters_by_type_and_visibility() {
        persist(NoticeFixture.create(NoticeType.MAINTENANCE, "m", "c", true, AUG_1));
        Notice hiddenAnnouncement = persist(NoticeFixture.hidden());
        persist(NoticeFixture.visible(AUG_10));

        List<Notice> hiddenOnly = repository.findAll(null, false, null, null, 10);
        List<Notice> announcements = repository.findAll(NoticeType.ANNOUNCEMENT, null, null, null, 10);

        assertThat(hiddenOnly).extracting(Notice::getId).containsExactly(hiddenAnnouncement.getId());
        assertThat(announcements).hasSize(2);
    }

    private Notice persist(Notice notice) {
        em.persist(notice);
        em.flush();
        return notice;
    }
}
