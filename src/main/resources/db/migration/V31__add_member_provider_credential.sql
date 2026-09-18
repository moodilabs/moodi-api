-- 소셜 제공자 계정 연결 철회용 자격. 탈퇴 시 Apple(심사 5.1.1(v))·Google 쪽 연결을 끊는 데 쓴다.
-- provider_client_id: 마지막 로그인 id_token의 aud (Apple client_secret의 sub로 필요)
-- provider_refresh_token: 인가 코드 교환으로 받은 제공자 refresh token. 탈퇴 시 철회 후 비운다.
ALTER TABLE member
    ADD COLUMN provider_client_id VARCHAR(255),
    ADD COLUMN provider_refresh_token VARCHAR(2048);
