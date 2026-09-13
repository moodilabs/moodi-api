# 마이페이지 (MY) 백엔드 스펙

기준 화면: `MY-01 ~ MY-07` (Figma `이력서` 파일, 2026-08-10 작성분).
기존 구현: `AuthController`(로그인·재발급·로그아웃), `MemberController`(`/api/v1/members/*` 온보딩·`/me`·탈퇴).

## 0. 결론 요약

| 항목 | 결정 |
|---|---|
| 컨텍스트 | 계정 관련(§1~3)은 **Member** 확장, 고객지원 콘텐츠(§4~7)는 **새 컨텍스트 `support`** 신설 |
| 마이 메인 수치 | `GET /members/me/summary` 하나로 닉네임·국가·연결계정·이메일·북마크 수·루트 수 반환. `/me`는 스플래시 분기용으로 그대로 둠 |
| 테마·언어 | 클라이언트 로컬 저장. 서버 API 없음 |
| 알림(Notifications) | 화면설계 없음 → **이번 범위 제외** |
| 닉네임·국가 변경 | `PATCH /members/me/nickname`, `PATCH /members/me/country`. `ACTIVE` 회원도 변경 가능하도록 도메인 메서드 분리 |
| 회원탈퇴 | 화면 정책 문구대로 **저장한 스팟·생성한 루트를 영구 삭제**한다(현재 구현의 보존·복구 제거). 탈퇴 사유 저장. 엔드포인트는 `POST /members/me/withdrawal`로 교체 |
| 공지·FAQ·약관 | 비회원도 조회 가능(로그인 불필요). 어드민에서 등록·수정 |
| 1:1 문의 | 로그인 필수. 첨부는 Pick과 같은 서명 URL 업로드 방식 |

---

## 1. 마이 메인 (MY-01-01 / 01-02 / 01-03)

### 화면 요구사항
- 회원: `{Nickname}` · `{Country}` · `Saved spots N` · `My routes N` 표시. 프로필 영역 탭 → MY-02-01.
- 비회원: `Log in ∙ Sign up` CTA. Settings(Appearance·Language)와 Support 항목은 비회원도 노출 → §4~7 조회 API는 **로그인 불필요**.
- MY-01-03 테마(System/Light/Dark), Language: 기기 설정. 서버 저장 없음.

### API — 내 정보 요약

`GET /api/v1/members/me/summary` · `@LoginRequired`

```json
{
  "data": {
    "nickname": "moi",
    "country": "US",
    "provider": "GOOGLE",
    "email": "moi1234@naver.com",
    "savedSpotCount": 36,
    "routeCount": 6
  }
}
```

- MY-01-01(닉네임·국가·수치), MY-02-01(닉네임·국가·연결계정·이메일), MY-03-01(수치 재확인) 세 화면이 공유한다.
- `routeCount`는 `route.deleted_at IS NULL`인 본인 루트 수.
- `savedSpotCount`는 `bookmark` 행 수(스팟 status와 무관, MY-03-01의 "Spots you saved"와 일치).
- 탈퇴 회원 → `MEMBER_NOT_FOUND` 404 (`/me`와 동일).

### 컨텍스트 경계
북마크·루트 수는 다른 컨텍스트 데이터다. 기존 `discovery/infrastructure/member/PreferredMoodReaderAdapter` 패턴(포트 + 네이티브 SQL 어댑터)을 그대로 따른다.

```
member/application/BookmarkCountReader.java      // 포트: long countByMemberId(UUID)
member/application/RouteCountReader.java         // 포트: long countActiveByMemberId(UUID)
member/infrastructure/spot/BookmarkCountReaderAdapter.java   // SELECT COUNT(*) FROM bookmark WHERE member_id = :id
member/infrastructure/route/RouteCountReaderAdapter.java     // SELECT COUNT(*) FROM route WHERE member_id = :id AND deleted_at IS NULL
```

---

## 2. 계정설정 (MY-02-01 / 02-02 / 02-03)

