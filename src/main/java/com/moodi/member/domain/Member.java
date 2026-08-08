package com.moodi.member.domain;

import com.moodi.shared.BaseEntity;
import com.moodi.shared.error.BusinessException;
import com.moodi.shared.error.ErrorCode;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
