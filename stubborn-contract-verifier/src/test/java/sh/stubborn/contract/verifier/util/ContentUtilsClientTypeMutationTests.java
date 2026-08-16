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

package sh.stubborn.contract.verifier.util;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import sh.stubborn.contract.spec.internal.Headers;
import sh.stubborn.contract.spec.internal.MatchingStrategy;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Mutation-focused tests for {@link ContentUtils} content-type guessing helpers.
 */
class ContentUtilsClientTypeMutationTests {

	@Test
	void map_is_recognised_as_json() {
		assertThat(ContentUtils.getClientContentType(Map.of("a", "b"))).isEqualTo(ContentType.JSON);
	}

	@Test
	void list_is_recognised_as_json() {
		assertThat(ContentUtils.getClientContentType(List.of(1, 2, 3))).isEqualTo(ContentType.JSON);
	}

	@Test
	void json_string_is_json_plain_string_is_unknown() {
		assertThat(ContentUtils.getClientContentType("{\"a\":1}")).isEqualTo(ContentType.JSON);
		assertThat(ContentUtils.getClientContentType("just text")).isEqualTo(ContentType.UNKNOWN);
	}

	@Test
	void object_without_header_falls_back_to_body_guess() {
		assertThat(ContentUtils.getClientContentType(Map.of("a", "b"), new Headers())).isEqualTo(ContentType.JSON);
	}

	@Test
	void object_with_json_header_uses_header() {
		Headers headers = new Headers();
		headers.contentType("application/json");
		assertThat(ContentUtils.getClientContentType("not json at all", headers)).isEqualTo(ContentType.JSON);
	}

	@Test
	void equals_type_for_json_and_xml_and_unknown() {
		assertThat(ContentUtils.getEqualsTypeFromContentType(ContentType.JSON))
			.isEqualTo(MatchingStrategy.Type.EQUAL_TO_JSON);
		assertThat(ContentUtils.getEqualsTypeFromContentType(ContentType.XML))
			.isEqualTo(MatchingStrategy.Type.EQUAL_TO_XML);
		assertThat(ContentUtils.getEqualsTypeFromContentType(ContentType.UNKNOWN))
			.isEqualTo(MatchingStrategy.Type.EQUAL_TO);
	}

}
