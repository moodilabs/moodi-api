# 관리자 백엔드 (Admin) 스펙

화면설계서에 어드민 화면은 없다. 화면설계서의 Function Description에 "어드민에서 관리"로 명시된 항목과
운영에 필요한 최소 기능을 범위로 잡는다.

화면설계서가 요구하는 어드민 기능:

| 출처 | 요구 |
|---|---|
| MY-04 공지 | 등록·수정·삭제, 유형 선택, 노출/숨김 |
| MY-05 FAQ | 질문 유형·항목 등록·수정·삭제, 노출/숨김, 유형 순서·항목 순서 |
| MY-06 문의 | 문의 접수 확인, 답변 등록 (`Received → Answered`) |
| MY-07 약관 | 약관 항목·전문 등록·수정, 버전·시행일 관리 |
| COM-01-02/03 삭제된 스팟 | "스팟 정보 어드민 관리 필요" — 스팟 삭제/미노출 처리 |
| COM-05-01 인기 지역 | 최근 7일 검색 횟수 집계 (검색 로그 필요) |

## 0. 결론 요약

| 항목 | 결정 |
|---|---|
| 배포 단위 | **같은 Cloud Run 앱**, 경로 `/api/admin/**`. 별도 모듈 분리는 트래픽·인원상 과함 |
| 관리자 계정 | 회원(`member`)과 **완전히 분리된 `admin_account`** (아이디 `login_id` + 비밀번호). 회원 테이블에 role 추가하지 않음 |
| 인증 | 관리자 전용 JWT (`type=ADMIN_ACCESS`), `@AdminRequired` + `AdminAuthInterceptor`. Spring Security 미도입(현 구조 유지) |
| 코드 위치 | 관리자 **인증·감사·대시보드**만 새 컨텍스트 `admin`. 각 도메인의 관리 API는 **해당 컨텍스트 `presentation/admin/`** 에 둔다 (스팟은 B, 나머지 A) |
| 프론트 | Vercel 배포 + `admin.moodi.kr` 커스텀 도메인. `/api/admin/**`만 CORS 허용(`admin.cors.allowed-origins`, Vercel 프리뷰 `*.vercel.app` 포함), 쿠키 미사용 |

## 1. 아키텍처

### 1.1 왜 관리 API를 각 컨텍스트에 두는가
`admin` 컨텍스트가 회원·공지·스팟을 직접 다루면 `admin.application → member.application` 같은 **컨텍스트 간 application 의존**이 생긴다.
CLAUDE.md 규칙(컨텍스트 간 참조는 ID만)과 소유권(스팟은 B)에 어긋난다.

→ 관리 API는 데이터를 소유한 컨텍스트가 제공한다. `admin` 컨텍스트는 다음만 소유한다.
- 관리자 계정·로그인·토큰
- 감사 로그(누가 무엇을 바꿨나)
- 대시보드 집계(여러 컨텍스트 테이블을 **읽기 전용 네이티브 SQL**로 조회 — `PreferredMoodReaderAdapter` 패턴)

```
/api/admin/auth/**        → admin 컨텍스트
/api/admin/dashboard      → admin 컨텍스트 (read model)
/api/admin/members/**     → member 컨텍스트 presentation/admin
/api/admin/notices/**     → support 컨텍스트 presentation/admin
/api/admin/faqs/**        → support
/api/admin/policies/**    → support
/api/admin/inquiries/**   → support
/api/admin/spots/**       → spot 컨텍스트 (개발자 B)
```

### 1.2 인증 흐름
기존 `AuthInterceptor`는 `@LoginRequired`만 본다. 관리자용 애노테이션·인터셉터를 `shared/auth`에 추가한다(공유 커널 변경 → 합의).

```
shared/auth/
├── AdminRequired.java              @Target(TYPE, METHOD)
├── AdminAuthInterceptor.java       Bearer 토큰 → JwtProvider.parseAdminAccessToken → request attr "authAdminId"
├── AuthAdmin.java                  @AuthAdmin UUID adminId 아규먼트 리졸버 애노테이션
├── AuthAdminArgumentResolver.java
├── TokenType.java                  + ADMIN_ACCESS, ADMIN_REFRESH
└── JwtProvider.java                + issueAdminAccessToken / issueAdminRefreshToken / parseAdminAccessToken
```

