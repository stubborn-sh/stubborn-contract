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
import java.nio.file.Files;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import sh.stubborn.contract.spec.Contract;
import sh.stubborn.contract.spec.internal.ExecutionProperty;
import sh.stubborn.contract.spec.internal.FromFileProperty;
import sh.stubborn.contract.spec.internal.MatchingStrategy;
import sh.stubborn.contract.spec.internal.NamedProperty;
import sh.stubborn.contract.spec.internal.QueryParameters;
import sh.stubborn.contract.spec.internal.RegexPatterns;
import sh.stubborn.contract.spec.internal.RegexProperty;
import sh.stubborn.contract.spec.internal.Url;
import sh.stubborn.contract.verifier.util.ContractVerifierDslConverter;
import sh.stubborn.contract.verifier.util.MapConverter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static sh.stubborn.contract.spec.internal.MatchingType.COMMAND;
import static sh.stubborn.contract.spec.internal.MatchingType.DATE;
import static sh.stubborn.contract.spec.internal.MatchingType.EQUALITY;
import static sh.stubborn.contract.spec.internal.MatchingType.NULL;
import static sh.stubborn.contract.spec.internal.MatchingType.REGEX;
import static sh.stubborn.contract.spec.internal.MatchingType.TIME;
import static sh.stubborn.contract.spec.internal.MatchingType.TIMESTAMP;
import static sh.stubborn.contract.spec.internal.MatchingType.TYPE;

/**
 * @author Marcin Grzejszczak
 * @author Tim Ysewyn
 * @author Olga Maciaszek-Sharma
 */
class YamlContractConverterTests {

	private static final String XML_CONTRACT_BODY = "\n<test>\n<duck type='xtype'>123</duck>\n"
			+ "<alpha>abc</alpha>\n<list>\n<elem>abc</elem>\n<elem>def</elem>\n<elem>ghi</elem>\n</list>\n"
			+ "<number>123</number>\n<aBoolean>true</aBoolean>\n<date>2017-01-01</date>\n"
			+ "<dateTime>2017-01-01T01:23:45</dateTime>\n<time>01:02:34</time>\n"
			+ "<valueWithoutAMatcher>foo</valueWithoutAMatcher>\n<valueWithTypeMatch>string</valueWithTypeMatch>\n"
			+ "<key><complex>foo</complex></key>\n</test>\n";

	private static final String XML_CONTRACT_BODY_WITH_NAMESPACES = "\n<ns1:customer xmlns:ns1=\"http://demo.com/testns\">\n"
			+ "<email>customer@test.com</email>\n</ns1:customer>\n";

	private final YamlContractConverter converter = new YamlContractConverter();

	private static File yamlFile(String name) {
		try {
			return new File(YamlContractConverterTests.class.getResource("/yml/" + name).toURI());
		}
		catch (URISyntaxException ex) {
			throw new RuntimeException(ex);
		}
	}

	private static File resourceFile(String path) {
		try {
			return new File(YamlContractConverterTests.class.getResource(path).toURI());
		}
		catch (URISyntaxException ex) {
			throw new RuntimeException(ex);
		}
	}

	private static boolean assertQueryParam(QueryParameters queryParameters, String queryParamName, Object serverValue,
			MatchingStrategy.Type clientType, Object clientValue) {
		if (clientType == MatchingStrategy.Type.ABSENT) {
			return queryParameters.getParameters().stream().noneMatch((p) -> p.getName().equals(queryParamName));
		}
		return queryParameters.getParameters()
			.stream()
			.anyMatch((p) -> p.getName().equals(queryParamName) && p.getServerValue().equals(serverValue)
					&& ((MatchingStrategy) p.getClientValue()).getType() == clientType
					&& ((MatchingStrategy) p.getClientValue()).getClientValue().equals(clientValue));
	}

	@Test
	void should_convert_yaml_with_cookies_to_dsl() {
		File ymlCookies = yamlFile("contract_cookies.yml");
		assertThat(converter.isAccepted(ymlCookies)).isTrue();
		Collection<Contract> contracts = converter.convertFrom(ymlCookies);
		assertThat(contracts).hasSize(1);
		Contract contract = contracts.iterator().next();
		assertThat(contract.getDescription()).isEqualTo("Contract with cookies");
		assertThat(contract.getName()).isEqualTo("cookies-contract");
		assertThat(contract.getPriority()).isEqualTo(1);
		assertThat(contract.isIgnored()).isTrue();
		assertThat(contract.getRequest().getMethod().getClientValue()).isEqualTo("PUT");
		assertThat(contract.getRequest().getUrl().getClientValue()).isEqualTo("/foo");
		assertThat(contract.getRequest().getCookies().getEntries()).anySatisfy((e) -> {
			assertThat(e.getKey()).isEqualTo("foo");
			assertThat(e.getServerValue()).isEqualTo("bar");
		});
		assertThat(contract.getRequest().getCookies().getEntries()).anySatisfy((e) -> {
			assertThat(e.getKey()).isEqualTo("fooRegex");
			assertThat(((Pattern) e.getClientValue()).pattern()).isEqualTo("reg");
			assertThat(e.getServerValue()).isEqualTo("reg");
		});
		assertThat(contract.getRequest().getCookies().getEntries()).anySatisfy((e) -> {
			assertThat(e.getKey()).isEqualTo("fooPredefinedRegex");
			assertThat(((Pattern) e.getClientValue()).pattern()).isEqualTo("(true|false)");
			assertThat(e.getServerValue()).isEqualTo(true);
		});
		assertThat(contract.getResponse().getStatus().getClientValue()).isEqualTo(200);
		assertThat(contract.getResponse().getCookies().getEntries()).anySatisfy((e) -> {
			assertThat(e.getKey()).isEqualTo("foo");
			assertThat(e.getClientValue()).isEqualTo("baz");
		});
		assertThat(contract.getResponse().getCookies().getEntries()).anySatisfy((e) -> {
			assertThat(e.getKey()).isEqualTo("fooRegex");
			assertThat(((Pattern) e.getServerValue()).pattern()).isEqualTo("[0-9]+");
			assertThat(e.getClientValue()).isEqualTo(123);
		});
		assertThat(contract.getResponse().getCookies().getEntries()).anySatisfy((e) -> {
			assertThat(e.getKey()).isEqualTo("source");
			assertThat(((Pattern) e.getServerValue()).pattern()).isEqualTo("ip_address");
			assertThat(e.getClientValue()).isEqualTo("ip_address");
		});
		assertThat(contract.getResponse().getCookies().getEntries()).anySatisfy((e) -> {
			assertThat(e.getKey()).isEqualTo("fooPredefinedRegex");
			assertThat(((Pattern) e.getServerValue()).pattern()).isEqualTo("(true|false)");
			assertThat(e.getClientValue()).isEqualTo(true);
		});
		assertThat(MapConverter.getStubSideValues(contract.getResponse().getBody()))
			.asInstanceOf(InstanceOfAssertFactories.MAP)
			.containsEntry("status", "OK");
	}

	static List<File> yamlFilesForRestToDsl() {
		return List.of(yamlFile("contract.yml"), yamlFile("contract_rest.yml"),
				yamlFile("contract_rest_with_path.yml"));
	}