### 화면 요구사항
- MY-02-01: Username·Country(수정 가능), Connected account·Email(읽기 전용), Log out(모달 확인 → `POST /auth/logout` 기존), Delete account(→ §3).
- MY-02-02 닉네임: 회원가입 입력 정책과 동일(2~20자, 영문·숫자·`_`·`.`). 기존과 같거나 정책 위반이면 Save 비활성. 저장 시 중복이면 "already taken" 에러 표시 후 버튼 비활성.
- MY-02-03 국가: 회원가입과 동일한 국가 선택 UI. 기존과 다른 값일 때만 Save 활성.

### API

| Method | Path | Body | 응답 | 에러 |
|---|---|---|---|---|
| `PATCH` | `/api/v1/members/me/nickname` | `{ "nickname": "moiaaaa_4141" }` | 204 | `INVALID_NICKNAME` 400 · `DUPLICATE_NICKNAME` 409 · `MEMBER_NOT_FOUND` 404 |
| `PATCH` | `/api/v1/members/me/country` | `{ "country": "CN" }` | 204 | `INVALID_COUNTRY` 400 · `MEMBER_NOT_FOUND` 404 |

- 중복 사전 체크는 기존 `GET /members/me/nickname-availability` 재사용 (실시간 체크 필요 시).
- 같은 값으로 PATCH 하면 204 (멱등). 클라이언트가 버튼을 막지만 서버는 거부하지 않는다.
- `PENDING` 회원(온보딩 미완료)이 호출하면 `INVALID_REQUEST` 400. 스플래시 분기가 이미 막고 있어 전용 에러 코드는 두지 않는다.

### 도메인 변경 (`Member`)
현재 `updateProfile()`은 `PENDING`에서만 동작한다(`ALREADY_ONBOARDED`). 온보딩과 분리한 변경 메서드를 추가한다.

```java
public void changeNickname(String nickname)   // requireActive() + validateNickname()
public void changeCountry(String country)     // requireActive() + validateCountry()
private void requireActive()                  // status != ACTIVE → INVALID_REQUEST
```

`MemberOnboardingService`가 아니라 새 `MemberProfileService`(application)에 둔다 — 온보딩 유스케이스와 섞지 않는다.

---

## 3. 회원탈퇴 (MY-03-01 / 03-02)

### 화면 요구사항
- 1단계: "Deleting your account will **permanently remove your saved spots and routes**" + 수치 표시 + 체크박스 동의.
  안내: "Routes saved by other users will remain in their accounts" (공유 → 복사된 루트는 유지).
- 2단계: 탈퇴 사유 **1개 이상 필수** (복수 선택). `Other` 선택 시 자유 입력(선택).
- 완료 후 로그인 화면(AUT-01)으로 이동 = 토큰 전부 무효.

### 정책 (화면 Function Description 그대로)

> **[BTN] Delete account** — 클릭 시 회원탈퇴 및 계정 데이터 삭제 처리
> - 사용자 계정에 저장된 스팟 및 생성한 루트 삭제
> - 로그인 세션 종료 및 비회원 상태로 전환
> - [AUT-01] 로그인/회원가입으로 이동
>
> 안내 문구: "Deleting your account will permanently remove your saved spots and routes" /
> "Routes saved by other users will remain in their accounts"

현재 `MemberWithdrawService`는 북마크·루트를 보존하고 같은 소셜 계정 재로그인 시 복구한다(`Member.restore()`).
이 동작은 정책 문구와 반대이므로 **제거**하고 아래로 바꾼다.

| 대상 | 처리 |
|---|---|
| `member` 행 | 개인정보 비움 + `deleted_at` (통계·탈퇴 사유 보존용, 정책 문구의 "계정 데이터 삭제"는 개인정보 삭제로 충족) |
| `member_preferred_mood` · `member_agreement` · `refresh_token` | 삭제 (세션 종료) |
| `bookmark` | **삭제** — "저장된 스팟 삭제" |
| `route` (+ day/spot/leg) | **소프트 삭제** (`deleted_at`) — "생성한 루트 삭제". 다른 회원이 복사한 루트는 그 회원 소유(`member_id`)라 영향 없음 — "Routes saved by other users will remain" |
| `pick_request` (+ area/result, GCS 원본 사진) | **삭제** — 개인 사진, "계정 데이터 삭제"에 포함 |
| `feed_impression` | 보존 (개인정보 아님, 30일 윈도우로 소멸) |
| 재로그인 | `Member.restore()`의 "이전 북마크·루트 복구" 의미는 사라진다. 같은 `provider·providerId` 행을 재활용해 `PENDING`으로 온보딩 시작 (사실상 신규 가입). 관련 Javadoc·`onboarding.adoc` 탈퇴 절 문구 갱신 |