- 회원 토큰으로 `/api/admin/**` 호출 → `type` 클레임 불일치 → `UNAUTHORIZED` 401. 반대도 마찬가지.
- 관리자 액세스 토큰 만료 30분 · 리프레시 12시간 (회원보다 짧게). 리프레시는 `admin_refresh_token` 테이블에 저장, 로그아웃 시 삭제.
- 비밀번호는 BCrypt(`spring-security-crypto`만 의존 추가, Security 필터체인은 안 씀).
- 로그인 실패 5회 → 계정 잠금 15분 (`admin_account.locked_until`).
- `WebConfig`에 `AdminAuthInterceptor`를 `/api/admin/**` 패턴으로 등록. 관리자 토큰 검증 실패는 인터셉터에서 바로 401.

### 1.3 최초 관리자
Flyway로 비밀번호를 심지 않는다. 앱 기동 시 `admin.bootstrap.login-id` / `admin.bootstrap.password`(env)가 설정돼 있고 `admin_account`가 비어 있으면 1명 생성(`AdminBootstrapRunner`, `ApplicationRunner`). 이후 관리자는 `SUPER`가 API로 추가.

### 1.3.1 초기 비밀번호 강제 변경 (V26)
아이디는 영문 소문자·숫자·밑줄 4~20자(`login_id`, V26에서 `email`을 rename — 기존 값은 `@` 앞부분). 새 계정(부트스트랩 포함)은 `password_change_required = true`로 시작하고 본인이 `PATCH /me/password`로 바꾸면(현재 비밀번호와 같은 값은 거부) `false`가 된다. `true`인 동안은 `admin/infrastructure/AdminPasswordChangeInterceptor`가 `/me`·`/me/password`·`/auth/logout` 외 요청을 `ADMIN_PASSWORD_CHANGE_REQUIRED` 403으로 막는다. 로그인·재발급·`/me` 응답에 `passwordChangeRequired`가 실려 프론트가 변경 모달을 강제한다.

### 1.4 권한
| Role | 범위 |
|---|---|
| `SUPER` | 전체 + 관리자 계정 관리 |
| `OPERATOR` | 콘텐츠(공지·FAQ·약관)·문의 답변·회원 조회. 회원 정지/탈퇴, 스팟 삭제 불가 |

`@AdminRequired(role = SUPER)`로 메서드 단위 제한. 인터셉터에서 role 클레임 비교.

### 1.5 감사 로그
`/api/admin/**`의 `POST/PUT/PATCH/DELETE` 성공 시 `admin_audit_log`에 기록. `AdminAuditInterceptor`(`afterCompletion`)에서 경로·메서드·adminId·응답 상태 저장. 요청 본문은 저장하지 않는다(개인정보). **`ADM-F05`(대시보드)와 함께 진행** — `ADM-F01`에서는 제외됨.

### 1.6 적용 현황 (ADM-F01 · F04 · F05)
- `shared/auth`: `TokenType.ADMIN_ACCESS/ADMIN_REFRESH`, `AdminRole`, `AdminPrincipal`, `@AdminRequired(role)`, `@AuthAdmin`, `AdminAuthInterceptor`, `AuthAdminArgumentResolver`, `JwtProvider` 관리자 토큰 발급/파싱. 회원 인터셉터는 `/api/admin/**`를 제외.
- CORS: `WebConfig.addCorsMappings` — `/api/admin/**`만, `ADMIN_ALLOWED_ORIGINS` env.
- `admin` 컨텍스트: 계정·로그인·재발급·로그아웃·내 정보·비밀번호 변경·계정 관리(SUPER). `V20__create_admin.sql`.
- `support/presentation/admin`: 공지·FAQ·약관 어드민 컨트롤러 3종.
- 문서: `src/docs/asciidoc/admin/index.adoc`, `./gradlew asciidoctorAdmin`.
- `ADM-F05`: `GET /api/admin/dashboard`(`DashboardReadModelAdapter` 네이티브 COUNT), `admin_audit_log` + `AdminAuditInterceptor`(`AdminAuditWebConfig`로 등록), `GET /api/admin/audit-logs`(SUPER). 탈퇴 사유 집계는 `MY-F02` 이후.