	@ParameterizedTest
	@MethodSource("yamlFilesForRestToDsl")
	void should_convert_yaml_with_rest_to_dsl(File ymlFile) {
		assertThat(converter.isAccepted(ymlFile)).isTrue();
		Collection<Contract> contracts = converter.convertFrom(ymlFile);
		assertThat(contracts).hasSize(1);
		Contract contract = contracts.iterator().next();
		assertThat(contract.getDescription()).isEqualTo("Some description");
		assertThat(contract.getName()).isEqualTo("some name");
		assertThat(contract.getPriority()).isEqualTo(8);
		assertThat(contract.isIgnored()).isTrue();

		File ymlWithRest3 = yamlFile("contract_rest_with_path.yml");
		Url url = ymlFile.equals(ymlWithRest3) ? contract.getRequest().getUrlPath() : contract.getRequest().getUrl();
		assertThat(url.getClientValue()).isEqualTo("/foo");
		assertThat(url.getQueryParameters().getParameters().get(0).getName()).isEqualTo("a");
		assertThat(url.getQueryParameters().getParameters().get(0).getServerValue()).isEqualTo("b");
		assertThat(url.getQueryParameters().getParameters().get(1).getName()).isEqualTo("b");
		assertThat(url.getQueryParameters().getParameters().get(1).getServerValue()).isEqualTo("c");
		assertThat(contract.getRequest().getMethod().getClientValue()).isEqualTo("PUT");
		assertThat(contract.getRequest().getHeaders().getEntries()).anySatisfy((e) -> {
			assertThat(e.getName()).isEqualTo("foo");
			assertThat(((Pattern) e.getClientValue()).pattern()).isEqualTo("bar");
			assertThat(e.getServerValue()).isEqualTo("bar");
		});
		assertThat(contract.getRequest().getHeaders().getEntries()).anySatisfy((e) -> {
			assertThat(e.getName()).isEqualTo("fooReq");
			assertThat(e.getServerValue()).isEqualTo("baz");
		});
		assertThat(MapConverter.getStubSideValues(contract.getRequest().getBody()))
			.asInstanceOf(InstanceOfAssertFactories.MAP)
			.containsEntry("foo", "bar");
		assertThat(contract.getRequest().getBodyMatchers().matchers().get(0).path()).isEqualTo("$.foo");
		assertThat(contract.getRequest().getBodyMatchers().matchers().get(0).matchingType()).isEqualTo(REGEX);
		assertThat(((RegexProperty) contract.getRequest().getBodyMatchers().matchers().get(0).value()).pattern())
			.isEqualTo("bar");

		assertThat(contract.getResponse().getStatus().getClientValue()).isEqualTo(200);
		if (ymlFile.equals(yamlFile("contract.yml"))) {
			assertThat(contract.getResponse().getDelay().getClientValue()).isEqualTo(1000);
		}
		else {
			assertThat(contract.getResponse().getDelay()).isNull();
		}
		assertThat(contract.getResponse().getHeaders().getEntries()).anySatisfy((e) -> {
			assertThat(e.getName()).isEqualTo("foo2");
			assertThat(((Pattern) e.getServerValue()).pattern()).isEqualTo("bar");
			assertThat(e.getClientValue()).isEqualTo("bar");
		});
		assertThat(contract.getResponse().getHeaders().getEntries()).anySatisfy((e) -> {
			assertThat(e.getName()).isEqualTo("foo3");
			assertThat(((ExecutionProperty) e.getServerValue()).insertValue("foo")).isEqualTo("andMeToo(foo)");
		});
		assertThat(contract.getResponse().getHeaders().getEntries()).anySatisfy((e) -> {
			assertThat(e.getName()).isEqualTo("fooRes");
			assertThat(e.getClientValue()).isEqualTo("baz");
		});
		assertThat(MapConverter.getStubSideValues(contract.getResponse().getBody()))
			.asInstanceOf(InstanceOfAssertFactories.MAP)
			.containsEntry("foo2", "bar")
			.containsEntry("foo3", "baz")
			.containsKey("nullValue")
			.satisfies((m) -> assertThat(((Map<?, ?>) m).get("nullValue")).isNull());
		assertThat(contract.getResponse().getBodyMatchers().matchers().get(0).path()).isEqualTo("$.foo2");
		assertThat(contract.getResponse().getBodyMatchers().matchers().get(0).matchingType()).isEqualTo(REGEX);
		assertThat(((RegexProperty) contract.getResponse().getBodyMatchers().matchers().get(0).value()).pattern())
			.isEqualTo("bar");
		assertThat(contract.getResponse().getBodyMatchers().matchers().get(1).path()).isEqualTo("$.foo3");
		assertThat(contract.getResponse().getBodyMatchers().matchers().get(1).matchingType()).isEqualTo(COMMAND);
		assertThat(contract.getResponse().getBodyMatchers().matchers().get(1).value())
			.isEqualTo(new ExecutionProperty("executeMe($it)"));
		assertThat(contract.getResponse().getBodyMatchers().matchers().get(2).path()).isEqualTo("$.nullValue");
		assertThat(contract.getResponse().getBodyMatchers().matchers().get(2).matchingType()).isEqualTo(NULL);
		assertThat(contract.getResponse().getBodyMatchers().matchers().get(2).value()).isNull();
	}

	@Test
	void should_convert_yaml_with_rest_to_dsl_with_advanced_request_referencing() {
		File ymlReference = yamlFile("contract_reference_request.yml");
		assertThat(converter.isAccepted(ymlReference)).isTrue();
		Collection<Contract> contracts = converter.convertFrom(ymlReference);
		assertThat(contracts).hasSize(1);
		Contract contract = contracts.iterator().next();
		Url url = contract.getRequest().getUrl();
		assertThat(url.getClientValue()).isEqualTo("/api/v1/xxxx");
		assertThat(url.getQueryParameters().getParameters().get(0).getName()).isEqualTo("foo");
		assertThat(url.getQueryParameters().getParameters().get(0).getServerValue()).isEqualTo("bar");
		assertThat(url.getQueryParameters().getParameters().get(1).getName()).isEqualTo("foo");
		assertThat(url.getQueryParameters().getParameters().get(1).getServerValue()).isEqualTo("bar2");
		assertThat(contract.getRequest().getMethod().getClientValue()).isEqualTo("GET");
		assertThat(contract.getRequest().getHeaders().getEntries()).anySatisfy((e) -> {
			assertThat(e.getName()).isEqualTo("Authorization");
			assertThat(e.getClientValue()).isEqualTo(List.of("secret", "secret2"));
		});
		assertThat(MapConverter.getStubSideValues(contract.getRequest().getBody()))
			.asInstanceOf(InstanceOfAssertFactories.MAP)
			.containsEntry("foo", "bar")
			.containsEntry("baz", 5);

		assertThat(contract.getResponse().getStatus().getClientValue()).isEqualTo(200);
		assertThat(contract.getResponse()
			.getHeaders()
			.getEntries()
			.stream()
			.filter((e) -> "Authorization".equals(e.getName()))
			.findFirst()
			.orElseThrow()
			.getClientValue()).isEqualTo("foo {{{ request.headers.Authorization.0 }}} bar");
		Map<?, ?> testSideBody = (Map<?, ?>) MapConverter.getTestSideValues(contract.getResponse().getBody());
		assertThat(testSideBody.get("url").toString()).isEqualTo("{{{ request.url }}}");
		assertThat(testSideBody.get("path").toString()).isEqualTo("{{{ request.path }}}");
		assertThat(testSideBody.get("pathIndex").toString()).isEqualTo("{{{ request.path.1 }}}");
		assertThat(testSideBody.get("param").toString()).isEqualTo("{{{ request.query.foo }}}");
		assertThat(testSideBody.get("paramIndex").toString()).isEqualTo("{{{ request.query.foo.1 }}}");
		assertThat(testSideBody.get("authorization").toString()).isEqualTo("{{{ request.headers.Authorization.0 }}}");
		assertThat(testSideBody.get("authorization2").toString()).isEqualTo("{{{ request.headers.Authorization.1 }}");
		assertThat(testSideBody.get("fullBody").toString()).isEqualTo("{{{ request.body }}}");
		assertThat(testSideBody.get("responseFoo").toString()).isEqualTo("{{{ jsonpath this '$.foo' }}}");
		assertThat(testSideBody.get("responseBaz").toString()).isEqualTo("{{{ jsonpath this '$.baz' }}}");
		assertThat(testSideBody.get("responseBaz2").toString())
			.isEqualTo("Bla bla {{{ jsonpath this '$.foo' }}} bla bla");
	}

	static List<String> urlPropertyNames() {
		return List.of("url", "urlPath");
	}

