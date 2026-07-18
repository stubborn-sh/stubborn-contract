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
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.assertj.core.api.InstanceOfAssertFactories;

import org.junit.jupiter.api.Test;
import sh.stubborn.contract.spec.Contract;
import sh.stubborn.contract.verifier.util.ContractVerifierDslConverter;

import static org.assertj.core.api.Assertions.assertThat;

class DslToYamlContractConverterTests {

	private static final String XML_CONTRACT_BODY = "\n<test>\n<duck type='xtype'>123</duck>\n"
			+ "<alpha>abc</alpha>\n<list>\n<elem>abc</elem>\n<elem>def</elem>\n<elem>ghi</elem>\n</list>\n"
			+ "<number>123</number>\n<aBoolean>true</aBoolean>\n<date>2017-01-01</date>\n"
			+ "<dateTime>2017-01-01T01:23:45</dateTime>\n<time>01:02:34</time>\n"
			+ "<valueWithoutAMatcher>foo</valueWithoutAMatcher>\n<valueWithTypeMatch>string</valueWithTypeMatch>\n"
			+ "<key><complex>foo</complex></key>\n</test>";

	private final YamlContractConverter converter = new YamlContractConverter();

	private static File contractFile(String name) {
		try {
			return new File(
					DslToYamlContractConverterTests.class.getResource("/contracts/dsl-to-yaml/" + name).toURI());
		}
		catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}

	private static YamlContract.BodyStubMatcher bodyStubMatcher(String path, YamlContract.StubMatcherType type) {
		YamlContract.BodyStubMatcher m = new YamlContract.BodyStubMatcher();
		m.path = path;
		m.type = type;
		return m;
	}

	private static YamlContract.BodyStubMatcher bodyStubMatcher(String path, YamlContract.StubMatcherType type,
			String value) {
		YamlContract.BodyStubMatcher m = bodyStubMatcher(path, type);
		m.value = value;
		return m;
	}

	private static YamlContract.BodyStubMatcher bodyStubMatcherWithDate(String path, YamlContract.StubMatcherType type,
			String value) {
		return bodyStubMatcher(path, type, value);
	}

	private static YamlContract.BodyTestMatcher bodyTestMatcher(String path, YamlContract.TestMatcherType type) {
		YamlContract.BodyTestMatcher m = new YamlContract.BodyTestMatcher();
		m.path = path;
		m.type = type;
		return m;
	}

	private static YamlContract.BodyTestMatcher bodyTestMatcher(String path, YamlContract.TestMatcherType type,
			String value) {
		YamlContract.BodyTestMatcher m = bodyTestMatcher(path, type);
		m.value = value;
		return m;
	}

	private static YamlContract.BodyTestMatcher bodyTestMatcherWithMin(String path, YamlContract.TestMatcherType type,
			int minOccurrence) {
		YamlContract.BodyTestMatcher m = bodyTestMatcher(path, type);
		m.minOccurrence = minOccurrence;
		return m;
	}

	private static YamlContract.BodyTestMatcher bodyTestMatcherWithMax(String path, YamlContract.TestMatcherType type,
			int maxOccurrence) {
		YamlContract.BodyTestMatcher m = bodyTestMatcher(path, type);
		m.maxOccurrence = maxOccurrence;
		return m;
	}

	private static YamlContract.BodyTestMatcher bodyTestMatcherWithMinMax(String path,
			YamlContract.TestMatcherType type, int minOccurrence, int maxOccurrence) {
		YamlContract.BodyTestMatcher m = bodyTestMatcher(path, type);
		m.minOccurrence = minOccurrence;
		m.maxOccurrence = maxOccurrence;
		return m;
	}

	private static YamlContract.KeyValueMatcher keyValueMatcher(String key, String regex,
			YamlContract.RegexType regexType) {
		YamlContract.KeyValueMatcher m = new YamlContract.KeyValueMatcher();
		m.key = key;
		m.regex = regex;
		m.regexType = regexType;
		return m;
	}

	private static YamlContract.TestHeaderMatcher testHeaderMatcher(String key, String regex,
			YamlContract.RegexType regexType) {
		YamlContract.TestHeaderMatcher m = new YamlContract.TestHeaderMatcher();
		m.key = key;
		m.regex = regex;
		m.regexType = regexType;
		return m;
	}

