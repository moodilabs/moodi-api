# 추천(Discovery) 컨텍스트

무드 기반 개인화 피드와 사진 기반 스팟 추천을 담당하는 바운디드 컨텍스트
(기능명세서 `FED`·`PCK`, 화면정의서 `DSC-01`~`DSC-05`).

**현재 상태 (2026-08-30)** — Feed(`DSC-01`·`DSC-02`)는 구현 완료, Pick(`DSC-04`·`DSC-05`)은 미구현.
이 문서는 **화면정의서 `DSC` 기준으로 갱신**했다. 기능명세서(`FED`/`PCK`)와 어긋나는 곳은 화면정의서를 따르고,
차이는 "명세서와의 차이" 절에 남긴다.

> 프로젝트 전역 규칙은 루트 `CLAUDE.md`, 회원 컨텍스트 규칙은 `member/CLAUDE.md` 참고.
> 여기서는 **추천 컨텍스트 한정 규칙**만 다룬다.

## 담당 범위

| 화면 ID | 화면명 | 기능 ID | 상태 |
|---|---|---|---|
| `DSC-01` | Discover 메인 A — 선호 무드 설정 완료 사용자 | `FED-F01` | ✅ 구현 |
| `DSC-02` | Discover 메인 B — 선호 무드 미설정 사용자 | `FED-F02` | 🔶 부분 구현 (인기 스팟만) |
| `DSC-03` | Discover 메인 C — 비회원 | — | ⚠️ **화면정의서 `작성 중`**. 확정 전 |
| `DSC-04` | 추천 정보 입력 (스팟 추천 STEP 1) | `PCK-F01` | 🔶 업로드·추천 API 구현. 지역 자동완성은 이슈 #55 대기 |
| `DSC-04-02` | 추천 처리 로딩 | `PCK-F01` | ❌ 미구현 (클라이언트 전용) |
| `DSC-05-01` | 추천 결과 (스팟 추천 STEP 2) | `PCK-F02`·`PCK-F03` | 🔶 추천 API 구현. 루트 연동은 클라이언트가 RTE 호출 |
| `DSC-05-02` | 추천 결과 없음 (Empty State) | `PCK-F02` | ✅ 대체 추천 포함 |

> **명세서 ID 오류 주의** — CSV에서 Pick 3개 기능이 전부 `PCK-F01`로 적혀 있다.
> 화면 ID로는 구분되므로 이 문서는 `F01`/`F02`/`F03`으로 나눠 부른다. 기획에 정정 요청 필요.

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

추천은 대부분 **조회 조합**이다. `spot`의 콘텐츠 원장과 `member`의 선호 무드를 읽어 정렬·필터링해 내보낼 뿐이다.
Feed가 갖는 자체 상태는 노출 이력(`feed_impression`) 하나뿐이고, Pick만 업로드 이미지와 추천 결과라는 자체 상태를 가진다.

그래서 이 컨텍스트의 무게중심은 **도메인 모델이 아니라 조회 쿼리와 추천 정책**에 있다.
`route`처럼 풀 DDD로 가지 않고, 조회 전용 리포지토리를 `application`에 포트로 두고 `infrastructure`에서 구현한다.

## 컨텍스트 간 참조 규칙 — **가장 중요**

### ACL(부패 방지 계층) 패턴 — `route`의 선례를 그대로 따른다

```
route/application/SpotSnapshotReader.java                 ← 포트(인터페이스). spot을 모른다
route/application/SpotSnapshot.java                       ← route 자신의 반환 타입
route/infrastructure/spot/SpotSnapshotReaderAdapter.java  ← 어댑터. 여기서만 spot.domain을 import
```

`discovery`도 동일하게 간다. 구현된 Feed가 이미 이 형태다.

| 필요한 것 | 포트 (`discovery.application`) | 어댑터 위치 |
|---|---|---|
| 피드 스팟 목록 | `FeedSpotReader` ✅ | `infrastructure/persistence/FeedSpotReaderAdapter` |
| 인기 스팟 (북마크 수) | `PopularSpotReader` ✅ | `infrastructure/persistence/PopularSpotReaderAdapter` |
| 회원 선호 무드 | `PreferredMoodReader` ✅ | `infrastructure/member/PreferredMoodReaderAdapter` |
| Pick 후보 스팟 (지역+무드) | `PickCandidateReader` ✅ | `infrastructure/persistence/PickCandidateReaderAdapter` |
| 지역 자동완성 | `AreaSuggestReader` (신설) | `infrastructure/spot/` — `spot`의 검색 자산 재사용 |
| 이미지 → 무드 분석 | `MoodAnalysisClient` ✅ | `infrastructure/mood/SpotMoodAnalysisClientAdapter` — `spot`의 분석기에 위임 |
| 이미지 업로드 URL 발급 | `ImageStorageClient` ✅ | `infrastructure/storage/GcsImageStorageClient` |
| 선호 벡터 갱신 | `PreferredVectorWriter` (신설) | `infrastructure/member/` — ⚠️ **`member` 상태를 쓴다.** 아래 참고 |

**핵심은 `application`·`domain`이 다른 컨텍스트를 모르는 것이다.** 반환 타입도 `discovery` 자신의 record로 정의한다
(`spot.domain.Spot`을 그대로 반환하지 않는다). 다른 컨텍스트 import는 **`infrastructure`의 어댑터 파일에만** 등장해야 한다.

### ArchUnit은 이걸 잡아주지 못한다

`LayeredArchitectureTest`는 **레이어**만 검사하고 **컨텍스트 경계는 검사하지 않는다.**
즉 **경계를 어겨도 빌드는 깨지지 않는다. 리뷰에서 잡아야 한다.**
(컨텍스트 경계를 강제하는 ArchUnit 규칙 추가는 팀 논의 대상.)

### 지역명 표기 — 원장은 한국어, 응답은 영문

`spot` 원장은 지역을 한국어로 저장하고(`spot.area`·`district`), 응답은 `RegionDictionary`로 영문 변환해 내보낸다
(`SpotDetailReader`·`SpotSearchService`·`BookmarkService`). **`discovery`도 같은 표기를 따른다.**

