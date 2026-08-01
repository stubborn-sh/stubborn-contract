// tag::method_trigger[]
			def dsl = Contract.make {
				// Human readable description
				description 'Some description'
				// Label by means of which the output message can be triggered
				label 'some_label'
				// input to the contract
				input {
					// the contract will be triggered by a method
					triggeredBy('bookReturnedTriggered()')
				}
				// output message of the contract
				outputMessage {
					// destination to which the output message will be sent
					sentTo('output')
					// the body of the output message
					body('''{ "bookName" : "foo" }''')
					// the headers of the output message
					headers {
						header('BOOK-NAME', 'foo')
					}
				}
			}
			
// end::method_trigger[]
