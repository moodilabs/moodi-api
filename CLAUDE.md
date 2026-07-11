# Moodi Backend

무드 기반 여행 스팟 큐레이션 & AI 루트 서비스의 백엔드 API.

## 도메인 컨텍스트

바운디드 컨텍스트 4개 + 공유 커널.

- **회원 Member** — 인증·온보딩·프로필·약관·사전조사 (ONB, AUT) · 개발자 A
- **스팟 Spot** — 콘텐츠 원장(TourAPI 동기화)·북마크 (COM-F) · 개발자 B
- **추천 Discovery** — Feed(무드 개인화)·Pick(사진 AI 추천) (FED, PCK) · 개발자 A
- **루트 Route** — AI 루트 생성·편집·공유 (RTE), 유일한 풀 DDD · 개발자 B
- **공유 커널(shared)** — 무드·커서 페이징·에러 포맷·회원/비회원 인증 분기 (COM-P)

### 유비쿼터스 언어
스팟(Spot) · 무드(Mood) · 픽(Pick) · 피드(Feed) · 루트(Route) · 이동정보(Leg) · 북마크(Bookmark) · 사전조사(Preference Survey)

## 아키텍처

레이어드 + 컨텍스트 최상위 (선택적 DDD). 루트에 바운디드 컨텍스트 단위로 패키지를 구성하고,
레이어는 각 컨텍스트 내부에 둔다. 외부 시스템만 포트 인터페이스로 격리(ACL)한다.
Route만 풀 DDD 애그리거트로, 나머지는 실용적 레이어드로 간다.

```
src/main/java/com/moodi/
├── shared/                    # 공통 인프라 + 공유 커널
│   ├── BaseEntity.java        # JPA Auditing 기반 슈퍼클래스
│   ├── config/                # JPA 설정
│   ├── response/              # 공통 응답 (SuccessResponse, PageResponse)
│   ├── error/                 # 공통 에러 (ErrorCode, BusinessException, GlobalExceptionHandler)
│   ├── logging/               # MDC 로깅 필터
│   └── auth/                  # 회원/비회원 인증 분기 (JWT, 인터셉터, 아규먼트 리졸버)
│
├── {domainName}/              # 바운디드 컨텍스트 단위 (컨텍스트 이름이 루트)
│   ├── presentation/          # 컨트롤러 + 요청/응답 DTO
│   ├── application/           # 유스케이스(Service) + 외부 시스템 포트 인터페이스
│   ├── domain/                # 순수 도메인 객체(JPA 애노테이션 없음) + Repository 포트
│   └── infrastructure/        # JPA 어댑터(Repository 구현) + 외부 시스템 어댑터
│
└── MoodiApplication.java
```

의존 방향: `presentation → application → domain`, `infrastructure → 포트 구현(application·domain)`.
domain은 순수(의존 없음). 컨텍스트 간 참조는 ID(+필요 스냅샷)만 — JPA 연관관계를 경계 너머로 걸지 않는다.
레이어 의존 규칙은 ArchUnit으로 강제한다 (기존 `HexagonalArchitectureTest` → `LayeredArchitectureTest`로 교체 필요).

## 패키지 · 네이밍

- 컨텍스트 최상위: `com.moodi.{context}` (member · spot · discovery · route).
- 레이어: `presentation` / `application` / `domain` / `infrastructure`.
- 클래스: 컨트롤러 `XxxController`, 서비스 `XxxService`, 요청/응답 DTO `XxxRequest`·`XxxResponse`(presentation/dto), 외부 시스템 포트 `XxxClient`(application), 도메인 Repository 포트 `XxxRepository`(domain, 순수 인터페이스).
- **리포지토리**: Spring Data 인터페이스는 `JpaRepository`가 아니라 `org.springframework.data.repository.Repository<T, ID>`를 상속하고 **필요한 메서드만 선언**한다. infrastructure/persistence에 두고 도메인 Repository 포트를 구현한다.

## JPA 매핑 방식 (orm.xml)

도메인 객체는 순수 POJO. JPA 매핑은 `src/main/resources/META-INF/orm.xml`에만 정의.

새 엔티티 추가 시 orm.xml에 `<entity>` 블록 추가:
```xml
<entity class="com.moodi.{domainName}.domain.{Entity}" access="FIELD">
    <table name="{table_name}"/>
    <attributes>
        <id name="id">
            <generated-value strategy="IDENTITY"/>
        </id>
    </attributes>
</entity>
```

