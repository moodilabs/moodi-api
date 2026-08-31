# 회원(Member) 컨텍스트

인증 · 온보딩 · 프로필 · 약관 · 사전조사를 담당하는 바운디드 컨텍스트 (기능명세서 `ONB`, `AUT`).
이 문서는 **인증 슬라이스(AUT-F01: 로그인 · 로그아웃 · 토큰 재발급)** 와 **온보딩(AUT-F03~F06 · ONB)** 설계 기준이다.
인증 슬라이스는 팀 레퍼런스 구현. 온보딩은 A(프로필·약관)·B(사전조사)·C(상태조회) 모두 구현 완료.

> 프로젝트 전역 규칙은 루트 `CLAUDE.md` 참고. 여기서는 **회원 컨텍스트 한정 규칙**만 다룬다.

## 아키텍처

헥사고날(포트/어댑터 전면)이 아니라 **레이어드 + 컨텍스트 최상위**. 외부 시스템만 포트로 격리한다.

```
com.moodi.member/
├── presentation/     # AuthController + 요청/응답 DTO
├── application/      # AuthService + OAuthClient(포트) + TokenPair/LoginResult
├── domain/           # Member, RefreshToken, OAuthProvider, MemberStatus + Repository(포트)
└── infrastructure/   # JPA 어댑터(포트 구현) + OIDC 검증/JWKS
```

- 의존 방향: `presentation → application → domain`, `infrastructure → 포트 구현(application·domain)`. **domain은 순수 POJO**(의존 없음).
- **회원/비회원 인증 분기는 공유 커널** → `com.moodi.shared.auth`에 둔다 (이 컨텍스트 밖).
- 컨텍스트 간 참조는 **ID만**. `member_id` 같은 값은 FK/JPA 연관관계로 걸지 않는다.

## 도메인 규칙

- **계정 식별 키는 `(provider, provider_id)`** — OIDC `sub` 기준. 로그인 조회는 항상 이걸로 한다.
- **`email`은 nullable + unique(있을 때만)** 인 보조 속성. 애플 이메일 가리기/미제공 케이스를 흡수하기 위함.
  - 명세 AUT-F01 "동일 이메일 차단"은 **email이 있을 때만** 적용하는 best-effort 규칙.
  - 화면정의서 `AUT-01`은 "수집된 이메일을 **계정 고유키**로 사용"이라고 적었지만, 같은 문서가
    Apple 이메일 가리기로 릴레이 주소가 오는 예외를 인정하고 있어 email은 고유키가 될 수 없다.
    **동작(동일 이메일·다른 provider 차단)은 정의서와 동일**하므로 구현은 그대로 두고 정의서 문구 정정을 요청한다.
- `status`: `PENDING`(온보딩 전) / `ACTIVE`. 최초 로그인 시 `PENDING`으로 자동 생성.
  - 프로필(`AUT-02`)과 약관(`AUT-03`)이 2단계라 `PENDING` 안에서도 진행도가 갈린다.
    `GET /members/me`의 `hasProfile`로 클라이언트가 되돌아갈 화면을 정한다.
- `nickname` 등 프로필 항목은 온보딩(AUT-F03~)에서 채운다. 인증 슬라이스에서는 null 허용.

## OAuth 검증 (Google · Apple)

- 클라이언트가 provider `id_token` 전달 → 백엔드가 **JWKS로 서명 검증** 후 자체 JWT 발급.
- Google·Apple 모두 RS256 JWT → 검증 로직 하나로 통일 (`OidcTokenVerifier`, `com.nimbusds:nimbus-jose-jwt`).
- 검증 순서: `kid`로 공개키 조회 → 서명 검증 → `iss`·`aud`·`exp` 검증 → `sub`, `email` 추출. 실패 시 `OAUTH_VERIFICATION_FAILED`.
- provider별 issuer/jwks는 `oauth.google.*`, `oauth.apple.*`로 주입. **`aud`(client-id)는 멀티 플랫폼 지원** — `GOOGLE_CLIENT_ID`/`APPLE_CLIENT_ID`에 콤마로 플랫폼별(iOS·Android·Web) client-id를 넣으면 그중 하나와 매칭 시 통과. 비어 있으면 audience 검증 생략.

## 인증 분기 (AOP · `shared/auth`)

