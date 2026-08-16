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

package sh.stubborn.contract.verifier.util.xml;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import sh.stubborn.contract.spec.internal.BodyMatcher;
import sh.stubborn.contract.spec.internal.MatchingType;
import sh.stubborn.contract.spec.internal.MatchingTypeValue;
import sh.stubborn.contract.spec.internal.PathBodyMatcher;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Mutation-focused tests for {@link XmlToXPathsConverter#retrieveValue} covering the
 * Groovy-truthiness {@code isFalsy} branches over every value type.
 */
class XmlToXPathsConverterFalsyMutationTests {

	private static final String XML = """
			<customer>
			      <email>customer@test.com</email>
			    </customer>
			    """;

	private static String retrieve(Object value) {
		BodyMatcher matcher = new PathBodyMatcher("/customer/email/text()",
				new MatchingTypeValue(MatchingType.REGEX, value));
		return XmlToXPathsConverter.retrieveValue(matcher, XML);
	}

	@Test
	void truthy_values_are_returned_verbatim() {
		assertThat(retrieve("[a-z]+")).isEqualTo("[a-z]+");
		assertThat(retrieve(List.of("x"))).isEqualTo(List.of("x").toString());
		assertThat(retrieve(Map.of("a", "b"))).isEqualTo(Map.of("a", "b").toString());
		assertThat(retrieve(5)).isEqualTo("5");
		assertThat(retrieve(true)).isEqualTo("true");
	}

	@Test
	void falsy_values_fall_back_to_reading_the_body() {
		assertThat(retrieve("")).isEqualTo("customer@test.com");
		assertThat(retrieve(List.of())).isEqualTo("customer@test.com");
		assertThat(retrieve(Map.of())).isEqualTo("customer@test.com");
		assertThat(retrieve(0)).isEqualTo("customer@test.com");
		assertThat(retrieve(0.0)).isEqualTo("customer@test.com");
		assertThat(retrieve(false)).isEqualTo("customer@test.com");
	}

}
