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
    private static final int MAX_SUSPEND_REASON_LENGTH = 200;

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
    private LocalDateTime suspendedAt;
    private String suspendReason;
    /** 마지막 로그인 id_token의 aud. 제공자 토큰 교환·철회는 이 client_id로 서명한 secret을 써야 한다. */
    private String providerClientId;
    /** 제공자(Apple) refresh token. 탈퇴 시 계정 연결 철회에 쓴다. 없으면 철회할 수 없다. */
    private String providerRefreshToken;

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

    /**
     * 계정설정(`MY-02-02`)에서의 닉네임 변경. 온보딩용 {@link #updateProfile}과 달리 `ACTIVE` 회원만 허용한다.
     */
    public void changeNickname(String nickname) {
        requireActive();
        validateNickname(nickname);
        this.nickname = nickname;
    }

    /**
     * 계정설정(`MY-02-03`)에서의 국가 변경.
     */
    public void changeCountry(String country) {
        requireActive();
        validateCountry(country);
        this.country = country;
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
     * 탈퇴. 개인정보 컬럼을 비우고 `deletedAt`을 찍는다. 행을 남기는 이유는 통계·탈퇴 사유 보존이며,
     * 북마크·루트·Pick 같은 활동 데이터는 화면 정책대로 각 컨텍스트가 지운다({@code MemberWithdrawnEvent}).
     * <p>
     * `provider`·`providerId`는 남긴다 — 같은 소셜 계정으로 다시 로그인하면 {@link #restore(String)}로
     * 이 행을 재활용해 온보딩을 처음부터 밟는다(사실상 신규 가입). 프로필이 비워지므로 상태는 `PENDING`.
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
        this.suspendedAt = null;
        this.suspendReason = null;
        this.providerRefreshToken = null;
        this.deletedAt = now;
    }

    /**
     * 로그인 때마다 client_id를 갱신하고, 인가 코드를 교환한 경우에만 refresh token을 덮어쓴다.
     * 코드 없이 로그인해도 이전에 받아둔 refresh token은 유지돼야 탈퇴 때 철회할 수 있다.
     */
    public void rememberProviderCredential(String clientId, String refreshToken) {
        if (clientId != null && !clientId.isBlank()) {
            this.providerClientId = clientId;
        }
        if (refreshToken != null && !refreshToken.isBlank()) {
            this.providerRefreshToken = refreshToken;
        }
    }

    /**
     * 탈퇴한 회원이 같은 소셜 계정으로 다시 로그인했을 때. 되찾을 활동 데이터는 없고(탈퇴 시 삭제),
     * 회원 행만 재활용해 `PENDING` 상태로 온보딩을 다시 진행한다.
     */
    public void restore(String email) {
        this.deletedAt = null;
        this.email = email;
    }

    /**
     * 어드민 정지(`ADM-F04`). 가입 완료 회원만 정지할 수 있다 — 온보딩 중인 회원은 막을 게 없다.
     */
    public void suspend(String reason, LocalDateTime now) {
        if (status != MemberStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        if (reason == null || reason.isBlank() || reason.length() > MAX_SUSPEND_REASON_LENGTH) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        this.status = MemberStatus.SUSPENDED;
        this.suspendedAt = now;
        this.suspendReason = reason;
    }

    public void unsuspend() {
        if (status != MemberStatus.SUSPENDED) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        this.status = MemberStatus.ACTIVE;
        this.suspendedAt = null;
        this.suspendReason = null;
    }

    public boolean isSuspended() {
        return status == MemberStatus.SUSPENDED;
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

    private void requireActive() {
        if (status != MemberStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
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
