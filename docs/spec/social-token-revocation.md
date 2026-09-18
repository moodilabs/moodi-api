# 소셜 계정 연결 철회 (Apple · Google) 스펙

회원 탈퇴 시 사용자 계정 설정에서 앱 연결을 지운다 — Apple ID 설정 "Apple로 로그인" 목록, Google 계정 "타사 앱 및 서비스".
Apple은 계정 삭제 시 토큰 철회가 **App Store 심사 지침 5.1.1(v) 필수**다. Google은 의무는 아니지만 같은 흐름으로 처리한다.

**상태 (2026-09-18)**: 백엔드 구현·머지·dev 배포 완료 (moodi-api #117, `523d847`). **플래그는 꺼져 있다** — 아래 "남은 일"을 끝내야 동작한다.

## 0. 결론 요약

| 항목 | 결정 |
|---|---|
| 철회 시점 | 탈퇴(`POST /members/me/withdrawal`, 관리자 강제 탈퇴 포함) 직전, 같은 요청 안에서 동기 호출 |
| 철회 수단 | 제공자 **refresh token** — 로그인 때 `authorizationCode`를 교환해 `member.provider_refresh_token`에 저장 |
| 토큰 없는 회원 | 탈퇴 요청의 `authorizationCode`(탈퇴 직전 재인증)로 그 자리에서 교환 → 철회 |
| 실패 정책 | 철회 실패는 **탈퇴를 막지 않는다** (ERROR 로그). 사용자의 삭제 요구가 우선 |
| Apple client_secret | 고정값이 아님 — .p8 키로 서명한 ES256 JWT(iss=Team ID, sub=client_id, aud=appleid.apple.com), 요청마다 생성 |
| Apple client_id | 회원마다 다를 수 있음(iOS 번들 ID / 웹 Services ID) → id_token `aud`를 `member.provider_client_id`에 저장해 씀 |
| Google client | serverAuthCode는 항상 **웹(서버) OAuth 클라이언트** 앞으로 발급 → 설정값(`GOOGLE_WEB_CLIENT_ID`/`SECRET`)으로 교환. 철회는 token만 |
| 기본 상태 | `APPLE_TOKEN_ENABLED`/`GOOGLE_TOKEN_ENABLED=false`. 꺼져 있으면 호출마다 WARN 후 best-effort 실패 |
| 하지 않기로 한 것 | Apple Server-to-Server 알림(사용자가 Apple 설정에서 직접 끊었을 때 수신), refresh token 컬럼 암호화 |

## 1. 흐름

```
[로그인]  앱 → POST /api/v1/auth/login { provider, idToken, authorizationCode? }
          서버: id_token 검증 → aud 저장 → (코드 있으면) 제공자 /token 교환 → refresh token 저장. 교환 실패해도 로그인 200

[탈퇴]    앱 → POST /api/v1/members/me/withdrawal { reasons, detail?, authorizationCode? }
          서버: 저장 토큰 ?: 코드 교환 → 제공자 /revoke → (성공/실패 무관) 기존 탈퇴 절차 → 204
```

| 제공자 | 교환 | 철회 |
|---|---|---|
| Apple | `POST https://appleid.apple.com/auth/token` `client_id, client_secret(JWT), code, grant_type=authorization_code` | `POST https://appleid.apple.com/auth/revoke` `client_id, client_secret, token, token_type_hint=refresh_token` |
| Google | `POST https://oauth2.googleapis.com/token` `client_id, client_secret, code, grant_type=authorization_code, redirect_uri=""` | `POST https://oauth2.googleapis.com/revoke` `token` |

## 2. 코드 위치 (member 컨텍스트)

| 레이어 | 파일 | 역할 |
|---|---|---|
| application | `SocialTokenClient` | 포트 — `exchangeRefreshToken(provider, clientId, code)` · `revoke(provider, clientId, token)` |
| application | `AuthService.login(provider, idToken, authorizationCode)` | 교환 → `Member.rememberProviderCredential(aud, token)` |
| application | `MemberWithdrawService.withdraw` | `revokeProviderConnection` → 기존 삭제 절차 |
| domain | `Member.providerClientId` · `providerRefreshToken` | `withdraw()`에서 토큰만 비움(client_id는 재인증 철회용으로 유지) |
| infrastructure/oauth | `AppleTokenClient` · `GoogleTokenClient` · `ProviderRoutingSocialTokenClient` · `SocialTokenClientConfig` | 어댑터·분기·조립(타임아웃 5s/10s) |
| infrastructure/oauth | `AppleTokenProperties`(`oauth.apple.token.*`) · `GoogleTokenProperties`(`oauth.google.token.*`) | 설정 바인딩 |
| DB | `V31__add_member_provider_credential.sql` | `member.provider_client_id VARCHAR(255)`, `provider_refresh_token VARCHAR(2048)` |
| 문서 | `src/docs/asciidoc/member/auth.adoc`(로그인) · `onboarding.adoc`(탈퇴) · `member/CLAUDE.md` · `docs/infrastructure.md` | |

테스트: `AppleTokenClientTest`(client_secret JWT 검증·폼·PEM 파싱) · `GoogleTokenClientTest` · `AuthServiceTest` · `MemberWithdrawServiceTest` · `MemberTest`.
로컬 기동 검증은 토큰/철회 URI를 스텁 서버로 바꿔 했다(`OAUTH_APPLE_TOKEN_TOKEN_URI` 등 환경변수로 덮어쓰기 가능).

## 3. 남은 일 (이 순서대로)

### 3.1 Apple 키 발급 — Apple Developer 계정 보유자
1. developer.apple.com → Certificates, Identifiers & Profiles → **Keys** → `+` → "Sign in with Apple" 체크 → Primary App ID = `kr.moodi.app` → Register.
2. **.p8 다운로드(1회만 가능)**, Key ID 메모. Team ID는 Membership 페이지.
3. 웹 Services ID(`moodi.test`)로도 로그인한다면 그 ID도 같은 키로 서명되므로 키는 하나면 된다.

### 3.2 Google 웹 클라이언트 secret — GCP 콘솔
1. `moodi-app-2026` → API 및 서비스 → 사용자 인증 정보 → OAuth 2.0 클라이언트 ID 중 **웹 애플리케이션** 유형(없으면 생성).
2. 클라이언트 ID·보안 비밀번호 메모. 이 웹 클라이언트 ID가 앱에서 `serverClientID`/`requestServerAuthCode`에 넣는 값과 **같아야 한다**.

### 3.3 Secret Manager + Cloud Run 환경변수 — 인프라
Cloud Run `moodi-api`는 시크릿을 Secret Manager 참조로 받는다(`.github/workflows/deploy.yaml` 주석). 콘솔 또는:
```bash
# 시크릿 생성 (값은 stdin)
printf '%s' 'TEAM_ID' | gcloud secrets create APPLE_TEAM_ID --data-file=- --project=moodi-app-2026
printf '%s' 'KEY_ID'  | gcloud secrets create APPLE_KEY_ID  --data-file=- --project=moodi-app-2026
gcloud secrets create APPLE_PRIVATE_KEY --data-file=AuthKey_KEYID.p8 --project=moodi-app-2026   # PEM 전체
printf '%s' 'xxx.apps.googleusercontent.com' | gcloud secrets create GOOGLE_WEB_CLIENT_ID --data-file=- --project=moodi-app-2026
printf '%s' 'GOCSPX-...'                     | gcloud secrets create GOOGLE_CLIENT_SECRET  --data-file=- --project=moodi-app-2026

# Cloud Run에 연결 + 플래그 on (deploy.yaml의 --update-env-vars는 기존 env를 지우지 않으므로 여기서 켠 값은 유지된다)
gcloud run services update moodi-api --region=asia-northeast3 --project=moodi-app-2026 \
  --update-secrets=APPLE_TEAM_ID=APPLE_TEAM_ID:latest,APPLE_KEY_ID=APPLE_KEY_ID:latest,APPLE_PRIVATE_KEY=APPLE_PRIVATE_KEY:latest,GOOGLE_WEB_CLIENT_ID=GOOGLE_WEB_CLIENT_ID:latest,GOOGLE_CLIENT_SECRET=GOOGLE_CLIENT_SECRET:latest \
  --update-env-vars=APPLE_TOKEN_ENABLED=true,GOOGLE_TOKEN_ENABLED=true
```
- Cloud Run 서비스 계정에 `roles/secretmanager.secretAccessor`가 없으면 각 시크릿에 부여.
- **`APPLE_TOKEN_ENABLED=true`인데 키가 비어 있으면 기동 실패**(의도된 fail-fast). 시크릿 먼저.
- 기동 후 로그에 `토큰 클라이언트 미구성` WARN이 안 나오면 정상.

### 3.4 앱 클라이언트 — 로그인 요청에 코드 전달
| 플랫폼 | 값 | 주의 |
|---|---|---|
| iOS Apple | `ASAuthorizationAppleIDCredential.authorizationCode` → `String(data:encoding:.utf8)` | 5분·1회용. 로그인 요청에 바로 |
| iOS Google | `GIDSignIn` 설정에 `serverClientID`(= 3.2 웹 클라이언트 ID) → `signInResult.serverAuthCode` | `serverClientID` 없으면 nil |
| Android Google | `GoogleSignInOptions.requestServerAuthCode(webClientId, /* forceCodeForRefreshToken */ true)` → `account.serverAuthCode` | **`true` 아니면 재동의가 없어 refresh_token이 안 내려온다** |
| 탈퇴 화면(선택) | 이 기능 전에 가입한 회원용. 탈퇴 직전 제공자 재인증 → 코드를 `authorizationCode`로 | 없어도 탈퇴는 됨(철회만 안 됨) |

### 3.5 검증
1. dev(`dev-api.moodi.kr`)에서 Apple로 새로 로그인(코드 포함) → DB `member.provider_refresh_token` 채워졌는지.
2. 탈퇴 → 서버 로그에 `철회 실패` ERROR 없음 → iPhone 설정 > Apple ID > 로그인 및 보안 > Apple로 로그인 목록에서 사라짐.
3. Google도 동일 → myaccount.google.com > 보안 > 타사 앱 및 서비스.
4. 이 기능 전에 가입한 계정(예: `moodi-test`)은 서버에 토큰이 없다 → 탈퇴 화면 재인증(3.4 마지막 행)을 붙이거나, 사용자가 Apple 설정에서 직접 "Apple ID 사용 중단".

## 4. 에러·로그 표

| 상황 | 응답 | 로그 |
|---|---|---|
| 로그인 코드 교환 실패 | 200 (로그인 성공) | WARN `제공자 refresh token 교환 실패` |
| 탈퇴, 토큰 없음 | 204 | INFO `철회할 제공자 토큰 없음` |
| 탈퇴, 철회 거절/장애 | 204 | ERROR `제공자 계정 연결 철회 실패` + WARN(제공자 응답 본문) |
| 플래그 off | 200/204 | WARN `토큰 클라이언트 미구성` |
| enabled인데 키 없음 | **기동 실패** | `oauth.apple.token.private-key가 비어 있습니다` |