	@ParameterizedTest
	@MethodSource("urlPropertyNames")
	void should_convert_yaml_with_rest_matchers_and_path_property_to_dsl(String urlPropertyName) throws Exception {
		File ymlMatchers = File.createTempFile("contract_matchers", ".yml");
		String content = new String(Files.readAllBytes(
				new File(YamlContractConverterTests.class.getResource("/yml/contract_matchers.yml").toURI()).toPath()))
			.replace("\n  urlPath:", "\n  " + urlPropertyName + ":");
		Files.writeString(ymlMatchers.toPath(), content);

		assertThat(converter.isAccepted(ymlMatchers)).isTrue();
		Collection<Contract> contracts = converter.convertFrom(ymlMatchers);
		assertThat(contracts).hasSize(1);
		Contract contract = contracts.iterator().next();
		assertThat(contract.getRequest().getHeaders().getEntries()).anySatisfy((e) -> {
			assertThat(e.getName()).isEqualTo("Content-Type");
			assertThat(((Pattern) e.getClientValue()).pattern()).isEqualTo("application/json.*");
			assertThat(e.getServerValue()).isEqualTo("application/json");
		});

		Url url = "url".equals(urlPropertyName) ? contract.getRequest().getUrl() : contract.getRequest().getUrlPath();
		assertThat(((Pattern) url.getClientValue()).pattern()).isEqualTo("/get/[0-9]");
		assertThat(url.getServerValue()).isEqualTo("/get/1");
		assertThat(url.getQueryParameters().getParameters()).hasSize(8);
		QueryParameters queryParameters = url.getQueryParameters();
		assertThat(assertQueryParam(queryParameters, "limit", 10, MatchingStrategy.Type.EQUAL_TO, 20)).isTrue();
		assertThat(assertQueryParam(queryParameters, "offset", 20, MatchingStrategy.Type.CONTAINS, 20)).isTrue();
		assertThat(assertQueryParam(queryParameters, "sort", "name", MatchingStrategy.Type.EQUAL_TO, "name")).isTrue();
		assertThat(assertQueryParam(queryParameters, "search", 55, MatchingStrategy.Type.NOT_MATCHING, "^[0-9]{2}$"))
			.isTrue();
		assertThat(assertQueryParam(queryParameters, "age", 99, MatchingStrategy.Type.NOT_MATCHING, "^\\\\w*$"))
			.isTrue();
		assertThat(assertQueryParam(queryParameters, "name", "John.Doe", MatchingStrategy.Type.MATCHING, "John.*"))
			.isTrue();
		assertThat(assertQueryParam(queryParameters, "hello", true, MatchingStrategy.Type.ABSENT, null)).isTrue();

		assertThat(contract.getRequest().getBodyMatchers().matchers().get(0).path()).isEqualTo("$.duck");
		assertThat(contract.getRequest().getBodyMatchers().matchers().get(0).matchingType()).isEqualTo(REGEX);
		assertThat(((RegexProperty) contract.getRequest().getBodyMatchers().matchers().get(0).value()).pattern())
			.isEqualTo("[0-9]{3}");
		assertThat(contract.getRequest().getBodyMatchers().matchers().get(1).path()).isEqualTo("$.duck");
		assertThat(contract.getRequest().getBodyMatchers().matchers().get(1).matchingType()).isEqualTo(EQUALITY);
		assertThat(contract.getRequest().getBodyMatchers().matchers().get(2).path()).isEqualTo("$.alpha");
		assertThat(contract.getRequest().getBodyMatchers().matchers().get(2).matchingType()).isEqualTo(REGEX);
		assertThat(((RegexProperty) contract.getRequest().getBodyMatchers().matchers().get(2).value()).pattern())
			.isEqualTo(RegexPatterns.onlyAlphaUnicode().pattern());
		assertThat(contract.getRequest().getBodyMatchers().matchers().get(3).path()).isEqualTo("$.alpha");
		assertThat(contract.getRequest().getBodyMatchers().matchers().get(3).matchingType()).isEqualTo(EQUALITY);
		assertThat(contract.getRequest().getBodyMatchers().matchers().get(4).path()).isEqualTo("$.number");
		assertThat(contract.getRequest().getBodyMatchers().matchers().get(4).matchingType()).isEqualTo(REGEX);
		assertThat(((RegexProperty) contract.getRequest().getBodyMatchers().matchers().get(4).value()).pattern())
			.isEqualTo(RegexPatterns.number().pattern());
		assertThat(contract.getRequest().getBodyMatchers().matchers().get(5).path()).isEqualTo("$.aBoolean");
		assertThat(contract.getRequest().getBodyMatchers().matchers().get(5).matchingType()).isEqualTo(REGEX);
		assertThat(((RegexProperty) contract.getRequest().getBodyMatchers().matchers().get(5).value()).pattern())
			.isEqualTo(RegexPatterns.anyBoolean().pattern());
		assertThat(contract.getRequest().getBodyMatchers().matchers().get(6).path()).isEqualTo("$.date");
		assertThat(contract.getRequest().getBodyMatchers().matchers().get(6).matchingType()).isEqualTo(DATE);
		assertThat(((RegexProperty) contract.getRequest().getBodyMatchers().matchers().get(6).value()).pattern())
			.isEqualTo(RegexPatterns.isoDate().pattern());
		assertThat(contract.getRequest().getBodyMatchers().matchers().get(7).path()).isEqualTo("$.dateTime");
		assertThat(contract.getRequest().getBodyMatchers().matchers().get(7).matchingType()).isEqualTo(TIMESTAMP);
		assertThat(((RegexProperty) contract.getRequest().getBodyMatchers().matchers().get(7).value()).pattern())
			.isEqualTo(RegexPatterns.isoDateTime().pattern());
		assertThat(contract.getRequest().getBodyMatchers().matchers().get(8).path()).isEqualTo("$.time");
		assertThat(contract.getRequest().getBodyMatchers().matchers().get(8).matchingType()).isEqualTo(TIME);
		assertThat(((RegexProperty) contract.getRequest().getBodyMatchers().matchers().get(8).value()).pattern())
			.isEqualTo(RegexPatterns.isoTime().pattern());
		assertThat(contract.getRequest().getBodyMatchers().matchers().get(9).path())
			.isEqualTo("$.['key'].['complex.key']");
		assertThat(contract.getRequest().getBodyMatchers().matchers().get(9).matchingType()).isEqualTo(EQUALITY);
		assertThat(contract.getRequest().getBodyMatchers().matchers().get(10).path()).isEqualTo("$.valueWithMin");
		assertThat(contract.getRequest().getBodyMatchers().matchers().get(10).matchingType()).isEqualTo(TYPE);
		assertThat(contract.getRequest().getBodyMatchers().matchers().get(10).minTypeOccurrence()).isEqualTo(1);
		assertThat(contract.getRequest().getBodyMatchers().matchers().get(11).path()).isEqualTo("$.valueWithMax");
		assertThat(contract.getRequest().getBodyMatchers().matchers().get(11).matchingType()).isEqualTo(TYPE);
		assertThat(contract.getRequest().getBodyMatchers().matchers().get(11).maxTypeOccurrence()).isEqualTo(3);
		assertThat(contract.getRequest().getBodyMatchers().matchers().get(12).path()).isEqualTo("$.valueWithMinMax");
		assertThat(contract.getRequest().getBodyMatchers().matchers().get(12).matchingType()).isEqualTo(TYPE);
		assertThat(contract.getRequest().getBodyMatchers().matchers().get(12).minTypeOccurrence()).isEqualTo(1);
		assertThat(contract.getRequest().getBodyMatchers().matchers().get(12).maxTypeOccurrence()).isEqualTo(3);
		assertThat(contract.getRequest()
			.getCookies()
			.getEntries()
			.stream()
			.filter((e) -> "foo".equals(e.getKey()))
			.findFirst()
			.orElseThrow()
			.getClientValue()).isInstanceOf(Pattern.class);
		assertThat(contract.getRequest()
			.getCookies()
			.getEntries()
			.stream()
			.filter((e) -> "bar".equals(e.getKey()))
			.findFirst()
			.orElseThrow()
			.getServerValue()).isEqualTo(new ExecutionProperty("equals($it)"));

		assertThat(contract.getResponse().getStatus().getClientValue()).isEqualTo(200);
		assertThat(contract.getResponse().getBodyMatchers().matchers().get(0).path()).isEqualTo("$.duck");
		assertThat(contract.getResponse().getBodyMatchers().matchers().get(0).matchingType()).isEqualTo(REGEX);
		assertThat(((RegexProperty) contract.getResponse().getBodyMatchers().matchers().get(0).value()).pattern())
			.isEqualTo("[0-9]{3}");
		assertThat(contract.getResponse().getBodyMatchers().matchers().get(1).path()).isEqualTo("$.duck");
		assertThat(contract.getResponse().getBodyMatchers().matchers().get(1).matchingType()).isEqualTo(EQUALITY);
		assertThat(contract.getResponse().getBodyMatchers().matchers().get(2).path()).isEqualTo("$.alpha");
		assertThat(contract.getResponse().getBodyMatchers().matchers().get(2).matchingType()).isEqualTo(REGEX);
		assertThat(((RegexProperty) contract.getResponse().getBodyMatchers().matchers().get(2).value()).pattern())
			.isEqualTo(RegexPatterns.onlyAlphaUnicode().pattern());
		assertThat(contract.getResponse().getBodyMatchers().matchers().get(3).path()).isEqualTo("$.alpha");
		assertThat(contract.getResponse().getBodyMatchers().matchers().get(3).matchingType()).isEqualTo(EQUALITY);
		assertThat(contract.getResponse().getBodyMatchers().matchers().get(4).path()).isEqualTo("$.number");
		assertThat(contract.getResponse().getBodyMatchers().matchers().get(4).matchingType()).isEqualTo(REGEX);
		assertThat(((RegexProperty) contract.getResponse().getBodyMatchers().matchers().get(4).value()).pattern())
			.isEqualTo(RegexPatterns.number().pattern());
		assertThat(contract.getResponse().getBodyMatchers().matchers().get(5).path()).isEqualTo("$.aBoolean");
		assertThat(contract.getResponse().getBodyMatchers().matchers().get(5).matchingType()).isEqualTo(REGEX);
		assertThat(((RegexProperty) contract.getResponse().getBodyMatchers().matchers().get(5).value()).pattern())
			.isEqualTo(RegexPatterns.anyBoolean().pattern());
		assertThat(contract.getResponse().getBodyMatchers().matchers().get(6).path()).isEqualTo("$.date");
		assertThat(contract.getResponse().getBodyMatchers().matchers().get(6).matchingType()).isEqualTo(DATE);
		assertThat(((RegexProperty) contract.getResponse().getBodyMatchers().matchers().get(6).value()).pattern())
			.isEqualTo(RegexPatterns.isoDate().pattern());
		assertThat(contract.getResponse().getBodyMatchers().matchers().get(7).path()).isEqualTo("$.dateTime");
		assertThat(contract.getResponse().getBodyMatchers().matchers().get(7).matchingType()).isEqualTo(TIMESTAMP);
		assertThat(((RegexProperty) contract.getResponse().getBodyMatchers().matchers().get(7).value()).pattern())
			.isEqualTo(RegexPatterns.isoDateTime().pattern());
		assertThat(contract.getResponse().getBodyMatchers().matchers().get(8).path()).isEqualTo("$.time");
		assertThat(contract.getResponse().getBodyMatchers().matchers().get(8).matchingType()).isEqualTo(TIME);
		assertThat(((RegexProperty) contract.getResponse().getBodyMatchers().matchers().get(8).value()).pattern())
			.isEqualTo(RegexPatterns.isoTime().pattern());
		assertThat(contract.getResponse().getBodyMatchers().matchers().get(9).path()).isEqualTo("$.valueWithTypeMatch");
		assertThat(contract.getResponse().getBodyMatchers().matchers().get(9).matchingType()).isEqualTo(TYPE);
		assertThat(contract.getResponse().getBodyMatchers().matchers().get(10).path()).isEqualTo("$.valueWithMin");
		assertThat(contract.getResponse().getBodyMatchers().matchers().get(10).matchingType()).isEqualTo(TYPE);
		assertThat(contract.getResponse().getBodyMatchers().matchers().get(10).minTypeOccurrence()).isEqualTo(1);
		assertThat(contract.getResponse().getBodyMatchers().matchers().get(11).path()).isEqualTo("$.valueWithMax");
		assertThat(contract.getResponse().getBodyMatchers().matchers().get(11).matchingType()).isEqualTo(TYPE);
		assertThat(contract.getResponse().getBodyMatchers().matchers().get(11).maxTypeOccurrence()).isEqualTo(3);
		assertThat(contract.getResponse().getBodyMatchers().matchers().get(12).path()).isEqualTo("$.valueWithMinMax");
		assertThat(contract.getResponse().getBodyMatchers().matchers().get(12).matchingType()).isEqualTo(TYPE);
		assertThat(contract.getResponse().getBodyMatchers().matchers().get(12).minTypeOccurrence()).isEqualTo(1);
		assertThat(contract.getResponse().getBodyMatchers().matchers().get(12).maxTypeOccurrence()).isEqualTo(3);
		assertThat(contract.getResponse().getBodyMatchers().matchers().get(13).path()).isEqualTo("$.valueWithMinEmpty");
		assertThat(contract.getResponse().getBodyMatchers().matchers().get(13).matchingType()).isEqualTo(TYPE);
		assertThat(contract.getResponse().getBodyMatchers().matchers().get(13).minTypeOccurrence()).isEqualTo(0);
		assertThat(contract.getResponse().getBodyMatchers().matchers().get(14).path()).isEqualTo("$.valueWithMaxEmpty");
		assertThat(contract.getResponse().getBodyMatchers().matchers().get(14).matchingType()).isEqualTo(TYPE);
		assertThat(contract.getResponse().getBodyMatchers().matchers().get(14).maxTypeOccurrence()).isEqualTo(0);
		assertThat(contract.getResponse().getBodyMatchers().matchers().get(15).path()).isEqualTo("$.duck");
		assertThat(contract.getResponse().getBodyMatchers().matchers().get(15).matchingType()).isEqualTo(COMMAND);
		assertThat(contract.getResponse().getBodyMatchers().matchers().get(15).value())
			.isEqualTo(new ExecutionProperty("assertThatValueIsANumber($it)"));
	}

