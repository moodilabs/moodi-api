package com.moodi.admin.infrastructure;

import com.moodi.admin.application.dto.DashboardSummary;
import com.moodi.member.domain.Gender;
import com.moodi.member.domain.Member;
import com.moodi.member.domain.MemberRepository;
import com.moodi.member.domain.OAuthProvider;
import com.moodi.member.support.MemberFixture;
import com.moodi.shared.support.RepositoryTestSupport;
import com.moodi.support.domain.Inquiry;
import com.moodi.support.domain.InquiryTopic;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DashboardReadModelAdapterTest extends RepositoryTestSupport {

    private static final LocalDateTime TODAY_START = LocalDateTime.of(2026, 8, 10, 0, 0);
    private static final LocalDateTime WEEK_START = LocalDateTime.of(2026, 8, 4, 0, 0);

    @Autowired
    private EntityManager em;

    @Autowired
    private MemberRepository memberRepository;

    private DashboardReadModelAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new DashboardReadModelAdapter(em);
    }

    @Test
    @DisplayName("회원 집계: 상태별·탈퇴·신규를 나눠 센다")
    void count_members_by_status_and_recency() {
        persistMember("active_1", "g-1", LocalDateTime.of(2026, 8, 10, 9, 0), true);   // 오늘 가입
        persistMember("active_2", "g-2", LocalDateTime.of(2026, 8, 5, 9, 0), true);    // 이번 주
        persistMember("pending_1", "g-3", LocalDateTime.of(2026, 7, 1, 9, 0), false);
        Member suspended = persistMember("suspended_1", "g-4", LocalDateTime.of(2026, 7, 1, 9, 0), true);
        suspended.suspend("reason", LocalDateTime.of(2026, 8, 1, 9, 0));
        memberRepository.save(suspended);
        Member withdrawn = persistMember("gone_1", "g-5", LocalDateTime.of(2026, 7, 1, 9, 0), true);
        withdrawn.withdraw(LocalDateTime.of(2026, 8, 2, 9, 0));
        memberRepository.save(withdrawn);
        em.flush();

        DashboardSummary.Members members = adapter.countMembers(TODAY_START, WEEK_START);

        assertThat(members.total()).isEqualTo(5);
        assertThat(members.active()).isEqualTo(2);
        assertThat(members.pending()).isEqualTo(1);
        assertThat(members.suspended()).isEqualTo(1);
        assertThat(members.withdrawn()).isEqualTo(1);
        assertThat(members.newToday()).isEqualTo(1);
        assertThat(members.newLast7Days()).isEqualTo(2);
    }

    @Test
    @DisplayName("문의 집계: 미답변 수와 최근 7일 답변 수")
    void count_inquiries() {
        UUID memberId = UUID.randomUUID();
        em.persist(Inquiry.create(memberId, InquiryTopic.OTHER, "s1", "c", List.of()));
        Inquiry answeredRecently = Inquiry.create(memberId, InquiryTopic.OTHER, "s2", "c", List.of());
        answeredRecently.answer("a", UUID.randomUUID(), LocalDateTime.of(2026, 8, 8, 9, 0));
        em.persist(answeredRecently);
        Inquiry answeredLongAgo = Inquiry.create(memberId, InquiryTopic.OTHER, "s3", "c", List.of());
        answeredLongAgo.answer("a", UUID.randomUUID(), LocalDateTime.of(2026, 7, 1, 9, 0));
        em.persist(answeredLongAgo);
        em.flush();

        DashboardSummary.Inquiries inquiries = adapter.countInquiries(WEEK_START);

        assertThat(inquiries.received()).isEqualTo(1);
        assertThat(inquiries.answeredLast7Days()).isEqualTo(1);
    }

    @Test
    @DisplayName("콘텐츠 집계는 빈 테이블에서 0을 돌려준다")
    void count_content_on_empty_tables() {
        DashboardSummary.Content content = adapter.countContent();

        assertThat(content.spots()).isZero();
        assertThat(content.bookmarks()).isZero();
        assertThat(content.routes()).isZero();
        assertThat(content.sharedRoutes()).isZero();
        assertThat(content.picks()).isZero();
    }

    private Member persistMember(String nickname, String providerId, LocalDateTime createdAt, boolean activate) {
        Member member = Member.create(OAuthProvider.GOOGLE, providerId, providerId + "@moodi.kr");
        if (activate) {
            member.updateProfile(nickname, "KR", 1996, Gender.FEMALE, MemberFixture.CURRENT_YEAR);
            member.activate();
        }
        Member saved = memberRepository.save(member);
        em.flush();
        em.createNativeQuery("UPDATE member SET created_at = :createdAt WHERE id = :id")
                .setParameter("createdAt", createdAt)
                .setParameter("id", saved.getId())
                .executeUpdate();
        em.clear();
        return memberRepository.findById(saved.getId()).orElseThrow();
    }
}
