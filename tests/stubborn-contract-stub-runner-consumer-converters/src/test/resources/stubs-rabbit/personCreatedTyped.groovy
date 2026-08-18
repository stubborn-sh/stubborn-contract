sh.stubborn.contract.spec.Contract.make {
	description 'Should produce valid person data with no __TypeId__ header'
	label 'person_created_typed'
	input {
		triggeredBy('createPerson()')
	}
	outputMessage {
		sentTo 'consumer-converters.person.queue'
		headers {
			header('contentType': 'application/json')
		}
		body([
				id  : $(consumer(9), producer(regex("[0-9]+"))),
				name: 'me'
		])
	}
}
