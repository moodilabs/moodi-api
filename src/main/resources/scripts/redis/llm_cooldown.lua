--[[
  429 Retry-After cooldown 설정.

  KEYS[1] = llm:cooldown
  ARGV[1] = cooldownUntilMs   (epoch ms)
  ARGV[2] = ttlMs             (key 만료 시간)

  반환: 항상 1
]]

redis.call('SET', KEYS[1], ARGV[1], 'PX', ARGV[2])
return 1
