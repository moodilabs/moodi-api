package com.moodi.member.support;

import com.moodi.member.domain.Member;
import com.moodi.member.domain.OAuthProvider;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

public class MemberFixture {

    private static final OAuthProvider DEFAULT_PROVIDER = OAuthProvider.GOOGLE;
    private static final String DEFAULT_PROVIDER_ID = "google-sub-123";
    private static final String DEFAULT_EMAIL = "user@moodi.kr";

    public static Member create() {
        return create(DEFAULT_PROVIDER, DEFAULT_PROVIDER_ID, DEFAULT_EMAIL);
    }

    public static Member create(OAuthProvider provider, String providerId, String email) {
        return Member.create(provider, providerId, email);
    }

    public static Member create(UUID id, OAuthProvider provider, String providerId, String email) {
        Member member = Member.create(provider, providerId, email);
        ReflectionTestUtils.setField(member, "id", id);
        return member;
    }

    public static Member createWithId(UUID id) {
        return create(id, DEFAULT_PROVIDER, DEFAULT_PROVIDER_ID, DEFAULT_EMAIL);
    }

    private MemberFixture() {
    }
}
