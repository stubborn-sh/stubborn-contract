import sh.stubborn.contract.spec.Contract
Contract.make {
	request {
		method 'GET'
		urlPath '/get'
		body([
				duck                : 123,
				alpha               : "abc",
				number              : 123,
				aBoolean            : true,
				date                : "2017-01-01",
				dateTime            : "2017-01-01T01:23:45",
				time                : "01:02:34",
				valueWithoutAMatcher: "foo",
				valueWithTypeMatch  : "string",
				key                 : [
						'complex.key': 'foo'
				]
		])
		bodyMatchers {
			jsonPath('$.duck', byRegex("[0-9]{3}"))
			jsonPath('$.duck', byEquality())
			jsonPath('$.alpha', byRegex(onlyAlphaUnicode()))
			jsonPath('$.alpha', byEquality())
			jsonPath('$.number', byRegex(number()))
			jsonPath('$.aBoolean', byRegex(anyBoolean()))
			jsonPath('$.date', byDate())
			jsonPath('$.dateTime', byTimestamp())
			jsonPath('$.time', byTime())
			jsonPath("\$.['key'].['complex.key']", byEquality())
		}
		headers {
			header("sample", $(c(regex('foo.*')), p('foo')))
			header("Content-Type", applicationJson())
		}
	}
	response {
		status OK()
		body([
				duck                 : 123,
				alpha                : "abc",
				number               : 123,
				positiveInteger      : 1234567890,
				negativeInteger      : -1234567890,
				positiveDecimalNumber: 123.4567890,
				negativeDecimalNumber: -123.4567890,
				aBoolean             : true,
				date                 : "2017-01-01",
				dateTime             : "2017-01-01T01:23:45",
				time                 : "01:02:34",
				valueWithoutAMatcher : "foo",
				valueWithTypeMatch   : "string",
				valueWithMin         : [
						1, 2, 3
				],
				valueWithMax         : [
						1, 2, 3
				],
				valueWithMinMax      : [
						1, 2, 3
				],
				valueWithMinEmpty    : [],
				valueWithMaxEmpty    : [],
				key                  : [
						'complex.key': 'foo'
				],
				nullValue            : null
		])
		bodyMatchers {
			jsonPath('$.duck', byRegex("[0-9]{3}"))
			jsonPath('$.duck', byEquality())
			jsonPath('$.alpha', byRegex(onlyAlphaUnicode()))
			jsonPath('$.alpha', byEquality())
			jsonPath('$.number', byRegex(number()))
			jsonPath('$.positiveInteger', byRegex(positiveInt()))
			jsonPath('$.integer', byRegex(anInteger()))
			jsonPath('$.double', byRegex(aDouble()))
			jsonPath('$.aBoolean', byRegex(anyBoolean()))
			jsonPath('$.date', byDate())
			jsonPath('$.dateTime', byTimestamp())
			jsonPath('$.time', byTime())
			jsonPath('$.valueWithTypeMatch', byType())
			jsonPath('$.valueWithMin', byType {
				minOccurrence(1)
			})
			jsonPath('$.valueWithMax', byType {
				maxOccurrence(3)
			})
			jsonPath('$.valueWithMinMax', byType {
				minOccurrence(1)
				maxOccurrence(3)
			})
			jsonPath('$.valueWithMinEmpty', byType {
				minOccurrence(0)
			})
			jsonPath('$.valueWithMaxEmpty', byType {
				maxOccurrence(0)
			})
			jsonPath('$.duck', byCommand('assertThatValueIsANumber($it)'))
			jsonPath("\$.['key'].['complex.key']", byEquality())
			jsonPath('$.nullValue', byNull())
		}
		headers {
			contentType(applicationJson())
			header('Some-Header', $(c('someValue'), p(regex('[a-zA-Z]{9}'))))
		}
	}
}