	@Test
	void should_convert_yaml_to_contract_and_back_to_yaml_without_losing_url_information() throws Exception {
		File ymlMatchers = File.createTempFile("contract_matchers_url", ".yml");
		String content = new String(Files.readAllBytes(
				new File(YamlContractConverterTests.class.getResource("/yml/contract_matchers_url.yml").toURI())
					.toPath()));
		Files.writeString(ymlMatchers.toPath(), content);

		assertThat(converter.isAccepted(ymlMatchers)).isTrue();
		Collection<Contract> contracts = converter.convertFrom(ymlMatchers);
		List<YamlContract> convertedBack = converter.convertTo(contracts);
		assertThat(convertedBack).hasSize(1);
		assertThat(convertedBack.get(0).request.matchers.url.regex).isEqualTo("/get/[0-9]");
	}

	@Test
	void should_convert_yaml_to_contract_and_back_to_yaml_without_exceptions() throws Exception {
		File ymlMatchers = File.createTempFile("contract_matchers", ".yml");
		String content = new String(Files.readAllBytes(
				new File(YamlContractConverterTests.class.getResource("/yml/contract_matchers.yml").toURI()).toPath()));
		Files.writeString(ymlMatchers.toPath(), content);

		assertThat(converter.isAccepted(ymlMatchers)).isTrue();
		Collection<Contract> contracts = converter.convertFrom(ymlMatchers);
		List<YamlContract> convertedBack = converter.convertTo(contracts);
		assertThat(convertedBack).hasSize(1);
	}

	@Test
	void should_convert_yaml_with_message_matchers_to_dsl() {
		File ymlMessagingMatchers = yamlFile("contract_message_matchers.yml");
		assertThat(converter.isAccepted(ymlMessagingMatchers)).isTrue();
		Collection<Contract> contracts = converter.convertFrom(ymlMessagingMatchers);
		assertThat(contracts).hasSize(1);
		Contract contract = contracts.iterator().next();
		assertThat(contract.getOutputMessage().getBodyMatchers().matchers().get(0).path()).isEqualTo("$.duck");
		assertThat(contract.getOutputMessage().getBodyMatchers().matchers().get(0).matchingType()).isEqualTo(REGEX);
		assertThat(((RegexProperty) contract.getOutputMessage().getBodyMatchers().matchers().get(0).value()).pattern())
			.isEqualTo("[0-9]{3}");
		assertThat(contract.getOutputMessage().getBodyMatchers().matchers().get(1).path()).isEqualTo("$.duck");
		assertThat(contract.getOutputMessage().getBodyMatchers().matchers().get(1).matchingType()).isEqualTo(EQUALITY);
		assertThat(contract.getOutputMessage().getBodyMatchers().matchers().get(2).path()).isEqualTo("$.alpha");
		assertThat(contract.getOutputMessage().getBodyMatchers().matchers().get(2).matchingType()).isEqualTo(REGEX);
		assertThat(((RegexProperty) contract.getOutputMessage().getBodyMatchers().matchers().get(2).value()).pattern())
			.isEqualTo(RegexPatterns.onlyAlphaUnicode().pattern());
		assertThat(contract.getOutputMessage().getBodyMatchers().matchers().get(3).path()).isEqualTo("$.alpha");
		assertThat(contract.getOutputMessage().getBodyMatchers().matchers().get(3).matchingType()).isEqualTo(EQUALITY);
		assertThat(contract.getOutputMessage().getBodyMatchers().matchers().get(4).path()).isEqualTo("$.number");
		assertThat(contract.getOutputMessage().getBodyMatchers().matchers().get(4).matchingType()).isEqualTo(REGEX);
		assertThat(((RegexProperty) contract.getOutputMessage().getBodyMatchers().matchers().get(4).value()).pattern())
			.isEqualTo(RegexPatterns.number().pattern());
		assertThat(contract.getOutputMessage().getBodyMatchers().matchers().get(5).path()).isEqualTo("$.aBoolean");
		assertThat(contract.getOutputMessage().getBodyMatchers().matchers().get(5).matchingType()).isEqualTo(REGEX);
		assertThat(((RegexProperty) contract.getOutputMessage().getBodyMatchers().matchers().get(5).value()).pattern())
			.isEqualTo(RegexPatterns.anyBoolean().pattern());
		assertThat(contract.getOutputMessage().getBodyMatchers().matchers().get(6).path()).isEqualTo("$.date");
		assertThat(contract.getOutputMessage().getBodyMatchers().matchers().get(6).matchingType()).isEqualTo(DATE);
		assertThat(((RegexProperty) contract.getOutputMessage().getBodyMatchers().matchers().get(6).value()).pattern())
			.isEqualTo(RegexPatterns.isoDate().pattern());
		assertThat(contract.getOutputMessage().getBodyMatchers().matchers().get(7).path()).isEqualTo("$.dateTime");
		assertThat(contract.getOutputMessage().getBodyMatchers().matchers().get(7).matchingType()).isEqualTo(TIMESTAMP);
		assertThat(((RegexProperty) contract.getOutputMessage().getBodyMatchers().matchers().get(7).value()).pattern())
			.isEqualTo(RegexPatterns.isoDateTime().pattern());
		assertThat(contract.getOutputMessage().getBodyMatchers().matchers().get(8).path()).isEqualTo("$.time");
		assertThat(contract.getOutputMessage().getBodyMatchers().matchers().get(8).matchingType()).isEqualTo(TIME);
		assertThat(((RegexProperty) contract.getOutputMessage().getBodyMatchers().matchers().get(8).value()).pattern())
			.isEqualTo(RegexPatterns.isoTime().pattern());
		assertThat(contract.getOutputMessage().getBodyMatchers().matchers().get(9).path())
			.isEqualTo("$.valueWithTypeMatch");
		assertThat(contract.getOutputMessage().getBodyMatchers().matchers().get(9).matchingType()).isEqualTo(TYPE);
		assertThat(contract.getOutputMessage().getBodyMatchers().matchers().get(10).path()).isEqualTo("$.valueWithMin");
		assertThat(contract.getOutputMessage().getBodyMatchers().matchers().get(10).matchingType()).isEqualTo(TYPE);
		assertThat(contract.getOutputMessage().getBodyMatchers().matchers().get(10).minTypeOccurrence()).isEqualTo(1);
		assertThat(contract.getOutputMessage().getBodyMatchers().matchers().get(11).path()).isEqualTo("$.valueWithMax");
		assertThat(contract.getOutputMessage().getBodyMatchers().matchers().get(11).matchingType()).isEqualTo(TYPE);
		assertThat(contract.getOutputMessage().getBodyMatchers().matchers().get(11).maxTypeOccurrence()).isEqualTo(3);
		assertThat(contract.getOutputMessage().getBodyMatchers().matchers().get(12).path())
			.isEqualTo("$.valueWithMinMax");
		assertThat(contract.getOutputMessage().getBodyMatchers().matchers().get(12).matchingType()).isEqualTo(TYPE);
		assertThat(contract.getOutputMessage().getBodyMatchers().matchers().get(12).minTypeOccurrence()).isEqualTo(1);
		assertThat(contract.getOutputMessage().getBodyMatchers().matchers().get(12).maxTypeOccurrence()).isEqualTo(3);
		assertThat(contract.getOutputMessage().getBodyMatchers().matchers().get(13).path())
			.isEqualTo("$.valueWithMinEmpty");
		assertThat(contract.getOutputMessage().getBodyMatchers().matchers().get(13).matchingType()).isEqualTo(TYPE);
		assertThat(contract.getOutputMessage().getBodyMatchers().matchers().get(13).minTypeOccurrence()).isEqualTo(0);
		assertThat(contract.getOutputMessage().getBodyMatchers().matchers().get(14).path())
			.isEqualTo("$.valueWithMaxEmpty");
		assertThat(contract.getOutputMessage().getBodyMatchers().matchers().get(14).matchingType()).isEqualTo(TYPE);
		assertThat(contract.getOutputMessage().getBodyMatchers().matchers().get(14).maxTypeOccurrence()).isEqualTo(0);
		assertThat(contract.getOutputMessage().getBodyMatchers().matchers().get(15).path()).isEqualTo("$.duck");
		assertThat(contract.getOutputMessage().getBodyMatchers().matchers().get(15).matchingType()).isEqualTo(COMMAND);
		assertThat(contract.getOutputMessage().getBodyMatchers().matchers().get(15).value())
			.isEqualTo(new ExecutionProperty("assertThatValueIsANumber($it)"));
	}

