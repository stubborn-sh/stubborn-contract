import sh.stubborn.contract.spec.Contract
Contract.make {
	request {
		method "PUT"
		url "/multipart"
		headers {
			contentType('multipart/form-data;boundary=AaB03x')
		}
		multipart(
				formParameter: $(c(regex('".+"')), p('"formParameterValue"')),
				someBooleanParameter: $(c(regex(anyBoolean())), p('true')),
				file: named(
						name: $(c(regex(nonEmpty())), p('filename.csv')),
						content: $(c(regex(nonEmpty())), p('file content')),
						contentType: $(c(regex(nonEmpty())), p('application/json')))
		)
	}
	response {
		status OK()
	}
}
