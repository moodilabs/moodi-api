package com.moodi.route.application;

import com.moodi.route.domain.RouteRepository;
import com.moodi.shared.event.MemberWithdrawnEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RouteWithdrawalListenerTest {

    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-08-31T10:00:00Z"), ZoneId.of("Asia/Seoul"));

    @Mock
    private RouteRepository routeRepository;

    @Test
    @DisplayName("탈퇴 이벤트를 받으면 그 회원의 루트를 현재 시각으로 소프트 삭제한다")
    void soft_deletes_routes_of_withdrawn_member() {
        UUID memberId = UUID.randomUUID();

        new RouteWithdrawalListener(routeRepository, FIXED_CLOCK).on(new MemberWithdrawnEvent(memberId));

        verify(routeRepository).softDeleteAllByMemberId(memberId, LocalDateTime.now(FIXED_CLOCK));
    }
}
