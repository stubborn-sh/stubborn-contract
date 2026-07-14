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

package sh.stubborn.contract.stubrunner;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;

import org.mockito.ArgumentMatcher;

import sh.stubborn.contract.verifier.messaging.avro.KafkaAvroMessageVerifierSender;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class StubRunnerExecutorAvroTests {

	private static final String CONTRACT_YAML = """
			label: book_returned
			input:
			  triggeredBy: publishBookReturned()
			outputMessage:
			  sentTo: book.returned
			  headers:
			    X-Correlation-Id: abc-123-def
			  body:
			    isbn: "978-1234567890"
			    title: "Contract Testing for Dummies"
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
			""";

	@SuppressWarnings("unchecked")
	private final KafkaTemplate<String, Object> kafkaTemplate = mock(KafkaTemplate.class);

	private final KafkaAvroMessageVerifierSender sender = new KafkaAvroMessageVerifierSender(this.kafkaTemplate);

	@Test
	void shouldSendAvroSerializedGenericRecordToKafkaForAvroContracts() throws Exception {
		File contractDir = saveTmpContract(CONTRACT_YAML);
		try {
			StubRunnerExecutor executor = new StubRunnerExecutor(new AvailablePortScanner(18000, 18999), this.sender,
					List.of());
			executor.runStubs(new StubRunnerOptionsBuilder().build(),
					new StubRepository(contractDir, List.of(), new StubRunnerOptionsBuilder().build(), null),
					new StubConfiguration("avro", "avro", "avro", ""));
			executor.trigger("book_returned");
			ArgumentMatcher<ProducerRecord<String, Object>> matcher = (record) -> {
				if (!record.topic().equals("book.returned")) {
					return false;
				}
				if (!(record.value() instanceof GenericRecord gr)) {
					return false;
				}
				if (!gr.get("isbn").toString().equals("978-1234567890")) {
					return false;
				}
				if (!gr.get("title").toString().equals("Contract Testing for Dummies")) {
					return false;
				}
				if (record.headers().lastHeader("X-Correlation-Id") == null) {
					return false;
				}
				return new String(record.headers().lastHeader("X-Correlation-Id").value()).equals("abc-123-def");
			};
			verify(this.kafkaTemplate).send(argThat(matcher));
			executor.shutdown();
		}
		finally {
			deleteDir(contractDir);
		}
	}

	private static File saveTmpContract(String contractYaml) throws IOException {
		File contractDir = Files.createTempDirectory("avro-contract").toFile();
		Files.writeString(new File(contractDir, "book_returned.yml").toPath(), contractYaml, StandardCharsets.UTF_8);
		return contractDir;
	}

	private static void deleteDir(File dir) {
		if (dir == null || !dir.exists()) {
			return;
		}
		File[] files = dir.listFiles();
		if (files != null) {
			for (File f : files) {
				deleteDir(f);
			}
		}
		dir.delete();
	}

}
