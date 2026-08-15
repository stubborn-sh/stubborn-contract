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
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.function.Function;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.BDDAssertions;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import sh.stubborn.contract.stubrunner.StubFinder;
import sh.stubborn.contract.stubrunner.StubsMode;
import sh.stubborn.contract.stubrunner.spring.AutoConfigureStubRunner;
import sh.stubborn.contract.verifier.messaging.MessageVerifierReceiver;

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

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = StubRunnerStubsPerConsumerWithConsumerNameTests.Config.class,
		properties = { "stubborn.contract.stubrunner.jms.enabled=false", "spring.cloud.function.definition=bookHandler",
				"spring.cloud.stream.bindings.bookHandler-in-0.destination=output",
				"spring.cloud.stream.bindings.bookHandler-out-0.destination=outputToAssertBook" })
@AutoConfigureStubRunner(ids = "sh.stubborn.contract.verifier.stubs:producerWithMultipleConsumers",
		repositoryRoot = "classpath:m2repo/repository/", consumerName = "foo-consumer", stubsMode = StubsMode.REMOTE,
		stubsPerConsumer = true)
@ActiveProfiles("streamconsumer")
class StubRunnerStubsPerConsumerWithConsumerNameTests {

	@Autowired
	StubFinder stubFinder;

	@Autowired
	Environment environment;

	@Autowired
	MessageVerifierReceiver<Message<?>> messaging;

	@Test
	void shouldStartHttpStubServersForFooConsumerOnly() throws Exception {
		URL stubUrl = this.stubFinder.findStubUrl("producerWithMultipleConsumers");
		RestTemplate template = new RestTemplate();
		ResponseEntity<String> entity = template.getForEntity(stubUrl + "/foo-consumer", String.class);
		assertThat(entity.getStatusCode().value()).isEqualTo(200);
		// the not-matching consumer's stub is not served — RestTemplate throws on the 404
		BDDAssertions.thenThrownBy(() -> template.getForEntity(stubUrl + "/bar-consumer", String.class))
			.isInstanceOf(org.springframework.web.client.HttpClientErrorException.NotFound.class);
	}

	@Test
	void shouldTriggerAMessageByLabelFromProperConsumer() {
		this.stubFinder.trigger("return_book_for_foo");
		Awaitility.await().untilAsserted(() -> {
			Message<?> receivedMessage = this.messaging.receive("outputToAssertBook");
			assertThat(receivedMessage).isNotNull();
			Message<?> message = Objects.requireNonNull(receivedMessage);
			assertThat(bookName(message.getPayload())).isEqualTo("foo_for_foo");
			assertThat(message.getHeaders().get("BOOK-NAME")).isEqualTo("foo_for_foo");
		});
	}

	@Test
	void shouldNotTriggerAMessageByTheNotMatchingConsumer() {
		BDDAssertions.thenThrownBy(() -> this.stubFinder.trigger("return_book_for_bar"))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("No label with name [return_book_for_bar] was found");
	}

	private static String bookName(Object payload) {
		try {
			String json = (payload instanceof byte[] bytes) ? new String(bytes, StandardCharsets.UTF_8)
					: (payload instanceof String string) ? string : new ObjectMapper().writeValueAsString(payload);
			return new ObjectMapper().readTree(json).get("bookName").asText();
		}
		catch (Exception ex) {
			throw new IllegalStateException("Could not read bookName from payload [" + payload + "]", ex);
		}
	}

	@Configuration
	@EnableAutoConfiguration
	@ImportAutoConfiguration(TestChannelBinderConfiguration.class)
	static class Config {

		// Identity function bridging the contract's 'output' destination to
		// 'outputToAssertBook'; deliberately not named after any destination so it does
		// not
		// shadow the destination-name bean lookup in the messaging backend.
		@Bean
		Function<String, String> bookHandler() {
			return (o) -> o;
		}

	}

}
