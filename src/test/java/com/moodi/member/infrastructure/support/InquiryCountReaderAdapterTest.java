package com.moodi.member.infrastructure.support;

import com.moodi.shared.support.RepositoryTestSupport;
import com.moodi.support.domain.Inquiry;
import com.moodi.support.domain.InquiryTopic;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class InquiryCountReaderAdapterTest extends RepositoryTestSupport {

    @Autowired
    private EntityManager em;

    private InquiryCountReaderAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new InquiryCountReaderAdapter(em);
    }

    @Test
    @DisplayName("본인 문의 수만 센다")
    void count_only_own_inquiries() {
        UUID memberId = UUID.randomUUID();
        em.persist(Inquiry.create(memberId, InquiryTopic.OTHER, "s1", "c", List.of()));
        em.persist(Inquiry.create(memberId, InquiryTopic.OTHER, "s2", "c", List.of()));
        em.persist(Inquiry.create(UUID.randomUUID(), InquiryTopic.OTHER, "s3", "c", List.of()));
        em.flush();

        assertThat(adapter.countByMemberId(memberId)).isEqualTo(2L);
        assertThat(adapter.countByMemberId(UUID.randomUUID())).isZero();
    }
}