- `@LoginRequired` (메서드/클래스): 없으면 **비회원 통과**, 있으면 유효 토큰 필수.
- `AuthInterceptor`: `Authorization: Bearer` 추출 → 검증 → `memberId`를 request attribute에 저장. 무효 시 401.
- `@AuthMember` (파라미터): 로그인 `memberId` 주입 → `logout(@AuthMember Long memberId)`.
- `WebConfig`: 인터셉터 · 리졸버 등록(`/api/**`).

## API 계약

| Method | Path | 인증 | Body → 응답 |
|---|---|---|---|
| POST | `/api/v1/auth/login` | 불필요 | `{ provider, idToken }` → `SuccessResponse<TokenResponse>` |
| POST | `/api/v1/auth/reissue` | 불필요 | `{ refreshToken }` → `SuccessResponse<TokenResponse>` |
| POST | `/api/v1/auth/logout` | `@LoginRequired` | 없음 → `204 No Content` |

```jsonc
// TokenResponse
{ "accessToken": "...", "refreshToken": "...", "tokenType": "Bearer", "isNewMember": true }
```

- 신규 로그인은 `isNewMember=true` → 클라이언트가 프로필 설정으로, 기존은 Feed로 분기(AUT-F01).

## 토큰 정책

- JWT(HS256, `io.jsonwebtoken:jjwt` 0.12.6), secret은 환경변수(`JWT_SECRET`, 32byte↑). claim: `sub=memberId`, `type=access|refresh`.
- 만료는 `jwt.access-token-expiry-ms`(짧게) / `jwt.refresh-token-expiry-ms`(길게)로 주입. access·refresh 모두 JWT이며 **refresh는 DB(`refresh_token`)에도 저장** → 회전·강제만료 가능.
- 재발급: refresh 파싱 → DB 조회(없음/만료 → `INVALID_REFRESH_TOKEN`) → 기존 삭제 후 새 pair 발급(회전).
- 로그아웃: 해당 회원 refresh **전체 삭제**(모든 기기). 기기별 로그아웃 필요 시 token 단건 삭제로 조정.

## 에러 코드

`ErrorCode` enum에 추가, 응답은 기존 ProblemDetail 포맷.

| 코드 | HTTP |
|---|---|
| `UNAUTHORIZED` | 401 |
| `OAUTH_VERIFICATION_FAILED` | 401 |
| `INVALID_REFRESH_TOKEN` | 401 |
| `DUPLICATE_EMAIL` | 409 |

## 영속성

- **PK는 UUID**. Hibernate `GenerationType.UUID`(orm.xml `<generated-value strategy="UUID"/>`)로 INSERT 시 생성 위임 → 애플리케이션에서 id를 채우지 않으므로 새 엔티티로 인식(merge 회피). 컨텍스트 간 참조도 UUID(`member_id`).
- 도메인은 순수 POJO. JPA 매핑은 `src/main/resources/META-INF/orm.xml`에만 `<entity>`로 추가.
- 스키마는 Flyway로 관리 → `V1__create_member.sql` (member, refresh_token). H2 테스트는 orm.xml 기반 create-drop이므로 **orm.xml과 마이그레이션이 항상 일치**해야 한다.
- 도메인 Repository는 순수 인터페이스(포트). `infrastructure/persistence`의 Spring Data 인터페이스가 `org.springframework.data.repository.Repository<T, ID>`(JpaRepository 아님)를 상속해 구현하고, 필요한 메서드만 노출한다.

## 테스트 (인증 슬라이스)

- 도메인: `MemberTest` · `RefreshTokenTest` — 팩토리·불변식·`isExpired`/회전 검증.
- 서비스: `AuthServiceTest` — 포트(OAuthClient · Repository · TokenProvider) mock. 신규·기존·이메일 중복·회전 케이스.
- 컨트롤러: 로그인·재발급은 `RestDocsSupport`, 로그아웃은 `AuthenticatedRestDocsSupport`로 문서화.
- Fixture: `MemberFixture` · `RefreshTokenFixture` (support).

## 온보딩 (AUT-F03~F06 · ONB) — A·B·C단계 구현 완료

인증 슬라이스 위에 얹는 온보딩. 로그인으로 만들어진 `PENDING` 회원을 프로필·약관으로 `ACTIVE` 승격시키고, 선호 무드를 사전조사로 수집한다.

