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

package sh.stubborn.contract.verifier.dsl.wiremock;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import com.github.tomakehurst.wiremock.matching.EqualToPattern;
import com.github.tomakehurst.wiremock.matching.StringValuePattern;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sh.stubborn.contract.spec.Contract;
import sh.stubborn.contract.verifier.converter.YamlContractConverter;
import sh.stubborn.contract.verifier.file.SingleContractMetadata;
import sh.stubborn.contract.verifier.util.ContentType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

/**
 * Tests for {@link WireMockRequestStubStrategy}.
 */
@ExtendWith(MockitoExtension.class)
class WireMockRequestStubStrategyTests {

	@Mock
	SingleContractMetadata metadata;

	@Test
	void should_match_header_regex_from_request_YAML() throws IOException {
		String yaml = """
				name: upload-file
				request:
				  method: POST
				  url: /user/upload-file
				  headers:
				    Content-Type: multipart/form-data;boundary=AaB03x
				  multipart:
				    params:
				      name: "fileName.md"
				    named:
				      - paramName: "file"
				        fileName: "fileName.md"
				        fileContent: "file content"
				  matchers:
				    headers:
				      - key: Content-Type
				        regex: "multipart/form\\\\-data.*"
				    multipart:
				      params:
				        - key: name
				          regex: ".+"
				      named:
				        - paramName: "file"
				          fileName:
				            predefined: non_empty
				          fileContent:
				            predefined: non_empty
				response:
				  status: 200
				  body:
				    foo: bar
				""";
		File tmp = File.createTempFile("foo" + System.currentTimeMillis(), ".yml");
		Files.writeString(tmp.toPath(), yaml, StandardCharsets.UTF_8);
		Contract contract = new YamlContractConverter().convertFrom(tmp).iterator().next();

		given(this.metadata.getEvaluatedInputStubContentType()).willReturn(ContentType.JSON);

		WireMockRequestStubStrategy subject = new WireMockRequestStubStrategy(contract, this.metadata);
		var content = subject.buildClientRequestContent();

		assertThat(content.getHeaders().get("Content-Type").getExpected())
			.isNotEqualTo("multipart/form-data;boundary=AaB03x");
	}

	private Contract contractFrom(String yaml) throws IOException {
		File tmp = File.createTempFile("foo" + System.currentTimeMillis(), ".yml");
		Files.writeString(tmp.toPath(), yaml, StandardCharsets.UTF_8);
		return new YamlContractConverter().convertFrom(tmp).iterator().next();
	}

	private String contentTypeYaml(String contentType) {
		return """
				name: upload-file
				request:
				  method: POST
				  url: /user/upload-file
				  headers:
				    Content-Type: %s
				  body:
				    foo: bar
				response:
				  status: 200
				""".formatted(contentType);
	}

	private StringValuePattern contentTypePatternFor(String contentType) throws IOException {
		given(this.metadata.getEvaluatedInputStubContentType()).willReturn(ContentType.JSON);
		WireMockRequestStubStrategy subject = new WireMockRequestStubStrategy(
				contractFrom(contentTypeYaml(contentType)), this.metadata);
		return subject.buildClientRequestContent().getHeaders().get("Content-Type");
	}

	@Test
	void should_not_match_a_bare_multipart_content_type_exactly() throws IOException {
		// An exact match can never fire: the boundary is mandatory in the wire
		// format, so no conforming client sends a bare "multipart/form-data".
		StringValuePattern pattern = contentTypePatternFor("multipart/form-data");

		assertThat(pattern).isNotInstanceOf(EqualToPattern.class);
	}

	@Test
	void should_match_what_a_real_client_actually_sends_for_multipart() throws IOException {
		StringValuePattern pattern = contentTypePatternFor("multipart/form-data");

		assertThat(pattern.match("multipart/form-data; boundary=----WebKitFormBoundary7MA4YWxk").isExactMatch())
			.isTrue();
		assertThat(pattern.match("multipart/form-data;boundary=AaB03x").isExactMatch()).isTrue();
	}

	@Test
	void should_still_match_a_multipart_content_type_sent_without_parameters() throws IOException {
		StringValuePattern pattern = contentTypePatternFor("multipart/form-data");

		assertThat(pattern.match("multipart/form-data").isExactMatch()).isTrue();
	}

	@Test
	void should_not_match_a_different_multipart_subtype() throws IOException {
		StringValuePattern pattern = contentTypePatternFor("multipart/form-data");

		assertThat(pattern.match("multipart/mixed; boundary=AaB03x").isExactMatch()).isFalse();
		assertThat(pattern.match("application/json").isExactMatch()).isFalse();
	}

	@Test
	void should_cover_the_whole_multipart_family_not_just_form_data() throws IOException {
		StringValuePattern pattern = contentTypePatternFor("multipart/mixed");

		assertThat(pattern.match("multipart/mixed; boundary=AaB03x").isExactMatch()).isTrue();
	}

	@Test
	void should_leave_a_multipart_content_type_that_already_carries_a_boundary_alone() throws IOException {
		// The author said exactly what they meant; a fixed boundary is their call.
		StringValuePattern pattern = contentTypePatternFor("multipart/form-data; boundary=AaB03x");

		assertThat(pattern).isInstanceOf(EqualToPattern.class);
		assertThat(pattern.getExpected()).isEqualTo("multipart/form-data; boundary=AaB03x");
	}

	@Test
	void should_leave_a_non_multipart_content_type_as_an_exact_match() throws IOException {
		StringValuePattern pattern = contentTypePatternFor("application/json");

		assertThat(pattern).isInstanceOf(EqualToPattern.class);
		assertThat(pattern.getExpected()).isEqualTo("application/json");
	}

	@Test
	void should_ignore_case_when_recognising_a_multipart_content_type() throws IOException {
		StringValuePattern pattern = contentTypePatternFor("Multipart/Form-Data");

		assertThat(pattern.match("Multipart/Form-Data; boundary=AaB03x").isExactMatch()).isTrue();
	}

	@Test
	void should_treat_the_content_type_as_a_literal_not_a_regex() throws IOException {
		// "multipart/form-data" contains characters that are regex-significant in
		// other media types; quoting keeps the prefix a literal.
		StringValuePattern pattern = contentTypePatternFor("multipart/form-data");

		assertThat(pattern.match("multipartXform-data; boundary=AaB03x").isExactMatch()).isFalse();
	}

}