	private static YamlContract.QueryParameterMatcher queryParameterMatcher(String key, YamlContract.MatchingType type,
			Object value) {
		YamlContract.QueryParameterMatcher m = new YamlContract.QueryParameterMatcher();
		m.key = key;
		m.type = type;
		m.value = value;
		return m;
	}

	@Test
	void should_convert_rest_dsl_without_request_headers_to_yaml() {
		File file = contractFile("contract1-simple.groovy");
		Collection<Contract> contracts = ContractVerifierDslConverter.convertAsCollection(file);
		Collection<YamlContract> yamlContracts = converter.convertTo(contracts);
		assertThat(yamlContracts).hasSize(1);
		YamlContract yamlContract = yamlContracts.iterator().next();
		assertThat(yamlContract.request.method).isEqualTo("GET");
		assertThat(yamlContract.request.urlPath).isEqualTo("/get");
		assertThat(yamlContract.request.headers).isNull();
	}

	@Test
	void should_convert_rest_dsl_to_yaml() {
		File file = contractFile("contract2-full.groovy");
		Collection<Contract> contracts = ContractVerifierDslConverter.convertAsCollection(file);
		Collection<YamlContract> yamlContracts = converter.convertTo(contracts);
		assertThat(yamlContracts).hasSize(1);
		YamlContract yamlContract = yamlContracts.iterator().next();
		assertThat(yamlContract.request.method).isEqualTo("GET");
		assertThat(yamlContract.request.urlPath).isEqualTo("/get");
		assertThat(yamlContract.request.body).isEqualTo(Map.of("duck", 123, "alpha", "abc", "number", 123, "aBoolean",
				true, "date", "2017-01-01", "dateTime", "2017-01-01T01:23:45", "time", "01:02:34",
				"valueWithoutAMatcher", "foo", "valueWithTypeMatch", "string", "key", Map.of("complex.key", "foo")));
		assertThat(yamlContract.request.headers).containsEntry("sample", "foo")
			.containsEntry("Content-Type", "application/json");
		assertThat(yamlContract.request.matchers.headers)
			.containsExactly(keyValueMatcher("sample", "foo.*", YamlContract.RegexType.as_string));
		assertThat(yamlContract.request.matchers.body).containsExactly(
				bodyStubMatcher("$.duck", YamlContract.StubMatcherType.by_regex, "[0-9]{3}"),
				bodyStubMatcher("$.duck", YamlContract.StubMatcherType.by_equality),
				bodyStubMatcher("$.alpha", YamlContract.StubMatcherType.by_regex, "[\\p{L}]*"),
				bodyStubMatcher("$.alpha", YamlContract.StubMatcherType.by_equality),
				bodyStubMatcher("$.number", YamlContract.StubMatcherType.by_regex, "-?(\\d*\\.\\d+|\\d+)"),
				bodyStubMatcher("$.aBoolean", YamlContract.StubMatcherType.by_regex, "(true|false)"),
				bodyStubMatcherWithDate("$.date", YamlContract.StubMatcherType.by_date,
						"(\\d\\d\\d\\d)-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])"),
				bodyStubMatcherWithDate("$.dateTime", YamlContract.StubMatcherType.by_timestamp,
						"([0-9]{4})-(1[0-2]|0[1-9])-(3[01]|0[1-9]|[12][0-9])T(2[0-3]|[01][0-9]):([0-5][0-9]):([0-5][0-9])"),
				bodyStubMatcherWithDate("$.time", YamlContract.StubMatcherType.by_time,
						"(2[0-3]|[01][0-9]):([0-5][0-9]):([0-5][0-9])"),
				bodyStubMatcher("$.['key'].['complex.key']", YamlContract.StubMatcherType.by_equality));
		assertThat(yamlContract.response.status).isEqualTo(200);
		assertThat(yamlContract.response.body).asInstanceOf(InstanceOfAssertFactories.MAP)
			.containsEntry("duck", 123)
			.containsEntry("alpha", "abc")
			.containsEntry("number", 123)
			.containsEntry("aBoolean", true)
			.containsEntry("date", "2017-01-01")
			.containsEntry("dateTime", "2017-01-01T01:23:45")
			.containsEntry("time", "01:02:34")
			.containsEntry("positiveInteger", 1234567890)
			.containsEntry("negativeInteger", -1234567890)
			.containsEntry("valueWithoutAMatcher", "foo")
			.containsEntry("valueWithTypeMatch", "string")
			.containsEntry("valueWithMin", List.of(1, 2, 3))
			.containsEntry("valueWithMax", List.of(1, 2, 3))
			.containsEntry("valueWithMinMax", List.of(1, 2, 3))
			.containsEntry("valueWithMinEmpty", List.of())
			.containsEntry("valueWithMaxEmpty", List.of())
			.containsKey("nullValue")
			.satisfies(m -> assertThat(((Map<?, ?>) m).get("nullValue")).isNull());
		assertThat(yamlContract.response.headers).containsEntry("Content-Type", "application/json")
			.containsEntry("Some-Header", "someValue");
		assertThat(yamlContract.response.matchers.headers).containsExactly(
				testHeaderMatcher("Content-Type", "application/json.*", YamlContract.RegexType.as_string),
				testHeaderMatcher("Some-Header", "[a-zA-Z]{9}", YamlContract.RegexType.as_string));
		assertThat(yamlContract.response.matchers.body).containsExactly(
				bodyTestMatcher("$.duck", YamlContract.TestMatcherType.by_regex, "[0-9]{3}"),
				bodyTestMatcher("$.duck", YamlContract.TestMatcherType.by_equality),
				bodyTestMatcher("$.alpha", YamlContract.TestMatcherType.by_regex, "[\\p{L}]*"),
				bodyTestMatcher("$.alpha", YamlContract.TestMatcherType.by_equality),
				bodyTestMatcher("$.number", YamlContract.TestMatcherType.by_regex, "-?(\\d*\\.\\d+|\\d+)"),
				bodyTestMatcher("$.positiveInteger", YamlContract.TestMatcherType.by_regex, "([1-9]\\d*)"),
				bodyTestMatcher("$.integer", YamlContract.TestMatcherType.by_regex, "-?(\\d+)"),
				bodyTestMatcher("$.double", YamlContract.TestMatcherType.by_regex, "-?(\\d*\\.\\d+)"),
				bodyTestMatcher("$.aBoolean", YamlContract.TestMatcherType.by_regex, "(true|false)"),
				bodyTestMatcher("$.date", YamlContract.TestMatcherType.by_date,
						"(\\d\\d\\d\\d)-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])"),
				bodyTestMatcher("$.dateTime", YamlContract.TestMatcherType.by_timestamp,
						"([0-9]{4})-(1[0-2]|0[1-9])-(3[01]|0[1-9]|[12][0-9])T(2[0-3]|[01][0-9]):([0-5][0-9]):([0-5][0-9])"),
				bodyTestMatcher("$.time", YamlContract.TestMatcherType.by_time,
						"(2[0-3]|[01][0-9]):([0-5][0-9]):([0-5][0-9])"),
				bodyTestMatcher("$.valueWithTypeMatch", YamlContract.TestMatcherType.by_type),
				bodyTestMatcherWithMin("$.valueWithMin", YamlContract.TestMatcherType.by_type, 1),
				bodyTestMatcherWithMax("$.valueWithMax", YamlContract.TestMatcherType.by_type, 3),
				bodyTestMatcherWithMinMax("$.valueWithMinMax", YamlContract.TestMatcherType.by_type, 1, 3),
				bodyTestMatcherWithMin("$.valueWithMinEmpty", YamlContract.TestMatcherType.by_type, 0),
				bodyTestMatcherWithMax("$.valueWithMaxEmpty", YamlContract.TestMatcherType.by_type, 0),
				bodyTestMatcher("$.duck", YamlContract.TestMatcherType.by_command, "assertThatValueIsANumber($it)"),
				bodyTestMatcher("$.['key'].['complex.key']", YamlContract.TestMatcherType.by_equality),
				bodyTestMatcher("$.nullValue", YamlContract.TestMatcherType.by_null));
	}

