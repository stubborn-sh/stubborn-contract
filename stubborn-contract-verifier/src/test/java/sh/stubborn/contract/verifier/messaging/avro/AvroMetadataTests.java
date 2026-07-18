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

import org.junit.jupiter.api.Test;
import sh.stubborn.contract.verifier.messaging.kafka.KafkaMetadata;
import tools.jackson.dataformat.yaml.YAMLMapper;

import static org.assertj.core.api.BDDAssertions.then;

/**
 * Tests for Avro metadata parsing.
 */
class AvroMetadataTests {

	YAMLMapper mapper = new YAMLMapper();

	@Test
	void should_parse_avro_metadata_nested_under_kafka() throws Exception {
		// @formatter:off
		String yamlEntry = "kafka:\n"
				+ "  avro:\n"
				+ "    schema: classpath:avro/Book.avsc\n";
		// @formatter:on

		KafkaMetadata kafkaMetadata = KafkaMetadata
			.fromMetadata(this.mapper.readerForMapOf(Object.class).readValue(yamlEntry));

		then(kafkaMetadata.getAvro().getSchema()).isEqualTo("classpath:avro/Book.avsc");
	}

	@Test
	void should_return_empty_avro_metadata_when_avro_key_is_absent() throws Exception {
		// @formatter:off
		String yamlEntry = "kafka:\n"
				+ "  outputMessage:\n"
				+ "    connectToBroker:\n"
				+ "      additionalOptions: foo\n";
		// @formatter:on

		KafkaMetadata kafkaMetadata = KafkaMetadata
			.fromMetadata(this.mapper.readerForMapOf(Object.class).readValue(yamlEntry));

		then(kafkaMetadata.getAvro()).isNull();
	}

}
