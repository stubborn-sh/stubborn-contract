import sh.stubborn.contract.spec.Contract
Contract.make {
	request {
		method 'GET'
		urlPath '/get'
	}
	response {
		status(OK())
	}
}
