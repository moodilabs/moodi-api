# 추천(Discovery) 컨텍스트

무드 기반 개인화 피드와 사진 기반 스팟 추천을 담당하는 바운디드 컨텍스트 (기능명세서 `FED`, `PCK`).
**설계 초안 — 미구현.** 패키지에 이 문서만 있다.

> 프로젝트 전역 규칙은 루트 `CLAUDE.md`, 회원 컨텍스트 규칙은 `member/CLAUDE.md` 참고.
> 여기서는 **추천 컨텍스트 한정 규칙**만 다룬다.

## 담당 범위

| 기능 ID | 화면 | 기능 |
|---|---|---|
| `FED-F01` | Feed 메인 A | 선호 무드 기반 개인화 추천 피드 |
| `FED-F02` | Feed 메인 B | 무드 미설정 사용자용 업로드 유도 + 인기 스팟 Top 5 |
| `PCK-F01` | Pick 메인 | 지역·이미지 입력받아 스팟 추천 요청 |
| `PCK-F02` | Pick 결과 | 지도·캐러셀로 추천 결과 조회 |
| `PCK-F03` | Pick 결과 | 추천 스팟으로 루트 생성/편집 진입 |

> **명세서 ID 오류 주의** — CSV에서 Pick 3개 기능이 전부 `PCK-F01`로 적혀 있다.
> 화면번호(`PCK-01`/`PCK-02`)로는 구분되므로 이 문서는 `F01`/`F02`/`F03`으로 나눠 부른다. 기획에 정정 요청 필요.

## 아키텍처

레이어드 + 컨텍스트 최상위. 다른 컨텍스트와 동일하다.

```
com.moodi.discovery/
├── presentation/     # FeedController · PickController + 요청/응답 DTO
├── application/      # 유스케이스 + 조회 전용 리포지토리 포트 + 외부 시스템 포트
├── domain/           # PickRequest 등 도메인 객체 + Repository 포트
└── infrastructure/   # JPA/네이티브 쿼리 어댑터 + 이미지 분석·저장 어댑터
```

### 다른 컨텍스트와 다른 점 — **자체 영속 데이터가 거의 없다**

추천은 대부분 **조회 조합**이다. `spot`의 콘텐츠 원장과 `member`의 선호 무드를 읽어 정렬·필터링해 내보낼 뿐,
Feed는 저장할 자체 상태가 없다. Pick만 업로드 이미지와 추천 결과라는 자체 상태를 가진다.

그래서 이 컨텍스트의 무게중심은 **도메인 모델이 아니라 조회 쿼리와 추천 정책**에 있다.
`route`처럼 풀 DDD로 가지 않고, 조회 전용 리포지토리를 `application`에 포트로 두고 `infrastructure`에서 구현한다.

## 컨텍스트 간 참조 규칙 — **가장 중요**

### ACL(부패 방지 계층) 패턴 — `route`의 선례를 그대로 따른다

`route`는 스팟 데이터가 필요할 때 이렇게 한다.

```
route/application/SpotSnapshotReader.java          ← 포트(인터페이스). spot을 모른다
route/application/SpotSnapshot.java                ← route 자신의 반환 타입
route/infrastructure/spot/SpotSnapshotReaderAdapter.java  ← 어댑터. 여기서만 spot.domain을 import
```

`discovery`도 동일하게 간다.

| 필요한 것 | 포트 (`discovery.application`) | 어댑터 (`discovery.infrastructure.spot` 등) |
|---|---|---|
| 무드 일치 스팟 목록 | `MoodSpotReader` | `spot.domain` 리포지토리 또는 네이티브 쿼리로 조회 |
| 인기 스팟 (북마크 수) | `PopularSpotReader` | `bookmark` 집계 |
| 회원 선호 무드 | `PreferredMoodReader` | `member_preferred_mood` 조회 |
| 이미지 → 무드 분석 | `MoodAnalysisClient` (자체 선언) | 외부 API 호출. `spot`과 같은 API를 써도 무방 |

**핵심은 `application`·`domain`이 다른 컨텍스트를 모르는 것이다.** 반환 타입도 `discovery` 자신의 record로 정의한다
(`spot.domain.Spot`을 그대로 반환하지 않는다). 다른 컨텍스트 import는 **`infrastructure`의 어댑터 파일에만** 등장해야 한다.

### ArchUnit은 이걸 잡아주지 못한다

