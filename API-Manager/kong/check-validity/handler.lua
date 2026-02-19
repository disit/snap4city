local http = require "resty.http"
local cjson = require "cjson"
local kong = kong

local plugin = {
    PRIORITY = 1000,
    VERSION = "1.0.0",
}

function plugin:access(conf)
    local httpc = http.new()

    -- Capture original request details
    local request_body = kong.request.get_raw_body()
    local request_headers = kong.request.get_headers()
    local request_method = kong.request.get_method()
    local request_path = kong.request.get_path()
    local request_query = kong.request.get_raw_query()
    
    -- Construct full original URL
    local full_request_url = request_path
    if request_query and request_query ~= "" then
        full_request_url = full_request_url .. "?" .. request_query
    end



    
    -- Add original URL to headers
    request_headers["X-Original-Request"] = full_request_url
    request_headers["X-Real-IP"] = client_ip
    local client_ip = ngx.var.realip_remote_addr or ngx.var.remote_addr or "unknown"

    -- build X-Forwarded-For header
    if request_headers["X-Forwarded-For"] then
    request_headers["X-Forwarded-For"] = request_headers["X-Forwarded-For"] .. ", " .. client_ip
    else
    request_headers["X-Forwarded-For"] = client_ip
    end

    -- Forward request to checkme
    local res, err = httpc:request_uri(conf.checkme_url, {
        method = request_method,
        body = request_body,
        headers = request_headers,
    })

    if not res then
        return kong.response.exit(500, { message = "Internal Server Error: Unable to contact verification service." })
    end

    if res.status ~= 200 then
        return kong.response.exit(403, { message = res.body or "Access Denied: Verification failed." })
    end
	
	local success, checkme_response = pcall(cjson.decode, res.body)
    if not success or type(checkme_response) ~= "table" then
        return kong.response.exit(500, { message = "Invalid response from verification service." })
    end

    -- Extract the ID from checkme's response
    local checkme_id = checkme_response.id
    if not checkme_id then
        return kong.response.exit(403, { message = "Access Denied: No ID provided by verification service." })
    end

    -- Add the received ID to the original request headers
    kong.service.request.set_header("X-Checkme-ID", checkme_id)
end

return plugin