`bookmark`·`route`·`pick_request`는 다른 컨텍스트 테이블이다. 삭제 순서를 DB가 막지 않으므로(V16) 애플리케이션이 책임진다.

**연동 방식: 도메인 이벤트**
```
shared/event/MemberWithdrawnEvent.java   (record: UUID memberId)   ← 공유 커널 추가
member/application/MemberWithdrawService  → ApplicationEventPublisher.publishEvent(...)
spot/application/BookmarkWithdrawalListener       @EventListener (같은 트랜잭션) → bookmarkRepository.deleteByMemberId
route/application/RouteWithdrawalListener         @EventListener → routeRepository.softDeleteByMemberId
discovery/application/PickWithdrawalListener      @EventListener → pickRequest 삭제 + @TransactionalEventListener(AFTER_COMMIT)로 GCS 삭제
```
- 같은 트랜잭션에서 실행(`@EventListener`)해 하나라도 실패하면 탈퇴 전체가 롤백된다. GCS 객체 삭제만 커밋 후, 실패 허용(재시도 배치는 후순위).
- spot·route 리스너까지 A가 함께 구현함(`BookmarkWithdrawalListener`, `RouteWithdrawalListener`, `PickWithdrawalListener`). `MemberWithdrawalIntegrationTest`로 이벤트 체인 검증.

### API

`POST /api/v1/members/me/withdrawal` · `@LoginRequired` · 204

```json
{
  "reasons": ["HARD_TO_USE", "OTHER"],
  "detail": "Tell us more (optional, ≤ 500자)"
}
```

- 기존 `DELETE /api/v1/members/me`는 사유 body를 받기 어색하므로(클라이언트가 DELETE body를 지우는 경우 있음) 교체한다. 스토어 계정삭제 안내 페이지(`account-deletion.html`)가 가리키는 경로가 있다면 함께 갱신.
- `reasons` 비어 있으면 `INVALID_REQUEST` 400. `detail`은 `OTHER` 없이도 허용.

### 도메인·DB

```java
enum WithdrawalReason { NOT_USED_MUCH, RECOMMENDATION_MISMATCH, HARD_TO_USE, FOUND_ANOTHER_APP, OTHER }
class MemberWithdrawal { UUID id; UUID memberId; Set<WithdrawalReason> reasons; String detail; }
```

```sql
-- V24__create_member_withdrawal.sql (적용됨)
CREATE TABLE member_withdrawal (
    id         UUID PRIMARY KEY,
    member_id  UUID          NOT NULL,
    detail     VARCHAR(500),
    created_at TIMESTAMP     NOT NULL,
    updated_at TIMESTAMP     NOT NULL
);
CREATE TABLE member_withdrawal_reason (
    withdrawal_id UUID        NOT NULL REFERENCES member_withdrawal (id),
    reason        VARCHAR(40) NOT NULL,
    PRIMARY KEY (withdrawal_id, reason)
);
CREATE INDEX idx_member_withdrawal_member_id ON member_withdrawal (member_id);
```
`reasons`는 orm.xml `<element-collection>`으로 매핑(`member_withdrawal_reason`). 회원이 재가입 후 다시 탈퇴할 수 있으므로 `member_id`는 unique 아님.

---

## 4. 공지사항 (MY-04-01 / 04-02)