- **읽기** — 어댑터에서 `area`를 영문으로 바꿔 내보낸다. `district`는 discovery 응답에 노출되지 않아 변환하지 않는다.
- **쓰기** — `POST /picks`의 지역 조건은 클라이언트가 자동완성에서 받은 **영문 지역명**으로 되돌아온다.
  원장은 한국어라 조회 전에 되돌려야 한다. 이 변환이 없으면 **필터가 아무것도 매칭하지 못한다.**
- **동·면(neighborhood)은 사전이 없어 한국어 그대로** 비교한다. 사전이 생기면 여기도 맞춰야 한다.

참조는 `discovery/infrastructure/region/RegionNames`에만 둔다(컨텍스트 경계).

> **`RegionDictionary`는 이제 두 컨텍스트가 함께 쓴다.** 성격상 `shared`로 옮기는 편이 맞지만
> 공유 커널 변경은 합의가 필요해 지금은 `spot.application`에 두고 위임한다. **팀 논의 대상.**

### 공유 커널은 그대로 쓴다

`shared.mood.MoodTag`·`MoodVector`, `shared.response.CursorResponse`,
`shared.auth`(`@LoginRequired`·`@AuthMember`·`@OptionalAuthMember`)는 레이어 밖이라 자유롭게 참조한다.

## A / B / C 분기 (`DSC-01` · `DSC-02` · `DSC-03`)

분기 판단은 **클라이언트가 `GET /members/me`의 `hasPreferredMood`로** 한다 (`ONB-F01`).
서버는 각 화면에 필요한 데이터를 별도 엔드포인트로 제공한다.

| 상태 | 화면 | 구성 | 엔드포인트 |
|---|---|---|---|
| 회원 · 선호 무드 있음 | `DSC-01` 메인 A | 개인화 무한 피드 (Masonry 2열) | `GET /feed` ✅ |
| 회원 · 선호 무드 없음 | `DSC-02` 메인 B | 인기 스팟 캐러셀 + **추천 루트 캐러셀** | `GET /feed/popular-spots` ✅ + 추천 루트 ❌ |
| 비회원 | `DSC-03` 메인 C | 가입 유도 CTA + 인기 스팟 + 추천 루트 | ⚠️ 미확정 |

### ⚠️ 비회원 피드 — 기존 결정과 화면정의서가 충돌한다

기존 결정(2026-08-03)은 **"비회원에게 개인화 없는 전체 스팟 무한 피드"**였고 `GET /feed`가
`@OptionalAuthMember`로 그렇게 구현돼 있다. 그런데 `DSC-03` 화면은 무한 피드가 아니라
**메인 B와 같은 구성(가입 유도 CTA + Popular Spots + Recommended Route)** 이다.

`DSC-03`은 화면정의서 상태가 `작성 중`이고 Function/Button Description이 전부 `DESCRIPTION` 플레이스홀더라
**확정된 스펙이 아니다.** 기획 확정 전까지 현재 구현(전체 피드)을 유지하되, 확정되면 다음 중 하나를 택한다.

- 화면정의서를 따른다 → `GET /feed`의 비회원 분기와 게스트 정렬(B) 로직을 걷어내고 `@LoginRequired`로 좁힌다.
  대신 `GET /feed/popular-spots`를 `@OptionalAuthMember`로 넓힌다.
- 기존 결정을 유지한다 → 화면정의서에 비회원 무한 피드를 반영 요청한다.

### 🔴 선호 벡터 — Pick이 Member 상태를 바꾼다 (신규 요구)

`DSC-02`·`DSC-04`에 이런 문장이 있다.

> `DSC-02`: 사진을 업로드하여 스팟 추천을 1회 이상 받으면 해당 사용자의 **선호 벡터를 설정**하여 `DSC-01` 메인 A로 전환
> `DSC-04`: 업로드 사진에서 **사용자 선호 벡터 갱신** → 선호 벡터가 없는 사용자는 해당 사진이 **초기 시드**가 되어 메인 A로 전환

즉 **사전조사(`AUT-F05`)만이 아니라 Pick 실행도 A/B 분기 조건이 된다.** 파장이 크다.

1. 현재 `member`는 선호 **무드 태그**(`member_preferred_mood`, 20종)만 갖고 있고 **벡터는 없다.**
   `MoodVector`는 `spot` 컨텍스트에만 있다(`spot_mood`).
2. `GET /members/me`의 `hasPreferredMood`가 "사전조사 완료 여부"가 아니라
   "**개인화 가능 여부**(사전조사 OR Pick 1회 이상)"로 의미가 바뀐다.
3. 피드 정렬 근거가 태그 교집합에서 벡터 유사도로 옮겨갈 수 있다(아래 "정렬" 참고).

**미결.** 확정 전까지 구현하지 않는다. 정해야 할 것:
- 벡터를 `member`에 둘지(`member_preferred_mood_vector`) `discovery`에 둘지 — **`member` 권장.**
  분기 판단(`GET /members/me`)이 `member` 소관이고, 컨텍스트 간 상태 소유가 갈리면 정합성이 깨진다.
- 갱신 규칙: 최신 사진으로 **덮어쓰기** vs 기존 벡터와 **가중 평균**(EMA) vs 최근 N장 평균
- 사전조사 태그 20종 ↔ 벡터의 관계. 둘 다 있으면 무엇이 우선인가
- 정렬 근거를 태그 일치 수에서 벡터 유사도로 바꿀 것인가

## Feed 메인 A (`DSC-01` · `FED-F01`) — ✅ 구현됨

### 화면정의서 요구

- **UI**: Masonry Layout 2열
- **무한 스크롤**: 최초 20개, 목록 하단 도달 시 20개씩 추가 조회
- **조회 순서**: 선호 무드 유사도 내림차순. **유사도 컷오프 없이 전체 스팟을 대상으로 조회**
- 전체 스팟 소진 시 목록 하단에 종료 안내 → 추가 조회 요청 미발생 (서버는 `hasNext=false`로만 알린다)
- **당겨서 새로고침** 지원 — 갱신 완료 시 피드 최상단에서 재조회
- **List Item 구성**: 대표 이미지 1장 · 지역 정보(**동, 시**) · 저장 아이콘 버튼
- **저장 버튼**: 클릭 시 저장 / 재클릭 시 해제. 아이콘 상태 변경(Default ↔ Selected).
  저장 완료 시 토스트, 해제 시 토스트 미표시. 저장된 스팟은 `COM-00` 저장한 스팟에서 조회
  → 저장은 `spot` 컨텍스트의 북마크 API(`COM-F`, 개발자 B)를 그대로 쓴다. Feed는 `bookmarked` 여부만 실어 보낸다.

