
counter = 0
body = "Test data for database"

function request()
	path = "/v1/entity/" .. counter
	counter = counter + 1
	return wrk.format("PUT", path, {["Content-Type"] = "text/plain"}, body)
end