`LayeredArchitectureTest`는 **레이어**만 검사하고 **컨텍스트 경계는 검사하지 않는다.**
같은 레이어끼리(`discovery.application` → `spot.application`)의 참조는 ArchUnit이 통과시키고,
`domain`은 `application`이 접근해도 되므로 `discovery.application` → `spot.domain`도 통과한다.

즉 **경계를 어겨도 빌드는 깨지지 않는다. 리뷰에서 잡아야 한다.**
(컨텍스트 경계를 강제하는 ArchUnit 규칙 추가는 팀 논의 대상.)

### 공유 커널은 그대로 쓴다

`com.moodi.shared.mood.MoodTag`·`MoodVector`, `shared.response.CursorResponse`,
`shared.auth`(`@LoginRequired`·`@AuthMember`·`@OptionalAuthMember`)는 레이어 밖이라 자유롭게 참조한다.

## Feed (FED-F01 · FED-F02)

### A/B 분기

분기 판단은 **클라이언트가 `GET /members/me`의 `hasPreferredMood`로** 한다 (`ONB-F01`).
서버는 A와 B를 별도 엔드포인트로 제공하고, 어느 쪽을 부를지는 클라이언트가 정한다.

| 상태 | 화면 | 엔드포인트 |
|---|---|---|
| 선호 무드 설정됨 | Feed 메인 A | `GET /api/v1/feed` |
| 미설정 · 사전조사 미참여 | Feed 메인 B | `GET /api/v1/feed/popular-spots` |

### FED-F01 개인화 피드

- 회원의 선호 무드(`member_preferred_mood`)와 `spot_mood.mood_tags`가 겹치는 스팟을 추천한다.
- **무한 스크롤**(`COM-P05`): 최초 20개, 이후 20개씩. `CursorResponse`로 커서 페이징한다.
- 당겨서 새로고침을 지원해야 하므로 **정렬이 안정적**이어야 한다. 매 요청 랜덤 정렬은 커서 페이징과 충돌한다.
- List Item에 필요한 필드: 대표 이미지 · 스팟명 · 지역 · 북마크 여부.
- 추천 소진 시 하단 탐색 유도 영역 → 서버는 `hasNext=false`로만 알리고 UI는 클라이언트가 처리.

### FED-F02 인기 스팟

- **북마크 수 기준 Top 5** 고정. 페이징 없음.
- 무드 미설정자용이므로 개인화 조건이 없다.

## Pick (PCK-F01 ~ F03)

### 흐름

```
Pick 메인(F01)                     Pick 결과(F02)              바텀시트(F03)
지역 1개↑ + 이미지 1~5장  →  추천 스팟 최대 5개  →  기존 루트에 추가 / 새 루트 추천
   [BTN] 추천받기                  지도 + 캐러셀            (RTE-F02로 위임)
```

### PCK-F01 추천 요청

- 입력: **지역 1개 이상**(자동완성 결과 중 선택, `COM-P03`) + **이미지 1~5장**.
- 업로드 이미지를 무드 벡터로 분석 → 선택 지역 내에서 유사 무드 스팟을 찾는다.
- 결과는 **최대 5개**.
- 실패 시 `COM-P06` 공통 서버 오류 모달 → 서버는 5xx 또는 비즈니스 예외로 응답하고 재시도 가능해야 한다.

### PCK-F02 결과 조회

- 캐러셀 아이템: 이미지 · 스팟명 · 지역 · 무드 태그 · 저장 버튼 · 루트에 추가 버튼.
- 결과 없음 → `[Empty State]`. 서버는 **빈 배열 + 200**으로 응답하고 404를 쓰지 않는다.

### PCK-F03 루트 연동

- "기존 코스에 추가"는 루트 목록 조회(`RTE-F01`)와 루트 편집(`RTE-F06`)을 호출한다.
- "이 스팟으로 새 루트 추천"은 루트 생성(`RTE-F02`)으로 위임한다.
- **추천 컨텍스트는 루트를 직접 만들지 않는다.** 스팟 ID만 넘긴다.

## 도메인 모델 (초안)

Feed는 자체 엔티티가 없다. Pick만 상태를 가진다.