### ⚠️ 구현과의 차이 — "유사도" vs "무드 일치 수"

현재 `FeedSpotReaderAdapter`는 `rank_score`를 **선호 무드 태그와 스팟 무드 태그의 교집합 개수**로 계산한다
(`matchCountExpression`). 화면정의서는 "선호 무드 **유사도**"라고 쓰고, `DSC-05`는 "**무드 벡터 유사도** 매칭"이라고 쓴다.

- 태그 일치 수는 **정수 등급**이라 동점이 많이 생기고, 그 안은 셔플로 갈린다.
- 벡터 유사도는 **연속값**이라 순서가 촘촘하지만, 회원 선호 **벡터가 아직 없다**(위 "선호 벡터" 참고).

**Feed는 미결로 남아 있고, Pick은 벡터 유사도로 구현했다 (2026-08-30).**
Pick은 상위 5개만 뽑으므로 정수 등급으로는 동점이 너무 많이 생겨 순서를 가릴 수 없다.
Feed는 무한 스크롤이라 동점을 셔플로 갈라도 되고, 회원 선호 **벡터가 아직 없어**(위 "선호 벡터" 참고)
당장 바꿀 수단도 없다. 선호 벡터가 도입되면 Feed의 `rank_score`도 코사인 유사도로 교체할 수 있다.

### 정렬 — 커서 페이징과 "당겨서 새로고침"을 동시에 만족시켜야 한다

**`spot`의 유사 무드 쿼리를 그대로 가져오면 안 된다.** `findSimilarMoodSpots`는 `ORDER BY RANDOM()`이다.
스팟 상세의 "랜덤 5개"(`COM-F03`)용이라 단발 조회에는 문제가 없다.

피드는 무한 스크롤이라 **매 요청마다 순서가 새로 섞이면 같은 스팟이 여러 페이지에 중복 노출되거나 아예 누락된다.**
커서 페이징이 성립하려면 **한 번 스크롤하는 동안은 순서가 고정**돼야 한다.
반면 화면정의서는 "당겨서 새로고침"을 요구한다. 완전 고정이면 새로고침해도 목록이 그대로다.

즉 요구가 둘이다. **스크롤 중에는 고정, 새로고침 시에는 갱신.**

##### 검토한 선택지

| 안 | 방법 | 스크롤 중 | 새로고침 시 | 비용 |
|---|---|---|---|---|
| A 결정적 정렬 | `일치 수 DESC → 북마크 수 DESC → spot_id DESC` | 고정 | **그대로** | 가장 낮음 |
| B 시드 셔플 | 새로고침 때 seed 발급 → 커서에 실어 전달 | 고정 | **같은 스팟이 순서만 바뀜** | 낮음 |
| **B+ 시드 셔플 + 노출 이력** | B에 노출 이력을 더해 안 본 스팟을 앞에 둠 | 고정 | **안 본 것 우선** | 낮음. 테이블 1개 |
| C 세션 스냅샷 | 랭킹 결과를 Redis에 저장하고 인덱스로 페이징 | 고정 | 새 조합 | 중간. Redis 필요 |

##### 결정 — **B+ 채택** (2026-08-03) · 구현 완료

기획 요구는 "인스타그램처럼"이다. 그 체감의 핵심은 스크롤 중 순서 고정과 **새로고침하면 새로운 것이 뜸** 두 가지인데,
B는 앞의 하나만 만족한다. 새로고침해도 같은 스팟이 순서만 바뀌기 때문이다.
인스타그램에서 새 게 보이는 것은 셔플이 아니라 **본 것을 기억해 뒤로 미루기 때문**이다.
C는 Redis 도입이 필요해 현 규모에 과하다.

> **주의** — 노출 이력 후순위는 **화면정의서에 없는 우리 쪽 정책**이다. 화면정의서는 정렬을 "유사도 내림차순"까지만 쓴다.
> 스펙을 초과하지만 "당겨서 새로고침"의 체감을 만들기 위한 것이므로 **기획에 공유가 필요하다.**

##### 구현된 정렬 키 (`FeedSpotReaderAdapter`)

```sql
ORDER BY seen ASC, rank_score DESC, shuffle_key ASC, spot_id ASC
```

1. `seen` — 최근 N일 내 노출된 것을 뒤로. **"제외"가 아니라 "후순위"**
2. `rank_score` — 무드 일치 수 DESC (위 "유사도" 미결 참고)
3. `shuffle_key` — `hash(spot_id, seed)`. 같은 점수 안에서의 셔플
4. `spot_id` — 동점 tiebreak, 커서 안정성 보장

**커서** — keyset 방식. `FeedCursor`가 `seed · sessionAt · seen · rankScore · shuffleKey · spotId`를 담는다.
커서 없이 요청 = 새로고침 → 서버가 **새 seed·새 sessionAt 발급**. 커서가 있으면 이어 써서 순서를 고정한다.

**노출 기록** — 응답에 나간 spotId를 `(member_id, spot_id)` upsert로 `shown_at` 갱신.
조회 API지만 쓰기가 일어나므로 `FeedService#getFeed`는 `@Transactional`(readOnly=false)이다.

**소진 처리 — 별도 리셋 로직을 두지 않는다.** 후순위일 뿐이라 다 본 뒤에도 피드가 비지 않는다.
판정 기준이 `shown_at > now() - N일` 이라 시간이 지나면 자동으로 "안 본 것"으로 되돌아온다.
이는 화면정의서의 "**유사도 컷오프 없이 전체 스팟 대상 조회**"와도 맞는다.

##### 노출 이력 유효 기간 — **30일** (`FeedProperties#impressionWindowDays`)

