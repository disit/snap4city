return {
  name = "total-accesses",
  fields = {
    { config = {
        type = "record",
        fields = {
          { limit = { type = "integer", default = 100 } },
          { redis_host = { type = "string", default = "redis" } },
          { redis_port = { type = "integer", default = 6379 } }
        }
    } }
  }
}