	@Test
	void should_convert_rest_dsl_with_dynamic_entries_to_yaml() {
		File file = contractFile("contract3-dynamic.groovy");
		Collection<Contract> contracts = ContractVerifierDslConverter.convertAsCollection(file);
		Collection<YamlContract> yamlContracts = converter.convertTo(contracts);
		assertThat(yamlContracts).hasSize(1);
		YamlContract yamlContract = yamlContracts.iterator().next();
		assertThat(yamlContract.request.method).isEqualTo("PUT");
		assertThat(yamlContract.request.urlPath).isEqualTo("/fraudcheck");
		assertThat(yamlContract.request.queryParameters).containsEntry("foo2", "foo3").containsEntry("foo", "bar");
		assertThat(yamlContract.request.body.toString()).contains("client.id");
		assertThat(yamlContract.request.headers).containsEntry("Content-Type", "application/json")
			.containsEntry("Authorization", "Bearer SOMETOKEN");
		assertThat(yamlContract.request.matchers.headers)
			.containsExactly(keyValueMatcher("Content-Type", "application/json.*", YamlContract.RegexType.as_string));
		assertThat(yamlContract.request.matchers.body).hasSize(1);
		assertThat(yamlContract.request.matchers.body.get(0).path).isEqualTo("$.['client.id']");
		assertThat(yamlContract.request.matchers.body.get(0).type).isEqualTo(YamlContract.StubMatcherType.by_regex);
		assertThat(yamlContract.request.matchers.body.get(0).value).isEqualTo("[0-9]{10}");
		assertThat(yamlContract.request.matchers.queryParameters).containsExactly(
				queryParameterMatcher("foo2", YamlContract.MatchingType.equal_to_json, "{\"foo\":\"bar\"}"));
		assertThat(yamlContract.response.status).isEqualTo(200);
		assertThat(yamlContract.response.body).asInstanceOf(InstanceOfAssertFactories.MAP).satisfies(m -> {
			assertThat(((Map<?, ?>) m).get("fraudCheckStatus").toString()).isEqualTo("FRAUD");
			assertThat(((Map<?, ?>) m).get("rejection.reason").toString()).isEqualTo("Amount too high");
		});
		assertThat(yamlContract.response.headers).containsEntry("Content-Type", "application/json");
		assertThat(yamlContract.response.matchers.headers)
			.containsExactly(testHeaderMatcher("Content-Type", "application/json.*", YamlContract.RegexType.as_string));
		assertThat(yamlContract.response.matchers.body).hasSize(1);
		assertThat(yamlContract.response.matchers.body.get(0).path).isEqualTo("$.['fraudCheckStatus']");
		assertThat(yamlContract.response.matchers.body.get(0).type).isEqualTo(YamlContract.TestMatcherType.by_regex);
		assertThat(yamlContract.response.matchers.body.get(0).value).isEqualTo("FRAUD");
	}

