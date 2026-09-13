-- 관리자 로그인 식별자를 이메일에서 아이디(login_id)로 바꾸고, 초기 비밀번호 강제 변경 플래그를 추가한다.
-- 기존 계정의 아이디는 이메일의 '@' 앞부분을 쓰고, 모두 비밀번호 변경 대상으로 둔다(초기 비밀번호가 env에 평문으로 남아 있어서).
ALTER TABLE admin_account RENAME COLUMN email TO login_id;
ALTER TABLE admin_account RENAME CONSTRAINT uk_admin_account_email TO uk_admin_account_login_id;
ALTER TABLE admin_account ALTER COLUMN login_id TYPE VARCHAR(20)
    USING LOWER(CASE WHEN POSITION('@' IN login_id) > 0
                     THEN SUBSTR(login_id, 1, POSITION('@' IN login_id) - 1)
                     ELSE login_id END);

ALTER TABLE admin_account ADD COLUMN password_change_required BOOLEAN NOT NULL DEFAULT FALSE;
UPDATE admin_account SET password_change_required = TRUE;
