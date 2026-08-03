package com.moodi.member.support;

import com.moodi.member.domain.Gender;
import com.moodi.member.domain.Member;
import com.moodi.member.domain.OAuthProvider;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

public class MemberFixture {

    public static final int CURRENT_YEAR = 2026;

    private static final OAuthProvider DEFAULT_PROVIDER = OAuthProvider.GOOGLE;
    private static final String DEFAULT_PROVIDER_ID = "google-sub-123";
    private static final String DEFAULT_EMAIL = "user@moodi.kr";
    private static final String DEFAULT_NICKNAME = "moodi_user";
    private static final String DEFAULT_COUNTRY = "KR";
    private static final int DEFAULT_BIRTH_YEAR = 1996;
    private static final Gender DEFAULT_GENDER = Gender.FEMALE;

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

    public static Member withProfile() {
        Member member = create();
        member.updateProfile(DEFAULT_NICKNAME, DEFAULT_COUNTRY, DEFAULT_BIRTH_YEAR, DEFAULT_GENDER, CURRENT_YEAR);
        return member;
    }

    public static Member active() {
        Member member = withProfile();
        member.activate();
        return member;
    }

    private MemberFixture() {
    }
}
