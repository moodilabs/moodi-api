package com.moodi.member.domain;

import com.moodi.member.support.MemberFixture;
import com.moodi.shared.error.BusinessException;
import com.moodi.shared.error.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static com.moodi.member.support.MemberFixture.CURRENT_YEAR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    @Test
    @DisplayName("프로필 설정 시 값이 채워지고 상태는 PENDING을 유지한다")
    void update_profile_fills_fields_and_keeps_pending() {
        Member member = MemberFixture.create();

        member.updateProfile("moodi_user", "KR", 1996, Gender.FEMALE, CURRENT_YEAR);

        assertThat(member.getNickname()).isEqualTo("moodi_user");
        assertThat(member.getCountry()).isEqualTo("KR");
        assertThat(member.getBirthYear()).isEqualTo(1996);
        assertThat(member.getGender()).isEqualTo(Gender.FEMALE);
        assertThat(member.getStatus()).isEqualTo(MemberStatus.PENDING);
        assertThat(member.hasProfile()).isTrue();
    }

    @ParameterizedTest
    @DisplayName("닉네임이 형식에 맞지 않으면 프로필 설정에 실패한다")
    @ValueSource(strings = {"a", "무디", "moodi user", "moodi!", "aaaaaaaaaaaaaaaaaaaaa"})
    void update_profile_with_invalid_nickname_throws(String nickname) {
        Member member = MemberFixture.create();

        assertThatThrownBy(() -> member.updateProfile(nickname, "KR", 1996, Gender.FEMALE, CURRENT_YEAR))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_NICKNAME);
    }

    @ParameterizedTest
    @DisplayName("출생연도가 1900 미만이거나 미래면 프로필 설정에 실패한다")
    @ValueSource(ints = {1899, CURRENT_YEAR + 1})
    void update_profile_with_invalid_birth_year_throws(int birthYear) {
        Member member = MemberFixture.create();

        assertThatThrownBy(() -> member.updateProfile("moodi_user", "KR", birthYear, Gender.FEMALE, CURRENT_YEAR))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_BIRTH_YEAR);
    }

    @Test
    @DisplayName("만 14세 미만이면 프로필 설정에 실패한다")
    void update_profile_with_underage_throws() {
        Member member = MemberFixture.create();

        assertThatThrownBy(() -> member.updateProfile("moodi_user", "KR", CURRENT_YEAR - 13, Gender.FEMALE, CURRENT_YEAR))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.UNDERAGE);
    }

    @Test
    @DisplayName("만 14세가 되는 해이면 프로필 설정에 성공한다")
    void update_profile_with_exactly_minimum_age_succeeds() {
        Member member = MemberFixture.create();

        member.updateProfile("moodi_user", "KR", CURRENT_YEAR - 14, Gender.FEMALE, CURRENT_YEAR);

        assertThat(member.hasProfile()).isTrue();
    }

    @Test
    @DisplayName("존재하지 않는 국가 코드면 프로필 설정에 실패한다")
    void update_profile_with_unknown_country_throws() {
        Member member = MemberFixture.create();

        assertThatThrownBy(() -> member.updateProfile("moodi_user", "ZZ", 1996, Gender.FEMALE, CURRENT_YEAR))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_COUNTRY);
    }

    @Test
    @DisplayName("이미 가입 완료된 회원은 프로필을 다시 설정할 수 없다")
    void update_profile_on_active_member_throws() {
        Member member = MemberFixture.active();

        assertThatThrownBy(() -> member.updateProfile("moodi_user", "KR", 1996, Gender.FEMALE, CURRENT_YEAR))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.ALREADY_ONBOARDED);
    }

    @Test
    @DisplayName("프로필이 채워진 회원은 활성화된다")
    void activate_changes_status_to_active() {
        Member member = MemberFixture.withProfile();

        member.activate();

        assertThat(member.getStatus()).isEqualTo(MemberStatus.ACTIVE);
        assertThat(member.isPending()).isFalse();
    }

    @Test
    @DisplayName("프로필 없이 활성화하면 실패한다")
    void activate_without_profile_throws() {
        Member member = MemberFixture.create();

        assertThatThrownBy(member::activate)
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.PROFILE_REQUIRED);
    }

    @Test
    @DisplayName("이미 활성화된 회원은 다시 활성화할 수 없다")
    void activate_twice_throws() {
        Member member = MemberFixture.active();

        assertThatThrownBy(member::activate)
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.ALREADY_ONBOARDED);
    }
}
