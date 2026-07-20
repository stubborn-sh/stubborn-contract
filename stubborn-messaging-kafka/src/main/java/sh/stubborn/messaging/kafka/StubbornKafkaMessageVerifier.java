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

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sh.stubborn.contract.verifier.converter.YamlContract;
import sh.stubborn.contract.verifier.messaging.MessageVerifierReceiver;
import sh.stubborn.contract.verifier.messaging.MessageVerifierSender;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

class StubbornKafkaMessageVerifier implements MessageVerifierSender<Message<?>>, MessageVerifierReceiver<Message<?>> {

	private static final Logger log = LoggerFactory.getLogger(StubbornKafkaMessageVerifier.class);

	@SuppressWarnings("rawtypes")
	private final KafkaTemplate kafkaTemplate;

	private final StubbornKafkaProperties properties;

	@SuppressWarnings("rawtypes")
	StubbornKafkaMessageVerifier(KafkaTemplate kafkaTemplate, StubbornKafkaProperties properties) {
		this.kafkaTemplate = kafkaTemplate;
		this.properties = properties;
	}

	@Override
	@SuppressWarnings("unchecked")
	public void send(Message<?> message, String destination, YamlContract contract) {
		log.info("Sending message to Kafka topic '{}': {}", destination, message.getPayload());
		kafkaTemplate.send(destination, message.getPayload());
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> void send(T payload, Map<String, Object> headers, String destination, YamlContract contract) {
		log.info("Sending message to Kafka topic '{}': {}", destination, payload);
		kafkaTemplate.send(destination, payload);
	}

	@Override
	public Message<?> receive(String destination, long timeout, TimeUnit timeUnit, YamlContract contract) {
		long timeoutMs = timeUnit.toMillis(timeout);
		log.info("Receiving message from Kafka topic '{}' with timeout {}ms", destination, timeoutMs);
		Properties consumerProps = buildConsumerProperties();
		try (KafkaConsumer<String, Object> consumer = new KafkaConsumer<>(consumerProps)) {
			consumer.subscribe(Collections.singletonList(destination));
			long deadline = System.currentTimeMillis() + timeoutMs;
			while (System.currentTimeMillis() < deadline) {
				long remaining = Math.max(deadline - System.currentTimeMillis(), 100L);
				ConsumerRecords<String, Object> records = consumer.poll(Duration.ofMillis(remaining));
				for (ConsumerRecord<String, Object> record : records) {
					log.info("Received message from '{}': {}", destination, record.value());
					MessageBuilder<Object> builder = MessageBuilder.withPayload(record.value());
					record.headers().forEach(header -> builder.setHeader(header.key(), new String(header.value())));
					builder.setHeaderIfAbsent("contentType", "application/json");
					return builder.build();
				}
			}
		}
		log.warn("No message received from '{}' within {}ms", destination, timeoutMs);
		return null;
	}

	@Override
	public Message<?> receive(String destination, YamlContract contract) {
		return receive(destination, properties.getReceiveTimeout().toSeconds(), TimeUnit.SECONDS, contract);
	}

	@SuppressWarnings("unchecked")
	private Properties buildConsumerProperties() {
		Object bootstrapServersRaw = kafkaTemplate.getProducerFactory()
			.getConfigurationProperties()
			.getOrDefault("bootstrap.servers", "localhost:9092");
		String bootstrapServers;
		if (bootstrapServersRaw instanceof Collection<?> col) {
			bootstrapServers = String.join(",", col.stream().map(Object::toString).toList());
		}
		else {
			bootstrapServers = bootstrapServersRaw.toString();
		}
		Properties props = new Properties();
		props.put("bootstrap.servers", bootstrapServers);
		props.put("group.id", "stubborn-contract-verifier-" + System.nanoTime());
		props.put("auto.offset.reset", "earliest");
		props.put("key.deserializer", StringDeserializer.class.getName());
		props.put("value.deserializer", StringDeserializer.class.getName());
		return props;
	}

}