	@Test
	void should_convert_yaml_with_rest_with_response_from_request() throws Exception {
		File ymlBody = yamlFile("contract_from_file.yml");
		assertThat(converter.isAccepted(ymlBody)).isTrue();
		Collection<Contract> contracts = converter.convertFrom(ymlBody);
		assertThat(contracts).hasSize(1);
		Contract contract = contracts.iterator().next();
		ObjectMapper objectMapper = new ObjectMapper();
		assertThat(objectMapper.readValue(contract.getRequest().getBody().getClientValue().toString(), Map.class))
			.isEqualTo(objectMapper.readValue("{ \"hello\" : \"request\" }", Map.class));
		assertThat(objectMapper.readValue(contract.getResponse().getBody().getClientValue().toString(), Map.class))
			.isEqualTo(objectMapper.readValue("{ \"hello\" : \"response\" }", Map.class));
	}

	@Test
	void should_convert_yaml_with_rest_with_multipart() {
		File yml = yamlFile("contract_multipart.yml");
		assertThat(converter.isAccepted(yml)).isTrue();
		Collection<Contract> contracts = converter.convertFrom(yml);
		assertThat(contracts).hasSize(1);
		Contract contract = contracts.iterator().next();
		Map<?, ?> stubSide = (Map<?, ?>) MapConverter.getStubSideValues(contract.getRequest().getMultipart());
		assertThat(((Pattern) stubSide.get("formParameter")).pattern()).isEqualTo(".+");
		assertThat(((Pattern) stubSide.get("someBooleanParameter")).pattern())
			.isEqualTo(RegexPatterns.anyBoolean().pattern());
		Map<?, ?> testSide = (Map<?, ?>) MapConverter.getTestSideValues(contract.getRequest().getMultipart());
		assertThat(testSide.get("formParameter")).isEqualTo("\"formParameterValue\"");
		assertThat(testSide.get("someBooleanParameter")).isEqualTo("true");
		assertThat(((NamedProperty) testSide.get("file")).getName().getServerValue()).isEqualTo("filename.csv");
		assertThat(((Pattern) ((NamedProperty) testSide.get("file")).getName().getClientValue()).pattern())
			.isEqualTo(RegexPatterns.nonEmpty().pattern());
		assertThat(((NamedProperty) testSide.get("file")).getValue().getServerValue()).isEqualTo("file content");
		assertThat(((Pattern) ((NamedProperty) testSide.get("file")).getValue().getClientValue()).pattern())
			.isEqualTo(RegexPatterns.nonEmpty().pattern());
		assertThat(contract.getResponse().getStatus().getServerValue()).isEqualTo(200);
	}

	@Test
	void should_convert_yaml_with_messaging_triggered_by_method_to_dsl() {
		File ymlMessagingMethod = yamlFile("contract_message_method.yml");
		assertThat(converter.isAccepted(ymlMessagingMethod)).isTrue();
		Collection<Contract> contracts = converter.convertFrom(ymlMessagingMethod);
		assertThat(contracts).hasSize(1);
		Contract contract = contracts.iterator().next();
		assertThat(contract.getDescription()).isEqualTo("Some description");
		assertThat(contract.getLabel()).isEqualTo("some_label");
		assertThat(contract.getInput().getTriggeredBy().toString()).isEqualTo("bookReturnedTriggered()");
		assertThat(contract.getOutputMessage().getSentTo().getClientValue()).isEqualTo("output");
		assertThat(contract.getOutputMessage().getHeaders().getEntries()).anySatisfy((e) -> {
			assertThat(e.getName()).isEqualTo("BOOK-NAME");
			assertThat(e.getClientValue()).isEqualTo("foo");
		});
		assertThat(contract.getOutputMessage().getBody().getClientValue()).asInstanceOf(InstanceOfAssertFactories.MAP)
			.containsEntry("bookName", "foo");
	}

	@Test
	void should_convert_yaml_with_http_binary_body_to_dsl() {
		File ymlBytes = yamlFile("contract_pdf.yml");
		assertThat(converter.isAccepted(ymlBytes)).isTrue();
		Collection<Contract> contracts = converter.convertFrom(ymlBytes);
		assertThat(contracts).hasSize(1);
		Contract contract = contracts.iterator().next();
		assertThat(contract.getRequest().getBody().getClientValue()).isInstanceOf(FromFileProperty.class);
		assertThat(((FromFileProperty) contract.getRequest().getBody().getClientValue()).getType())
			.isEqualTo(byte[].class);
		assertThat(contract.getResponse().getBody().getClientValue()).isInstanceOf(FromFileProperty.class);
		assertThat(((FromFileProperty) contract.getResponse().getBody().getClientValue()).getType())
			.isEqualTo(byte[].class);
	}

	@Test
	void should_convert_yaml_with_messaging_binary_body_to_dsl() {
		File ymlMessagingBytes = yamlFile("contract_messaging_pdf.yml");
		assertThat(converter.isAccepted(ymlMessagingBytes)).isTrue();
		Collection<Contract> contracts = converter.convertFrom(ymlMessagingBytes);
		assertThat(contracts).hasSize(1);
		Contract contract = contracts.iterator().next();
		assertThat(contract.getOutputMessage().getBody().getClientValue()).isInstanceOf(FromFileProperty.class);
		assertThat(((FromFileProperty) contract.getOutputMessage().getBody().getClientValue()).getType())
			.isEqualTo(byte[].class);
	}

	@Test
	void should_assert_request_headers_when_converting_yaml_to_dsl() {
		File yml = resourceFile("/yml/contract_broken_request_headers.yml");
		assertThat(converter.isAccepted(yml)).isFalse();
		assertThatThrownBy(() -> converter.convertFrom(yml)).isInstanceOf(IllegalStateException.class)
			.hasMessageContaining(
					"Broken headers! A header with key [foo] with value [bar] is not matched by regex [barrrr]");
	}

	@Test
	void should_assert_response_headers_when_converting_yaml_to_dsl() {
		File yml = resourceFile("/yml/contract_broken_response_headers.yml");
		assertThat(converter.isAccepted(yml)).isFalse();
		assertThatThrownBy(() -> converter.convertFrom(yml)).isInstanceOf(IllegalStateException.class)
			.hasMessageContaining(
					"Broken headers! A header with key [foo2] with value [bar] is not matched by regex [barrrr]");
	}