## 2. 관리자 인증 API

| Method | Path | Body | 응답 |
|---|---|---|---|
| `POST` | `/api/admin/auth/login` | `{ email, password }` | `{ accessToken, refreshToken, role }` |
| `POST` | `/api/admin/auth/reissue` | `{ refreshToken }` | 동일 |
| `POST` | `/api/admin/auth/logout` | – | 204 |
| `GET` | `/api/admin/me` | – | `{ id, email, name, role }` |
| `GET` | `/api/admin/accounts` (SUPER) | – | 목록 |
| `POST` | `/api/admin/accounts` (SUPER) | `{ loginId, name, role, password }` | 201 |
| `PATCH` | `/api/admin/accounts/{id}/status` (SUPER) | `{ status: ACTIVE\|DISABLED }` | 204 |

에러: `ADMIN_LOGIN_FAILED` 401 (아이디/비밀번호 불일치 — 어느 쪽인지 알려주지 않음), `ADMIN_PASSWORD_CHANGE_REQUIRED` 403 (초기 비밀번호 미변경), `ADMIN_ACCOUNT_LOCKED` 423, `ADMIN_FORBIDDEN` 403 (role 부족), `INVALID_REFRESH_TOKEN` 401 (기존).

## 3. 회원 관리 (member 컨텍스트)

| Method | Path | 설명 |
|---|---|---|
| `GET` | `/api/admin/members?keyword=&status=&provider=&cursor=&size=` | 목록. keyword는 닉네임·이메일 부분일치. status `PENDING\|ACTIVE\|SUSPENDED\|WITHDRAWN`(`WITHDRAWN`은 `deleted_at IS NOT NULL` 가상 상태) |
| `GET` | `/api/admin/members/{memberId}` | 상세: 프로필, 가입일, 약관 동의(유형·시각), 선호 무드, 북마크 수·루트 수·문의 수, 탈퇴 사유(탈퇴 시) |
| `PATCH` | `/api/admin/members/{memberId}/status` (SUPER) | `{ status: SUSPENDED \| ACTIVE, reason }` 정지/해제 |
| `POST` | `/api/admin/members/{memberId}/withdrawal` (SUPER) | 강제 탈퇴 — `MemberWithdrawService.withdraw()` 재사용, 사유 `ADMIN_FORCED` |
| `GET` | `/api/admin/members/stats?from=&to=` | 일별 가입·탈퇴 수 (대시보드 그래프용) |

### 도메인 변경
- `MemberStatus`에 `SUSPENDED` 추가. `Member.suspend(reason)` / `Member.unsuspend()`.
- 정지 회원 처리: `AuthService.login`·`reissue`에서 `MEMBER_SUSPENDED` 403. 이미 발급된 access 토큰은 30분 내 자연 만료(탈퇴와 동일 정책). 정지 시 리프레시 토큰 삭제.
- `WithdrawalReason`에 `ADMIN_FORCED` 추가.
- 목록·상세 집계는 `member/application/MemberAdminQueryRepository`(포트) + `infrastructure/persistence/MemberAdminQueryRepositoryImpl`(네이티브 SQL, bookmark/route/inquiry는 COUNT 서브쿼리).

```sql
-- V22__add_member_suspension.sql (적용됨)
ALTER TABLE member ADD COLUMN suspended_at TIMESTAMP, ADD COLUMN suspend_reason VARCHAR(200);
CREATE INDEX idx_member_created ON member (created_at DESC, id DESC);
```
- `ADM-F04` 적용: `member/presentation/admin/AdminMemberController`, `MemberAdminService`, `MemberAdminQueryRepositoryImpl`(네이티브 SQL). 탈퇴 사유 표시는 `MY-F02` 이후.

