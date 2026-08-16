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

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Mutation-focused unit tests for the pure {@link KafkaMessage} carrier.
 *
 * @author Marcin Grzejszczak
 */
@SuppressWarnings("NullAway")
class KafkaMessageMutationTests {

	@Test
	void payloadAndHeadersAreReturnedVerbatim() {
		Map<String, Object> headers = new LinkedHashMap<>();
		headers.put("h1", "v1");
		KafkaMessage message = new KafkaMessage("body", headers);
		assertThat(message.getPayload()).isEqualTo("body");
		assertThat(message.getHeaders()).containsExactly(Map.entry("h1", "v1"));
	}

	@Test
	void nullPayloadIsPreserved() {
		KafkaMessage message = new KafkaMessage(null, new LinkedHashMap<>());
		assertThat(message.getPayload()).isNull();
	}

	@Test
	void nullHeadersBecomeEmptyMutableMap() {
		KafkaMessage message = new KafkaMessage("body", null);
		assertThat(message.getHeaders()).isNotNull().isEmpty();
		message.getHeaders().put("added", "later");
		assertThat(message.getHeaders()).containsExactly(Map.entry("added", "later"));
	}

	@Test
	void headersAreDefensivelyCopied() {
		Map<String, Object> headers = new LinkedHashMap<>();
		headers.put("h1", "v1");
		KafkaMessage message = new KafkaMessage("body", headers);
		headers.put("h2", "v2");
		assertThat(message.getHeaders()).containsExactly(Map.entry("h1", "v1"));
	}

	@Test
	void toStringContainsPayloadAndHeaders() {
		Map<String, Object> headers = new LinkedHashMap<>();
		headers.put("h1", "v1");
		KafkaMessage message = new KafkaMessage("body", headers);
		assertThat(message.toString()).isEqualTo("KafkaMessage{payload=body, headers={h1=v1}}");
	}

}
