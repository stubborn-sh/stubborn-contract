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

import org.junit.jupiter.api.Test;
import sh.stubborn.contract.spec.internal.BodyMatcher;
import sh.stubborn.contract.spec.internal.MatchingType;
import sh.stubborn.contract.spec.internal.MatchingTypeValue;
import sh.stubborn.contract.spec.internal.PathBodyMatcher;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Mutation-focused tests for {@link XmlToXPathsConverter#retrieveValue} edge cases: a
 * null matcher value (falsy) and an unrecognised value type (not falsy).
 */
class XmlToXPathsConverterRetrieveMutationTests {

	private static final String XML = "<customer><email>a@b.com</email></customer>";

	@Test
	void null_value_is_falsy_and_reads_from_body() {
		BodyMatcher matcher = new PathBodyMatcher("/customer/email/text()",
				new MatchingTypeValue(MatchingType.REGEX, null));
		assertThat(XmlToXPathsConverter.retrieveValue(matcher, XML)).isEqualTo("a@b.com");
	}

	@Test
	void unrecognised_value_type_is_not_falsy_and_returned_verbatim() {
		Object value = new Object() {
			@Override
			public String toString() {
				return "literal";
			}
		};
		BodyMatcher matcher = new PathBodyMatcher("/customer/email/text()",
				new MatchingTypeValue(MatchingType.REGEX, value));
		assertThat(XmlToXPathsConverter.retrieveValue(matcher, XML)).isEqualTo("literal");
	}

}