## 4. 공지 관리 (support)

| Method | Path | 설명 |
|---|---|---|
| `GET` | `/api/admin/notices?type=&visible=&cursor=&size=` | 숨김 포함 전체 |
| `GET` | `/api/admin/notices/{id}` | |
| `POST` | `/api/admin/notices` | `{ type, title, content, visible, publishedAt }` → 201 |
| `PUT` | `/api/admin/notices/{id}` | 전체 수정 |
| `PATCH` | `/api/admin/notices/{id}/visibility` | `{ visible }` |
| `DELETE` | `/api/admin/notices/{id}` | 하드 삭제 (앱 노출 이력 필요 없음) |

검증: `title ≤ 100`, `content ≤ 10,000`, `type` enum. `publishedAt` 미지정 시 오늘.

## 5. FAQ 관리 (support)

| Method | Path | 설명 |
|---|---|---|
| `GET` | `/api/admin/faqs` | 카테고리·항목 전체(숨김 포함) |
| `POST` | `/api/admin/faq-categories` | `{ name, visible }` — sortOrder는 마지막+1 |
| `PUT` | `/api/admin/faq-categories/{id}` | `{ name, visible }` |
| `PUT` | `/api/admin/faq-categories/order` | `{ ids: [3,1,2] }` 전체 순서 교체 |
| `DELETE` | `/api/admin/faq-categories/{id}` | 항목이 있으면 `FAQ_CATEGORY_NOT_EMPTY` 409 |
| `POST` | `/api/admin/faqs` | `{ categoryId, question, answer, visible }` |
| `PUT` | `/api/admin/faqs/{id}` | 카테고리 이동 포함 |
| `PUT` | `/api/admin/faq-categories/{id}/faqs/order` | `{ ids: [...] }` 카테고리 내 순서 |
| `DELETE` | `/api/admin/faqs/{id}` | |

순서 교체는 카테고리(또는 카테고리 내 항목) 전체 ID 배열을 받아 `sort_order = index`로 한 트랜잭션에 갱신. 누락·중복 ID → `INVALID_REQUEST`.

## 6. 약관 관리 (support)

| Method | Path | 설명 |
|---|---|---|
| `GET` | `/api/admin/policies?type=` | 버전 전체(미래 시행분 포함), `effectiveAt DESC`. `agreed`(동의한 회원 존재 여부)로 수정 가능 여부 판단 |
| `GET` | `/api/admin/policies/{id}` | 전문 |
| `POST` | `/api/admin/policies` | `{ type, version, content, effectiveAt }` → 201. `(type, version)` 중복 → `POLICY_VERSION_DUPLICATE` 409 |
| `PUT` | `/api/admin/policies/{id}` | **동의한 회원이 없는 버전(`agreed=false`)만 수정 가능**. 동의가 있으면 `POLICY_ALREADY_AGREED` 409 → 새 버전으로 등록 |
| `DELETE` | `/api/admin/policies/{id}` | `agreed=false`인 버전만 |

동의된 약관을 고치지 못하게 막는 이유: 회원이 동의한 시점의 문서가 보존돼야 한다(법적 근거). 오탈자 수정도 새 버전. 시행됐어도 아직 아무도 동의하지 않은 버전(최초 등록 직후 등)은 고칠 수 있다 — `member_agreement.policy_id` 참조 유무로 판단.

## 7. 문의 관리 (support)

| Method | Path | 설명 |
|---|---|---|
| `GET` | `/api/admin/inquiries?status=&topic=&cursor=&size=` | 기본 `RECEIVED` 우선·`createdAt DESC`. 항목에 회원 닉네임·이메일 포함(탈퇴 회원은 null) |
| `GET` | `/api/admin/inquiries/{id}` | 상세 + 첨부 읽기 URL + 회원 정보 |
| `PUT` | `/api/admin/inquiries/{id}/answer` | `{ content }` → 상태 `ANSWERED`, `answered_by`, `answered_at`. 재호출 시 덮어쓰기 |
| `GET` | `/api/admin/inquiries/count?status=RECEIVED` | 대시보드 미답변 수 |

