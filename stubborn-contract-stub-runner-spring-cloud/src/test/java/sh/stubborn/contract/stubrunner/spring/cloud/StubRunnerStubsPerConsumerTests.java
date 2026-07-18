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

package sh.stubborn.contract.stubrunner.spring.cloud;

import java.net.URL;
import java.util.function.Function;

import org.assertj.core.api.BDDAssertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.Message;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestTemplate;

import sh.stubborn.contract.stubrunner.StubFinder;
import sh.stubborn.contract.stubrunner.spring.AutoConfigureStubRunner;
import sh.stubborn.contract.stubrunner.StubsMode;
import sh.stubborn.contract.stubrunner.spring.StubRunnerProperties;
import sh.stubborn.contract.verifier.messaging.MessageVerifierReceiver;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = StubRunnerStubsPerConsumerTests.Config.class,
		properties = { "spring.application.name=bar-consumer", "spring.cloud.contract.stubrunner.jms.enabled=false" })
@AutoConfigureStubRunner(ids = "sh.stubborn.contract.verifier.stubs:producerWithMultipleConsumers",
		repositoryRoot = "classpath:m2repo/repository/", stubsMode = StubsMode.REMOTE, stubsPerConsumer = true)
@ActiveProfiles("streamconsumer")
@Disabled("TODO: verify stubs per consumer stream test")
class StubRunnerStubsPerConsumerTests {

	@Autowired
	StubFinder stubFinder;

	@Autowired
	Environment environment;

	@Autowired
	MessageVerifierReceiver<Message<?>> messaging;

	@Test
	void shouldStartHttpStubServersForBarConsumerOnly() throws Exception {
		URL stubUrl = this.stubFinder.findStubUrl("producerWithMultipleConsumers");
		RestTemplate template = new RestTemplate();
		ResponseEntity<String> entity = template.getForEntity(stubUrl + "/bar-consumer", String.class);
		assertThat(entity.getStatusCode().value()).isEqualTo(200);
		ResponseEntity<String> notFound = template.getForEntity(stubUrl + "/foo-consumer", String.class);
		assertThat(notFound.getStatusCode().value()).isEqualTo(404);
	}

	@Test
	void shouldTriggerAMessageByLabelFromProperConsumer() {
		this.stubFinder.trigger("return_book_for_bar");
		Message<?> receivedMessage = this.messaging.receive("output");
		assertThat(receivedMessage).isNotNull();
		assertThat(receivedMessage.getPayload()).isEqualTo("{\"bookName\":\"foo_for_bar\"}".getBytes());
		assertThat(receivedMessage.getHeaders().get("BOOK-NAME")).isEqualTo("foo_for_bar");
	}

	@Test
	void shouldNotTriggerAMessageByTheNotMatchingConsumer() {
		BDDAssertions.thenThrownBy(() -> this.stubFinder.trigger("return_book_for_foo"))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("No label with name [return_book_for_foo] was found");
	}

	@Configuration
	@EnableAutoConfiguration
	@ImportAutoConfiguration(TestChannelBinderConfiguration.class)
	static class Config {

		@Bean
		Function<Object, Object> output() {
			return (o) -> o;
		}

	}

}