	@Test
	void should_convert_rest_dsl_with_multipart_entries_to_yaml() {
		File file = contractFile("contract4-multipart.groovy");
		Collection<Contract> contracts = ContractVerifierDslConverter.convertAsCollection(file);
		Collection<YamlContract> yamlContracts = converter.convertTo(contracts);
		assertThat(yamlContracts).hasSize(1);
		YamlContract yamlContract = yamlContracts.iterator().next();
		assertThat(yamlContract.request.method).isEqualTo("PUT");
		assertThat(yamlContract.request.url).isEqualTo("/multipart");
		assertThat(yamlContract.request.multipart.params).containsEntry("formParameter", "\"formParameterValue\"")
			.containsEntry("someBooleanParameter", "true");
		assertThat(yamlContract.request.multipart.named).hasSize(1);
		YamlContract.Named named = yamlContract.request.multipart.named.get(0);
		assertThat(named.paramName).isEqualTo("file");
		assertThat(named.fileName).isEqualTo("filename.csv");
		assertThat(named.fileContent).isEqualTo("file content");
		assertThat(named.contentType).isEqualTo("application/json");
		assertThat(yamlContract.request.headers).containsEntry("Content-Type", "multipart/form-data;boundary=AaB03x");
		assertThat(yamlContract.request.matchers.multipart.params).containsExactly(
				keyValueMatcher("formParameter", "\".+\"", YamlContract.RegexType.as_string),
				keyValueMatcher("someBooleanParameter", "(true|false)", YamlContract.RegexType.as_string));
		assertThat(yamlContract.request.matchers.multipart.named).hasSize(1);
		YamlContract.MultipartNamedStubMatcher namedMatcher = yamlContract.request.matchers.multipart.named.get(0);
		assertThat(namedMatcher.paramName).isEqualTo("file");
		assertThat(namedMatcher.fileName).isEqualTo(new YamlContract.ValueMatcher("[\\S\\s]+"));
		assertThat(namedMatcher.fileContent).isEqualTo(new YamlContract.ValueMatcher("[\\S\\s]+"));
		assertThat(namedMatcher.contentType).isEqualTo(new YamlContract.ValueMatcher("[\\S\\s]+"));
		assertThat(yamlContract.response.status).isEqualTo(200);
	}

