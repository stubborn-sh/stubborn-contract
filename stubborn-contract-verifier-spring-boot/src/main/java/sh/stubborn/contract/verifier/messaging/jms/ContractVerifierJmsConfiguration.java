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

package sh.stubborn.contract.verifier.messaging.jms;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import jakarta.jms.Message;
import org.jspecify.annotations.Nullable;
import sh.stubborn.contract.verifier.converter.YamlContract;
import sh.stubborn.contract.verifier.messaging.MessageVerifierReceiver;
import sh.stubborn.contract.verifier.messaging.MessageVerifierSender;
import sh.stubborn.contract.verifier.messaging.integration.ContractVerifierIntegrationConfiguration;
import sh.stubborn.contract.verifier.messaging.internal.ContractVerifierMessaging;
import sh.stubborn.contract.verifier.messaging.noop.NoOpContractVerifierAutoConfiguration;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.core.JmsTemplate;

/**
 * Configuration that registers JMS messaging beans for contract verification.
 *
 * @author Marcin Grzejszczak
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(JmsTemplate.class)
@ConditionalOnProperty(name = "stubborn.contract.stubrunner.jms.enabled", havingValue = "true", matchIfMissing = true)
@AutoConfigureBefore({ ContractVerifierIntegrationConfiguration.class, NoOpContractVerifierAutoConfiguration.class })
public class ContractVerifierJmsConfiguration {

	@Bean
	@ConditionalOnMissingBean(MessageVerifierSender.class)
	MessageVerifierSender<Message> contractVerifierJmsMessageSender(ObjectProvider<JmsTemplate> jmsTemplateProvider) {
		JmsTemplate jmsTemplate = jmsTemplateProvider.getIfAvailable(JmsTemplate::new);
		JmsStubMessages jmsStubMessages = new JmsStubMessages(jmsTemplate);
		return new MessageVerifierSender<>() {
			@Override
			public void send(Message message, String destination, @Nullable YamlContract contract) {
				jmsStubMessages.send(message, destination, contract);
			}

			@Override
			public <T> void send(T payload, @Nullable Map<String, Object> headers, String destination,
					@Nullable YamlContract contract) {
				jmsStubMessages.send(payload, headers, destination, contract);
			}
		};
	}

	@Bean
	@ConditionalOnMissingBean(MessageVerifierReceiver.class)
	MessageVerifierReceiver<Message> contractVerifierJmsMessageReceiver(
			ObjectProvider<JmsTemplate> jmsTemplateProvider) {
		JmsTemplate jmsTemplate = jmsTemplateProvider.getIfAvailable(JmsTemplate::new);
		JmsStubMessages jmsStubMessages = new JmsStubMessages(jmsTemplate);
		return new MessageVerifierReceiver<>() {
			@Override
			public @Nullable Message receive(String destination, long timeout, TimeUnit timeUnit,
					@Nullable YamlContract contract) {
				return jmsStubMessages.receive(destination, timeout, timeUnit, contract);
			}

			@Override
			public @Nullable Message receive(String destination, @Nullable YamlContract contract) {
				return jmsStubMessages.receive(destination, contract);
			}
		};
	}

	@Bean
	@ConditionalOnMissingBean(ContractVerifierMessaging.class)
	ContractVerifierMessaging<Message> contractVerifierJmsMessaging(MessageVerifierSender<Message> sender,
			MessageVerifierReceiver<Message> receiver) {
		return new ContractVerifierJmsHelper(sender, receiver);
	}

}
