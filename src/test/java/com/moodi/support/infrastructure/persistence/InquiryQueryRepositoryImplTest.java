package com.moodi.support.infrastructure.persistence;

import com.moodi.shared.support.RepositoryTestSupport;
import com.moodi.support.domain.Inquiry;
import com.moodi.support.domain.InquiryRepository;
import com.moodi.support.domain.InquiryStatus;
import com.moodi.support.domain.InquiryTopic;
import com.moodi.support.support.InquiryFixture;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class InquiryQueryRepositoryImplTest extends RepositoryTestSupport {

    private static final UUID MEMBER_ID = UUID.randomUUID();
    private static final UUID OTHER_ID = UUID.randomUUID();

    @Autowired
    private EntityManager em;

    @Autowired
    private InquiryRepository inquiryRepository;

    private InquiryQueryRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        repository = new InquiryQueryRepositoryImpl(em);
    }

    @Test
    @DisplayName("본인 문의만 최신순으로 조회하고 커서 이후만 돌려준다")
    void find_by_member_orders_and_applies_cursor() {
        Inquiry first = persist(MEMBER_ID, LocalDateTime.of(2026, 8, 1, 10, 0));
        Inquiry second = persist(MEMBER_ID, LocalDateTime.of(2026, 8, 3, 10, 0));
        Inquiry third = persist(MEMBER_ID, LocalDateTime.of(2026, 8, 10, 10, 0));
        persist(OTHER_ID, LocalDateTime.of(2026, 8, 11, 10, 0));

        List<Inquiry> all = repository.findByMember(MEMBER_ID, null, null, 10);
        List<Inquiry> afterThird = repository.findByMember(MEMBER_ID, third.getCreatedAt(), third.getId(), 10);

        assertThat(all).extracting(Inquiry::getId).containsExactly(third.getId(), second.getId(), first.getId());
        assertThat(afterThird).extracting(Inquiry::getId).containsExactly(second.getId(), first.getId());
    }

    @Test
    @DisplayName("어드민 조회는 상태·주제로 거른다")
    void find_all_filters_by_status_and_topic() {
        Inquiry answered = persist(MEMBER_ID, LocalDateTime.of(2026, 8, 1, 10, 0));
        answered.answer("done", UUID.randomUUID(), LocalDateTime.of(2026, 8, 2, 10, 0));
        inquiryRepository.save(answered);
        em.flush();
        persist(OTHER_ID, LocalDateTime.of(2026, 8, 3, 10, 0));

        List<Inquiry> received = repository.findAll(InquiryStatus.RECEIVED, null, null, null, 10);
        List<Inquiry> routes = repository.findAll(null, InquiryTopic.ROUTES, null, null, 10);
        List<Inquiry> account = repository.findAll(null, InquiryTopic.ACCOUNT, null, null, 10);

        assertThat(received).hasSize(1);
        assertThat(routes).hasSize(2);
        assertThat(account).isEmpty();
        assertThat(inquiryRepository.countByStatus(InquiryStatus.ANSWERED)).isEqualTo(1L);
    }

    @Test
    @DisplayName("첨부는 순서대로 함께 로드된다")
    void attachments_are_loaded_in_order() {
        Inquiry inquiry = inquiryRepository.save(InquiryFixture.createWithAttachment(MEMBER_ID));
        em.flush();
        em.clear();

        Inquiry loaded = inquiryRepository.findById(inquiry.getId()).orElseThrow();

        assertThat(loaded.getAttachments()).hasSize(1);
        assertThat(loaded.getAttachments().getFirst().getContentType()).isEqualTo("image/jpeg");
    }

    /** createdAt은 Auditing이 채우므로 저장 후 네이티브 UPDATE로 원하는 시각을 심는다. */
    private Inquiry persist(UUID memberId, LocalDateTime createdAt) {
        Inquiry inquiry = inquiryRepository.save(InquiryFixture.create(memberId));
        em.flush();
        em.createNativeQuery("UPDATE inquiry SET created_at = :createdAt WHERE id = :id")
                .setParameter("createdAt", createdAt)
                .setParameter("id", inquiry.getId())
                .executeUpdate();
        em.clear();
        return inquiryRepository.findById(inquiry.getId()).orElseThrow();
    }
}