### 흐름 · 단계 분할

```
로그인(F01) → 프로필(F03) → 약관·가입완료(F04/F05) → 사전조사(F06, 스킵가능) → Feed
              └────────── A ──────────┘               └──── B ────┘
스플래시 상태분기(ONB-F01) = C
```

| 단계 | 기능 | 로드맵 | 무드 의존 | 비고 |
|---|---|---|---|---|
| A 프로필+약관+가입완료 | F03/F04/F05 | 2주차 | ✕ | 로그인 슬라이스와 직결, 즉시 완결 |
| B 사전조사(선호 무드) | F06 | 3주차 | ○ | 무드 20종은 `MoodTag`로 확정됨 |
| C 스플래시 상태조회 | ONB-F01 | 5주차 | ✕ | `GET /members/me` 하나 |

### 도메인 모델 (Member 애그리거트 — 물리 매핑은 실용적 레이어드)

- **Member 확장**: `country`(ISO 3166-1 alpha-2, 예 `KR`) · `birthYear`(Integer) · `gender`(`Gender` = `MALE`/`FEMALE`/`OTHER`). `nickname`은 온보딩에서 채운다.
- **MemberAgreement**(신설): 약관 종류별 1행. `member_id · type · agreed · agreed_at`, unique `(member_id, type)`. `type` = `TERMS_OF_SERVICE`·`PRIVACY_POLICY`·`AGE_OVER_14`(필수 3) / `MARKETING`(선택).
- **MemberPreferredMood**(B, 신설): `member_id · mood`. 무드는 공유 커널 `com.moodi.shared.mood.MoodTag`(20종 닫힌 집합, 확정 완료).

### 불변식 · 규칙

- 닉네임 **`2~20자, [A-Za-z0-9_.]`** — 명세서 `AUT-F03` 기준 **한글 불가**. 중복은 서버 검증(`existsByNickname`) + `uk_member_nickname`으로 이중 보장.
- 만 14세 미만 가입 불가: `현재연도 − birthYear < 14` 차단. `birthYear`는 `1900~현재연도`. `AUT-F03/F04`
- 국가는 `Locale.getISOCountries()` 실존 코드만 허용.
- 필수 약관 3종 미동의 시 가입 완료 불가. 마케팅은 개별. `AUT-F04`
- 선호 무드는 **`0개 또는 3개 이상`**. 스킵 허용 → 미설정이 정상 상태. `AUT-F06` `FED-F02`
- 프로필과 약관은 **2단계로 분리**(명세 `AUT-F03` 출력 "프로필 저장 → 약관동의 진입"):
  - `Member.updateProfile(nickname, country, birthYear, gender, currentYear)` — `isPending()` 아니면 `ALREADY_ONBOARDED` → 닉네임·국가·연도·나이 검증 → 프로필 세팅. **상태는 `PENDING` 유지**.
  - `Member.activate()` — `isPending()` 아니면 `ALREADY_ONBOARDED`, `hasProfile()` 아니면 `PROFILE_REQUIRED` → `status` `ACTIVE`.
  - `currentYear`는 서비스가 `Clock`(`shared/config/ClockConfig`)으로 주입(domain 순수 유지).
- 프로필 단계는 뒤로가기 후 재제출이 가능하므로, **자기 자신의 닉네임은 중복으로 보지 않는다**(`MemberOnboardingService.validateNicknameAvailable`).

### 필드 유효성 검증 (서버)

명세상 형식 검증은 클라이언트 실시간, 닉네임 중복은 `[다음]` 클릭 시 서버 1회(`AUT-F03`). 그러나 **백엔드는 클라를 신뢰하지 않고 전 필드를 재검증**한다. 요청 DTO는 Bean Validation(`@Valid`)으로 1차, 도메인 불변식으로 2차.

요청 DTO의 Bean Validation은 **필드 존재 여부만**(`@NotBlank`/`@NotNull` → `INVALID_REQUEST`) 검증하고,
형식·범위 등 비즈니스 규칙은 전부 도메인에서 **전용 에러 코드**로 던진다. 규칙의 출처를 한 곳으로 모으기 위함.