- **PickRequest**(신설): 추천 요청 1건. `id · member_id · created_at`. 요청한 지역·이미지·결과를 묶는 애그리거트 루트.
- **PickRequestImage**(신설): 업로드 이미지. `pick_request_id · image_url · sort_order`.
- **PickRequestArea**(신설): 선택 지역. `pick_request_id · area · district`.
- **PickResultSpot**(신설): 추천 결과. `pick_request_id · spot_id · rank`.

> 결과를 저장하는 이유는 명세 `PCK-F02` 사전조건이 "추천받기 완료(**결과 생성됨**)"이고,
> `PCK-F03`에서 "모달 종료 시 기존 결과/카드/지도 위치 유지"를 요구하기 때문이다.
> **다만 결과를 서버에 저장할지 클라이언트가 들고 있을지는 미결**(아래 참고).

## 규칙 · 불변식

- 지역은 **1개 이상**, 자동완성 결과에서 고른 값만 허용. 자유 입력 불가 (`COM-P03`).
- 이미지는 **1장 이상 5장 이하** (`PCK-F01`).
- Pick 추천 결과는 **최대 5개** (`PCK-F02`).
- Feed 페이지 크기는 **20** (`COM-P05`).
- FED-F02 인기 스팟은 **정확히 Top 5**.
- 추천 목록에서 **비활성 스팟(`SpotStatus`)은 제외**한다. `spot` 원장 정책을 따른다.
- 비회원은 Feed 조회는 가능하나 저장(북마크)이 제한된다 (`AUT-F02`, `COM-P01`).
  → Feed 조회는 `@OptionalAuthMember`, 북마크 여부 필드는 비회원이면 항상 `false`.

## API 계약 (초안)

| Method | Path | 인증 | 요청 → 응답 | 기능 |
|---|---|---|---|---|
| GET | `/api/v1/feed?cursor=&size=20` | `@OptionalAuthMember` | → `CursorResponse<FeedSpotResponse>` | FED-F01 |
| GET | `/api/v1/feed/popular-spots` | `@OptionalAuthMember` | → `List<PopularSpotResponse>` (5개) | FED-F02 |
| POST | `/api/v1/picks` | `@LoginRequired` | `{ areas: [...], imageUrls: [...] }` → `{ pickId, spots: [...최대 5] }` | PCK-F01·F02 |
| GET | `/api/v1/picks/{pickId}` | `@LoginRequired` | → `{ pickId, spots: [...] }` | PCK-F02 재조회 |

- 이미지 업로드는 **별도 단계**로 본다. 클라이언트가 먼저 업로드하고 URL을 넘기는 방식(presigned URL)을 전제로 했다. → 미결.
- `POST /picks`가 생성과 조회 응답을 겸한다. 명세상 `[BTN] 추천받기` 한 번에 결과 화면까지 이동하므로 왕복을 줄인다.

## 에러 코드 (`ErrorCode` 추가 예정)

| 코드 | HTTP | 설명 |
|---|---|---|
| `PICK_NOT_FOUND` | 404 | 추천 요청 없음 |
| `PICK_FORBIDDEN` | 403 | 타인의 추천 결과 접근 |
| `PICK_INVALID_AREA_SELECTION` | 400 | 지역 미선택 |
| `PICK_INVALID_IMAGE_COUNT` | 400 | 이미지 0장 또는 6장 이상 |
| `PICK_ANALYSIS_FAILED` | 422 | 이미지 무드 분석 실패 (`COM-P06` 대상) |

추천 결과가 0건인 것은 **에러가 아니다.** 빈 배열 + 200으로 응답한다 (`PCK-F02` Empty State).

## 영속성

- 마이그레이션 번호는 **`V11` 이후**를 쓴다. `V1`~`V8`은 member·spot·route가, `V9`·`V10`은 온보딩이 점유했다.
- 도메인은 순수 POJO, JPA 매핑은 `META-INF/orm.xml`에만 `<entity>`로 추가한다.
- 도메인 Repository 포트는 **단건 `save`만 선언**한다. `Repository<T, ID>`는 `saveAll`을 CRUD 메서드로
  인식하지 못해 쿼리 파생을 시도하다 컨텍스트 로딩에 실패한다 (온보딩 구현 중 실제로 겪음).
- 조회 전용 쿼리는 도메인 Repository와 분리해 `application`에 `XxxQueryRepository` 포트로 둔다
  (`spot.application.BookmarkQueryRepository` 선례를 따른다).

## 테스트

