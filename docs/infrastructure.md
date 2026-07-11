# Moodi 인프라 구성

## 전체 구조

```
[모바일 앱]
    ↓
[moodi.kr / dev-api.moodi.kr]
    ↓
[Firebase Hosting] ← SSL 자동 관리, 도메인 프록시
    ↓ (rewrite)
[Cloud Run] ← moodi-api (Spring Boot)
    ↓
[Cloud SQL] ← PostgreSQL 17
```

## GCP 프로젝트 정보

| 항목 | 값 |
|------|-----|
| 프로젝트 ID | `moodi-app-2026` |
| 리전 | `asia-northeast3` (서울) |

## Cloud SQL

| 항목 | 값 |
|------|-----|
| 인스턴스명 | `moodi-db` |
| DB 버전 | PostgreSQL 17 |
| 티어 | `db-f1-micro` (vCPU 공유 1개, 메모리 614MB) |
| 에디션 | Enterprise |
| 스토리지 | 10GB SSD (자동 증가) |
| 데이터베이스명 | `moodi` |
| 연결 방식 | Cloud SQL Java Connector (Socket Factory) |
| 연결 이름 | `moodi-app-2026:asia-northeast3:moodi-db` |

## Cloud Run

| 항목 | 값 |
|------|-----|
| 서비스명 | `moodi-api` |
| 메모리 | 1Gi |
| 포트 | 8080 |
| 서비스 URL | `https://moodi-api-954020560650.asia-northeast3.run.app` |
| 인증 | 비인증 허용 (공개 API) |

### 환경변수

| 변수 | 용도 |
|------|------|
| `SPRING_PROFILES_ACTIVE` | `prod` |
| `DB_USERNAME` | Cloud SQL 사용자명 |
| `DB_PASSWORD` | Cloud SQL 비밀번호 |

## Artifact Registry

| 항목 | 값 |
|------|-----|
| 저장소명 | `moodi-repo` |
| 형식 | Docker |
| 이미지 경로 | `asia-northeast3-docker.pkg.dev/moodi-app-2026/moodi-repo/moodi-api` |

## 수동 배포 방법

```bash
# 1. JAR 빌드
./gradlew bootJar -x test

# 2. Docker 이미지 빌드
docker build --platform linux/amd64 -t asia-northeast3-docker.pkg.dev/moodi-app-2026/moodi-repo/moodi-api:latest .

# 3. 이미지 푸시
docker push asia-northeast3-docker.pkg.dev/moodi-app-2026/moodi-repo/moodi-api:latest

# 4. Cloud Run 배포
gcloud run deploy moodi-api \
  --image=asia-northeast3-docker.pkg.dev/moodi-app-2026/moodi-repo/moodi-api:latest \
  --region=asia-northeast3 \
  --platform=managed \
  --allow-unauthenticated \
  --port=8080 \
  --memory=1Gi \
  --set-env-vars="SPRING_PROFILES_ACTIVE=prod" \
  --set-secrets="DB_USERNAME=DB_USERNAME:latest,DB_PASSWORD=DB_PASSWORD:latest" \
  --add-cloudsql-instances=moodi-app-2026:asia-northeast3:moodi-db
```

## Firebase Hosting

| 항목 | 값 |
|------|-----|
| 사이트 | `moodi-app-2026` |
| 기본 URL | `https://moodi-app-2026.web.app` |
| 역할 | 커스텀 도메인 → Cloud Run 프록시 (rewrite) |

### 설정 파일

`firebase.json`에서 모든 요청을 Cloud Run으로 rewrite:
```json
{
  "hosting": {
    "rewrites": [{ "source": "**", "run": { "serviceId": "moodi-api", "region": "asia-northeast3" } }]
  }
}
```

## 도메인

| 항목 | 값 |
|------|-----|
| 구매처 | 가비아 |
| 운영 도메인 | `moodi.kr` |
| 개발 도메인 | `dev-api.moodi.kr` |
| SSL | Google 관리형 (자동 발급/갱신) |

### 가비아 DNS 레코드

| 타입 | 호스트 | 값 |
|------|--------|-----|
| A | @ | `199.36.158.100` |
| TXT | @ | `hosting-site=moodi-app-2026` |
| CNAME | dev-api | Firebase가 제공한 값 |
1
## 남은 작업

- [ ] CI/CD 구성 (GitHub Actions → Cloud Run 자동 배포)
