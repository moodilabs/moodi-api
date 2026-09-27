--[[
  Semaphore permit 반납.

  KEYS[1] = llm:semaphore
  ARGV[1] = callerId

  반환: 제거된 멤버 수 (1 = 정상 반납, 0 = 이미 만료/반납됨)
]]

return redis.call('ZREM', KEYS[1], ARGV[1])
