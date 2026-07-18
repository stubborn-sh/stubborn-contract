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

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;

class XPathTests {

	static Stream<Arguments> xpathCases() {
		return Stream.of(
				Arguments.of(
						XPathBuilder.builder().node("some").node("nested").node("anothervalue").isEqualTo(4).xPath(),
						"/some/nested[anothervalue=4]"),
				Arguments.of(XPathBuilder.builder()
					.node("some")
					.node("nested")
					.array("withlist")
					.contains("name")
					.isEqualTo("name1")
					.xPath(), "/some/nested/withlist[name='name1']"),
				Arguments.of(XPathBuilder.builder()
					.node("some")
					.node("nested")
					.array("withlist")
					.contains("name")
					.isEqualTo("name2")
					.xPath(), "/some/nested/withlist[name='name2']"),
				Arguments.of(XPathBuilder.builder()
					.node("some")
					.node("nested")
					.node("json")
					.isEqualTo("with \"val'ue")
					.xPath(), "/some/nested[json=concat('with \"val',\"'\",'ue')]"),
				Arguments.of(XPathBuilder.builder().node("some", "nested", "json").isEqualTo("with \"val'ue").xPath(),
						"/some/nested[json=concat('with \"val',\"'\",'ue')]"));
	}

	@ParameterizedTest
	@MethodSource("xpathCases")
	void should_generate_expected_xpath(String xPath, String expectedXPath) {
		assertThat(xPath).isEqualTo(expectedXPath);
	}

}
