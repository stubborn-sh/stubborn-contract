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

import tools.jackson.databind.json.JsonMapper;

import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.amqp.support.converter.ContentTypeDelegatingMessageConverter;
import org.springframework.amqp.support.converter.DefaultJacksonJavaTypeMapper;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@SpringBootApplication
public class AmqpMessagingApplication {

	public static void main(String[] args) {
		SpringApplication.run(AmqpMessagingApplication.class, args);
	}

	@Bean
	public MessageConverter messageConverter(JsonMapper jsonMapper) {
		final JacksonJsonMessageConverter jsonMessageConverter = new JacksonJsonMessageConverter(jsonMapper);
		jsonMessageConverter.setCreateMessageIds(true);
		// The contract's outputMessage carries a __TypeId__ header pointing at the
		// original Spring Cloud Contract package. Map that stale id onto the local
		// Person so the JSON body still deserializes on this side of a real broker.
		DefaultJacksonJavaTypeMapper typeMapper = new DefaultJacksonJavaTypeMapper();
		typeMapper.setTrustedPackages("*");
		typeMapper.setIdClassMapping(
				Map.of("org.springframework.cloud.contract.stubrunner.messaging.amqp.Person", Person.class));
		jsonMessageConverter.setJavaTypeMapper(typeMapper);
		final ContentTypeDelegatingMessageConverter messageConverter = new ContentTypeDelegatingMessageConverter(
				jsonMessageConverter);
		messageConverter.addDelegate(MessageProperties.CONTENT_TYPE_JSON, jsonMessageConverter);
		return messageConverter;
	}

	@Bean
	public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
		RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
		rabbitTemplate.setMessageConverter(messageConverter);
		return rabbitTemplate;
	}

	@Configuration(proxyBeanMethods = false)
	@Profile("!listener")
	static class MessageListenerAdapterConfig {

		@Bean
		MessageListenerAdapter messageListenerAdapter(MessageSubscriber messageSubscriber,
				MessageConverter messageConverter) {
			return new MessageListenerAdapter(messageSubscriber, messageConverter);
		}

		// tag::amqp_binding[]

		// The stub-runner Rabbit backend publishes the contract's outputMessage to a
		// queue named after its sentTo destination ("contract-test.exchange") through the
		// AMQP default exchange, so the listener consumes that queue directly. Declared
		// with the same parameters the sender uses (non-durable, non-exclusive,
		// non-auto-delete) so the sender's idempotent redeclare does not conflict.
		@Bean
		Queue contractTestQueue() {
			return new Queue("contract-test.exchange", false, false, false);
		}
		// end::amqp_binding[]

		// tag::amqp_listener[]
		@Bean
		SimpleMessageListenerContainer simpleMessageListenerContainer(ConnectionFactory connectionFactory,
				MessageListenerAdapter listenerAdapter) {
			SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
			container.setConnectionFactory(connectionFactory);
			container.setQueueNames("contract-test.exchange");
			container.setMessageListener(listenerAdapter);

			return container;
		}
		// end::amqp_listener[]

		@Bean
		MessageSubscriber messageSubscriber() {
			return new MessageSubscriber();
		}

	}

	@Configuration(proxyBeanMethods = false)
	@EnableRabbit
	@Profile("listener")
	static class RabbitListenerConfig {

		// Same queue as the adapter profile: the stub-runner Rabbit backend delivers the
		// triggered message to a queue named after the contract's sentTo destination.
		@Bean
		Queue contractTestQueue() {
			return new Queue("contract-test.exchange", false, false, false);
		}

		@Bean
		MessageSubscriberRabbitListener messageSubscriberRabbitLister() {
			return new MessageSubscriberRabbitListener();
		}

		@Bean
		SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory,
				MessageConverter messageConverter) {
			SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
			factory.setConnectionFactory(connectionFactory);
			factory.setConcurrentConsumers(3);
			factory.setMaxConcurrentConsumers(10);
			factory.setMessageConverter(messageConverter);
			return factory;
		}

	}

}