### 화면 요구사항
- 목록: 등록일 기준 최신순. 제목 앞 `[유형]` 표시, 최대 2줄 말줄임. 본문 2줄 미리보기.
- 유형: `Announcement / Maintenance / Update / Issue / Event / Other`.
- 어드민에서 등록·수정·삭제, 노출/숨김. 앱에는 노출 상태만.
- 상세: 제목·날짜·본문 전체.

### API (로그인 불필요)

| Method | Path | 설명 |
|---|---|---|
| `GET` | `/api/v1/notices?cursor=&size=20` | 노출 공지 목록, `publishedAt DESC, id DESC` 커서 |
| `GET` | `/api/v1/notices/{noticeId}` | 상세. 숨김/삭제 → `NOTICE_NOT_FOUND` 404 |

```json
// 목록 항목
{ "id": 12, "type": "ANNOUNCEMENT", "title": "Route sharing is now available",
  "preview": "본문 앞 200자", "publishedAt": "2026-08-10" }
// 상세
{ "id": 12, "type": "ANNOUNCEMENT", "title": "...", "content": "본문 전체(plain text, 줄바꿈 \n)", "publishedAt": "2026-08-10" }
```
- 페이징은 기존 `PageResponse` + 커서 규칙(`shared/overview.adoc`)을 따른다.
- `preview`는 서버에서 본문 앞 200자를 잘라 내려준다(클라이언트 2줄 말줄임용).

---

## 5. FAQ (MY-05-01)

### 화면 요구사항
- 질문 유형(카테고리)별 그룹, 유형 순서·유형 내 순서는 어드민 설정. 노출 항목만.
- 아코디언(클라이언트). 화면의 유형 예: Account / Travel times / Routes.

### API (로그인 불필요)

`GET /api/v1/faqs`

```json
{ "data": { "categories": [
  { "id": 1, "name": "Account", "items": [
    { "id": 10, "question": "How do I change my username?", "answer": "..." } ] },
  { "id": 2, "name": "Travel times", "items": [ ... ] } ] } }
```
- 양이 적어 페이징 없음. 카테고리·항목 모두 `sortOrder ASC`. 항목이 0개인 카테고리는 제외.

---

## 6. 1:1 문의 (MY-06-01 / 06-02 / 06-03)

### 화면 요구사항
- 목록: 내 문의 최신순, 상태 배지 `Received`(접수) / `Answered`(답변완료).
- 작성: Topic(필수, 드롭다운) · Subject(필수 ≤ 60자) · Details(필수 ≤ 1000자) · Attachments(선택, 최대 5개, 사진·동영상·파일).
- Topic: `Account / Recommendations / Routes / Spot Information / Technical Issues / Feedback & Suggestions / Other`.
- 상세: 문의 내용 + 첨부 썸네일 + (답변완료 시) `Moodi Support` 답변·일시.
- 비회원의 Customer Service 진입은 화면설계 없음 → **로그인 필수**로 간다. 비회원은 로그인 유도.

### API · `@LoginRequired`

| Method | Path | 설명 |
|---|---|---|
| `GET` | `/api/v1/inquiries?cursor=&size=20` | 내 문의 목록, `createdAt DESC` |
| `GET` | `/api/v1/inquiries/{inquiryId}` | 상세 (본인 아니면 `INQUIRY_FORBIDDEN` 403) |
| `POST` | `/api/v1/inquiries` | 등록 → 201 `{ "id": "uuid" }` |
| `GET` | `/api/v1/inquiries/upload-url?contentType=image/jpeg` | 첨부 업로드용 서명 URL (Pick `GET /picks/upload-url`과 동일 방식) |

```json
// POST body
{ "topic": "ROUTES", "subject": "Can I add my own spot to a route?",
  "content": "...(≤1000)", "attachmentKeys": ["inquiries/{memberId}/{uuid}.jpg"] }
// 상세
{ "id": "...", "topic": "ROUTES", "subject": "...", "content": "...", "status": "ANSWERED",
  "createdAt": "2026-08-03T10:00:00",
  "attachments": [ { "url": "signed-read-url", "contentType": "image/jpeg" } ],
  "answer": { "content": "...", "answeredAt": "2026-08-04T09:00:00" } }
```

