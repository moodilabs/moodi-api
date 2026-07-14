package com.moodi.member.domain;

import com.moodi.shared.BaseEntity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseEntity {

    private UUID id;
    private OAuthProvider provider;
    private String providerId;
    private String email;
    private String nickname;
    private MemberStatus status;

    private Member(OAuthProvider provider, String providerId, String email) {
        this.provider = provider;
        this.providerId = providerId;
        this.email = email;
        this.status = MemberStatus.PENDING;
    }

    public static Member create(OAuthProvider provider, String providerId, String email) {
        return new Member(provider, providerId, email);
    }

    public boolean isPending() {
        return status == MemberStatus.PENDING;
    }
}
