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
| `ADMIN_ALLOWED_ORIGINS` | 관리자 프론트 CORS 오리진 (쉼표 구분, `*` 와일드카드 가능). GitHub Secret이 비어 있으면 기본 `https://admin.moodi.kr,https://*.vercel.app,http://localhost:5173,http://localhost:3000` |
| `APPLE_TOKEN_ENABLED` · `APPLE_TEAM_ID` · `APPLE_KEY_ID` · `APPLE_PRIVATE_KEY` | 탈퇴 시 Sign in with Apple 연결 철회(심사 5.1.1(v)). Apple Developer > Certificates, Identifiers & Profiles > Keys에서 "Sign in with Apple" 키를 만들어 Team ID·Key ID·.p8 내용(PEM 전체, 개행은 `\n` 리터럴 허용)을 Secret Manager에 넣고 `APPLE_TOKEN_ENABLED=true`. **키 없이 enabled를 켜면 기동 실패**하므로 시크릿 먼저. |
| `GOOGLE_TOKEN_ENABLED` · `GOOGLE_WEB_CLIENT_ID` · `GOOGLE_CLIENT_SECRET` (· `GOOGLE_TOKEN_REDIRECT_URI`) | 탈퇴 시 Google 계정 "타사 앱 및 서비스" 연결 철회. serverAuthCode는 **웹 애플리케이션 OAuth 클라이언트** 앞으로 발급되므로 그 클라이언트의 ID·secret(GCP 콘솔 > API 및 서비스 > 사용자 인증 정보). 모바일 코드 교환은 redirect URI 빈 값. |
| `ADMIN_BOOTSTRAP_LOGIN_ID` / `ADMIN_BOOTSTRAP_PASSWORD` | 최초 관리자(SUPER) 생성용 GitHub Secret. `admin_account`가 비어 있을 때만 기동 시 1회 사용되며, 이후에는 남아 있어도 무해. 생성된 계정은 초기 비밀번호 상태라 첫 로그인 후 `PATCH /api/admin/me/password`로 바꿔야 다른 API를 쓸 수 있다. (예전 `ADMIN_BOOTSTRAP_EMAIL`은 배포 시 `--remove-env-vars`로 제거) |

관리자 프론트는 Vercel에 배포하고 `admin.moodi.kr` 커스텀 도메인(가비아 DNS → Vercel CNAME)을 연결한다. 프론트는 `https://moodi.kr/api/admin/**`를 호출하며,
Vercel 프리뷰 배포(`*.vercel.app`)에서도 같은 API를 쓸 수 있도록 CORS 기본값에 와일드카드가 포함돼 있다.

### GCS 버킷 (비공개, 서명 URL)

| 버킷 | 용도 | 활성 플래그 |
|------|------|------------|
| `moodi-pick-uploads` | Pick 사용자 사진 | `gcs.pick-image.enabled` |
| `moodi-inquiry-uploads` | 1:1 문의 첨부 (사진·동영상·PDF) | `gcs.inquiry-upload.enabled` |

둘 다 `asia-northeast3`, uniform access, public access prevention으로 생성됨(2026-09-13). `prod` 프로필에서 두 플래그 모두 켜져 있다.

서명 URL을 만들려면 Cloud Run 서비스 계정(`954020560650-compute@developer.gserviceaccount.com`)이 **자기 자신에 대해**
`roles/iam.serviceAccountTokenCreator`(signBlob)를 가져야 한다. 이 바인딩은 프로젝트 Owner(`moodikr.2026@gmail.com`)만 걸 수 있다:

```bash
gcloud iam service-accounts add-iam-policy-binding 954020560650-compute@developer.gserviceaccount.com \
  --member="serviceAccount:954020560650-compute@developer.gserviceaccount.com" \
  --role="roles/iam.serviceAccountTokenCreator" --project=moodi-app-2026
```

권한이 붙기 전에는 업로드 URL 요청이 503(`IMAGE_UPLOAD_UNAVAILABLE`)으로 떨어지며, 권한이 붙으면 재배포 없이 바로 동작한다
(바인딩 후 전파에 수 분 걸릴 수 있음). 2026-09-13 바인딩 완료, Pick·문의 upload-url → PUT → 객체 생성까지 확인됨.

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