- 답변 등록 시 회원에게 알림(푸시/이메일)은 알림 기능 자체가 범위 밖 → 앱 진입 시 목록 상태 배지로만 확인.
- 회원 닉네임·이메일은 `support/infrastructure/member/MemberSummaryReaderAdapter`(네이티브 SQL, `member_id IN (...)`)로 읽는다.

## 8. 스팟 관리 (spot 컨텍스트 · `ADM-F06` 적용됨)

COM-01-02/03 "This spot is no longer available"을 만들려면 어드민이 스팟을 비노출/삭제 처리할 수 있어야 한다.
기존 `SpotStatus`가 있으므로 상태 전이 API가 핵심.

| Method | Path | 설명 |
|---|---|---|
| `GET` | `/api/admin/spots?keyword=&status=&region=&cursor=&size=` | |
| `GET` | `/api/admin/spots/{id}` | 원본 TourAPI 데이터·무드 태그·설명·번역·북마크 수 |
| `PATCH` | `/api/admin/spots/{id}/status` | `{ status: HIDDEN \| ACTIVE \| DELETED, reason }` |
| `PUT` | `/api/admin/spots/{id}/moods` | 무드 태그 수동 보정 |
| `PUT` | `/api/admin/spots/{id}/description` | AI 설명 수동 수정 |
| `PATCH` | `/api/admin/spots/{id}/route-exclusion` | 루트 생성 후보 제외/포함 |

적용: `SpotStatus.HIDDEN/DELETED` 추가(앱은 PUBLISHED만 읽으므로 즉시 미노출), `V25__add_spot_status_reason.sql`,
`spot/presentation/admin/AdminSpotController`, `SpotAdminService`, `SpotAdminQueryRepositoryImpl`.
재생성·재동기화 트리거는 배치 프로필로 유지(후순위).

## 8.1 앱 API 요청 로그 (admin 컨텍스트)

`GET /api/admin/api-logs?memberId=&method=&path=&statusClass=&cursor=&size=` (SUPER). 앱 API(`/api/**`, 관리자 제외) 요청을
`admin/infrastructure/ApiRequestLogInterceptor`(order -10, 인증 인터셉터보다 앞이라 401도 남는다)가 결과와 무관하게 `api_request_log`(V27)에
한 줄씩 기록한다. 회원 ID는 `AuthInterceptor`가 남긴 속성을 완료 시점에 읽고, 본문·헤더·쿼리스트링은 저장하지 않는다.
조회는 `ApiRequestLogQueryRepositoryImpl`(네이티브 SQL, member LEFT JOIN으로 닉네임·이메일)로 ID 커서 페이징.
`admin.api-log.retention-days`(기본 30) 지난 행은 `ApiRequestLogPurgeScheduler`가 매일 04:30 KST 벌크 삭제.

## 9. 대시보드 (admin 컨텍스트, 읽기 전용)

`GET /api/admin/dashboard`

```json
{ "members": { "total": 1200, "active": 1100, "pending": 60, "suspended": 3, "withdrawn": 37, "newToday": 12, "newLast7Days": 80 },
  "content": { "spots": 3400, "bookmarks": 9800, "routes": 1200, "sharedRoutes": 210, "picks": 560 },
  "inquiries": { "received": 4, "answeredLast7Days": 11 },
  "withdrawalReasonsLast30Days": { "NOT_USED_MUCH": 10, "HARD_TO_USE": 3, ... } }
```
- `admin/infrastructure/DashboardReadModelAdapter` — 테이블별 COUNT 네이티브 SQL. 데이터가 커지면 일별 스냅샷 테이블로 전환(후순위).
- COM-05-01 "인기 지역"(최근 7일 검색 상위 6, 동일 사용자 반복 1회 집계)은 검색 로그(`search_log`)가 있어야 한다. 검색 API는 B 소유이므로 **로그 적재는 B, 집계 API는 discovery 또는 spot에서** — 이 문서 범위 밖, 항목만 기록.