| 필드 | 서버 검증 | 위반 시 | 검증 위치 |
|---|---|---|---|
| `nickname` | not blank | `INVALID_REQUEST` | `ProfileRequest` |
| `nickname` | 2~20자 · `^[A-Za-z0-9_.]+$` | `INVALID_NICKNAME` | `Member` |
| `nickname` | 중복 아님 (`existsByNickname`) | `DUPLICATE_NICKNAME` | `MemberOnboardingService` |
| `country` | not blank | `INVALID_REQUEST` | `ProfileRequest` |
| `country` | ISO 3166-1 alpha-2 실존 코드 | `INVALID_COUNTRY` | `Member` |
| `birthYear` | not null | `INVALID_REQUEST` | `ProfileRequest` |
| `birthYear` | `1900 ≤ birthYear ≤ 현재연도` | `INVALID_BIRTH_YEAR` | `Member` |
| `birthYear` | `현재연도 − birthYear ≥ 14` | `UNDERAGE` | `Member` |
| `gender` | not null · enum(`MALE`/`FEMALE`/`OTHER`) | `INVALID_REQUEST` | `ProfileRequest` |
| `agreements.{termsOfService, privacyPolicy, ageOver14}` | 모두 `true` | `REQUIRED_AGREEMENT_MISSING` | `MemberOnboardingService` |
| `agreements.marketing` | 존재만 필수, 값은 자유 | `INVALID_REQUEST` | `AgreementRequest` |
| — | 프로필 없이 약관 동의 호출 | `PROFILE_REQUIRED` | `Member` |
| `moods` (B) | not null · 원소는 유효 `MoodTag` | `INVALID_REQUEST` | `PreferredMoodRequest` |
| `moods` (B) | 중복 제거 후 크기 `0 또는 ≥3` | `INSUFFICIENT_MOOD_SELECTION` | `PreferredMoods` |

- 닉네임 중복 확인 API(`GET /nickname-availability`)는 UX용 사전 체크일 뿐, `POST /members/profile`에서 **한 번 더 검증**한다.
  조회와 저장 사이의 선점은 `uk_member_nickname` 위반을 잡아 `DUPLICATE_NICKNAME`으로 변환해 막는다(TOCTOU 방지).
- 중복 조회는 **자기 자신을 제외**한다(`existsByNicknameAndIdNot`). 프로필 단계는 뒤로가기 후 재제출이 가능하기 때문이다.
  사전 체크 API와 저장 API가 같은 기준을 써야 "사용 불가"로 보였다가 저장은 되는 불일치가 없다.
- 사용자 노출 문구(예: "이미 사용 중인 닉네임이에요")는 `ErrorCode.message`로 매핑한다.

### API 계약 (전부 `@LoginRequired` — `PENDING`도 통과. 컨트롤러 `MemberController` 신설)

| Method | Path | Body → 응답 | 단계 | 상태 |
|---|---|---|---|---|
| GET | `/api/v1/members/nickname-availability?nickname=` | → `{ available }` | A | ✅ |
| POST | `/api/v1/members/profile` | `{ nickname, country, birthYear, gender }` → `204` (PENDING 유지) | A | ✅ |
| POST | `/api/v1/members/agreements` | `{ termsOfService, privacyPolicy, ageOver14, marketing }` → `204` (PENDING→ACTIVE) | A | ✅ |
| GET | `/api/v1/members/me` | → `{ status, nickname, hasProfile, hasPreferredMood }` | C | ✅ |
| DELETE | `/api/v1/members/me` | 없음 → `204` (소프트 삭제) | D | ✅ |
| POST | `/api/v1/members/me/preferred-moods` | `{ moods: [ …0개 또는 ≥3 ] }` → `204` | B | ✅ |

### 에러 코드 (`ErrorCode` 추가)

사용자 노출 문구는 명세서 `AUT-F03` 인라인 메시지를 그대로 `ErrorCode.message`에 넣었다.

| 코드 | HTTP | 단계 | 상태 |
|---|---|---|---|
| `DUPLICATE_NICKNAME` | 409 | A | ✅ |
| `INVALID_NICKNAME` | 400 | A | ✅ |
| `UNDERAGE` | 400 | A | ✅ |
| `INVALID_BIRTH_YEAR` | 400 | A | ✅ |
| `INVALID_COUNTRY` | 400 | A | ✅ |
| `REQUIRED_AGREEMENT_MISSING` | 400 | A | ✅ |
| `PROFILE_REQUIRED` | 400 | A | ✅ |
| `ALREADY_ONBOARDED` | 409 | A | ✅ |
| `MEMBER_NOT_FOUND` | 404 | A·C | ✅ |
| `INSUFFICIENT_MOOD_SELECTION` | 400 | B | ✅ |

