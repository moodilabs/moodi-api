package com.moodi.shared.event;

import java.util.UUID;

/**
 * 회원이 탈퇴했다. 회원 컨텍스트가 탈퇴 트랜잭션 안에서 발행하고,
 * 스팟(북마크)·루트·추천(Pick)이 같은 트랜잭션에서 자기 데이터를 지운다 — 화면 정책 "저장한 스팟과 루트를 영구 삭제".
 * 하나라도 실패하면 탈퇴 전체가 롤백된다. 컨텍스트 경계를 넘는 유일한 쓰기 경로라 공유 커널에 둔다.
 */
public record MemberWithdrawnEvent(UUID memberId) {
}
