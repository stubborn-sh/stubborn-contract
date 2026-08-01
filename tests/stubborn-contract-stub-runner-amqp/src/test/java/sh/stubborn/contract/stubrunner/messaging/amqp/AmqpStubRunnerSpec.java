/*
 * Copyright 2013-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package sh.stubborn.contract.stubrunner.messaging.amqp;

import java.util.Map;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import sh.stubborn.contract.spec.Contract;
import sh.stubborn.contract.stubrunner.StubTrigger;
import sh.stubborn.contract.stubrunner.spring.AutoConfigureStubRunner;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;

@AutoConfigureStubRunner
@SpringBootTest(classes = AmqpMessagingApplication.class)
@Disabled("TODO: Migrate to middleware based approach")
class AmqpStubRunnerSpec {

	@Autowired
	StubTrigger stubTrigger;

	@MockitoSpyBean
	MessageSubscriber messageSubscriber;

	ArgumentCaptor<Person> personArgumentCaptor = ArgumentCaptor.forClass(Person.class);

	@Test
	@DisplayName("should trigger stub amqp message")
	void shouldTriggerStubAmqpMessage() {
		// tag::amqp_contract[]

		Contract.make((c) -> {
			// Human readable description
			c.description("Should produce valid person data");
			// Label by means of which the output message can be triggered
			c.label("contract-test.person.created.event");
			// input to the contract
			c.input((i) -> i.triggeredBy("createPerson()"));
			// output message of the contract
			c.outputMessage((o) -> {
				// destination to which the output message will be sent
				o.sentTo("contract-test.exchange");
				o.headers((h) -> {
					h.header("contentType", "application/json");
					h.header("__TypeId__", "sh.stubborn.contract.stubrunner.messaging.amqp.Person");
				});
				// the body of the output message
				o.body(Map.of("id", o.$(o.consumer(9), o.producer(o.regex("[0-9]+"))), "name", "me"));
			});
		});
		// end::amqp_contract[]

		// tag::client_trigger[]
		this.stubTrigger.trigger("contract-test.person.created.event");
		// end::client_trigger[]

		then(this.messageSubscriber).should().handleMessage(this.personArgumentCaptor.capture());
		assertThat(this.personArgumentCaptor.getValue().getName()).isNotNull();
	}

}
