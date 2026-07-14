# 회원(Member) 컨텍스트

인증 · 온보딩 · 프로필 · 약관 · 사전조사를 담당하는 바운디드 컨텍스트 (기능명세서 `ONB`, `AUT`).
이 문서는 **인증 슬라이스(AUT-F01: 로그인 · 로그아웃 · 토큰 재발급)** 와 **온보딩(AUT-F03~F06 · ONB)** 설계 기준이다.
인증 슬라이스는 팀 레퍼런스 구현, 온보딩은 설계 초안(미구현).

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
- `status`: `PENDING`(온보딩 전) / `ACTIVE`. 최초 로그인 시 `PENDING`으로 자동 생성.
- `nickname` 등 프로필 항목은 온보딩(AUT-F03~)에서 채운다. 인증 슬라이스에서는 null 허용.

## OAuth 검증 (Google · Apple)

- 클라이언트가 provider `id_token` 전달 → 백엔드가 **JWKS로 서명 검증** 후 자체 JWT 발급.
- Google·Apple 모두 RS256 JWT → 검증 로직 하나로 통일 (`OidcTokenVerifier`, `com.nimbusds:nimbus-jose-jwt`).
- 검증 순서: `kid`로 공개키 조회 → 서명 검증 → `iss`·`aud`·`exp` 검증 → `sub`, `email` 추출. 실패 시 `OAUTH_VERIFICATION_FAILED`.
- provider별 issuer/audience(client-id)/jwks는 `oauth.google.*`, `oauth.apple.*` 설정으로 주입.

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

## 온보딩 (AUT-F03~F06 · ONB) — 설계 초안, 미구현

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
| B 사전조사(선호 무드) | F06 | 3주차 | ○ | 무드 20종(공유 커널) 확정 대기 |
| C 스플래시 상태조회 | ONB-F01 | 5주차 | ✕ | `GET /members/me` 하나 |

### 도메인 모델 (Member 애그리거트 — 물리 매핑은 실용적 레이어드)

- **Member 확장**: `country`(ISO 3166-1 alpha-2, 예 `KR`) · `birthYear`(Integer) · `gender`(`Gender` = `MALE`/`FEMALE`/`OTHER`). `nickname`은 온보딩에서 채운다.
- **MemberAgreement**(신설): 약관 종류별 1행. `member_id · type · agreed · agreed_at`, unique `(member_id, type)`. `type` = `TERMS_OF_SERVICE`·`PRIVACY_POLICY`·`AGE_OVER_14`(필수 3) / `MARKETING`(선택).
- **MemberPreferredMood**(B, 신설): `member_id · mood`. 무드는 공유 커널 `Mood`(20종 닫힌 집합, **미정** — 1주차 산출물 대기).

### 불변식 · 규칙

- 닉네임 **`2~20자, [가-힣A-Za-z0-9_.]`** (한글 포함), 중복은 서버 검증(`existsByNickname`). `AUT-F03`
- 만 14세 미만 가입 불가: `현재연도 − birthYear < 14` 차단. `birthYear`는 `1900~현재연도`. `AUT-F03/F04`
- 필수 약관 3종 미동의 시 가입 완료 불가. 마케팅은 개별. `AUT-F04`
- 선호 무드는 **`0개 또는 3개 이상`**. 스킵 허용 → 미설정이 정상 상태. `AUT-F06` `FED-F02`
- `Member.completeOnboarding(nickname, country, birthYear, gender, currentYear)`: `isPending()` 아니면 `ALREADY_ONBOARDED` → 연도·나이 검증 → 프로필 세팅 → `status` `ACTIVE`. `currentYear`는 서비스가 주입(domain 순수 유지).

### 필드 유효성 검증 (서버)

명세상 형식 검증은 클라이언트 실시간, 닉네임 중복은 `[다음]` 클릭 시 서버 1회(`AUT-F03`). 그러나 **백엔드는 클라를 신뢰하지 않고 전 필드를 재검증**한다. 요청 DTO는 Bean Validation(`@Valid`)으로 1차, 도메인 불변식으로 2차.