	@Test
	void should_convert_rest_xml_dsl_to_yaml() {
		File file = contractFile("contract5-xml.groovy");
		Collection<Contract> contracts = ContractVerifierDslConverter.convertAsCollection(file);
		Collection<YamlContract> yamlContracts = converter.convertTo(contracts);
		assertThat(yamlContracts).hasSize(1);
		YamlContract yamlContract = yamlContracts.iterator().next();
		assertThat(yamlContract.request.method).isEqualTo("GET");
		assertThat(yamlContract.request.url).isEqualTo("/get");
		assertThat(yamlContract.request.body.toString().replaceAll("\n", "").replaceAll(" ", ""))
			.isEqualTo(XML_CONTRACT_BODY.replaceAll("\n", "").replaceAll(" ", ""));
		assertThat(yamlContract.request.headers).containsEntry("Content-Type", "application/xml");
		assertThat(yamlContract.request.matchers.body)
			.containsExactly(bodyStubMatcher("/test/duck/text()", YamlContract.StubMatcherType.by_regex, "[0-9]{3}"));
		assertThat(yamlContract.response.status).isEqualTo(200);
		assertThat(yamlContract.response.body.toString().replaceAll("\n", "").replaceAll(" ", ""))
			.isEqualTo(XML_CONTRACT_BODY.replaceAll("\n", "").replaceAll(" ", ""));
		assertThat(yamlContract.response.matchers.body)
			.containsExactly(bodyTestMatcher("/test/duck/xxx", YamlContract.TestMatcherType.by_null));
	}

	@Test
	void should_convert_rest_dsl_to_yaml_with_save_priority_property() {
		File file = contractFile("contract6-priority.groovy");
		Collection<Contract> contracts = ContractVerifierDslConverter.convertAsCollection(file);
		Collection<YamlContract> yamlContracts = converter.convertTo(contracts);
		assertThat(yamlContracts).hasSize(1);
		YamlContract yamlContract = yamlContracts.iterator().next();
		assertThat(yamlContract.request.method).isEqualTo("POST");
		assertThat(yamlContract.request.urlPath).isEqualTo("/account");
		assertThat(yamlContract.request.matchers.headers).containsExactly(
				keyValueMatcher("Content-Type", "application/json.*", YamlContract.RegexType.as_string),
				keyValueMatcher("userId", "^\\s*\\S[\\S\\s]*", YamlContract.RegexType.as_string));
		assertThat(yamlContract.request.matchers.body).hasSize(1);
		YamlContract.BodyStubMatcher numberMatcher = yamlContract.request.matchers.body.get(0);
		assertThat(numberMatcher.path).isEqualTo("$.['number']");
		assertThat(numberMatcher.type).isEqualTo(YamlContract.StubMatcherType.by_regex);
		assertThat(numberMatcher.value).isEqualTo("^\\s*\\S[\\S\\s]*");
		assertThat(yamlContract.response.status).isEqualTo(201);
		assertThat(yamlContract.response.headers).containsEntry("Content-Type", "application/json");
		assertThat(yamlContract.response.matchers.headers)
			.containsExactly(testHeaderMatcher("Content-Type", "application/json.*", YamlContract.RegexType.as_string));
		assertThat(yamlContract.response.matchers.body).hasSize(1);
		YamlContract.BodyTestMatcher idMatcher = yamlContract.response.matchers.body.get(0);
		assertThat(idMatcher.path).isEqualTo("$.['id']");
		assertThat(idMatcher.type).isEqualTo(YamlContract.TestMatcherType.by_regex);
		assertThat(idMatcher.value).isEqualTo("^\\s*\\S[\\S\\s]*");
		assertThat(yamlContract.priority).isEqualTo(1);
	}

}
