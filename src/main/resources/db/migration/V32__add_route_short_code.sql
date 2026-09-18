-- 루트 공유 단축 링크(/s/{code})용 코드. 공유 활성화 시점에 base62 8자리로 생성한다.
-- 이미 공유 중인 루트는 NULL로 두고, 소유자가 다시 공유(share)할 때 채운다 — 앱은 공유할 때마다
-- share API를 호출하므로 자연스럽게 백필된다. 부분 유니크 인덱스라 NULL 행은 충돌하지 않는다.
ALTER TABLE route ADD COLUMN short_code VARCHAR(10);
CREATE UNIQUE INDEX uk_route_short_code ON route (short_code) WHERE short_code IS NOT NULL;
