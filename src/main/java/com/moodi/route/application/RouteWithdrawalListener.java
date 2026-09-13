package com.moodi.route.application;

import com.moodi.route.domain.RouteRepository;
import com.moodi.shared.event.MemberWithdrawnEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * 회원 탈퇴 시 본인 루트 소프트 삭제 — 화면 정책 "생성한 루트를 영구 삭제".
 * 다른 회원이 복사해 간 루트는 그 회원 소유(member_id)라 건드리지 않는다 — "Routes saved by other users will remain".
 */
@Component
public class RouteWithdrawalListener {

    private final RouteRepository routeRepository;
    private final Clock clock;

    public RouteWithdrawalListener(RouteRepository routeRepository, Clock clock) {
        this.routeRepository = routeRepository;
        this.clock = clock;
    }

    @EventListener
    public void on(MemberWithdrawnEvent event) {
        routeRepository.softDeleteAllByMemberId(event.memberId(), LocalDateTime.now(clock));
    }
}
