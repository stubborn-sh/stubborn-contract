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

package sh.stubborn.contract.verifier.messaging.kafka;

import java.util.Collection;

import sh.stubborn.contract.verifier.messaging.MessageVerifierReceiver;
import sh.stubborn.contract.verifier.messaging.MessageVerifierSender;
import sh.stubborn.contract.verifier.messaging.integration.ContractVerifierIntegrationConfiguration;
import sh.stubborn.contract.verifier.messaging.internal.ContractVerifierMessaging;
import sh.stubborn.contract.verifier.messaging.noop.NoOpContractVerifierAutoConfiguration;
import sh.stubborn.messaging.kafka.KafkaMessage;
import sh.stubborn.messaging.kafka.StubbornKafkaMessageVerifier;

import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;

/**
 * Spring-on-top wiring for the Spring-free Kafka {@link StubbornKafkaMessageVerifier}. It
 * exposes the verifier as the messaging
 * {@code MessageVerifierSender}/{@code MessageVerifierReceiver} and a
 * {@link ContractVerifierMessaging} bean that a {@code @AutoConfigureMessageVerifier}
 * test picks up.
 *
 * <p>
 * The Kafka broker address is taken from the application's {@link KafkaTemplate} (which
 * Spring Boot autoconfigures from {@code spring.kafka.*} or a {@code @ServiceConnection}
 * Testcontainers Kafka), so the same test setup used with Spring Kafka keeps working —
 * only the underlying verifier is now the Spring-free, plain {@code kafka-clients}
 * implementation.
 *
 * @author Marcin Grzejszczak
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({ KafkaTemplate.class, StubbornKafkaMessageVerifier.class })
@ConditionalOnProperty(name = "stubborn.contract.stubrunner.kafka.enabled", havingValue = "true", matchIfMissing = true)
@AutoConfigureBefore({ ContractVerifierIntegrationConfiguration.class, NoOpContractVerifierAutoConfiguration.class })
public class ContractVerifierKafkaConfiguration {

	@Bean(destroyMethod = "close")
	@ConditionalOnMissingBean({ MessageVerifierSender.class, MessageVerifierReceiver.class })
	StubbornKafkaMessageVerifier stubbornKafkaMessageVerifier(KafkaTemplate<String, Object> kafkaTemplate) {
		return new StubbornKafkaMessageVerifier(resolveBootstrapServers(kafkaTemplate));
	}

	@Bean
	@ConditionalOnMissingBean(ContractVerifierMessaging.class)
	ContractVerifierMessaging<KafkaMessage> contractVerifierKafkaMessaging(StubbornKafkaMessageVerifier verifier) {
		return new ContractVerifierMessaging<>(verifier, verifier);
	}

	private static String resolveBootstrapServers(KafkaTemplate<String, Object> kafkaTemplate) {
		Object value = kafkaTemplate.getProducerFactory()
			.getConfigurationProperties()
			.getOrDefault("bootstrap.servers", "localhost:9092");
		if (value instanceof Collection<?> collection) {
			return String.join(",", collection.stream().map(Object::toString).toList());
		}
		return value.toString();
	}

}