- 첨부 검증: 개수 ≤ 5, MIME `image/jpeg|png|heic`, `video/mp4|quicktime`, 그 외 파일은 `application/pdf`까지만. 크기 제한은 서명 URL 정책(Pick과 동일 10MB, 동영상 50MB)으로 GCS에서 막는다.
- 버킷: `moodi-inquiry-uploads` 분리(개인 사진·개인정보 포함 가능). `gcs.inquiry-upload` 설정 추가.
- 답변은 어드민이 등록(admin.md §7). 앱은 읽기만.

---

## 7. 약관 (MY-07-01 / 07-02)

### 화면 요구사항
- 목록: `Terms of Service` · `Privacy Policy`. 상세: 현재 적용 중인 최신 버전 전문.
- 어드민에서 버전 등록·수정, 시행일·버전 관리.
- 상세 화면에 글자 크기 토글(클라이언트).

### API (로그인 불필요)

| Method | Path | 설명 |
|---|---|---|
| `GET` | `/api/v1/policies` | `[ { "type": "TERMS_OF_SERVICE", "version": "1.2", "effectiveAt": "2026-08-01" }, ... ]` |
| `GET` | `/api/v1/policies/{type}` | 현재 적용본 `{ type, version, effectiveAt, content }` |

- "현재 적용본" = `effective_at <= today` 중 `effective_at DESC, version DESC` 첫 행. 미래 시행분은 어드민에만 보임.
- 기존 `static/privacy-policy.html`(스토어 등록용)은 별도 유지. 장기적으로는 정책 API 내용을 렌더링하도록 통합 가능(후순위).
- 회원가입 약관 동의(`member_agreement`)에 동의 시점 버전을 남기려면 `policy_version` 컬럼 추가가 필요하지만 **이번 범위 제외**(재동의 플로우 화면 없음).

---

## 8. 새 컨텍스트 `support` 패키지 구조

CLAUDE.md의 레이어드 + 컨텍스트 최상위 구조를 따른다. Route처럼 풀 DDD가 아닌 실용적 레이어드.
공지·FAQ·약관·문의는 서로 독립적이지만 "고객지원 콘텐츠"라는 한 팀(개발자 A)이 관리하고 어드민 CRUD가 주 용도라 한 컨텍스트로 묶는다.

```
src/main/java/com/moodi/support/
├── presentation/
│   ├── NoticeController.java            GET /api/v1/notices, /{id}
│   ├── FaqController.java               GET /api/v1/faqs
│   ├── PolicyController.java            GET /api/v1/policies, /{type}
│   ├── InquiryController.java           GET/POST /api/v1/inquiries, upload-url
│   ├── admin/                           ← 관리자용 (admin.md 참고, @AdminRequired)
│   │   ├── AdminNoticeController.java
│   │   ├── AdminFaqController.java
│   │   ├── AdminPolicyController.java
│   │   └── AdminInquiryController.java
│   └── dto/
│       ├── NoticeSummaryResponse.java · NoticeDetailResponse.java
│       ├── FaqResponse.java
│       ├── PolicySummaryResponse.java · PolicyDetailResponse.java
│       ├── InquiryCreateRequest.java · InquirySummaryResponse.java · InquiryDetailResponse.java
│       └── admin/ (AdminNoticeRequest 등)
├── application/
│   ├── NoticeQueryService.java · NoticeAdminService.java
│   ├── FaqQueryService.java · FaqAdminService.java
│   ├── PolicyQueryService.java · PolicyAdminService.java
│   ├── InquiryService.java (등록·내 목록·상세) · InquiryAdminService.java (목록·답변)
│   ├── InquiryAttachmentStorage.java    ← 포트: 업로드 URL 발급 / 읽기 URL 발급 / 삭제
│   ├── NoticeQueryRepository.java       ← 커서 목록용 조회 포트 (기존 BookmarkQueryRepository 패턴)
│   ├── InquiryQueryRepository.java
│   └── dto/
├── domain/
│   ├── Notice.java · NoticeType.java · NoticeRepository.java
│   ├── FaqCategory.java · Faq.java · FaqCategoryRepository.java · FaqRepository.java
│   ├── Policy.java · PolicyType.java · PolicyRepository.java
│   ├── Inquiry.java · InquiryTopic.java · InquiryStatus.java · InquiryAttachment.java · InquiryRepository.java
└── infrastructure/
    ├── persistence/
    │   ├── NoticeJpaRepository.java (Repository<Notice, Long> + 도메인 포트 구현)
    │   ├── NoticeQueryRepositoryImpl.java (네이티브 SQL 커서 조회)
    │   ├── FaqCategoryJpaRepository.java · FaqJpaRepository.java
    │   ├── PolicyJpaRepository.java
    │   ├── InquiryJpaRepository.java · InquiryQueryRepositoryImpl.java
    └── storage/
        └── GcsInquiryAttachmentStorage.java (discovery/infrastructure/storage의 Pick 구현 참고)
```