## 10. 패키지 구조

```
src/main/java/com/moodi/admin/
├── presentation/
│   ├── AdminAuthController.java          /api/admin/auth/*, /api/admin/me
│   ├── AdminAccountController.java       /api/admin/accounts (SUPER)
│   ├── DashboardController.java          /api/admin/dashboard
│   └── dto/ AdminLoginRequest · AdminTokenResponse · AdminAccountRequest · AdminMeResponse · DashboardResponse
├── application/
│   ├── AdminAuthService.java             로그인·재발급·로그아웃·잠금
│   ├── AdminAccountService.java          계정 생성·상태 변경
│   ├── AdminTokenProvider.java           포트 (member의 TokenProvider와 대칭)
│   ├── PasswordEncoder.java              포트
│   ├── DashboardQueryService.java
│   ├── DashboardReadModel.java           포트: MemberStats · ContentStats · InquiryStats 조회
│   └── dto/
├── domain/
│   ├── AdminAccount.java                 email · passwordHash · name · role · status · failedLoginCount · lockedUntil · lastLoginAt
│   ├── AdminRole.java                    SUPER · OPERATOR
│   ├── AdminAccountStatus.java           ACTIVE · DISABLED
│   ├── AdminRefreshToken.java
│   ├── AdminAuditLog.java
│   ├── AdminAccountRepository.java · AdminRefreshTokenRepository.java · AdminAuditLogRepository.java
└── infrastructure/
    ├── persistence/ AdminAccountJpaRepository · AdminRefreshTokenJpaRepository · AdminAuditLogJpaRepository
    ├── DashboardReadModelAdapter.java    네이티브 SQL (member · bookmark · route · pick_request · inquiry · member_withdrawal)
    ├── BcryptPasswordEncoder.java
    ├── JwtAdminTokenProvider.java        shared JwtProvider 위임
    ├── AdminBootstrapRunner.java         최초 관리자 생성
    └── AdminAuditInterceptor.java        (WebConfig에서 /api/admin/** 등록)

각 컨텍스트 관리 API:
member/presentation/admin/AdminMemberController.java
member/application/MemberAdminService.java · MemberAdminQueryRepository.java(포트)
member/infrastructure/persistence/MemberAdminQueryRepositoryImpl.java

support/presentation/admin/{AdminNoticeController, AdminFaqController, AdminPolicyController, AdminInquiryController}.java
support/application/{NoticeAdminService, FaqAdminService, PolicyAdminService, InquiryAdminService}.java
support/application/MemberSummaryReader.java(포트) · support/infrastructure/member/MemberSummaryReaderAdapter.java

spot/presentation/admin/AdminSpotController.java (B)
```

ArchUnit: `presentation/admin`은 `..presentation..` 패턴에 이미 포함되므로 규칙 변경 없음.
컨텍스트 간 application 참조 금지 규칙은 현재 ArchUnit에 없다 — 이번에 `noClasses().that().resideInAPackage("com.moodi.admin..").should().dependOnClassesThat().resideInAnyPackage("com.moodi.member.application..", ...)` 형태로 추가하면 위 결정을 강제할 수 있다(선택).

## 11. DB 마이그레이션

