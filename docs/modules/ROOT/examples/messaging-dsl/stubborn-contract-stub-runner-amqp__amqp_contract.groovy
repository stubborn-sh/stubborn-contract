// tag::amqp_contract[]
			Contract.make {
				// Human readable description
				description 'Should produce valid person data'
				// Label by means of which the output message can be triggered
				label 'contract-test.person.created.event'
				// input to the contract
				input {
					// the contract will be triggered by a method
					triggeredBy('createPerson()')
				}
				// output message of the contract
				outputMessage {
					// destination to which the output message will be sent
					sentTo 'contract-test.exchange'
					headers {
						header('contentType': 'application/json')
						header('__TypeId__': 'sh.stubborn.contract.stubrunner.messaging.amqp.Person')
					}
					// the body of the output message
					body([
							id  : $(consumer(9), producer(regex("[0-9]+"))),
							name: "me"
					])
				}
			}
			
// end::amqp_contract[]
