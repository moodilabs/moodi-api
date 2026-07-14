package com.moodi.member.domain;

import com.moodi.member.support.MemberFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MemberTest {

    @Test
    @DisplayName("회원 생성 시 상태는 PENDING이고 필드가 세팅된다")
    void create_sets_pending_status_and_fields() {
        Member member = Member.create(OAuthProvider.GOOGLE, "google-sub-123", "user@moodi.kr");

        assertThat(member.getProvider()).isEqualTo(OAuthProvider.GOOGLE);
        assertThat(member.getProviderId()).isEqualTo("google-sub-123");
        assertThat(member.getEmail()).isEqualTo("user@moodi.kr");
        assertThat(member.getStatus()).isEqualTo(MemberStatus.PENDING);
        assertThat(member.getNickname()).isNull();
    }

    @Test
    @DisplayName("이메일 없이도 회원을 생성할 수 있다")
    void create_allows_null_email() {
        Member member = Member.create(OAuthProvider.APPLE, "apple-sub-456", null);

        assertThat(member.getEmail()).isNull();
        assertThat(member.getStatus()).isEqualTo(MemberStatus.PENDING);
    }

    @Test
    @DisplayName("생성 직후 회원은 온보딩 전 상태이다")
    void is_pending_returns_true_for_new_member() {
        Member member = MemberFixture.create();

        assertThat(member.isPending()).isTrue();
    }
}
