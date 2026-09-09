package com.moodi.member.infrastructure.persistence;

import com.moodi.member.domain.MemberPreferredMood;
import com.moodi.member.domain.MemberPreferredMoodRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface MemberPreferredMoodJpaRepository
        extends MemberPreferredMoodRepository, Repository<MemberPreferredMood, UUID> {

    /**
     * 파생 delete는 엔티티를 로드한 뒤 remove로 예약만 하고, Hibernate는 flush 시 INSERT를 DELETE보다 먼저 실행한다.
     * 같은 트랜잭션에서 "삭제 후 재저장"하면 uk_member_preferred_mood_member_mood 위반이 나므로
     * 즉시 실행되는 벌크 DELETE로 바꾼다. flushAutomatically로 앞선 변경(예: 탈퇴 처리)을 먼저 반영한다.
     */
    @Override
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM MemberPreferredMood m WHERE m.memberId = :memberId")
    void deleteByMemberId(@Param("memberId") UUID memberId);
}
