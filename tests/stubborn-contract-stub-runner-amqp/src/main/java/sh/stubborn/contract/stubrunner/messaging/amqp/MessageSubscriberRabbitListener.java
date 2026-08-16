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

import org.jspecify.annotations.Nullable;

import org.springframework.amqp.rabbit.annotation.RabbitListener;

/**
 * Rabbit listener.
 *
 * @author Mathias Düsterhöft
 */
public class MessageSubscriberRabbitListener {

	private @Nullable Person person;

	// tag::amqp_annotated_listener[]
	// The stub-runner Rabbit backend publishes to a queue named after the contract's
	// sentTo destination via the AMQP default exchange (the queue is declared as a bean
	// in RabbitListenerConfig), so the annotated listener consumes it by name.
	@RabbitListener(queues = "contract-test.exchange")
	public void handlePerson(Person person) {
		this.person = person;
	}

	// end::amqp_annotated_listener[]
	public @Nullable Person getPerson() {
		return this.person;
	}

}
