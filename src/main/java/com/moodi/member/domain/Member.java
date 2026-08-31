package com.moodi.member.domain;

import com.moodi.shared.BaseEntity;
import com.moodi.shared.error.BusinessException;
import com.moodi.shared.error.ErrorCode;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseEntity {

    private static final Pattern NICKNAME_PATTERN = Pattern.compile("^[A-Za-z0-9_.]{2,20}$");
    private static final Set<String> ISO_COUNTRIES = Set.of(Locale.getISOCountries());
    private static final int MIN_BIRTH_YEAR = 1900;
    private static final int MINIMUM_AGE = 14;

    private UUID id;
    private OAuthProvider provider;
    private String providerId;
    private String email;
    private String nickname;
    private String country;
    private Integer birthYear;
    private Gender gender;
    private MemberStatus status;
    private LocalDateTime deletedAt;

    private Member(OAuthProvider provider, String providerId, String email) {
        this.provider = provider;
        this.providerId = providerId;
        this.email = email;
        this.status = MemberStatus.PENDING;
    }

    public static Member create(OAuthProvider provider, String providerId, String email) {
        return new Member(provider, providerId, email);
    }

    public void updateProfile(String nickname, String country, Integer birthYear, Gender gender, int currentYear) {
        if (!isPending()) {
            throw new BusinessException(ErrorCode.ALREADY_ONBOARDED);
        }
        validateNickname(nickname);
        validateCountry(country);
        validateBirthYear(birthYear, currentYear);
        if (gender == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        this.nickname = nickname;
        this.country = country;
        this.birthYear = birthYear;
        this.gender = gender;
    }

    public void activate() {
        if (!isPending()) {
            throw new BusinessException(ErrorCode.ALREADY_ONBOARDED);
        }
        if (!hasProfile()) {
            throw new BusinessException(ErrorCode.PROFILE_REQUIRED);
        }
        this.status = MemberStatus.ACTIVE;
    }

    /**
     * 탈퇴(소프트 삭제). 개인정보 컬럼을 비우고 `deletedAt`을 찍는다.
     * <p>
     * 행을 지우지 않는 이유는 `bookmark`·`route`가 `member(id)`를 FK로 참조하고 있어서다
     * (`fk_bookmark_member`·`fk_route_member`, `ON DELETE CASCADE` 없음).
     * 하드 삭제하려면 그 행들까지 지워야 하는데, 재로그인 시 되살릴 수 있도록 남겨둔다.
     * <p>
     * `provider`·`providerId`는 **의도적으로 남긴다** — 같은 소셜 계정으로 다시 로그인했을 때
     * {@link #restore(String)}로 이전 북마크·루트를 되찾게 하기 위한 유일한 식별 수단이다.
     * 프로필이 비워지므로 상태는 `PENDING`으로 되돌려 온보딩을 다시 밟게 한다.
     */
    public void withdraw(LocalDateTime now) {
        if (isWithdrawn()) {
            throw new BusinessException(ErrorCode.MEMBER_NOT_FOUND);
        }
        this.email = null;
        this.nickname = null;
        this.country = null;
        this.birthYear = null;
        this.gender = null;
        this.status = MemberStatus.PENDING;
        this.deletedAt = now;
    }

    /**
     * 탈퇴한 회원이 같은 소셜 계정으로 다시 로그인했을 때의 복구.
     * 프로필은 탈퇴 시 비워졌으므로 `PENDING` 상태로 온보딩을 다시 진행한다.
     */
    public void restore(String email) {
        this.deletedAt = null;
        this.email = email;
    }

    public boolean isWithdrawn() {
        return deletedAt != null;
    }

    public boolean isPending() {
        return status == MemberStatus.PENDING;
    }

    public boolean hasProfile() {
        return nickname != null && country != null && birthYear != null && gender != null;
    }

    private void validateNickname(String nickname) {
        if (nickname == null || !NICKNAME_PATTERN.matcher(nickname).matches()) {
            throw new BusinessException(ErrorCode.INVALID_NICKNAME);
        }
    }

    private void validateCountry(String country) {
        if (country == null || !ISO_COUNTRIES.contains(country)) {
            throw new BusinessException(ErrorCode.INVALID_COUNTRY);
        }
    }

    private void validateBirthYear(Integer birthYear, int currentYear) {
        if (birthYear == null || birthYear < MIN_BIRTH_YEAR || birthYear > currentYear) {
            throw new BusinessException(ErrorCode.INVALID_BIRTH_YEAR);
        }
        if (currentYear - birthYear < MINIMUM_AGE) {
            throw new BusinessException(ErrorCode.UNDERAGE);
        }
    }
}