### 영속성

- `V9__add_onboarding.sql`: `member`에 `country`·`birth_year`·`gender` 컬럼 + `uk_member_nickname` 추가, `member_agreement` 신설. **`V1`은 건드리지 않는다.** (V2~V8은 spot·route가 점유)
- `V10__create_member_preferred_mood.sql`: `member_preferred_mood` 신설. 무드당 1행, `uk_member_preferred_mood_member_mood (member_id, mood)`.
- orm.xml에 `Member` 속성 3개 + `MemberAgreement` `<entity>` 추가 완료. orm ↔ 마이그레이션 일치 유지.
- 도메인 Repository 포트는 **단건 `save`만 선언**한다. `Repository<T, ID>`는 `saveAll`을 CRUD 메서드로 인식하지 못해 쿼리 파생을 시도하다 컨텍스트 로딩에 실패한다.

### 화면정의서 반영으로 확정된 것 (2026-08-23)

화면정의서 `ONB-01`~`ONB-04` · `AUT-01`~`AUT-05` 대조 결과 기존 미결 항목이 해소됐다.

- **사전조사 입력 형태**(B) — `AUT-05`가 "무드 이미지 **20종**을 2열 Grid로 제공, **20개 무드 태그 1:1 대응**"으로
  명시했다. 기존 가정이 맞았으므로 `{ moods: [...] }`(무드 값 직접 전달) 계약을 그대로 유지한다.
- **약관 저장 방식** — `AUT-03`이 요구하는 저장 항목은 "**항목별 동의 여부 + 동의 일시**"뿐이다.
  현 구현(종류별 1행, `member_agreement(member_id, type, agreed, agreed_at)`)으로 충족한다.
- **약관 전문 · 무드 이미지 · 국가 목록은 전부 클라이언트 담당**으로 확정(2026-08-23).
  서버는 약관의 **동의 여부만** 저장하고 전문은 갖지 않는다. `AUT-02`의 국가 `Popular`/`All Countries` 섹션과
  `AUT-05`의 이미지 20종도 클라이언트가 보유한다. → 관련 조회 API를 만들지 않는다.

무드 20종은 `com.moodi.shared.mood.MoodTag`로 확정 완료.

## 회원 탈퇴 (D) — 소프트 삭제

명세서에 행이 없지만 **Google Play가 계정 삭제 경로를 의무화**해 출시 요건으로 구현했다.
공개 안내 페이지는 `src/main/resources/static/account-deletion.html`
(→ `https://moodi.kr/account-deletion.html`).

### 왜 하드 삭제가 아닌가

`bookmark`·`route`가 `member(id)`를 FK로 참조하는데 **`ON DELETE CASCADE`가 없다**
(`fk_bookmark_member`·`fk_route_member`). 행을 지우려면 다른 컨텍스트의 데이터까지 지워야 한다.

> **루트 `CLAUDE.md`와 스키마가 어긋나 있다.** 문서는 "컨텍스트 간 참조는 ID만, JPA 연관관계를
> 경계 너머로 걸지 않는다"고 하는데 **DB FK는 실제로 걸려 있다**. 별도 정리 대상.

### 처리 내용 (`MemberWithdrawService`)

| 처리 | 대상 |
|---|---|
| 비움 | `email` · `nickname` · `country` · `birth_year` · `gender`, `status`는 `PENDING`으로 복귀 |
| 삭제 | `member_preferred_mood` · `member_agreement` · `refresh_token` |
| 유지 | `provider` · `provider_id`(복구용), `bookmark` · `route` · `feed_impression` · `pick_request` |

- `deleted_at`은 `V15__add_member_deleted_at.sql`. 루트의 `V12__add_route_deleted_at.sql` 선례를 따랐다.
- **`provider_id`를 남기는 것이 설계의 핵심이자 약점**이다. 같은 소셜 계정으로 다시 로그인하면
  `AuthService`가 `Member.restore()`로 복구하고 북마크·루트가 되살아난다. 대신 OIDC `sub`라는
  개인 식별자가 남으므로 **엄밀히는 삭제가 아니라 비활성화**다. 안내 페이지에 이 사실과
  "전부 삭제를 원하면 메일로 요청" 경로를 함께 적어 고지와 동작을 일치시켰다.