`shown_at > now() - 30일` 인 것만 "본 것"으로 친다.

**새로고침 동작과 혼동하지 말 것.** 새로고침하면 유효 기간과 무관하게 매번 피드가 새로 구성된다
(새 seed + 방금 본 것 후순위). 유효 기간이 정하는 것은 **한 번 본 스팟을 언제 다시 앞으로 올릴까**, 즉 재노출 주기다.

스팟 카탈로그가 크므로 주 목적은 재순환이 아니라 **최근에 본 것 재등장 방지**다. 그래서 짧게(7일) 잡을 이유가 없다.
설정값이므로 운영 중 조정 가능하다.

##### 비회원 피드의 정렬 (`readGuestFeed`)

비회원은 `member_id`가 없어 **노출 이력을 쓸 수 없고** 선호 무드도 없다.
B+의 정렬 키 4개 중 앞 2개가 무의미해지고 자동으로 **B(시드 셔플)** 가 된다.

| 정렬 키 | 회원 | 비회원 |
|---|---|---|
| `seen` (노출 안 함 우선) | ○ | ✕ (이력 없음) |
| 무드 일치 수 DESC | ○ | ✕ (선호 무드 없음) |
| 북마크 수 DESC | — | **○** |
| `hash(spot_id, seed)` | ○ | ○ |
| `spot_id` | ○ | ○ |

개인화 근거가 없으므로 **북마크 수를 품질 신호로 대신 쓴다.** 초기에는 북마크가 거의 없어 사실상 셔플로 동작하고,
데이터가 쌓일수록 인기 스팟이 위로 올라온다.

⚠️ 단, `DSC-03`이 확정되면 이 분기 자체가 없어질 수 있다. 위 "비회원 피드" 절 참고.

## Feed 메인 B (`DSC-02` · `FED-F02`) — 🔶 부분 구현

무드 미설정자용이라 개인화 조건이 없다. 캐러셀 두 개로 구성된다.

### 1. 인기 스팟 캐러셀 — ✅ 구현됨 (`GET /feed/popular-spots`)

- **저장 수(북마크) 기준 상위 5개** 고정. 페이징 없음
- 카드 형태의 캐러셀 리스트, **좌우 스와이프** 지원
- **카드 구성**: 대표 이미지 1장 · 지역 정보(동, 시) · 스팟명 · **스팟 설명(2줄 초과 시 말줄임)**
- List Item 클릭 → `COM-00` 스팟 상세를 Full Screen Layer Modal로 표시

> **해소됨 (2026-08-30)** — `PopularSpotResponse.description` 추가 완료.
> `spot_description`(locale별)을 `LEFT JOIN`해 조회하므로 **설명이 없는 스팟도 목록에서 빠지지 않고 `null`로 나간다.**
> 말줄임은 클라이언트 처리.

### 2. 추천 루트 캐러셀 — ❌ 미구현 · 담당 미정

- **운영자가 사전 등록한 추천 루트 3개** 표시. 어드민에서 등록·수정·노출 여부 관리 필요
- 세로 리스트 형태
- **루트 카드**: 대표 이미지 1장 · 루트명 · 포함 지역 배지(복수 지역이면 **최대 2개**까지 다중 표시,
  루트에 포함된 스팟이 많은 지역 순서로) · 여행 일수 · 스팟 수
- List Item 클릭 → `RTE-04` 루트 상세를 Full Screen Layer Modal로 표시

> **🔴 미결** — 이건 `route` 컨텍스트(개발자 B) 데이터이고 **어드민 기능**이 전제인데,
> 어느 컨텍스트에도 정의된 게 없다. 정해야 할 것:
> - 운영자 등록 루트를 어떻게 표시할지 (`route` 테이블의 플래그? 별도 큐레이션 테이블?)
> - 어드민 UI/API를 누가 만드는지. MVP에서는 수동 INSERT로 갈 것인지
> - 조회 API를 `discovery`가 제공(ACL로 route 조회)할지 `route`가 제공할지
>   → **`route`가 제공하고 클라이언트가 직접 부르는 쪽이 단순하다.** 캐러셀 클릭도 `RTE-04`로 가므로 일관된다.

## Feed 메인 C (`DSC-03`) — ⚠️ 화면정의서 `작성 중`

화면상 구성은 **가입 유도 CTA(`Discover Korea, your way` / `Get Started`) + Popular Spots + Recommended Route**로
메인 B와 같다. 다만 Function/Button Description이 전부 플레이스홀더라 **확정 스펙이 아니다.**

기존 결정(비회원 전체 피드)과 충돌하므로 위 "⚠️ 비회원 피드" 절의 결정이 선행되어야 한다.
`Get Started` → `AUT-01` 로그인/회원가입으로 보내는 것은 `AUT-F02`(제한 기능 이용 시 가입 유도)와 일관된다.

## Pick STEP 1 — 추천 정보 입력 (`DSC-04` · `PCK-F01`) — ❌ 미구현

### 진입 경로

- `DSC-01` 메인 A → **[FAB] Find by photo**
- `DSC-02` 메인 B → **[BTN] Find by mood**

Full Screen Layer Modal로 표시. 종료 시 진입 화면으로 복귀(스크롤 위치 유지).

> 명세서 버튼 라벨이 화면마다 `Find by photo` / `Find by mood`로 다르다. 서버 영향은 없으나 기획에 통일 요청.

### 사진 — **1장만** (필수)

> 🔴 **기능명세서와 화면정의서가 충돌한다.**
> - 기능명세서 `PCK-F01`: "이미지 (1~5장)" · "이미지 최소 1장 ~ 최대 5장" · 기기 갤러리에서 1~5장 선택
> - 화면정의서 `DSC-04`(26-04-01, `작성 완료`): **1장만 등록 가능** (MAX 1)
>
> 화면정의서가 더 나중에 확정됐고 사유("한 장의 사진에 여러 무드가 섞여 분석되면 정확한 추천이 어렵다")까지
> 적혀 있으므로 **이 문서는 화면정의서를 따른다.** 다만 기능명세서 정정 요청이 필요하다.
> 5장을 유지한다면 `PickRequestImage` 테이블과 다중 벡터 병합 규칙이 되살아나므로 **설계가 달라진다.**

