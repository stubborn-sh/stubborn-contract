// tag::sample_dsl[]
			Contract.make {
				label 'return_book_1'
				input {
					triggeredBy('bookReturnedTriggered()')
				}
				outputMessage {
					sentTo('output')
					body('''{ "bookName" : "foo" }''')
					headers {
						header('BOOKNAME', 'foo')
					}
				}
			}
	
// end::sample_dsl[]