- 복구된 회원은 프로필이 비어 있어 로그인 응답 `isNewMember`가 `true`로 나간다 → 클라이언트가
  온보딩으로 분기(`AUT-F01`).
- access token 만료가 30분이라 탈퇴 직후 잠시 유효하다. `MemberQueryService.getMe`는
  탈퇴 회원을 `MEMBER_NOT_FOUND`로 막지만, 다른 컨텍스트 API는 만료로 자연 해소된다.

### 미결 (구현 전 확정 필요)

- **공유된 루트가 탈퇴 후에도 열린다** — `route.is_shared`가 그대로라 `publicId`를 아는 사람은
  계속 조회할 수 있다. 탈퇴 시 공유를 내릴지는 루트 컨텍스트(개발자 B) 협의 대상.
  현재는 안내 페이지에 "탈퇴 전 직접 삭제하라"고 고지해 두었다.
- **`DUPLICATE_EMAIL` 응답에 provider 포함** — `AUT-01` 에러 모달 문구가
  "This email is linked to **{provider}**. Log in with {provider} to continue." 라 클라이언트가 provider 값을 받아야 한다.
  현재 `AuthService.register()`는 `existsByEmail`(boolean)이라 provider를 알 수 없고,
  `BusinessException`에도 추가 데이터를 실을 슬롯이 없다.
  `findByEmail` 전환 + `BusinessException` 확장이 필요한데 **`shared/error` 변경이라 개발자 B와 합의 후 착수**한다.
- **무드 값 표기 통일** — 이 API는 `MoodTag` **enum 이름**(`NATURE`)을 받는다.
  `MoodTag.key`(`nature`)와 `fromKey()`가 있으나 `@JsonValue`/`@JsonCreator`가 없어 Jackson에 연결돼 있지 않다.
  spot 쪽 DTO는 `SpotDetailResponse.moodTags`가 `List<String>`, `BookmarkSpotResponse.moodTags`가 `List<MoodTag>`로
  갈려 있어 같은 무드가 엔드포인트마다 다른 표기로 나갈 소지가 있다.
  `MoodTag`는 공유 커널이므로 **개발자 B와 표기를 맞춘 뒤** 통일 여부를 결정한다.

### 화면ID 매핑 (화면정의서 기준)

기능ID(`ONB-F01` 등)와 화면ID(`ONB-01` 등)가 번호대가 다르므로 혼용하지 않는다.

| 화면ID | 화면 | 관련 API |
|---|---|---|
| `ONB-01` | 스플래시 | `GET /members/me` · `POST /auth/reissue` |
| `ONB-02`~`ONB-04` | 서비스 소개 슬라이드 3장 | 없음 (클라이언트 전용) |
| `AUT-01` | 로그인/회원가입 | `POST /auth/login` |
| `AUT-02` | 프로필 설정 | `GET /members/nickname-availability` · `POST /members/profile` |
| `AUT-03` | 약관 동의 | `POST /members/agreements` |
| `AUT-04` | 가입 완료 및 사전조사 안내 | 없음 |
| `AUT-05` | 사전조사 | `POST /members/me/preferred-moods` |
| `DSC-01`/`DSC-02`/`DSC-03` | Feed 메인 A / 메인 B / 비회원 | `GET /feed` · `GET /feed/popular-spots` |

> **주의** — 화면정의서가 Feed 화면을 `DSC`로 부르지만 기능명세서·엔드포인트는 `FED`(`/api/v1/feed`)를 쓴다.
> 정의서 내부에서도 `ONB-01`은 "[Feed] 탭", `AUT-01`은 "[DSC-01]"로 섞여 있다. 엔드포인트는 그대로 두고 매핑으로만 흡수한다.

## 주의

- 레이어 의존은 루트 `LayeredArchitectureTest`(ArchUnit)로 강제한다. `HexagonalArchitectureTest`는 이 교체로 제거됐다.
- 인증 컨트롤러/서비스는 REST Docs 스니펫(`auth/login`·`auth/reissue`·`auth/logout`)을 생성한다(2주차 팀 REST Docs 레퍼런스).