Member 컨텍스트 추가분:
```
src/main/java/com/moodi/member/
├── presentation/
│   ├── MemberController.java              + GET /me/summary · PATCH /me/nickname · PATCH /me/country · POST /me/withdrawal
│   └── dto/ MemberSummaryResponse · NicknameChangeRequest · CountryChangeRequest · WithdrawalRequest
├── application/
│   ├── MemberProfileService.java          (닉네임·국가 변경)
│   ├── MemberQueryService.java            + getSummary()
│   ├── MemberWithdrawService.java         (사유 저장 + 이벤트 발행)
│   ├── BookmarkCountReader.java · RouteCountReader.java   (포트)
│   └── dto/ MemberSummary · WithdrawalCommand
├── domain/
│   ├── Member.java                        + changeNickname() · changeCountry()
│   ├── MemberWithdrawal.java · WithdrawalReason.java · MemberWithdrawalRepository.java
└── infrastructure/
    ├── persistence/ MemberWithdrawalJpaRepository.java
    ├── spot/ BookmarkCountReaderAdapter.java
    └── route/ RouteCountReaderAdapter.java
```

공유 커널 추가(합의 필요):
```
shared/event/MemberWithdrawnEvent.java
```

## 9. DB 마이그레이션 (support)

기능 단위 PR마다 마이그레이션을 따로 낸다. `notice`는 `V17__create_notice.sql`, `faq_category`·`faq`는 `V18__create_faq.sql`, `policy`는 `V19__create_policy.sql`, `inquiry`·`inquiry_attachment`는 `V21__create_inquiry.sql`로 적용됨 (V20은 admin).

