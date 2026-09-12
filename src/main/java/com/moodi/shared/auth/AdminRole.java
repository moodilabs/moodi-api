package com.moodi.shared.auth;

/**
 * 관리자 권한. 인터셉터가 토큰 클레임과 {@link AdminRequired#role()}를 비교해야 하므로 공유 커널에 둔다.
 */
public enum AdminRole {
    /** 콘텐츠(공지·FAQ·약관)·문의 답변·회원 조회. */
    OPERATOR,
    /** 전체 + 관리자 계정 관리 + 회원 정지·강제 탈퇴. */
    SUPER;

    public boolean covers(AdminRole required) {
        return this == SUPER || this == required;
    }
}
