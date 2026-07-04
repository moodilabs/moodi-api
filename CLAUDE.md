# Moodi Backend

## 아키텍처

DDD + 헥사고날 아키텍처 기반. 루트에 바운디드 컨텍스트 단위로 패키지 구성.

```
src/main/java/com/moodi/
├── shared/                    # 공통 인프라 코드
│   ├── BaseEntity.java        # JPA Auditing 기반 슈퍼클래스
│   ├── config/                # JPA 설정
│   ├── response/              # 공통 응답 (SuccessResponse, PageResponse)
│   ├── error/                 # 공통 에러 (ErrorCode, BusinessException, GlobalExceptionHandler)
│   └── logging/               # MDC 로깅 필터
│
├── {domainName}/              # 바운디드 컨텍스트 단위 (도메인 이름이 루트)
│   ├── domain/                # 순수 도메인 객체 (JPA 애노테이션 없음)
│   ├── application/           # 유스케이스
│   │   ├── provided/          # 외부에 제공하는 포트 인터페이스 (adapter → application)
│   │   └── required/          # 외부에 요구하는 포트 인터페이스 (application → adapter)
│   └── adapter/
│       ├── webapi/            # 컨트롤러 (provided 포트 호출)
│       └── persistence/       # JPA 어댑터 (required 포트 구현)
│
└── MoodiApplication.java
```

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

## Git Hook

push 전 테스트 자동 실행. 최초 설정 시:
```bash
./gradlew setupGitHooks
```

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