- **1장만 등록 가능.** 삭제 버튼으로 제거 후 재등록
- **파일 정책**: 허용 형식 **JPG · PNG · HEIC** / 용량 상한 **10MB**
- **에러 처리 — 모달 표시**
  - 미지원 형식: `This file type isn't supported. Try a JPG or PNG.`
  - 용량 초과: `This photo is too large. Max {n}MB.`
  - 업로드 실패: `COM-00` 서버 오류 모달
- 업로드 소스는 Photo Library / Take Photo / Choose File (클라이언트 액션 시트)

**서버 검증은 클라이언트 검증을 신뢰하지 않는다.** 확장자·Content-Type·용량을 서버에서도 검증한다.

### 지역(Areas) — 1개 이상 5개 이하 (필수)

- **자동완성 필드.** 자유 입력 불가, 검색 결과에서 고른 값만 허용 (`COM-P03`). 선택 시 Chip 형태로 표시
- **다중 선택 MAX 5.** (기능명세서는 "복수 가능"이라고만 쓰고 **상한이 없다.** 화면정의서의 MAX 5를 따른다) 5개 선택 시 자동완성 필드와 인기 지역 섹션을 Disabled 처리
- 입력값에 따라 하단 패널에 검색 결과 **실시간 조회**. 결과 없으면 `No results found`
- 검색 결과는 **3레벨**로 배지 표시 — `Region`(시/도) · `District`(시/군/구) · `Neighborhood`(동/면)
- **선택된 지역은 Chip으로 표시.** Chip 라벨은 레벨에 따라 다르다
  - Region: 지역명만 (`Daegu`)
  - District·Neighborhood: **상위 지역까지 함께** (`Seongsu, Seoul`)
- **인기 지역 섹션 MAX 5** — 칩 클릭 시 자동완성 필드에서 선택한 것과 동일하게 처리.
  이미 선택한 지역은 목록에서 제거. 🔴 **어드민에서 등록·수정·노출 여부 관리 필요** (담당 미정)

#### 상하위 중복 방지 규칙 — 서버도 동일하게 검증한다

| 상황 | 처리 |
|---|---|
| 상위 지역을 이미 선택한 상태에서 그 하위 지역 선택 시도 | 하위 옵션을 **Disabled** (예: `Seoul` 선택 → `Seongsu, Seoul` Disabled) |
| 하위 지역을 선택한 상태에서 그 상위 지역 선택 | 기존 하위 칩을 **제거하고 상위 지역으로 대체** (예: `Seongsu, Seoul` 선택 상태에서 `Seoul` 선택 → `Seoul` 하나만 남음) |

### [BTN] Find spots

- 기본 **Disabled**. **사진 1장 등록 + 지역 1개 이상 선택** 시 Enabled
- 클릭 → 무드 분석·매칭 요청 → 로딩(`DSC-04-02`) → 완료 시 `DSC-05` 추천 결과로 전환

### 로딩 (`DSC-04-02`) — 클라이언트 전용

5단계 카피를 1.5초 간격으로 순차 전환하고, 마지막 단계는 응답 수신 시까지 유지. 최소 2초 노출.
서버가 할 일은 없지만 **응답이 2초 이내여도 클라이언트가 2초를 채운다**는 점만 알아두면 된다.

### 🔴 선호 벡터 갱신

`DSC-04`는 "업로드 사진에서 사용자 선호 벡터 갱신"을 요구한다. 위 "선호 벡터" 절 참고. **확정 전 미구현.**

## Pick STEP 2 — 추천 결과 (`DSC-05` · `PCK-F02`·`PCK-F03`) — ❌ 미구현

업로드한 사진의 무드 벡터와 매칭된 스팟 추천 결과를 제공한다.

### 추천 정책

- **무드 벡터 유사도 상위 최대 5개**
- **`DSC-04`에서 선택한 지역 내 스팟만 후보로 적용**
- 결과 없음은 에러가 아니다. **빈 배열 + 200**으로 응답한다 (`DSC-05-02` Empty State)

### 지도 + 카드 캐러셀 (`DSC-05-01`)

- **[Map]**: 추천 스팟 위치 마커. 선택 상태에 따라 표시가 다르다(Selected / Unselected).
  최초 진입 시 Selected 상태 스팟이 화면 중심에 오도록 지도 영역 자동 조정. 지도 드래그·확대·축소 지원
- **[Marker] (Selected)**: 스팟명 + 지역 정보 (`Ex. Seoul Forest / Seongsu, Seoul`)
- **[Card] 스팟 카드**: 스팟명 · **스팟 주소** · 스팟 설명 · **무드 태그 MAX 3** · `[BTN] Save` · `[BTN] Add to route`
  - 좌우 스와이프로 이전/다음 스팟 전환 → 지도 마커 선택 동기화
  - 카드 클릭 → `COM-00` 스팟 상세를 Full Screen Layer Modal로 표시

> **응답에 좌표(위도·경도)와 주소, 무드 태그가 필요하다.** Feed 카드보다 필드가 많다.

### 루트 연동 (`PCK-F03`)

`[BTN] Add to route` → 바텀시트로 두 갈래.

1. **기존 루트에 추가** → `COM-NN-01` 루트 목록을 바텀시트로 표시 → 선택한 루트의 마지막 일정에 스팟 추가.
   시트 종료 시 스낵바 표시. 보유 루트가 없으면 `COM-NN-02` 루트 목록 Empty State
2. **이 스팟으로 새 루트 생성** → `RTE-02-01` 루트 생성 1단계로 이동.
   **선택한 스팟이 `Spots`에, `DSC-04`에서 입력한 지역이 `Areas`에 자동 입력된 상태로 렌더링**

**추천 컨텍스트는 루트를 직접 만들지 않는다.** 스팟 ID와 지역 값만 넘기고 `route`(`RTE-F01`·`RTE-F02`·`RTE-F06`)에 위임한다.

### 저장 (`[Toggle BTN] Save`)

