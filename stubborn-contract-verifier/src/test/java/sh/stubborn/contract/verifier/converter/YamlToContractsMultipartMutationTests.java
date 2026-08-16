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

package sh.stubborn.contract.verifier.converter;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import sh.stubborn.contract.spec.Contract;
import sh.stubborn.contract.spec.internal.ExecutionProperty;
import sh.stubborn.contract.spec.internal.NamedProperty;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Mutation-focused tests for {@link YamlToContracts} multipart named-parameter mapping
 * and input/output message mapping via {@link YamlToContracts#convertFrom}.
 */
class YamlToContractsMultipartMutationTests {

	private final YamlToContracts converter = new YamlToContracts();

	@TempDir
	java.nio.file.Path temp;

	private Contract convert(String yaml) throws IOException {
		File file = this.temp.resolve("c" + System.nanoTime() + ".yml").toFile();
		Files.writeString(file.toPath(), yaml, StandardCharsets.UTF_8);
		return this.converter.convertFrom(file).iterator().next();
	}

	@Test
	void multipart_named_commands_map_to_execution_properties() throws IOException {
		Contract contract = convert("""
				request:
				  method: POST
				  url: /upload
				  multipart:
				    params:
				      p1: "v1"
				    named:
				      - paramName: "file"
				        fileName: "name.txt"
				        fileNameCommand: "fileName()"
				        fileContentCommand: "fileContent()"
				        contentTypeCommand: "contentType()"
				response:
				  status: 200
				""");
		Map<?, ?> multipart = (Map<?, ?>) contract.getRequest().getMultipart().getClientValue();
		NamedProperty file = (NamedProperty) multipart.get("file");
		assertThat(file.getName().getServerValue()).isInstanceOf(ExecutionProperty.class);
		assertThat(file.getValue().getServerValue()).isInstanceOf(ExecutionProperty.class);
		assertThat(file.getContentType().getServerValue()).isInstanceOf(ExecutionProperty.class);
	}

	@Test
	void multipart_named_matchers_map_to_patterns_on_client_side() throws IOException {
		Contract contract = convert("""
				request:
				  method: POST
				  url: /upload
				  multipart:
				    params:
				      p1: "v1"
				    named:
				      - paramName: "file"
				        fileName: "name.txt"
				        fileContent: "hello"
				  matchers:
				    multipart:
				      named:
				        - paramName: "file"
				          fileName:
				            regex: "[a-z]+\\\\.txt"
				          fileContent:
				            predefined: non_empty
				response:
				  status: 200
				""");
		Map<?, ?> multipart = (Map<?, ?>) contract.getRequest().getMultipart().getClientValue();
		NamedProperty file = (NamedProperty) multipart.get("file");
		assertThat(file.getName().getClientValue()).isInstanceOf(Pattern.class);
		assertThat(file.getValue().getClientValue()).isInstanceOf(Pattern.class);
	}

	@Test
	void multipart_param_value_becomes_dsl_property() throws IOException {
		Contract contract = convert("""
				request:
				  method: POST
				  url: /upload
				  multipart:
				    params:
				      p1: "v1"
				response:
				  status: 200
				""");
		Map<?, ?> multipart = (Map<?, ?>) contract.getRequest().getMultipart().getClientValue();
		assertThat(multipart.containsKey("p1")).isTrue();
	}

	@Test
	void input_and_output_assert_that_and_triggered_by_are_mapped() throws IOException {
		Contract contract = convert("""
				input:
				  triggeredBy: "trigger()"
				  assertThat: "verifyInput()"
				outputMessage:
				  sentTo: "topic"
				  assertThat: "verifyOutput()"
				  headers:
				    Content-Type: application/json
				  body:
				    a: b
				""");
		assertThat(contract.getInput().getTriggeredBy().getExecutionCommand()).isEqualTo("trigger()");
		assertThat(contract.getInput().getAssertThat().getExecutionCommand()).isEqualTo("verifyInput()");
		assertThat(contract.getOutputMessage().getAssertThat().getExecutionCommand()).isEqualTo("verifyOutput()");
		assertThat(contract.getOutputMessage().getSentTo().getClientValue()).isEqualTo("topic");
	}

	@Test
	void output_message_header_regex_matcher_sets_pattern_server_value() throws IOException {
		Contract contract = convert("""
				outputMessage:
				  sentTo: "topic"
				  headers:
				    Code: "1234"
				  body:
				    a: b
				  matchers:
				    headers:
				      - key: Code
				        regex: "[0-9]+"
				""");
		Object serverValue = contract.getOutputMessage()
			.getHeaders()
			.getEntries()
			.stream()
			.filter((h) -> h.getName().equals("Code"))
			.findFirst()
			.orElseThrow()
			.getServerValue();
		assertThat(serverValue).isInstanceOf(Pattern.class);
	}

}