```sql
-- V20__create_admin.sql · V23__create_admin_audit_log.sql (적용됨)
CREATE TABLE admin_account (
    id                 UUID         PRIMARY KEY,
    email              VARCHAR(255) NOT NULL,
    password_hash      VARCHAR(100) NOT NULL,
    name               VARCHAR(50)  NOT NULL,
    role               VARCHAR(20)  NOT NULL,
    status             VARCHAR(20)  NOT NULL,
    failed_login_count INT          NOT NULL DEFAULT 0,
    locked_until       TIMESTAMP,
    last_login_at      TIMESTAMP,
    created_at         TIMESTAMP    NOT NULL,
    updated_at         TIMESTAMP    NOT NULL,
    CONSTRAINT uk_admin_account_email UNIQUE (email)
);

CREATE TABLE admin_refresh_token (
    id         UUID         PRIMARY KEY,
    admin_id   UUID         NOT NULL REFERENCES admin_account (id),
    token      VARCHAR(512) NOT NULL,
    expires_at TIMESTAMP    NOT NULL,
    created_at TIMESTAMP    NOT NULL,
    updated_at TIMESTAMP    NOT NULL,
    CONSTRAINT uk_admin_refresh_token_token UNIQUE (token)
);
CREATE INDEX idx_admin_refresh_token_admin_id ON admin_refresh_token (admin_id);

CREATE TABLE admin_audit_log (
    id          BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    admin_id    UUID         NOT NULL,
    method      VARCHAR(10)  NOT NULL,
    path        VARCHAR(300) NOT NULL,
    status_code INT          NOT NULL,
    request_id  VARCHAR(64),             -- MDC requestId, 로그와 대조용
    created_at  TIMESTAMP    NOT NULL
);
CREATE INDEX idx_admin_audit_log_admin_created ON admin_audit_log (admin_id, created_at DESC);
```

## 12. 설정

```yaml
admin:
  jwt:
    access-token-expiry-ms: 1800000      # 30분
    refresh-token-expiry-ms: 43200000    # 12시간
  cors:
    allowed-origins: ${ADMIN_ALLOWED_ORIGINS:https://admin.moodi.kr,https://*.vercel.app,http://localhost:5173,http://localhost:3000}
  bootstrap:
    login-id: ${ADMIN_BOOTSTRAP_LOGIN_ID:}
    password: ${ADMIN_BOOTSTRAP_PASSWORD:}
  login:
    max-failures: 5
    lock-minutes: 15
```
JWT secret은 회원과 같은 `jwt.secret`을 쓰되 `type` 클레임으로 구분한다(키 분리는 후순위).

## 13. 에러 코드 추가

| 코드 | HTTP |
|---|---|
| `ADMIN_LOGIN_FAILED` | 401 |
| `ADMIN_ACCOUNT_LOCKED` | 423 |
| `ADMIN_ACCOUNT_DISABLED` | 403 |
| `ADMIN_FORBIDDEN` | 403 |
| `ADMIN_ACCOUNT_NOT_FOUND` | 404 |
| `DUPLICATE_ADMIN_EMAIL` | 409 |
| `MEMBER_SUSPENDED` | 403 |
| `FAQ_CATEGORY_NOT_EMPTY` | 409 |
| `FAQ_CATEGORY_NOT_FOUND` · `FAQ_NOT_FOUND` | 404 |
| `POLICY_VERSION_DUPLICATE` | 409 |
| `POLICY_ALREADY_AGREED` | 409 |

## 14. 문서·테스트

- REST Docs는 앱 문서(`app/index.adoc`)와 분리해 `src/docs/asciidoc/admin/index.adoc` 신설, Gradle 태스크 `asciidoctorAdmin` 추가(현재 `asciidoctorApp`과 대칭). 관리자 문서는 외부에 배포하지 않는다.
- 컨트롤러 테스트용 `AdminRestDocsSupport`(adminId 주입 + `AuthAdminArgumentResolver` 등록) — `AuthenticatedRestDocsSupport`와 대칭.
- 인터셉터 테스트: 회원 토큰으로 `/api/admin/**` → 401, `OPERATOR`로 SUPER 엔드포인트 → 403.

## 15. 구현 순서 제안

1. `ADM-F01` 관리자 계정·인증·인터셉터·부트스트랩 (shared/auth 변경 합의 선행)
2. `ADM-F02` 공지·FAQ·약관 관리 (support 읽기 API와 같은 PR로 묶어도 됨)
3. `ADM-F03` 문의 관리
4. `ADM-F04` 회원 관리 + 정지
5. `ADM-F05` 대시보드 · 감사 로그 조회
6. `ADM-F06` 스팟 관리 — 적용됨