클릭 시 저장 / 재클릭 시 해제. 상태에 따라 아이콘 변경(Unselected ↔ Selected).
저장 완료 시 토스트. `spot` 북마크 API를 그대로 쓴다.

### 화면 벗어나기 확인 모달

`[X]` 클릭 시, 저장하거나 루트에 추가한 스팟이 **없는 경우에만** 확인 모달을 띄운다
(`Leave these results?` / `If you leave now, you'll need to get recommendations again`).
저장·추가한 스팟이 있으면 모달 없이 바로 종료. **전부 클라이언트 판단이므로 서버 영향은 없다.**

### Empty State (`DSC-05-02`) — 추천 결과 0개

- `No spots matched your search` / `Try adding another area or a different photo`
- `[BTN] Try Again` → `DSC-04` STEP 1로 복귀. **입력값(사진·지역) 유지**
- 🔴 **[Carousel] 대체 추천 (MAX 5)** — `You might like these`
  - **선정 기준: 지역 조건을 해제한 후 유사도 상위 스팟**
  - 카드 구성: 스팟명 · 지역 정보(동, 시). 좌우 스와이프
  - 카드 클릭 → `COM-00` 스팟 상세

> 대체 추천은 **기능명세서에 없는 신규 요구다.** 기능명세서는 Empty State를
> `"조건에 맞는 추천 결과가 없어요." + [BTN] 다시 추천받기`까지만 정의한다. **같은 요청에서 지역 필터만 뺀 2차 조회**로 구현한다.
> 응답 스키마를 어떻게 나눌지는 아래 "API 계약" 참고.

## 도메인 모델

Feed는 노출 이력만, Pick은 요청·이미지·지역·결과를 가진다.

- **FeedImpression** ✅ 구현됨: `member_id · spot_id · shown_at`, unique `(member_id, spot_id)`, index `(member_id, shown_at)`
- **PickRequest**(신설): 추천 요청 1건. `id · member_id · image_url · created_at`.
  **이미지가 1장이므로 별도 이미지 테이블이 필요 없다** — `PickRequest`에 `image_url` 컬럼으로 둔다
- **PickRequestArea**(신설): 선택 지역. `pick_request_id · level(REGION/DISTRICT/NEIGHBORHOOD) · region · district · neighborhood · sort_order`
- **PickResultSpot**(신설): 추천 결과. `pick_request_id · spot_id · rank · fallback(대체 추천 여부)`

> **결정 — 저장한다 (2026-08-30).** `pickId`가 있어야 `DSC-05` 재조회가 가능하고,
> 선호 벡터 갱신(신규 요구)도 어떤 사진으로 추천받았는지에 대한 이력을 전제로 한다.
> 저장 비용은 요청당 최대 11행(요청 1 + 지역 5 + 결과 5)이라 크지 않다.
> 다만 `GET /picks/{pickId}` 재조회 API는 아직 만들지 않았다.

> **사진이 5장으로 되돌아가면 이 스키마가 달라진다.** `PickRequest.image_key` 컬럼 대신
> `PickRequestImage` 테이블이 필요해지고, 여러 벡터를 어떻게 합칠지도 정해야 한다. 미결 0번 참고.

### 유사도 계산 위치 — SQL이 아니라 자바

후보 선별은 SQL(지역 또는 무드 태그 필터), 순위 매기기는 `PickService`에서 한다.
`spot_mood.mood_vector`가 JSONB 맵이라 SQL로 코사인 유사도를 계산하려면 6개 축을 전부 펼쳐야 해서
쿼리를 읽기 어렵고 인덱스도 타지 못한다.