	@Test
	void should_parse_multiple_documents_into_a_list_of_contracts() {
		File ymlMultiple = yamlFile("multiple_contracts.yml");
		assertThat(converter.isAccepted(ymlMultiple)).isTrue();
		Collection<Contract> contracts = converter.convertFrom(ymlMultiple);
		assertThat(contracts).hasSize(3);
		List<Contract> contractList = contracts instanceof List ? (List<Contract>) contracts : List.copyOf(contracts);
		assertThat(contractList.get(0).getRequest().getUrl().getClientValue()).isEqualTo("/users/1");
		assertThat(contractList.get(contractList.size() - 1).getRequest().getUrl().getClientValue())
			.isEqualTo("/users/3");
		assertThat(contracts.stream().map(Contract::getName).collect(Collectors.toSet())).hasSize(3);
	}

	@Test
	void should_dump_yml_as_string() {
		String expectedYaml1 = "---\n" + "description: null\n" + "ignored: false\n" + "inProgress: false\n"
				+ "input: null\n" + "label: null\n" + "metadata: {}\n" + "name: \"post1\"\n" + "outputMessage: null\n"
				+ "priority: null\n" + "request:\n" + "  body: null\n" + "  bodyFromFile: null\n"
				+ "  bodyFromFileAsBytes: null\n" + "  cookies: {}\n" + "  headers: {}\n" + "  matchers:\n"
				+ "    body: []\n" + "    cookies: []\n" + "    headers: []\n" + "    multipart: null\n"
				+ "    queryParameters: []\n" + "    url: null\n" + "  method: \"POST\"\n" + "  multipart: null\n"
				+ "  queryParameters: {}\n" + "  url: \"/users/1\"\n" + "  urlPath: null\n" + "response:\n"
				+ "  async: null\n" + "  body: null\n" + "  bodyFromFile: null\n" + "  bodyFromFileAsBytes: null\n"
				+ "  cookies: {}\n" + "  fixedDelayMilliseconds: null\n" + "  headers: {}\n" + "  matchers:\n"
				+ "    body: []\n" + "    cookies: []\n" + "    headers: []\n" + "  status: 200\n";
		String expectedYaml2 = "---\n" + "description: null\n" + "ignored: false\n" + "inProgress: false\n"
				+ "input: null\n" + "label: null\n" + "metadata: {}\n" + "name: \"post2\"\n" + "outputMessage: null\n"
				+ "priority: null\n" + "request:\n" + "  body: null\n" + "  bodyFromFile: null\n"
				+ "  bodyFromFileAsBytes: null\n" + "  cookies: {}\n" + "  headers: {}\n" + "  matchers:\n"
				+ "    body: []\n" + "    cookies: []\n" + "    headers: []\n" + "    multipart: null\n"
				+ "    queryParameters: []\n" + "    url: null\n" + "  method: \"POST\"\n" + "  multipart: null\n"
				+ "  queryParameters: {}\n" + "  url: \"/users/2\"\n" + "  urlPath: null\n" + "response:\n"
				+ "  async: null\n" + "  body: null\n" + "  bodyFromFile: null\n" + "  bodyFromFileAsBytes: null\n"
				+ "  cookies: {}\n" + "  fixedDelayMilliseconds: null\n" + "  headers: {}\n" + "  matchers:\n"
				+ "    body: []\n" + "    cookies: []\n" + "    headers: []\n" + "  status: 200\n";
		Map<String, byte[]> strings = converter.store(yamlContracts());
		assertThat(strings).hasSize(2);
		assertThat(new String(strings.get("post1.yml")).trim()).isEqualTo(expectedYaml1.trim());
		assertThat(new String(strings.get("post2.yml")).trim()).isEqualTo(expectedYaml2.trim());
	}

	private List<YamlContract> yamlContracts() {
		YamlContract contract1 = new YamlContract();
		contract1.name = "post1";
		contract1.request = new YamlContract.Request();
		contract1.request.method = "POST";
		contract1.request.url = "/users/1";
		contract1.response = new YamlContract.Response();
		contract1.response.status = 200;

		YamlContract contract2 = new YamlContract();
		contract2.name = "post2";
		contract2.request = new YamlContract.Request();
		contract2.request.method = "POST";
		contract2.request.url = "/users/2";
		contract2.response = new YamlContract.Response();
		contract2.response.status = 200;

		return List.of(contract1, contract2);
	}

	@Test
	void should_parse_messaging_contract_for_messaging_scenario_1() {
		File file = resourceFile("/yml/contract_message_scenario1.yml");
		assertThat(converter.isAccepted(file)).isTrue();
		Collection<Contract> contracts = converter.convertFrom(file);
		assertThat(contracts).hasSize(1);
		Contract contract = contracts.iterator().next();
		assertThat(contract.getInput() != null || contract.getOutputMessage() != null).isTrue();
	}

	@Test
	void should_convert_http_dsl_to_yaml() {
		File ymlWithRest = yamlFile("contract.yml");
		assertThat(converter.isAccepted(ymlWithRest)).isTrue();
		Collection<Contract> contracts = ContractVerifierDslConverter.convertAsCollection(new File("/"),
				"import sh.stubborn.contract.spec.Contract\n" + "Contract.make {\n" + "  request {\n"
						+ "    inProgress()\n" + "    url('/foo')\n" + "    method('PUT')\n" + "    headers {\n"
						+ "      header('foo', 'bar')\n" + "    }\n" + "    cookies {\n"
						+ "      cookie(foo: value(c('client'), p('server')))\n"
						+ "      cookie('bar', value(c('client'), p('server')))\n" + "    }\n"
						+ "    body([foo: 'bar'])\n" + "  }\n" + "  response {\n" + "    fixedDelayMilliseconds 1000\n"
						+ "    status(200)\n" + "    headers {\n" + "      header('foo2', 'bar')\n" + "    }\n"
						+ "    cookies {\n" + "      cookie(foo: value(c('client'), p('server')))\n"
						+ "      cookie('bar', value(c('client'), p('server')))\n" + "    }\n"
						+ "    body([foo2: 'bar'])\n" + "  }\n" + "}");
		Collection<YamlContract> yamlContracts = converter.convertTo(contracts);
		assertThat(yamlContracts).hasSize(1);
		YamlContract yamlContract = yamlContracts.iterator().next();
		assertThat(yamlContract.inProgress).isTrue();
		assertThat(yamlContract.request.url).isEqualTo("/foo");
		assertThat(yamlContract.request.method).isEqualTo("PUT");
		assertThat(yamlContract.request.headers).anySatisfy((k, v) -> {
			assertThat(k).isEqualTo("foo");
			assertThat(v).isEqualTo("bar");
		});
		assertThat(yamlContract.request.cookies).anySatisfy((k, v) -> {
			assertThat(k).isEqualTo("bar");
			assertThat(v).isEqualTo("server");
		});
		assertThat(yamlContract.request.cookies).anySatisfy((k, v) -> {
			assertThat(k).isEqualTo("foo");
			assertThat(v).isEqualTo("server");
		});
		assertThat(yamlContract.request.body).asInstanceOf(InstanceOfAssertFactories.MAP).containsEntry("foo", "bar");
		assertThat(yamlContract.response.status).isEqualTo(200);
		assertThat(yamlContract.response.headers).anySatisfy((k, v) -> {
			assertThat(k).isEqualTo("foo2");
			assertThat(v).isEqualTo("bar");
		});
		assertThat(yamlContract.response.body).asInstanceOf(InstanceOfAssertFactories.MAP).containsEntry("foo2", "bar");
		assertThat(yamlContract.response.cookies).anySatisfy((k, v) -> {
			assertThat(k).isEqualTo("foo");
			assertThat(v).isEqualTo("client");
		});
		assertThat(yamlContract.response.cookies).anySatisfy((k, v) -> {
			assertThat(k).isEqualTo("bar");
			assertThat(v).isEqualTo("client");
		});
		assertThat(yamlContract.response.fixedDelayMilliseconds).isEqualTo(1000);
	}