```sql
-- V17__create_notice.sql (적용됨)
CREATE TABLE notice (
    id           BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    type         VARCHAR(20)  NOT NULL,      -- ANNOUNCEMENT | MAINTENANCE | UPDATE | ISSUE | EVENT | OTHER
    title        VARCHAR(100) NOT NULL,
    content      TEXT         NOT NULL,
    visible      BOOLEAN      NOT NULL DEFAULT TRUE,
    published_at DATE         NOT NULL,      -- 목록 정렬·표시 날짜 (어드민이 지정, 기본 오늘)
    created_at   TIMESTAMP    NOT NULL,
    updated_at   TIMESTAMP    NOT NULL
);
CREATE INDEX idx_notice_visible_published ON notice (visible, published_at DESC, id DESC);

-- V18__create_faq.sql (적용됨)
CREATE TABLE faq_category (
    id         BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    name       VARCHAR(50) NOT NULL,
    sort_order INT         NOT NULL,
    visible    BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP   NOT NULL,
    updated_at TIMESTAMP   NOT NULL
);
CREATE TABLE faq (
    id          BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    category_id BIGINT       NOT NULL REFERENCES faq_category (id),
    question    VARCHAR(200) NOT NULL,
    answer      TEXT         NOT NULL,
    sort_order  INT          NOT NULL,
    visible     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP    NOT NULL,
    updated_at  TIMESTAMP    NOT NULL
);
CREATE INDEX idx_faq_category_sort ON faq (category_id, sort_order);

-- V19__create_policy.sql (적용됨)
CREATE TABLE policy (
    id           BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    type         VARCHAR(30) NOT NULL,       -- TERMS_OF_SERVICE | PRIVACY_POLICY
    version      VARCHAR(20) NOT NULL,
    content      TEXT        NOT NULL,
    effective_at DATE        NOT NULL,
    created_at   TIMESTAMP   NOT NULL,
    updated_at   TIMESTAMP   NOT NULL,
    CONSTRAINT uk_policy_type_version UNIQUE (type, version)
);
CREATE INDEX idx_policy_type_effective ON policy (type, effective_at DESC, id DESC);

-- V21__create_inquiry.sql (적용됨)
CREATE TABLE inquiry (
    id             UUID          PRIMARY KEY,
    member_id      UUID          NOT NULL,   -- 컨텍스트 경계: FK 없음
    topic          VARCHAR(30)   NOT NULL,
    subject        VARCHAR(60)   NOT NULL,
    content        VARCHAR(1000) NOT NULL,
    status         VARCHAR(20)   NOT NULL,   -- RECEIVED | ANSWERED
    answer_content TEXT,
    answered_by    UUID,                     -- admin_account.id, FK 없음
    answered_at    TIMESTAMP,
    created_at     TIMESTAMP     NOT NULL,
    updated_at     TIMESTAMP     NOT NULL
);
CREATE INDEX idx_inquiry_member_created ON inquiry (member_id, created_at DESC);
CREATE INDEX idx_inquiry_status_created ON inquiry (status, created_at DESC);

-- element-collection이라 별도 id 없이 (inquiry_id, sort_order)가 PK
CREATE TABLE inquiry_attachment (
    inquiry_id   UUID         NOT NULL REFERENCES inquiry (id),
    object_key   VARCHAR(500) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    sort_order   INT          NOT NULL,
    PRIMARY KEY (inquiry_id, sort_order)
);
```

- 문의 답변은 1:1이라 `inquiry` 컬럼에 접는다. 재답변은 덮어쓰기(이력 필요해지면 분리).
- 탈퇴 회원의 문의는 `member_id`만 남고 회원 정보는 비어 있다 — 어드민 목록에서 "탈퇴 회원"으로 표시.

## 10. 에러 코드 추가 (`ErrorCode`)

| 코드 | HTTP |
|---|---|
| `NOTICE_NOT_FOUND` | 404 |
| `POLICY_NOT_FOUND` | 404 |
| `INQUIRY_NOT_FOUND` | 404 |
| `INQUIRY_FORBIDDEN` | 403 |
| `INQUIRY_TOO_MANY_ATTACHMENTS` | 400 (5개 초과) |
| `INQUIRY_UNSUPPORTED_ATTACHMENT_TYPE` | 400 |
| `WITHDRAWAL_REASON_REQUIRED` | 400 |

## 11. 문서·테스트

- `src/docs/asciidoc/member/profile.adoc` (요약·닉네임·국가), `member/withdrawal.adoc`(기존 onboarding.adoc의 탈퇴 절을 옮김),
  `support/notice.adoc` · `faq.adoc` · `policy.adoc` · `inquiry.adoc` 신설 후 `app/index.adoc`에 include.
- 테스트: `support/support/*Fixture`, 도메인 단위(`Inquiry.answer()` 상태 전이, `Policy` 현재본 선택), 서비스 Mockito, 컨트롤러 `RestDocsSupport`/`AuthenticatedRestDocsSupport`.
- 탈퇴 리스너는 각 컨텍스트 테스트에서 이벤트 발행 후 삭제 검증(`RepositoryTestSupport`).

## 12. 구현 순서 제안

1. `MY-F01` 내 정보 요약 + 닉네임·국가 변경 (Member만, 타 컨텍스트 read 어댑터)
2. `MY-F02` 회원탈퇴 개편 (이벤트 + B와 리스너 합의)
3. `SUP-F01` 공지 · `SUP-F02` FAQ · `SUP-F03` 약관 (읽기 API + 어드민 CRUD 같이)
4. `SUP-F04` 1:1 문의 (GCS 버킷 준비 필요)
