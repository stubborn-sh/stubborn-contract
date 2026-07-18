import sh.stubborn.contract.spec.Contract
Contract.make {
	request {
		method 'PUT'
		urlPath('/fraudcheck') {
			queryParameters {
				parameter("foo", "bar")
				parameter("foo2", $(c(equalToJson('{"foo":"bar"}')), p("foo3")))
			}
		}
		body([
				"client.id": $(regex('[0-9]{10}')),
				loanAmount : 99999
		])
		headers {
			contentType('application/json')
			header(authorization(), $(c('Bearer SOMETOKEN'), p(execute('authToken()'))))
		}
	}
	response {
		status OK()
		body([
				fraudCheckStatus  : "${value(regex("FRAUD"))}",
				"rejection.reason": "Amount too high"
		])
		headers {
			contentType('application/json')
		}
	}
}
