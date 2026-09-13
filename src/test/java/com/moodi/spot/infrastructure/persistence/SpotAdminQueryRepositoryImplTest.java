package com.moodi.spot.infrastructure.persistence;

import com.moodi.shared.support.RepositoryTestSupport;
import com.moodi.spot.application.dto.SpotAdminFilter;
import com.moodi.spot.application.dto.SpotAdminRow;
import com.moodi.spot.domain.Spot;
import com.moodi.spot.domain.SpotRepository;
import com.moodi.spot.domain.SpotStatus;
import com.moodi.spot.domain.SpotTranslationRepository;
import com.moodi.spot.support.BookmarkFixture;
import com.moodi.spot.support.SpotFixture;
import com.moodi.spot.support.SpotTranslationFixture;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SpotAdminQueryRepositoryImplTest extends RepositoryTestSupport {

    private static final SpotAdminFilter NO_FILTER = new SpotAdminFilter(null, null, null);

    @Autowired
    private EntityManager em;

    @Autowired
    private SpotRepository spotRepository;

    @Autowired
    private SpotTranslationRepository spotTranslationRepository;

    private SpotAdminQueryRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        repository = new SpotAdminQueryRepositoryImpl(em);
    }

    @Test
    @DisplayName("id 내림차순으로 조회하고 커서 이후만 돌려주며 영문 제목·북마크 수를 붙인다")
    void find_all_orders_by_id_with_title_and_bookmarks() {
        Spot first = spotRepository.save(SpotFixture.create("c-1", "TOURAPI"));
        Spot second = spotRepository.save(SpotFixture.create("c-2", "TOURAPI"));
        spotTranslationRepository.save(SpotTranslationFixture.create(second.getId(), "en-US", "Daelim Changgo"));
        em.persist(BookmarkFixture.create(UUID.randomUUID(), second.getId()));
        em.persist(BookmarkFixture.create(UUID.randomUUID(), second.getId()));
        em.flush();

        List<SpotAdminRow> all = repository.findAll(NO_FILTER, null, 10);
        List<SpotAdminRow> afterSecond = repository.findAll(NO_FILTER, second.getId(), 10);

        assertThat(all).extracting(SpotAdminRow::id).containsExactly(second.getId(), first.getId());
        assertThat(all.getFirst().title()).isEqualTo("Daelim Changgo");
        assertThat(all.getFirst().bookmarkCount()).isEqualTo(2);
        assertThat(all.get(1).title()).isNull();
        assertThat(afterSecond).extracting(SpotAdminRow::id).containsExactly(first.getId());
    }

    @Test
    @DisplayName("키워드는 영문 제목·contentId, 상태·지역은 정확히 거른다")
    void find_all_filters() {
        Spot published = spotRepository.save(SpotFixture.create("c-1", "TOURAPI"));
        published.publish();
        spotRepository.save(published);
        spotTranslationRepository.save(SpotTranslationFixture.create(published.getId(), "en-US", "Daelim Changgo"));
        Spot hidden = spotRepository.save(SpotFixture.create("c-2", "TOURAPI"));
        hidden.publish();
        hidden.hide("closed", LocalDateTime.of(2026, 8, 10, 9, 0));
        spotRepository.save(hidden);
        em.flush();

        List<SpotAdminRow> byTitle = repository.findAll(new SpotAdminFilter("changgo", null, null), null, 10);
        List<SpotAdminRow> byContentId = repository.findAll(new SpotAdminFilter("c-2", null, null), null, 10);
        List<SpotAdminRow> hiddenOnly = repository.findAll(new SpotAdminFilter(null, SpotStatus.HIDDEN, null), null, 10);
        List<SpotAdminRow> byArea = repository.findAll(new SpotAdminFilter(null, null, "부산"), null, 10);

        assertThat(byTitle).extracting(SpotAdminRow::id).containsExactly(published.getId());
        assertThat(byContentId).extracting(SpotAdminRow::id).containsExactly(hidden.getId());
        assertThat(hiddenOnly).extracting(SpotAdminRow::id).containsExactly(hidden.getId());
        assertThat(hiddenOnly.getFirst().statusChangedAt()).isEqualTo(LocalDateTime.of(2026, 8, 10, 9, 0));
        assertThat(byArea).isEmpty();
    }
}