## 공통 응답 형식

```java
// 단건
SuccessResponse.of(data)           // { "data": ... }

// 페이징
PageResponse.of(data, page, size, totalElements)
```

## 에러 처리

비즈니스 예외는 `BusinessException(ErrorCode.XXX)` 사용. 새 에러 코드는 `ErrorCode` enum에 추가.

## 로깅

MDC로 requestId 자동 주입. 로그 패턴에 `[%X{requestId}]` 포함됨. 응답 헤더 `X-Request-Id`로도 전달.

## 테스트

- **단위(도메인)**: 프레임워크 없이 팩토리·행위·불변식 검증. 예) `RefreshToken.isExpired()`.
- **단위(서비스)**: `@ExtendWith(MockitoExtension.class)` + `@Mock`(포트) / `@InjectMocks`(서비스). Spring 컨텍스트 없이.
- **컨트롤러(문서)**: `RestDocsSupport` 상속 (standalone MockMvc + REST Docs 스니펫). 보호 엔드포인트는 `AuthenticatedRestDocsSupport`(memberId 주입 + `AuthMemberArgumentResolver` 등록)로 문서화.
- **리포지토리·통합**: `RepositoryTestSupport` 상속 — `@DataJpaTest` 슬라이스 · H2 · `@ActiveProfiles("test")` · 트랜잭션 롤백 권장 (현재는 `@SpringBootTest(NONE)` 기반, 전환 검토).
- **아키텍처**: ArchUnit `LayeredArchitectureTest`.
- **Fixture**: 테스트 객체는 `support`의 정적 팩토리로 생성 — 기본값 + 필요한 필드만 오버라이드. 예) `MemberFixture.create(provider, providerId, email)`.
- assertion은 **AssertJ**(`assertThat`), mock 상호작용 검증은 Mockito(`verify`).
- 메서드명은 snake_case + `@DisplayName` 한글. 예) `social_login_success` + `@DisplayName("소셜 로그인 성공")`.
- given-when-then 구조. push 전 전체 테스트 자동 실행(git hook).

## Git Hook

push 전 테스트 자동 실행. 최초 설정 시:
```bash
./gradlew setupGitHooks
```

## Git 커밋

한국어 + Conventional Commits (`feat:`, `fix:`, `refactor:`, `chore:`, `test:`, `docs:`).
예) `feat: 소셜 로그인 API 추가`

## 브랜치 전략

Git Flow.
- `main`(배포) · `develop`(통합 개발) · `feature/*`(develop 분기) · `release/*` · `hotfix/*`(main 분기).
- 브랜치명은 기능ID 기반: `feature/AUT-F01-social-login`.
- `feature` → `develop` PR, 상대 개발자 크로스리뷰 후 머지.

## 협업 규칙

- 컨텍스트 간 계약(다른 컨텍스트 ID 참조, Pick→Route 연계 등)이 담긴 PR은 상대 개발자 크로스리뷰 필수.
- 공유 커널(`shared`, 무드·공통 정책) 변경은 합의 후에만.

## API 문서

Spring REST Docs 기반. 테스트 실행 후 문서 생성:
```bash
./gradlew asciidoctorApp
```

## 기술 스택

- Java 21, Spring Boot 4.1.0
- Spring Data JPA (orm.xml 방식), Flyway
- PostgreSQL (운영), H2 (테스트)
- Lombok, Spring REST Docs, ArchUnit

## 인프라 (GCP)

- **프로젝트**: `moodi-app-2026` / 리전: `asia-northeast3` (서울)
- **Cloud Run**: `moodi-api` (1Gi, 포트 8080)
- **Cloud SQL**: `moodi-db` (PostgreSQL 17, db-f1-micro, 소켓 연결)
- **Artifact Registry**: `asia-northeast3-docker.pkg.dev/moodi-app-2026/moodi-repo/moodi-api`
- **Firebase Hosting**: 도메인 프록시 → Cloud Run rewrite
- **도메인**: `moodi.kr` (운영), `dev-api.moodi.kr` (개발) / 가비아 구매, Firebase Hosting 연결
- **프로필**: 운영 환경은 `prod` 프로필 사용 (`application-prod.yaml`)

상세 내용은 `docs/infrastructure.md` 참고.
