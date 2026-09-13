package com.moodi.member.application;

import com.moodi.member.application.dto.WithdrawalCommand;
import com.moodi.member.domain.Member;
import com.moodi.member.domain.MemberRepository;
import com.moodi.member.domain.MemberWithdrawalRepository;
import com.moodi.member.domain.WithdrawalReason;
import com.moodi.member.support.MemberFixture;
import com.moodi.route.domain.Route;
import com.moodi.route.support.RouteFixture;
import com.moodi.shared.support.RepositoryTestSupport;
import com.moodi.spot.support.BookmarkFixture;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 탈퇴 이벤트가 실제 Spring 컨텍스트에서 스팟·루트·추천 리스너까지 이어지는지 확인한다.
 * 한 트랜잭션 안에서 도는 것이 핵심이라 {@code @Transactional} 테스트 그대로 둔다.
 */
class MemberWithdrawalIntegrationTest extends RepositoryTestSupport {

    private static final LocalDate START = LocalDate.of(2026, 8, 10);

    @Autowired
    private MemberWithdrawService memberWithdrawService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private MemberWithdrawalRepository memberWithdrawalRepository;

    @Autowired
    private EntityManager em;

    @Test
    @DisplayName("탈퇴하면 북마크는 삭제되고 루트는 소프트 삭제되며 Pick은 삭제된다 — 다른 회원 데이터는 그대로")
    void withdraw_cascades_to_other_contexts() {
        Member member = memberRepository.save(MemberFixture.active());
        UUID memberId = member.getId();
        UUID other = UUID.randomUUID();
        em.persist(BookmarkFixture.create(memberId, 1L));
        em.persist(BookmarkFixture.create(other, 1L));
        Route mine = RouteFixture.createRoute(memberId, "mine", START, START, List.of(RouteFixture.createDay(1, START, 1)));
        Route theirs = RouteFixture.createRoute(other, "theirs", START, START, List.of(RouteFixture.createDay(1, START, 1)));
        em.persist(mine);
        em.persist(theirs);
        em.createNativeQuery("INSERT INTO pick_request (id, member_id, image_key, created_at, updated_at) "
                        + "VALUES (:id, :memberId, 'picks/a.jpg', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)")
                .setParameter("id", UUID.randomUUID()).setParameter("memberId", memberId).executeUpdate();
        em.flush();

        memberWithdrawService.withdraw(memberId, new WithdrawalCommand(Set.of(WithdrawalReason.HARD_TO_USE), "x"));
        em.flush();
        em.clear();

        assertThat(count("bookmark", "member_id", memberId)).isZero();
        assertThat(count("bookmark", "member_id", other)).isEqualTo(1);
        assertThat(count("pick_request", "member_id", memberId)).isZero();
        assertThat(em.find(Route.class, mine.getId()).isDeleted()).isTrue();
        assertThat(em.find(Route.class, theirs.getId()).isDeleted()).isFalse();
        assertThat(memberRepository.findById(memberId).orElseThrow().isWithdrawn()).isTrue();
        assertThat(memberWithdrawalRepository.findFirstByMemberIdOrderByCreatedAtDesc(memberId))
                .map(withdrawal -> withdrawal.getReasons())
                .contains(Set.of(WithdrawalReason.HARD_TO_USE));
    }

    private long count(String table, String column, UUID id) {
        return ((Number) em.createNativeQuery("SELECT COUNT(*) FROM " + table + " WHERE " + column + " = :id")
                .setParameter("id", id).getSingleResult()).longValue();
    }
}