| 필드 | 서버 검증 | 위반 시 |
|---|---|---|
| `nickname` | not blank · 2~20자 · `^[가-힣A-Za-z0-9_.]+$` | `INVALID_REQUEST` |
| `nickname` | 중복 아님 (`existsByNickname`) | `DUPLICATE_NICKNAME` |
| `country` | not blank · ISO 3166-1 alpha-2 (대문자 2자·실존 코드) | `INVALID_REQUEST` |
| `birthYear` | not null · `1900 ≤ birthYear ≤ 현재연도` | `INVALID_BIRTH_YEAR` |
| `birthYear` | `현재연도 − birthYear ≥ 14` | `UNDERAGE` |
| `gender` | not null · enum(`MALE`/`FEMALE`/`OTHER`) | `INVALID_REQUEST` |
| `agreements.{termsOfService, privacyPolicy, ageOver14}` | 모두 `true` | `REQUIRED_AGREEMENT_MISSING` |
| `agreements.marketing` | 선택(제약 없음) | — |
| `moods` (B) | 크기 `0 또는 ≥3` · 원소는 유효 `Mood` | `INSUFFICIENT_MOOD_SELECTION` |

- 닉네임 중복 확인 API(`GET /nickname-availability`)는 UX용 사전 체크일 뿐, `POST /onboarding`에서 **한 번 더 검증**한다(TOCTOU 방지).
- 사용자 노출 문구(예: "이미 사용 중인 닉네임이에요")는 `ErrorCode.message`로 매핑한다.

### API 계약 (전부 `@LoginRequired` — `PENDING`도 통과. 컨트롤러 `MemberController` 신설)

| Method | Path | Body → 응답 | 단계 |
|---|---|---|---|
| GET | `/api/v1/members/nickname-availability?nickname=` | → `{ available }` | A |
| POST | `/api/v1/members/onboarding` | `{ nickname, country, birthYear, gender, agreements: { termsOfService, privacyPolicy, ageOver14, marketing } }` → `204` (PENDING→ACTIVE) | A |
| GET | `/api/v1/members/me` | → `{ status, nickname, hasPreferredMood }` | C |
| POST | `/api/v1/members/me/preferred-moods` | `{ moods: [ …≥3 ] }` → `204` | B |

### 에러 코드 (`ErrorCode` 추가)

| 코드 | HTTP | 단계 |
|---|---|---|
| `DUPLICATE_NICKNAME` | 409 | A |
| `UNDERAGE` | 400 | A |
| `INVALID_BIRTH_YEAR` | 400 | A |
| `REQUIRED_AGREEMENT_MISSING` | 400 | A |
| `ALREADY_ONBOARDED` | 409 | A |
| `MEMBER_NOT_FOUND` | 404 | A·C |
| `INSUFFICIENT_MOOD_SELECTION` | 400 | B |

### 영속성

- `V2__add_onboarding.sql`: `member`에 `country`·`birth_year`·`gender` 컬럼 추가 + `member_agreement`(B에서 `member_preferred_mood`) 신설. **`V1`은 건드리지 않는다.**
- orm.xml에 `Member` 속성 3개 + `<entity>` 추가. orm ↔ 마이그레이션 일치 유지.

### 미결 (구현 전 확정 필요)

- **무드 20종 정의**(공유 커널 `Mood`) — B의 선결 조건. 1주차 산출물이나 아직 코드 미반영.
- **약관 저장 방식** — 종류별 1행(현 설계) vs append-only 이력. 법적 요건 확정 필요.

## 주의

- 레이어 의존은 루트 `LayeredArchitectureTest`(ArchUnit)로 강제한다. `HexagonalArchitectureTest`는 이 교체로 제거됐다.
- 인증 컨트롤러/서비스는 REST Docs 스니펫(`auth/login`·`auth/reissue`·`auth/logout`)을 생성한다(2주차 팀 REST Docs 레퍼런스).
