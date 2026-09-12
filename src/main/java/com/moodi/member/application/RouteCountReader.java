package com.moodi.member.application;

import java.util.UUID;

/**
 * 루트 컨텍스트의 루트 수를 읽는 포트. 마이 메인(`MY-01-01`)의 "My routes"에 쓰인다.
 */
public interface RouteCountReader {

    long countActiveByMemberId(UUID memberId);
}