	@Test
	void should_convert_multiple_messaging_dsls_to_yaml() {
		File ymlMessagingMethod = yamlFile("contract_message_method.yml");
		assertThat(converter.isAccepted(ymlMessagingMethod)).isTrue();
		Collection<Contract> contracts = ContractVerifierDslConverter.convertAsCollection(new File("/"),
				"import sh.stubborn.contract.spec.Contract\n" + "[Contract.make {\n" + "  input {\n"
						+ "    description('Some description')\n" + "    label('some_label')\n"
						+ "    triggeredBy('bookReturnedTriggered()')\n" + "  }\n" + "  outputMessage {\n"
						+ "    sentTo('output')\n" + "    body([bookName: 'foo'])\n" + "    headers {\n"
						+ "      header('BOOK-NAME', 'foo')\n" + "    }\n" + "  }\n" + "}, Contract.make {\n"
						+ "  input {\n" + "    description('Some description2')\n" + "    label('some_label2')\n"
						+ "    triggeredBy('bookReturnedTriggered()2')\n" + "  }\n" + "  outputMessage {\n"
						+ "    sentTo('output2')\n" + "    body([bookName2: 'foo'])\n" + "    headers {\n"
						+ "      header('BOOK-NAME2', 'foo')\n" + "    }\n" + "  }\n" + "}]");
		Collection<YamlContract> yamlContracts = converter.convertTo(contracts);
		assertThat(yamlContracts).hasSize(2);
		List<YamlContract> yamlList = yamlContracts instanceof List ? (List<YamlContract>) yamlContracts
				: List.copyOf(yamlContracts);
		YamlContract yamlContract = yamlList.get(0);
		assertThat(yamlContract.description).isEqualTo("Some description");
		assertThat(yamlContract.label).isEqualTo("some_label");
		assertThat(yamlContract.input.triggeredBy).isEqualTo("bookReturnedTriggered()");
		assertThat(yamlContract.outputMessage.sentTo).isEqualTo("output");
		assertThat(yamlContract.outputMessage.body).asInstanceOf(InstanceOfAssertFactories.MAP)
			.containsEntry("bookName", "foo");
		assertThat(yamlContract.outputMessage.headers).containsEntry("BOOK-NAME", "foo");
		YamlContract yamlContract2 = yamlList.get(1);
		assertThat(yamlContract2.description).isEqualTo("Some description2");
		assertThat(yamlContract2.label).isEqualTo("some_label2");
		assertThat(yamlContract2.input.triggeredBy).isEqualTo("bookReturnedTriggered()2");
		assertThat(yamlContract2.outputMessage.sentTo).isEqualTo("output2");
		assertThat(yamlContract2.outputMessage.body).asInstanceOf(InstanceOfAssertFactories.MAP)
			.containsEntry("bookName2", "foo");
		assertThat(yamlContract2.outputMessage.headers).containsEntry("BOOK-NAME2", "foo");
	}

	@Test
	void should_convert_messaging_dsl_with_input_triggered_by_method_to_yaml() {
		File ymlMessagingMethod = yamlFile("contract_message_method.yml");
		assertThat(converter.isAccepted(ymlMessagingMethod)).isTrue();
		Collection<Contract> contracts = ContractVerifierDslConverter.convertAsCollection(new File("/"),
				"import sh.stubborn.contract.spec.Contract\n" + "Contract.make {\n" + "  input {\n"
						+ "    description('Some description')\n" + "    label('some_label')\n"
						+ "    triggeredBy('bookReturnedTriggered()')\n" + "  }\n" + "  outputMessage {\n"
						+ "    sentTo('output')\n" + "    body([bookName: 'foo'])\n" + "    headers {\n"
						+ "      header('BOOK-NAME', 'foo')\n" + "    }\n" + "  }\n" + "}");
		Collection<YamlContract> yamlContracts = converter.convertTo(contracts);
		assertThat(yamlContracts).hasSize(1);
		YamlContract yamlContract = yamlContracts.iterator().next();
		assertThat(yamlContract.description).isEqualTo("Some description");
		assertThat(yamlContract.label).isEqualTo("some_label");
		assertThat(yamlContract.input.triggeredBy).isEqualTo("bookReturnedTriggered()");
		assertThat(yamlContract.outputMessage.sentTo).isEqualTo("output");
		assertThat(yamlContract.outputMessage.body).asInstanceOf(InstanceOfAssertFactories.MAP)
			.containsEntry("bookName", "foo");
		assertThat(yamlContract.outputMessage.headers).containsEntry("BOOK-NAME", "foo");
	}