**대신 후보를 자바로 다 읽어 온다.** 지역이 넓게 잡히면 후보가 크게 불어날 수 있어
`moodi.pick.candidate-limit`(기본 500)로 상한을 둔다. **이 상한을 넘으면 `spot.id` 순으로 잘리므로
유사도 상위가 후보에서 빠질 수 있다.** 카탈로그가 커지면 재검토 대상이다(이슈 #21과 함께 본다).

## 규칙 · 불변식

- 사진은 **정확히 1장** (`DSC-04`). JPG·PNG·HEIC, **10MB 이하**
- 지역은 **1개 이상 5개 이하**, 자동완성 결과에서 고른 값만 허용 (`COM-P03`)
- 지역 선택에 **상하위 중복이 있으면 안 된다** (상위가 있으면 하위 제거)
- Pick 추천 결과는 **최대 5개**, 대체 추천도 **최대 5개**
- Pick 후보는 **선택 지역 내 스팟으로 한정**. 단 대체 추천은 지역 조건을 해제한다
- Feed 페이지 크기는 **20** (`COM-P05`)
- 인기 스팟은 **저장 수 기준 정확히 Top 5**
- 추천 루트는 **운영자 등록분 3개**
- 인기 지역은 **최대 5개**
- 추천 목록에서 **비활성 스팟(`SpotStatus`)은 제외**한다. `spot` 원장 정책을 따른다

## API 계약

| Method | Path | 인증 | 요청 → 응답 | 화면 | 상태 |
|---|---|---|---|---|---|
| GET | `/api/v1/feed?cursor=` | `@OptionalAuthMember` | → `CursorResponse<FeedSpotResponse>` | `DSC-01` | ✅ |
| GET | `/api/v1/feed/popular-spots` | `@LoginRequired` | → `List<PopularSpotResponse>` (5) | `DSC-02` | ✅ |
| GET | `/api/v1/picks/upload-url` | `@LoginRequired` | `?contentType=&contentLength=` → `{ uploadUrl, imageKey, expiresInSeconds }` | `DSC-04` | ✅ |
| GET | `/api/v1/picks/popular-areas` | `@OptionalAuthMember` | → `List<AreaResponse>` (5) | `DSC-04` | ❌ |
| POST | `/api/v1/picks` | `@LoginRequired` | `{ imageKey, areas: [...] }` → `{ pickId, spots: [...5], fallbackSpots: [...5] }` | `DSC-04`→`DSC-05` | ✅ |
| GET | `/api/v1/picks/{pickId}` | `@LoginRequired` | → 위와 동일 | `DSC-05` 재조회 | ❌ |

- **지역 자동완성은 `spot`의 통합 검색(`COM-02`, 이슈 #55)을 재사용한다.** `discovery`가 따로 만들지 않는다.
  Region/District/Neighborhood 3레벨 응답이 필요하므로 **개발자 B와 응답 스키마 합의가 필요하다.**
- `POST /picks`가 생성과 조회 응답을 겸한다. `[BTN] Find spots` 한 번에 결과 화면까지 이동하므로 왕복을 줄인다
- **`fallbackSpots`는 `spots`가 비었을 때만 채운다.** 별도 엔드포인트로 나누면 Empty State에서 왕복이 한 번 더 생긴다
- 이미지 업로드는 **presigned URL 방식**을 전제로 한다 (Cloud Run 요청 크기·타임아웃 제약). 미결 항목 참고

## 에러 코드 (`ErrorCode` 추가 예정)

| 코드 | HTTP | 설명 |
|---|---|---|
| `PICK_NOT_FOUND` | 404 | 추천 요청 없음 (재조회 API 미구현, 코드만 선언) |
| `PICK_FORBIDDEN` | 403 | 타인의 추천 결과 접근 (재조회 API 미구현, 코드만 선언) |
| `PICK_INVALID_AREA_SELECTION` | 400 | 지역 미선택 · 5개 초과 · 단계별 필수값 누락 ✅ |
| `PICK_IMAGE_REQUIRED` | 400 | 사진 미등록 |
| `PICK_UNSUPPORTED_IMAGE_TYPE` | 400 | JPG·PNG·HEIC 외 형식 ✅ |
| `PICK_IMAGE_TOO_LARGE` | 400 | 10MB 초과 · 0바이트 이하 ✅ |
| `IMAGE_UPLOAD_UNAVAILABLE` | 503 | 버킷·IAM 미구성 환경에서 업로드 URL 발급 시도 ✅ |
| `PICK_ANALYSIS_FAILED` | 422 | 이미지 무드 분석 실패 (`COM-00` 서버 오류 모달 대상) ✅ |

추천 결과가 0건인 것은 **에러가 아니다.** 빈 배열 + 200으로 응답한다 (`DSC-05-02` Empty State).

## 영속성

- 마이그레이션 번호는 **현재 최신 이후**를 쓴다. 추가 전 `db/migration` 디렉터리를 확인할 것
- 도메인은 순수 POJO, JPA 매핑은 `META-INF/orm.xml`에만 `<entity>`로 추가한다
- 도메인 Repository 포트는 **단건 `save`만 선언**한다. `Repository<T, ID>`는 `saveAll`을 CRUD 메서드로
  인식하지 못해 쿼리 파생을 시도하다 컨텍스트 로딩에 실패한다 (온보딩 구현 중 실제로 겪음)
- 조회 전용 쿼리는 도메인 Repository와 분리해 `application`에 포트로 둔다
  (`FeedSpotReader`·`PopularSpotReader` 선례를 따른다)

## 테스트

- 도메인: 사진 1장·지역 1~5개·상하위 중복 불변식, 결과 개수 상한
- 서비스: 조회 포트·무드 분석 포트를 mock. 유사도 정렬, 결과 0건 → 대체 추천, 비회원 북마크 필드
- 컨트롤러: Feed는 `RestDocsSupport`(비회원 가능), Pick은 `AuthenticatedRestDocsSupport`
- 쿼리 리포지토리: `RepositoryTestSupport` 또는 Testcontainers(`PostgresTestSupport`).
  **jsonb 무드 필터는 H2에서 재현되지 않으므로 Postgres 통합 테스트가 필요하다**
- REST Docs 작성 후 `src/docs/asciidoc/discovery/`에 adoc 추가 + `app/index.adoc`에 include (루트 `CLAUDE.md` 체크리스트)

## 재사용 가능한 기존 자산

`application`에서 직접 호출하진 않되(어댑터 경유), **쿼리 로직과 접근 방식은 그대로 베낄 수 있다.**

| 자산 | 위치 | 참고 포인트 |
|---|---|---|
| ACL 어댑터 구조 | `discovery.application.PreferredMoodReader` + `discovery.infrastructure.member.PreferredMoodReaderAdapter` | **이 컨텍스트에 이미 있는 선례.** Pick도 동일하게 |
| 커서 페이징 + 노출 이력 | `discovery.application.FeedService`·`FeedCursor` | keyset 커서 인코딩 패턴 |
| 유사 무드 스팟 조회 | `spot.application.SpotDetailQueryRepository#findSimilarMoodSpots` | 무드 태그 교집합 쿼리. Pick 후보 조회의 원형 |
| 북마크 수 기준 인기 조회 | `spot.application.SpotDetailQueryRepository#findPopularSpotsByArea` | 지역 필터 + 인기순. Pick 후보 조회에 지역 조건 참고 |
| 북마크 수 집계·여부 조회 | `spot.application.BookmarkQueryRepository` | `countBySpotIds`·`findBookmarkedSpotIds` 패턴 |
| 이미지 → 무드 분석 | `spot.application.MoodAnalysisClient` + `infrastructure.mood.VisionLlmMoodAnalysisClient` | 포트 시그니처(`analyze(imageUrls, overview) → MoodVector`)를 그대로 본뜬다 |
| 무드 태그 규칙 엔진 | `shared.mood.MoodTagRuleEngine`·`MoodVector` | 공유 커널이라 **직접 사용 가능**. 벡터 → 태그 변환, 카드의 무드 태그 MAX 3 |
| 지역 파싱 | `spot.application.RegionParser` | `DSC-04` 3레벨(Region/District/Neighborhood) 매핑 |
| 통합 검색 | 이슈 #55 (`COM-02`, 개발자 B) | `DSC-04` 지역 자동완성이 여기에 의존한다 |

## 미결 (구현 전 확정 필요)

### 🔴 기획 확정이 필요한 것

0. **기능명세서 ↔ 화면정의서 충돌 정리** — 사진 장수(1~5장 vs 1장), 지역 상한(없음 vs MAX 5),
   Empty State 대체 추천(없음 vs MAX 5 캐러셀). **화면정의서 기준으로 기능명세서 갱신을 요청한다**
1. **비회원 Discover 구성** (`DSC-03`) — 전체 무한 피드(기존 결정) vs 인기 스팟+추천 루트(화면정의서).
   `DSC-03`이 `작성 중` 상태라 확정 전. **이미 구현된 `GET /feed`의 비회원 분기 존폐가 걸려 있다**
2. **선호 벡터 도입** — Pick 실행이 `member` 상태를 바꾸고 A/B 분기를 뒤집는다. 저장 위치·갱신 규칙·
   사전조사 태그와의 관계·피드 정렬 근거 교체 여부. **Member 컨텍스트 스펙도 함께 수정해야 한다**
3. **추천 루트 캐러셀** (`DSC-02`·`DSC-03`) — 운영자 등록 방식, 어드민 담당, 조회 API 소유 컨텍스트
4. **인기 지역** (`DSC-04`) — 어드민 등록 방식. MVP는 상수/수동 INSERT로 갈 것인지
5. **Feed의 "유사도" 정의** — Pick은 벡터 코사인으로 구현했으나 Feed는 여전히 태그 일치 수다. 2번(선호 벡터)과 연동
6. ~~추천 결과 저장 여부~~ — **저장으로 결정(2026-08-30).** `GET /picks/{pickId}` 재조회 API는 후속
7. **버튼 라벨 통일** — `Find by photo`(메인 A) / `Find by mood`(메인 B)

### 🟠 인프라 결정이 필요한 것

- **GCS는 이미 연결돼 있다. 없는 것은 "사용자 업로드" 경로다.**

  **있는 것** (2026-08-30 확인)
  - `build.gradle.kts:41` — `com.google.cloud:google-cloud-storage:2.49.0`
  - `application.yaml` — `gcs.spot-image.bucket: moodi-spot-images` (`enabled: false`로 기본 비활성)
  - `spot/infrastructure/storage/GcsSpotImageUploader` — `@ConditionalOnProperty`로 켜지는 업로더

  **추가된 것 (2026-08-30)**
  - `discovery/application/ImageStorageClient` — 업로드 대상 발급 포트
  - `discovery/infrastructure/storage/GcsImageStorageClient` — V4 서명 URL 발급 어댑터
  - `discovery/domain/PickImage`·`PickImageType` — 형식·용량 불변식
  - `ImageStorageClient#issueReadUrl` — 무드 분석기에 넘길 읽기용 서명 URL
  - `gcs.pick-image.*` 설정. **버킷·IAM이 붙기 전까지 `enabled: false`**이고,
    이때는 `UnavailableImageStorageClient`가 503으로 실패시킨다(가짜 URL을 내주지 않는다)

  **아직 없는 것**
  - **버킷 자체가 없다.** 비공개 버킷 생성이 콘솔 작업으로 남아 있다
  - **`signBlob` IAM이 없다.** 이게 없으면 `signUrl` 호출이 런타임에 실패한다
  - **업로드된 사진을 읽는 경로가 없다.** 비공개 버킷이라 무드 분석 시 읽기용 서명 URL이 따로 필요하다

  **결정·작업이 남은 것**
  - **버킷 분리** — 기존 `moodi-spot-images`는 `PUBLIC_URL_FORMAT`(`storage.googleapis.com/{bucket}/{obj}`)으로
    **공개 URL**을 만든다. 스팟 사진은 공개해도 되지만 **사용자 개인 사진은 안 된다.**
    별도 비공개 버킷(예: `moodi-pick-uploads`) + 읽기도 signed URL로 가는 것을 권장한다
  - **서명 권한** — Cloud Run 기본 서비스 계정으로 signUrl을 하려면 `iam.serviceAccounts.signBlob`
    (Service Account Token Creator) 권한이 필요하다. 키 파일 없이 가려면 이 IAM 설정이 선결이다
  - **HEIC** — 기존 `detectContentType`은 jpeg/png/webp만 안다. 화면정의서는 **JPG·PNG·HEIC**를 요구하므로
    허용 목록을 Pick 쪽에서 새로 정의한다(서버 검증 필수, 클라이언트 검증을 신뢰하지 않는다)
  - **보관 기간** — 개인 사진이라 개인정보 정책과 직결된다. 버킷 lifecycle rule로 자동 삭제를 거는 것을 권장
  - 파일 정책은 화면정의서에서 확정됐다: **1장 · JPG/PNG/HEIC · 10MB**
  - `docs/infrastructure.md`의 "이미지"는 전부 **Docker 이미지**(Artifact Registry)라 파일 스토리지와 무관하다.
    GCS 관련 기재가 없으므로 **인프라 문서에도 버킷을 추가해야 한다**

### 🟡 개발자 B와 합의가 필요한 것

- **지역 자동완성 응답 스키마** — `DSC-04`가 Region/District/Neighborhood 3레벨과 상위 지역 라벨을 요구한다.
  이슈 #55(통합 검색)에 이 요구가 반영돼야 한다
- **이슈 #38** 유사 무드 스팟 조회가 `?|` 연산자로 항상 실패 — Pick 후보 조회가 같은 쿼리를 원형으로 쓴다. **선결**
- **이슈 #21** JSONB GIN 인덱스 기반 무드 필터링 성능 검증 — 피드와 Pick 양쪽 병목

### 🟢 우리 쪽 정책이라 기획 공유만 필요한 것

- **노출 이력 후순위(B+)** — 화면정의서에 없는 초과 구현. "당겨서 새로고침" 체감을 위한 것
- **노출 이력 유효 기간 30일** — `FeedProperties`로 운영 중 조정 가능

## 주의

- 레이어 의존만 `LayeredArchitectureTest`(ArchUnit)로 강제된다. **컨텍스트 경계는 자동 검증되지 않으니 리뷰에서 본다**
- `spot`·`route`는 개발자 B 담당이다. 조회 쿼리를 복제하게 되므로 **인덱스·성능 변경은 공유**한다
- 공유 커널(`shared`) 변경은 합의 후에만 (루트 `CLAUDE.md`)
- **선호 벡터는 `member` 상태를 쓰는 일이다.** Discovery 단독으로 결정하지 않는다