- 도메인: 지역·이미지 개수 불변식, 결과 개수 상한.
- 서비스: 조회 포트·무드 분석 포트를 mock. 무드 교집합 정렬, 결과 0건, 비회원 북마크 필드.
- 컨트롤러: Feed는 `RestDocsSupport`(비회원 가능), Pick은 `AuthenticatedRestDocsSupport`.
- 쿼리 리포지토리: `RepositoryTestSupport` 또는 Testcontainers(`PostgresTestSupport`).
  **jsonb 무드 필터는 H2에서 재현되지 않으므로 Postgres 통합 테스트가 필요하다.**
- REST Docs 작성 후 `src/docs/asciidoc/discovery/`에 adoc 추가 + `app/index.adoc`에 include (루트 `CLAUDE.md` 체크리스트).

## 재사용 가능한 기존 자산

`application`에서 직접 호출하진 않되(어댑터 경유), **쿼리 로직과 접근 방식은 그대로 베낄 수 있다.**

| 자산 | 위치 | 참고 포인트 |
|---|---|---|
| ACL 어댑터 구조 | `route.application.SpotSnapshotReader` + `route.infrastructure.spot.SpotSnapshotReaderAdapter` | **이 패턴을 그대로 따른다.** 포트·반환 타입·어댑터 3종 세트 |
| 유사 무드 스팟 조회 | `spot.application.SpotDetailQueryRepository#findSimilarMoodSpots` | 무드 태그 교집합 쿼리가 이미 있다. FED-F01·PCK-F02의 원형 |
| 북마크 수 기준 인기 조회 | `spot.application.SpotDetailQueryRepository#findPopularSpotsByArea` | FED-F02 Top 5의 원형 |
| 북마크 수 집계·여부 조회 | `spot.application.BookmarkQueryRepository` | `countBySpotIds`·`findBookmarkedSpotIds` 패턴 |
| 이미지 → 무드 분석 | `spot.application.MoodAnalysisClient` | 포트 시그니처(`analyze(imageUrls, overview) → MoodVector`)를 그대로 본뜬다 |
| 무드 태그 규칙 엔진 | `shared.mood.MoodTagRuleEngine` | 공유 커널이라 **직접 사용 가능**. 벡터 → 태그 변환 |
| 커서 페이징 | `shared.response.CursorResponse` | 공유 커널. 그대로 사용 |

`spot`의 무드 필터는 `spot_mood.mood_tags`(jsonb)를 조회한다. 성능 이슈는 이슈 #21(GIN 인덱스 검증)에서 다루는 중이다.

## 미결 (구현 전 확정 필요)

- **이미지 업로드 방식** — 명세서에 업로드 API가 없다. presigned URL(GCS) vs multipart 직접 업로드.
  용량·확장자 제한, 보관 기간, 비용 정책 전부 미정. **PCK 착수의 선결 조건.**
- **추천 결과 저장 여부** — 서버에 저장(재조회·이력 분석 가능, 테이블 3개 추가) vs 응답만 하고 클라이언트 보관(단순, 새로고침 시 소실).
  명세 `PCK-F03`의 "모달 종료 시 결과 유지"는 클라이언트 상태로도 충족 가능하다.
- **FED-F01 정렬 기준** — "추천 알고리즘 결과"라고만 되어 있다. 무드 일치도 점수인지, 북마크 수 가중인지,
  최신순인지 미정. **당겨서 새로고침 시 결과가 바뀌어야 하는지**도 정해야 커서 설계가 가능하다.
- **선호 무드 다중 매칭 규칙** — 회원 선호 무드가 3개 이상일 때 OR(하나라도 겹침) vs 가중 점수. 후자면 정렬 기준과 함께 정의 필요.
- **PCK 지역 파라미터 형식** — `COM-P03` 자동완성이 시/도·구/군·동 단위다. `spot.area`/`district`/`neighborhood`와
  매핑 규칙 필요. `spot.application.RegionParser` 참고.
- **명세서 `PCK-F01` ID 중복** — 기획에 F01/F02/F03 정정 요청.

## 주의

- 레이어 의존만 `LayeredArchitectureTest`(ArchUnit)로 강제된다. **컨텍스트 경계는 자동 검증되지 않으니 리뷰에서 본다.**
- `spot`은 개발자 B 담당이다. 조회 쿼리를 복제하게 되므로 **인덱스·성능 변경은 공유**한다.
- 공유 커널(`shared`) 변경은 합의 후에만 (루트 `CLAUDE.md`).
