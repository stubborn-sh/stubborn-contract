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

package sh.stubborn.contract.stubrunner.consumer.converters;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import org.awaitility.Awaitility;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;
import sh.stubborn.contract.stubrunner.StubFinder;
import sh.stubborn.contract.stubrunner.StubsMode;
import sh.stubborn.contract.stubrunner.spring.AutoConfigureStubRunner;
import sh.stubborn.contract.verifier.messaging.boot.AutoConfigureMessageVerifier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.assertj.core.api.BDDAssertions.then;

/**
 * Zero-config Kafka consumer contract test against a real broker (Testcontainers). The
 * consumer application configures <strong>no</strong> JSON deserializer and
 * <strong>no</strong> converter: it relies entirely on
 * {@code ContractVerifierKafkaConsumerConverterConfiguration} (activated through
 * {@code @AutoConfigureStubRunner} / {@code @AutoConfigureMessageVerifier}) to populate a
 * typed {@code @KafkaListener} parameter from the contract's JSON {@code outputMessage}.
 *
 * <p>
 * Without the auto-config the record value arrives as a {@code String} (Boot's default
 * {@code StringDeserializer}) and the framework has no converter to bind it to
 * {@link BookReturned}, so the listener would never be invoked and this test would time
 * out — which is exactly what makes it a genuine end-to-end proof of the feature.
 *
 * @author Marcin Grzejszczak
 */
@SpringBootTest(classes = KafkaConsumerConverterSpec.Config.class,
		properties = { "stubborn.contract.stubrunner.kafka.enabled=true",
				"stubborn.contract.stubrunner.rabbit.enabled=false", "spring.kafka.consumer.group-id=converters-test",
				"spring.kafka.consumer.auto-offset-reset=earliest",
				"spring.autoconfigure.exclude=org.springframework.boot.amqp.autoconfigure.RabbitAutoConfiguration" })
@AutoConfigureStubRunner(ids = "my:kafka", repositoryRoot = "stubs:classpath:/stubs-kafka/",
		stubsMode = StubsMode.REMOTE)
@AutoConfigureMessageVerifier
@DisabledOnOs(OS.WINDOWS)
@Testcontainers
class KafkaConsumerConverterSpec {

	@Container
	private static final KafkaContainer KAFKA = new KafkaContainer(DockerImageName.parse("apache/kafka:3.8.1"));

	@DynamicPropertySource
	static void kafkaProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
	}

	@Autowired
	StubFinder stubFinder;

	@Autowired
	TypedBookListener listener;

	@Test
	void shouldBindTypedListenerParameterWithoutAnyManualJsonConfiguration() {
		this.stubFinder.trigger("return_book_typed");

		Awaitility.await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
			BookReturned received = this.listener.received();
			then(received).isNotNull();
			then(received.getBookName()).isEqualTo("foo");
		});
	}

	@Configuration
	@EnableAutoConfiguration
	@EnableKafka
	static class Config {

		@Bean
		TypedBookListener typedBookListener() {
			return new TypedBookListener();
		}

	}

	static class TypedBookListener {

		private final AtomicReference<BookReturned> received = new AtomicReference<>();

		// The stub message carries a JSON body and no __TypeId__ header; the target type
		// is
		// inferred from this method parameter.
		@KafkaListener(topics = "book-returned")
		void listen(BookReturned book) {
			this.received.set(book);
		}

		@Nullable BookReturned received() {
			return this.received.get();
		}

	}

}
