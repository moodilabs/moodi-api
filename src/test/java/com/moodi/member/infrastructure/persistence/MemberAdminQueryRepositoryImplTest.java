package com.moodi.member.infrastructure.persistence;

import com.moodi.member.application.dto.MemberAdminFilter;
import com.moodi.member.application.dto.MemberAdminRow;
import com.moodi.member.application.dto.MemberAdminStatus;
import com.moodi.member.application.dto.MemberDailyStat;
import com.moodi.member.domain.Gender;
import com.moodi.member.domain.Member;
import com.moodi.member.domain.MemberRepository;
import com.moodi.member.domain.OAuthProvider;
import com.moodi.member.support.MemberFixture;
import com.moodi.shared.support.RepositoryTestSupport;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MemberAdminQueryRepositoryImplTest extends RepositoryTestSupport {

    private static final MemberAdminFilter NO_FILTER = new MemberAdminFilter(null, null, null);

    @Autowired
    private EntityManager em;

    @Autowired
    private MemberRepository memberRepository;

    private MemberAdminQueryRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        repository = new MemberAdminQueryRepositoryImpl(em);
    }

    @Test
    @DisplayName("가입일 최신순으로 조회하고 커서 이후만 돌려준다")
    void find_all_orders_and_applies_cursor() {
        Member first = persistActive("a_user", "a@moodi.kr", "g-1", LocalDateTime.of(2026, 8, 1, 10, 0));
        Member second = persistActive("b_user", "b@moodi.kr", "g-2", LocalDateTime.of(2026, 8, 3, 10, 0));
        Member third = persistActive("c_user", "c@moodi.kr", "g-3", LocalDateTime.of(2026, 8, 10, 10, 0));

        List<MemberAdminRow> all = repository.findAll(NO_FILTER, null, null, 10);
        List<MemberAdminRow> afterThird = repository.findAll(NO_FILTER, third.getCreatedAt(), third.getId(), 10);

        assertThat(all).extracting(MemberAdminRow::id).containsExactly(third.getId(), second.getId(), first.getId());
        assertThat(afterThird).extracting(MemberAdminRow::id).containsExactly(second.getId(), first.getId());
    }

    @Test
    @DisplayName("키워드는 닉네임·이메일 부분일치(대소문자 무시)로 거른다")
    void find_all_filters_by_keyword() {
        persistActive("moi_travel", "moi@moodi.kr", "g-1", LocalDateTime.of(2026, 8, 1, 10, 0));
        persistActive("other", "someone@example.com", "g-2", LocalDateTime.of(2026, 8, 2, 10, 0));

        List<MemberAdminRow> byNickname = repository.findAll(new MemberAdminFilter("MOI", null, null), null, null, 10);
        List<MemberAdminRow> byEmail = repository.findAll(new MemberAdminFilter("example", null, null), null, null, 10);

        assertThat(byNickname).extracting(MemberAdminRow::nickname).containsExactly("moi_travel");
        assertThat(byEmail).extracting(MemberAdminRow::nickname).containsExactly("other");
    }

    @Test
    @DisplayName("상태 필터: 탈퇴는 deleted_at으로, 정지는 status로 거른다")
    void find_all_filters_by_status() {
        Member active = persistActive("active", "a@moodi.kr", "g-1", LocalDateTime.of(2026, 8, 1, 10, 0));
        Member suspended = persistActive("suspended", "s@moodi.kr", "g-2", LocalDateTime.of(2026, 8, 2, 10, 0));
        suspended.suspend("reason", LocalDateTime.of(2026, 8, 5, 10, 0));
        memberRepository.save(suspended);
        Member withdrawn = persistActive("gone", "w@moodi.kr", "g-3", LocalDateTime.of(2026, 8, 3, 10, 0));
        withdrawn.withdraw(LocalDateTime.of(2026, 8, 6, 10, 0));
        memberRepository.save(withdrawn);
        em.flush();

        List<MemberAdminRow> actives = repository.findAll(new MemberAdminFilter(null, MemberAdminStatus.ACTIVE, null), null, null, 10);
        List<MemberAdminRow> suspendeds = repository.findAll(new MemberAdminFilter(null, MemberAdminStatus.SUSPENDED, null), null, null, 10);
        List<MemberAdminRow> withdrawns = repository.findAll(new MemberAdminFilter(null, MemberAdminStatus.WITHDRAWN, null), null, null, 10);
        List<MemberAdminRow> pendings = repository.findAll(new MemberAdminFilter(null, MemberAdminStatus.PENDING, null), null, null, 10);

        assertThat(actives).extracting(MemberAdminRow::id).containsExactly(active.getId());
        assertThat(suspendeds).extracting(MemberAdminRow::id).containsExactly(suspended.getId());
        assertThat(withdrawns).extracting(MemberAdminRow::id).containsExactly(withdrawn.getId());
        assertThat(withdrawns.getFirst().status()).isEqualTo(MemberAdminStatus.WITHDRAWN);
        assertThat(pendings).isEmpty();
    }

    @Test
    @DisplayName("일별 가입·탈퇴 수를 집계한다")
    void count_daily_aggregates_signups_and_withdrawals() {
        persistActive("aa", "a@moodi.kr", "g-1", LocalDateTime.of(2026, 8, 1, 9, 0));
        persistActive("bb", "b@moodi.kr", "g-2", LocalDateTime.of(2026, 8, 1, 18, 0));
        Member gone = persistActive("cc", "c@moodi.kr", "g-3", LocalDateTime.of(2026, 8, 2, 9, 0));
        gone.withdraw(LocalDateTime.of(2026, 8, 3, 12, 0));
        memberRepository.save(gone);
        em.flush();
        persistActive("out_of_range", "d@moodi.kr", "g-4", LocalDateTime.of(2026, 9, 1, 9, 0));

        List<MemberDailyStat> stats = repository.countDaily(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31));

        assertThat(stats).containsExactly(
                new MemberDailyStat(LocalDate.of(2026, 8, 1), 2, 0),
                new MemberDailyStat(LocalDate.of(2026, 8, 2), 1, 0),
                new MemberDailyStat(LocalDate.of(2026, 8, 3), 0, 1));
    }

    /** created_at은 Auditing이 채우므로 저장 후 네이티브 UPDATE로 원하는 시각을 심는다. */
    private Member persistActive(String nickname, String email, String providerId, LocalDateTime createdAt) {
        Member member = Member.create(OAuthProvider.GOOGLE, providerId, email);
        member.updateProfile(nickname, "KR", 1996, Gender.FEMALE, MemberFixture.CURRENT_YEAR);
        member.activate();
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
