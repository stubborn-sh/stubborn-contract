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

package sh.stubborn.messaging.kafka;

import sh.stubborn.contract.verifier.messaging.integration.ContractVerifierIntegrationConfiguration;
import sh.stubborn.contract.verifier.messaging.noop.NoOpContractVerifierAutoConfiguration;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.KafkaTemplate;

@AutoConfiguration
@ConditionalOnClass({ ContractVerifierIntegrationConfiguration.class, NoOpContractVerifierAutoConfiguration.class,
		KafkaTemplate.class })
@AutoConfigureBefore(ContractVerifierIntegrationConfiguration.class)
@EnableConfigurationProperties(StubbornKafkaProperties.class)
public class StubbornKafkaAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	@SuppressWarnings({ "rawtypes", "unchecked" })
	StubbornKafkaMessageVerifier stubbornKafkaMessageVerifier(KafkaTemplate kafkaTemplate,
			StubbornKafkaProperties properties) {
		return new StubbornKafkaMessageVerifier(kafkaTemplate, properties);
	}

}
