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

package sh.stubborn.contract.verifier.wiremock;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.BDDAssertions.then;

class DslToWireMockConverterMutationTests {

	private static final String VALID_STUB = "{\n" + "    \"request\": { \"method\": \"GET\", \"url\": \"/ping\" },\n"
			+ "    \"response\": { \"status\": 200, \"body\": \"pong\" }\n" + "}";

	private final DslToWireMockConverter converter = new DslToWireMockClientConverter();

	@TempDir
	@Nullable Path tmpDir;

	private Path tmpDir() {
		return Objects.requireNonNull(this.tmpDir);
	}

	@Test
	void canReadStubMapping_is_false_for_non_json_extension() throws Exception {
		File file = tmpDir().resolve("stub.txt").toFile();
		Files.writeString(file.toPath(), VALID_STUB);
		then(this.converter.canReadStubMapping(file)).isFalse();
	}

	@Test
	void canReadStubMapping_is_true_for_valid_json_stub() throws Exception {
		File file = tmpDir().resolve("stub.json").toFile();
		Files.writeString(file.toPath(), VALID_STUB);
		then(this.converter.canReadStubMapping(file)).isTrue();
	}

	@Test
	void canReadStubMapping_is_false_for_invalid_json_content() throws Exception {
		File file = tmpDir().resolve("broken.json").toFile();
		Files.writeString(file.toPath(), "this is definitely not a valid stub mapping }{");
		then(this.converter.canReadStubMapping(file)).isFalse();
	}

	@Test
	void canReadStubMapping_is_false_when_json_file_cannot_be_read() {
		File file = tmpDir().resolve("missing.json").toFile();
		then(this.converter.canReadStubMapping(file)).isFalse();
	}

	@Test
	void generateOutputFileNameForInput_replaces_the_trailing_extension() {
		then(this.converter.generateOutputFileNameForInput("dsl1.groovy")).isEqualTo("dsl1.json");
		then(this.converter.generateOutputFileNameForInput("contract.yml")).isEqualTo("contract.json");
	}

	@Test
	void generateOutputFileNameForInput_treats_leading_dot_as_no_extension() {
		// For a name whose only dot is at index 0 the extension is empty (i > 0 is
		// false). If the boundary were mutated to i >= 0 the extension would be
		// "groovy" and the result would collapse to ".json"; asserting the leading
		// "json" (produced by replaceAll with an empty match) kills that mutant.
		String result = this.converter.generateOutputFileNameForInput(".groovy");
		then(result).startsWith("json");
		then(result).isNotEqualTo(".json");
	}

}
