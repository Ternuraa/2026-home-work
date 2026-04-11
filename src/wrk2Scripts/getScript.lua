
counter = 0
max_counter = 430.8

function request()
	path = "/v1/entity/" .. (counter % max_counter)
	counter = counter + 1
	return wrk.format("GET", path)
end