	@Test
	void should_convert_messaging_with_a_message_dsl_to_yaml() {
		File ymlMessagingMatchers = yamlFile("contract_message_matchers.yml");
		assertThat(converter.isAccepted(ymlMessagingMatchers)).isTrue();
		Collection<Contract> contracts = ContractVerifierDslConverter
			.convertAsCollection(new File("/"), "import sh.stubborn.contract.spec.Contract\n"
					+ "import static sh.stubborn.contract.spec.internal.RegexPatterns.*\n" + "Contract.make {\n"
					+ "  name('fooo')\n" + "  label('card_rejected')\n" + "  ignored()\n" + "  inProgress()\n"
					+ "  input {\n" + "    triggeredBy('foo()')\n" + "  }\n" + "  outputMessage {\n"
					+ "    sentTo('channel')\n" + "    body([duck: 123, alpha: 'abc', number: 123, aBoolean: true,\n"
					+ "          date: '2017-01-01', dateTime: '2017-01-01T01:23:45', time: '01:02:34',\n"
					+ "          valueWithoutAMatcher: 'foo', valueWithTypeMatch: 'string',\n"
					+ "          valueWithMin: [1, 2, 3], valueWithMax: [1, 2, 3], valueWithMinMax: [1, 2, 3],\n"
					+ "          valueWithMinEmpty: [], valueWithMaxEmpty: [],\n"
					+ "          'key': ['complex.key': 'foo'], nullValue: null])\n" + "    bodyMatchers {\n"
					+ "      jsonPath('$.duck', byRegex('[0-9]{3}'))\n" + "      jsonPath('$.duck', byEquality())\n"
					+ "      jsonPath('$.alpha', byRegex(onlyAlphaUnicode()))\n"
					+ "      jsonPath('$.alpha', byEquality())\n" + "      jsonPath('$.number', byRegex(number()))\n"
					+ "      jsonPath('$.positiveInteger', byRegex(positiveInt()))\n"
					+ "      jsonPath('$.integer', byRegex(anInteger()))\n"
					+ "      jsonPath('$.double', byRegex(aDouble()))\n"
					+ "      jsonPath('$.aBoolean', byRegex(anyBoolean()))\n" + "      jsonPath('$.date', byDate())\n"
					+ "      jsonPath('$.dateTime', byTimestamp())\n" + "      jsonPath('$.time', byTime())\n"
					+ "      jsonPath('$.valueWithTypeMatch', byType())\n"
					+ "      jsonPath('$.valueWithMin', byType { minOccurrence(1) })\n"
					+ "      jsonPath('$.valueWithMax', byType { maxOccurrence(3) })\n"
					+ "      jsonPath('$.valueWithMinMax', byType { minOccurrence(1); maxOccurrence(3) })\n"
					+ "      jsonPath('$.valueWithMinEmpty', byType { minOccurrence(0) })\n"
					+ "      jsonPath('$.valueWithMaxEmpty', byType { maxOccurrence(0) })\n"
					+ "      jsonPath('$.duck', byCommand('assertThatValueIsANumber($it)'))\n"
					+ "      jsonPath(\"\\$.[\\'key\\'].[\\'complex.key\\']\", byEquality())\n"
					+ "      jsonPath('$.nullValue', byNull())\n" + "    }\n" + "    headers {\n"
					+ "      messagingContentType(applicationJson())\n"
					+ "      header('Some-Header', $(c('someValue'), p(regex('[a-zA-Z]{9}'))))\n" + "    }\n" + "  }\n"
					+ "}");
		Collection<YamlContract> yamlContracts = converter.convertTo(contracts);
		assertThat(yamlContracts).hasSize(1);
		YamlContract yamlContract = yamlContracts.iterator().next();
		assertThat(yamlContract.name).isEqualTo("fooo");
		assertThat(yamlContract.ignored).isTrue();
		assertThat(yamlContract.inProgress).isTrue();
		assertThat(yamlContract.input.triggeredBy).isEqualTo("foo()");
		assertThat(yamlContract.label).isEqualTo("card_rejected");
		assertThat(yamlContract.outputMessage.sentTo).isEqualTo("channel");
		assertThat(yamlContract.outputMessage.body).asInstanceOf(InstanceOfAssertFactories.MAP)
			.containsEntry("duck", 123)
			.containsEntry("alpha", "abc")
			.containsEntry("number", 123)
			.containsEntry("aBoolean", true)
			.containsEntry("date", "2017-01-01")
			.containsEntry("dateTime", "2017-01-01T01:23:45")
			.containsEntry("time", "01:02:34")
			.containsEntry("valueWithoutAMatcher", "foo")
			.containsEntry("valueWithTypeMatch", "string")
			.containsEntry("valueWithMin", List.of(1, 2, 3))
			.containsEntry("valueWithMax", List.of(1, 2, 3))
			.containsEntry("valueWithMinMax", List.of(1, 2, 3))
			.containsEntry("valueWithMinEmpty", List.of())
			.containsEntry("valueWithMaxEmpty", List.of())
			.containsKey("nullValue")
			.satisfies((m) -> assertThat(((Map<?, ?>) m).get("nullValue")).isNull());
		assertThat(yamlContract.outputMessage.headers).containsEntry("contentType", "application/json")
			.containsEntry("Some-Header", "someValue");

		YamlContract.TestHeaderMatcher expectedHeaderMatcher = new YamlContract.TestHeaderMatcher();
		expectedHeaderMatcher.key = "Some-Header";
		expectedHeaderMatcher.regex = "[a-zA-Z]{9}";
		expectedHeaderMatcher.regexType = YamlContract.RegexType.as_string;
		assertThat(yamlContract.outputMessage.matchers.headers).containsExactly(expectedHeaderMatcher);

		assertThat(yamlContract.outputMessage.matchers.body).containsExactly(
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

	@Test
	void should_convert_contract_with_body_as_bytes() {
		File groovyBytes = resourceFile("/body_builder/worksWithPdf.groovy");
		Collection<Contract> contracts = ContractVerifierDslConverter.convertAsCollection(groovyBytes);
		Collection<YamlContract> yamlContracts = converter.convertTo(contracts);
		assertThat(yamlContracts).hasSize(1);
		YamlContract yamlContract = yamlContracts.iterator().next();
		assertThat(yamlContract.request.body).isNull();
		assertThat(yamlContract.request.bodyFromFileAsBytes).isNotNull();
	}

	@Test
	void should_read_contract_from_bytes() {
		YamlContract inputContract = new YamlContract();
		inputContract.name = "post1";
		inputContract.request = new YamlContract.Request();
		inputContract.request.method = "POST";
		inputContract.request.url = "/users/1";
		inputContract.response = new YamlContract.Response();
		inputContract.response.status = 200;
		Map<String, byte[]> strings = converter.store(List.of(inputContract));
		List<YamlContract> yamlContracts = converter.read(strings.values().iterator().next());
		assertThat(yamlContracts).hasSize(1);
		YamlContract yamlContract = yamlContracts.get(0);
		assertThat(yamlContract.request.method).isEqualTo("POST");
		assertThat(yamlContract.request.url).isEqualTo("/users/1");
		assertThat(yamlContract.response.status).isEqualTo(200);
	}

	@Test
	void should_convert_rest_yaml_with_xml_request_and_response_to_dsl() {
		File ymlRestXml = yamlFile("contract_rest_xml.yml");
		assertThat(converter.isAccepted(ymlRestXml)).isTrue();
		Collection<Contract> contracts = converter.convertFrom(ymlRestXml);
		assertThat(contracts).hasSize(1);
		Contract contract = contracts.iterator().next();
		assertThat(contract.getRequest().getHeaders().getEntries()).anySatisfy((e) -> {
			assertThat(e.getName()).isEqualTo("Content-Type");
			assertThat(e.getClientValue()).isEqualTo("application/xml");
			assertThat(e.getServerValue()).isEqualTo("application/xml");
		});
		assertThat(contract.getRequest().getBodyMatchers().matchers().get(0).path()).isEqualTo("/test/duck/text()");
		assertThat(contract.getRequest().getBodyMatchers().matchers().get(0).matchingType()).isEqualTo(REGEX);
		assertThat(((RegexProperty) contract.getRequest().getBodyMatchers().matchers().get(0).value()).pattern())
			.isEqualTo("[0-9]{10}");
		assertThat(contract.getRequest().getBodyMatchers().matchers().get(1).path()).isEqualTo("/test/duck/text()");
		assertThat(contract.getRequest().getBodyMatchers().matchers().get(1).matchingType()).isEqualTo(EQUALITY);
		assertThat(contract.getRequest().getBodyMatchers().matchers().get(2).path()).isEqualTo("/test/time/text()");
		assertThat(contract.getRequest().getBodyMatchers().matchers().get(2).matchingType()).isEqualTo(TIME);
		assertThat(((RegexProperty) contract.getRequest().getBodyMatchers().matchers().get(2).value()).pattern())
			.isEqualTo(RegexPatterns.isoTime().pattern());
		assertThat(contract.getRequest().getBody().getClientValue().toString().replaceAll("\n", "").replaceAll(" ", ""))
			.isEqualTo(XML_CONTRACT_BODY.replaceAll("\n", "").replaceAll(" ", ""));
		assertThat(contract.getRequest().getBody().getServerValue().toString().replaceAll("\n", "").replaceAll(" ", ""))
			.isEqualTo(XML_CONTRACT_BODY.replaceAll("\n", "").replaceAll(" ", ""));

		assertThat(contract.getResponse().getBodyMatchers().matchers().get(0).path()).isEqualTo("/test/duck/text()");
		assertThat(contract.getResponse().getBodyMatchers().matchers().get(0).matchingType()).isEqualTo(REGEX);
		assertThat(((RegexProperty) contract.getResponse().getBodyMatchers().matchers().get(0).value()).pattern())
			.isEqualTo("[0-9]{10}");
		assertThat(contract.getResponse().getBodyMatchers().matchers().get(1).path()).isEqualTo("/test/duck/text()");
		assertThat(contract.getResponse().getBodyMatchers().matchers().get(1).matchingType()).isEqualTo(COMMAND);
		assertThat(((ExecutionProperty) contract.getResponse().getBodyMatchers().matchers().get(1).value())
			.getExecutionCommand()).isEqualTo("test($it)");
		assertThat(contract.getResponse().getBodyMatchers().matchers().get(2).path()).isEqualTo("/test/duck/xxx");
		assertThat(contract.getResponse().getBodyMatchers().matchers().get(2).matchingType()).isEqualTo(NULL);
		assertThat(contract.getResponse().getBodyMatchers().matchers().get(3).path()).isEqualTo("/test/duck/text()");
		assertThat(contract.getResponse().getBodyMatchers().matchers().get(3).matchingType()).isEqualTo(EQUALITY);
		assertThat(contract.getResponse().getBodyMatchers().matchers().get(4).path()).isEqualTo("/test/time/text()");
		assertThat(contract.getResponse().getBodyMatchers().matchers().get(4).matchingType()).isEqualTo(TIME);
		assertThat(((RegexProperty) contract.getResponse().getBodyMatchers().matchers().get(4).value()).pattern())
			.isEqualTo(RegexPatterns.isoTime().pattern());
		assertThat(
				contract.getResponse().getBody().getClientValue().toString().replaceAll("\n", "").replaceAll(" ", ""))
			.isEqualTo(XML_CONTRACT_BODY.replaceAll("\n", "").replaceAll(" ", ""));
		assertThat(
				contract.getResponse().getBody().getServerValue().toString().replaceAll("\n", "").replaceAll(" ", ""))
			.isEqualTo(XML_CONTRACT_BODY.replaceAll("\n", "").replaceAll(" ", ""));
	}

	@Test
	void should_convert_rest_yaml_with_xml_with_namespace_in_request_and_response_to_dsl() {
		File ymlRestNamedXml = yamlFile("contract_rest_named_xml.yml");
		assertThat(converter.isAccepted(ymlRestNamedXml)).isTrue();
		Collection<Contract> contracts = converter.convertFrom(ymlRestNamedXml);
		assertThat(contracts).hasSize(1);
		Contract contract = contracts.iterator().next();
		assertThat(contract.getRequest().getHeaders().getEntries()).anySatisfy((e) -> {
			assertThat(e.getName()).isEqualTo("Content-Type");
			assertThat(e.getClientValue()).isEqualTo("application/xml");
			assertThat(e.getServerValue()).isEqualTo("application/xml");
		});
		assertThat(contract.getRequest().getBodyMatchers().matchers()).isEmpty();
		assertThat(contract.getRequest().getBody().getClientValue().toString().replaceAll("\n", "").replaceAll(" ", ""))
			.isEqualTo(XML_CONTRACT_BODY_WITH_NAMESPACES.replaceAll("\n", "").replaceAll(" ", ""));
		assertThat(contract.getRequest().getBody().getServerValue().toString().replaceAll("\n", "").replaceAll(" ", ""))
			.isEqualTo(XML_CONTRACT_BODY_WITH_NAMESPACES.replaceAll("\n", "").replaceAll(" ", ""));

		assertThat(contract.getResponse().getBodyMatchers().matchers()).isEmpty();
		assertThat(
				contract.getResponse().getBody().getClientValue().toString().replaceAll("\n", "").replaceAll(" ", ""))
			.isEqualTo(XML_CONTRACT_BODY_WITH_NAMESPACES.replaceAll("\n", "").replaceAll(" ", ""));
		assertThat(
				contract.getResponse().getBody().getServerValue().toString().replaceAll("\n", "").replaceAll(" ", ""))
			.isEqualTo(XML_CONTRACT_BODY_WITH_NAMESPACES.replaceAll("\n", "").replaceAll(" ", ""));
	}

	@Test
	void should_accept_a_yaml_file_that_is_a_proper_scc_yaml_contract() {
		File ymlWithRest3 = yamlFile("contract_rest_with_path.yml");
		assertThat(converter.isAccepted(ymlWithRest3)).isTrue();
	}

	@Test
	void should_not_accept_a_yaml_file_that_is_not_a_scc_yaml_contract() {
		File oa3File = resourceFile("/yml/oa3/openapi_petstore.yml");
		assertThat(converter.isAccepted(oa3File)).isFalse();
	}

}