## 16. 열린 질문 (합의 필요)

1. **탈퇴 시 북마크·루트 삭제** — 화면 정책 문구대로 삭제하기로 확정(mypage.md §3). B에게 spot·route 리스너 작업 요청 필요.
2. `shared/auth` 변경 — `ADM-F01` PR에서 추가만(회원 인증 동작 불변) 진행됨. B 리뷰 필요.
3. 관리자 접근 제한: 프론트는 `admin.moodi.kr`로 분리됐지만 API 자체는 `moodi.kr/api/admin/**`로 열려 있다(CORS는 브라우저만 막음). IP 허용 목록 또는 별도 API 호스트는 후순위.
4. 정지(`SUSPENDED`) 회원의 공유 루트 링크는 계속 열리는가 — 정책 미정, 기본은 열림.
5. 1:1 문의 첨부 버킷 생성·IAM(signBlob) — Pick 버킷과 동일한 준비 필요.


## 17. 2026-09-14 추가 요청 반영 (V29)

요청서 `[어드민] 추가 개발 및 기능 수정 사항`의 5개 항목을 반영한다.

| 항목 | 관리자 API | 앱 연동 |
|---|---|---|
| 운영 추천 루트 | `/api/admin/recommended-routes` 목록·상세·POST·PUT·DELETE, `PUT /order`, `PATCH /{id}/visibility` | `GET /api/v1/feed/recommended-routes`, 공개 스팟으로 구성된 노출 루트 최대 3개 |
| 추천 지역 | `/api/admin/recommended-areas` 목록·상세·POST·PUT·DELETE, `PUT /order` | `GET /api/v1/picks/recommended-areas`, 기존 지역 자동완성과 같은 지역 값 |
| 약관·정책 | 기존 API에 locale/enabled/visible 추가, `PATCH /api/admin/policies/{id}/publication` | `GET /api/v1/policies[/{type}]?locale=ko-KR`, 기본 en-US |
| 사전조사 이미지 | `/api/admin/survey-images` 목록·상세·POST·PUT·DELETE, `PUT /order` | `GET /api/v1/members/survey-images` |
| 회원 동의 버전 | 기존 회원 상세 agreements에 policyId/policyVersion/policyLocale 추가 | 동의 요청의 locale/policyIds 및 서버 현재 시행본 스냅샷 저장 |

관리 API는 `@AdminRequired`이며 OPERATOR 이상 권한이다. 변경은 기존 감사 인터셉터가 기록한다.
새 관리 콘텐츠는 별도 테이블이며 컨텍스트 간 FK를 추가하지 않는다. 스팟의 공개 상태는 등록과 앱 조회 때 검증한다.
사전조사 이미지는 관리자 스팟 상세의 images에서 선택하며 원본 이미지의 업로드·삭제 기능과는 별도다.

약관의 중복 기준은 `(type, version, locale)`로 변경한다. 언어는 `ko-KR`/`en-US`, 마케팅 문서용 `MARKETING` 유형을 지원한다.
기존 문서는 기존 영문 API 계약에 따라 en-US로 이관하고 공개·시행 활성 상태를 유지한다.
회원이 동의한 버전은 상태를 끄더라도 본문·버전·언어·시행일 수정 및 삭제가 불가하다.
과거 버전을 관리자 API로 보관·조회하며, 동의 기록의 약관 버전은 새 약관 등록으로 바뀌지 않는다.
기존 동의의 버전을 추정해 채우지 않으며 null로 남긴다. 현재 시행본이 없는 종류의 신규 동의도 기존 가입 호환성을 위해 null로 기록한다.
프론트는 조회한 문서 ID를 policyIds로 보내 동의 직전 버전 변경을 검출할 수 있다.

상세 요청·응답 예시는 `./gradlew asciidoctorApp asciidoctorAdmin` 문서에 포함된다.
마이그레이션은 `V29__admin_curated_content_and_policy_versions.sql`이며 배포 시 Flyway로 적용한다.
