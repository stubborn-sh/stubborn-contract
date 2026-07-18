import sh.stubborn.contract.spec.Contract
Contract.make {
	priority(1)
	name "Account creating without sign"
	request {
		method POST()
		urlPath("/account")
		headers {
			contentType(applicationJson())
			header("userId", anyNonBlankString())
			header("sign", "")
		}
		body(
				"number": anyNonBlankString(),
				"currency": "USD",
				"owner": "owner"
		)
	}
	response {
		status CREATED()
		headers {
			contentType(applicationJson())
		}
		body(
				"id": anyNonBlankString(),
				"number": "number",
				"currency": "USD",
				"owner": "owner",
				"balance": 0
		)
	}
}
