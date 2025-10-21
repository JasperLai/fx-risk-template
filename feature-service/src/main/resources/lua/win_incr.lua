-- Sliding window counter by seconds
local base = KEYS[1]
local now = tonumber(ARGV[1])
local win = tonumber(ARGV[2])
local bucket = math.floor(now/1)
local key = base..":"..bucket
local ttl = win + 2
redis.call('INCR', key)
redis.call('EXPIRE', key, ttl)
local sum = 0
for i=0,win-1,1 do
  local k = base..":"..(bucket - i)
  local v = redis.call('GET', k)
  if v then sum = sum + tonumber(v) end
end
return sum
