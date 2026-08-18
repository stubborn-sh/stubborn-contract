sh.stubborn.contract.spec.Contract.make {
	label 'return_book_typed'
	input {
		triggeredBy('bookReturnedTriggered()')
	}
	outputMessage {
		sentTo('book-returned')
		body('''{ "bookName" : "foo" }''')
		headers {
			header('contentType': 'application/json')
		}
	}
}
