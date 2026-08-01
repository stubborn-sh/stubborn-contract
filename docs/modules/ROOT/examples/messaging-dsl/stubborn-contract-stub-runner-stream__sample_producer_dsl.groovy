// tag::sample_producer_dsl[]
			Contract.make {
				label 'return_book_2'
				input { triggeredBy('gotAMessageFromFunction()') }
				outputMessage {
					sentTo('outputToAssertBook')
					body('''{ "bookName" : "foo" }''')
					headers { header('BOOK-NAME', 'foo') }
				}
			}
	
// end::sample_producer_dsl[]
