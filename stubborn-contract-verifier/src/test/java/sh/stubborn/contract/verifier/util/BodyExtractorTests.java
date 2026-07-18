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

import java.io.File;
import java.net.URI;
import java.util.Map;

import org.junit.jupiter.api.Test;
import sh.stubborn.contract.spec.internal.FromFileProperty;

import static org.assertj.core.api.Assertions.assertThat;

class BodyExtractorTests {

	@Test
	void should_extract_body_from_json_file() throws Exception {
		URI uri = BodyExtractorTests.class.getResource("/classpath/response.json").toURI();
		FromFileProperty jsonFromFile = new FromFileProperty(new File(uri), String.class);
		assertThat(BodyExtractor.extractClientValueFromBody(jsonFromFile)).isEqualTo(Map.of("status", "RESPONSE"));
	}

}
