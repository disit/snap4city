local Redis = require "resty.redis"
local kong = kong

local RequestLimiter = {
  PRIORITY = 1000,  -- High priority to run early
  VERSION = "1.0",
}

function RequestLimiter:access(config)
  local red = Redis:new()
  red:set_timeout(1000)

  -- Connect to Redis
  local ok, err = red:connect(config.redis_host, config.redis_port)
  if not ok then
    kong.log.err("Failed to connect to Redis: ", err)
    return kong.response.exit(500, { message = "Internal server error" })
  end

  local key = "request_counter"

  -- Get current request count
  local count, err = red:get(key)
  if err then
    kong.log.err("Failed to get count from Redis: ", err)
    return kong.response.exit(500, { message = "Internal server error" })
  end

  -- Convert count to number or initialize to 0
  count = tonumber(count) or 0

  if count >= config.limit then
    return kong.response.exit(429, { message = "Request limit reached" })
  end

  -- Increment request count
  red:incr(key)
end

return RequestLimiter
