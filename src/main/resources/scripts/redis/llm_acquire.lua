--[[
  LLM 호출 허가 — 분산 Semaphore + 슬라이딩 윈도우 Rate Limiter.

  KEYS[1] = llm:semaphore       (sorted set: callerId → expireAt)
  KEYS[2] = llm:rate            (sorted set: callerId → timestamp)
  KEYS[3] = llm:cooldown        (string: cooldown-until epoch ms)

  ARGV[1] = maxConcurrent       (Semaphore permit 수)
  ARGV[2] = maxRequestsPerMinute
  ARGV[3] = callerId            (UUID — permit 소유자 식별)
  ARGV[4] = now                 (epoch ms)
  ARGV[5] = leaseMillis         (permit TTL — 소유자가 release 못 하면 자동 만료)

  반환: 1 = 허가, 0 = 거부
]]

local semKey      = KEYS[1]
local rateKey     = KEYS[2]
local cooldownKey = KEYS[3]

local maxConcurrent  = tonumber(ARGV[1])
local maxRpm         = tonumber(ARGV[2])
local callerId       = ARGV[3]
local now            = tonumber(ARGV[4])
local leaseMillis    = tonumber(ARGV[5])

-- 1. Cooldown 체크 (429 Retry-After)
local cooldownUntil = redis.call('GET', cooldownKey)
if cooldownUntil and tonumber(cooldownUntil) > now then
    return 0
end

-- 2. Semaphore: 만료된 lease 제거 후 현재 점유 수 확인
redis.call('ZREMRANGEBYSCORE', semKey, '-inf', now)
local currentPermits = redis.call('ZCARD', semKey)
if currentPermits >= maxConcurrent then
    return 0
end

-- 3. Rate Limiter: 1분 윈도우 밖의 기록 제거 후 카운트
local windowStart = now - 60000
redis.call('ZREMRANGEBYSCORE', rateKey, '-inf', windowStart)
local requestCount = redis.call('ZCARD', rateKey)
if requestCount >= maxRpm then
    return 0
end

-- 4. 허가: Semaphore에 lease 등록 + Rate Limiter에 타임스탬프 기록
local expireAt = now + leaseMillis
redis.call('ZADD', semKey, expireAt, callerId)
redis.call('ZADD', rateKey, now, callerId)

-- 키 TTL 설정 (가비지 방지)
redis.call('PEXPIRE', semKey, leaseMillis + 1000)
redis.call('PEXPIRE', rateKey, 61000)

return 1
