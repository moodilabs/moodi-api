# 회원(Member) 컨텍스트

인증 · 온보딩 · 프로필 · 약관 · 사전조사를 담당하는 바운디드 컨텍스트 (기능명세서 `ONB`, `AUT`).
이 문서는 **인증 슬라이스(AUT-F01: 로그인 · 로그아웃 · 토큰 재발급)** 설계 기준이며, 팀 레퍼런스 구현이다.

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
- Google·Apple 모두 RS256 JWT → 검증 로직 하나로 통일 (`OidcTokenVerifier`).
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

- JWT(HS256), secret은 환경변수(`JWT_SECRET`, 32byte↑). claim: `sub=memberId`, `type=access|refresh`.
- access는 짧게 / refresh는 길게. **refresh는 DB(`refresh_token`)에 저장** → 회전·강제만료 가능.
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

- 도메인은 순수 POJO. JPA 매핑은 `src/main/resources/META-INF/orm.xml`에만 `<entity>`로 추가.
- 스키마는 Flyway로 관리 → `V1__create_member.sql` (member, refresh_token). H2 테스트는 orm.xml 기반 create-drop이므로 **orm.xml과 마이그레이션이 항상 일치**해야 한다.
- 도메인 Repository는 순수 인터페이스(포트). `infrastructure/persistence`의 Spring Data 인터페이스가 `org.springframework.data.repository.Repository<T, ID>`(JpaRepository 아님)를 상속해 구현하고, 필요한 메서드만 노출한다.

## 테스트 (인증 슬라이스)

- 도메인: `MemberTest` · `RefreshTokenTest` — 팩토리·불변식·`isExpired`/회전 검증.
- 서비스: `AuthServiceTest` — 포트(OAuthClient · Repository · TokenProvider) mock. 신규·기존·이메일 중복·회전 케이스.
- 컨트롤러: 로그인·재발급은 `RestDocsSupport`, 로그아웃은 `AuthenticatedRestDocsSupport`로 문서화.
- Fixture: `MemberFixture` · `RefreshTokenFixture` (support).

## 주의

- 이 구조를 적용하려면 루트의 `HexagonalArchitectureTest`를 **`LayeredArchitectureTest`로 교체**해야 한다. 안 그러면 새 레이어 구조가 ArchUnit에서 전부 실패한다.
- 인증 컨트롤러/서비스는 REST Docs 스니펫을 생성한다(2주차 팀 REST Docs 레퍼런스).
