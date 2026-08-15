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

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import sh.stubborn.contract.stubrunner.StubTrigger;
import sh.stubborn.contract.stubrunner.spring.AutoConfigureStubRunner;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;

@AutoConfigureStubRunner
@SpringBootTest(classes = AmqpMessagingApplication.class)
@Disabled("Legacy embedded-broker spec superseded by real-broker coverage - the "
		+ "stubborn-contract-messaging-tck conformance suite and the broker-backed consumer ITs "
		+ "(stubborn samples). Kept until migrated to the Testcontainers/middleware approach.")
class AmqpStubRunnerSpec {

	@Autowired
	StubTrigger stubTrigger;

	@MockitoSpyBean
	MessageSubscriber messageSubscriber;

	ArgumentCaptor<Person> personArgumentCaptor = ArgumentCaptor.forClass(Person.class);

	@Test
	void shouldTriggerStubAmqpMessage() {
		// tag::client_trigger[]
		this.stubTrigger.trigger("contract-test.person.created.event");
		// end::client_trigger[]

		then(this.messageSubscriber).should().handleMessage(this.personArgumentCaptor.capture());
		assertThat(this.personArgumentCaptor.getValue().getName()).isNotNull();
	}

}
