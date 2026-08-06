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

package sh.stubborn.contract.verifier.messaging.avro;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import sh.stubborn.contract.verifier.converter.YamlContract;
import tools.jackson.dataformat.yaml.YAMLMapper;

import org.springframework.kafka.core.KafkaTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link KafkaAvroMessageVerifierSender}.
 */
class KafkaAvroMessageVerifierSenderTests {

	private static final String DUMMY_ISBN = "978-1234567890";

	private static final String DUMMY_TITLE = "Contract Testing for Dummies";

	@SuppressWarnings("unchecked")
	private final KafkaTemplate<String, Object> kafkaTemplate = mock(KafkaTemplate.class);

	private final KafkaAvroMessageVerifierSender sender = new KafkaAvroMessageVerifierSender(this.kafkaTemplate);

	private final YAMLMapper yamlMapper = new YAMLMapper();

	@Test
	void should_parse_yml_contract_with_inline_schema_and_send_avro_message_to_kafka() throws Exception {
		String contractYaml = """
				label: book_returned
				input:
				  triggeredBy: publishBookReturned()
				outputMessage:
				  sentTo: book.returned
				  body:
				    isbn: "%s"
				    title: "%s"
				metadata:
				  kafka:
				    avro:
				      schema: >
				        {
				          "type": "record",
				          "name": "Book",
				          "fields": [
				            {"name": "isbn", "type": "string"},
				            {"name": "title", "type": "string"}
				          ]
				        }
				""".formatted(DUMMY_ISBN, DUMMY_TITLE);
		YamlContract contract = this.yamlMapper.readerFor(YamlContract.class).readValue(contractYaml);
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("isbn", DUMMY_ISBN);
		payload.put("title", DUMMY_TITLE);

		this.sender.send(payload, Map.of(), "book.returned", contract);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<ProducerRecord<String, Object>> captor = ArgumentCaptor.forClass(ProducerRecord.class);
		verify(this.kafkaTemplate).send(captor.capture());
		ProducerRecord<String, Object> record = captor.getValue();
		assertThat(record.topic()).isEqualTo("book.returned");
		GenericRecord value = (GenericRecord) record.value();
		assertThat(value.get("isbn").toString()).isEqualTo(DUMMY_ISBN);
		assertThat(value.get("title").toString()).isEqualTo(DUMMY_TITLE);
	}

	@Test
	void should_parse_yml_contract_with_classpath_schema_and_send_avro_message_to_kafka() throws Exception {
		String contractYaml = """
				label: book_returned
				input:
				  triggeredBy: publishBookReturned()
				outputMessage:
				  sentTo: book.returned
				  body:
				    isbn: "%s"
				    title: "%s"
				metadata:
				  kafka:
				    avro:
				      schema: classpath:avro/Book.avsc
				""".formatted(DUMMY_ISBN, DUMMY_TITLE);
		YamlContract contract = this.yamlMapper.readerFor(YamlContract.class).readValue(contractYaml);
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("isbn", DUMMY_ISBN);
		payload.put("title", DUMMY_TITLE);

		this.sender.send(payload, Map.of(), "book.returned", contract);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<ProducerRecord<String, Object>> captor = ArgumentCaptor.forClass(ProducerRecord.class);
		verify(this.kafkaTemplate).send(captor.capture());
		ProducerRecord<String, Object> record = captor.getValue();
		assertThat(record.topic()).isEqualTo("book.returned");
		GenericRecord value = (GenericRecord) record.value();
		assertThat(value.get("isbn").toString()).isEqualTo(DUMMY_ISBN);
		assertThat(value.get("title").toString()).isEqualTo(DUMMY_TITLE);
	}

	@Test
	void should_propagate_headers_to_the_kafka_ProducerRecord() throws Exception {
		String contractYaml = """
				label: book_returned
				input:
				  triggeredBy: publishBookReturned()
				outputMessage:
				  sentTo: book.returned
				  body:
				    isbn: "%s"
				    title: "%s"
				metadata:
				  kafka:
				    avro:
				      schema: classpath:avro/Book.avsc
				""".formatted(DUMMY_ISBN, DUMMY_TITLE);
		YamlContract contract = this.yamlMapper.readerFor(YamlContract.class).readValue(contractYaml);
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("isbn", DUMMY_ISBN);
		payload.put("title", DUMMY_TITLE);
		Map<String, Object> headers = new LinkedHashMap<>();
		headers.put("X-Correlation-Id", "abc-123");
		headers.put("Content-Type", "avro/binary");

		this.sender.send(payload, headers, "book.returned", contract);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<ProducerRecord<String, Object>> captor = ArgumentCaptor.forClass(ProducerRecord.class);
		verify(this.kafkaTemplate).send(captor.capture());
		ProducerRecord<String, Object> record = captor.getValue();
		assertThat(record.topic()).isEqualTo("book.returned");
		assertThat(headerValue(record, "X-Correlation-Id")).isEqualTo("abc-123");
		assertThat(headerValue(record, "Content-Type")).isEqualTo("avro/binary");
	}

	@Test
	void should_fail_when_StubRunnerExecutor_passes_a_JSON_string_payload_instead_of_a_map() throws Exception {
		String contractYaml = """
				label: book_returned
				input:
				  triggeredBy: publishBookReturned()
				outputMessage:
				  sentTo: book.returned
				  body:
				    isbn: "%s"
				    title: "%s"
				metadata:
				  kafka:
				    avro:
				      schema: classpath:avro/Book.avsc
				""".formatted(DUMMY_ISBN, DUMMY_TITLE);
		YamlContract contract = this.yamlMapper.readerFor(YamlContract.class).readValue(contractYaml);
		String jsonPayload = "{\"isbn\":\"%s\",\"title\":\"%s\"}".formatted(DUMMY_ISBN, DUMMY_TITLE);

		assertThatThrownBy(() -> this.sender.send(jsonPayload, Map.of(), "book.returned", contract))
			.isInstanceOf(IllegalArgumentException.class);
	}

	private String headerValue(ProducerRecord<?, ?> record, String key) {
		return new String(record.headers().lastHeader(key).value(), StandardCharsets.UTF_8);
	}

}
