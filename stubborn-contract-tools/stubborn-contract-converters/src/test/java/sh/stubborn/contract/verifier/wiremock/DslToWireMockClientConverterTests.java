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
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.github.tomakehurst.wiremock.matching.RegexPattern;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.skyscreamer.jsonassert.JSONAssert;
import sh.stubborn.contract.spec.Contract;
import sh.stubborn.contract.verifier.dsl.wiremock.WireMockStubMapping;
import sh.stubborn.contract.verifier.file.ContractMetadata;
import sh.stubborn.contract.verifier.util.ContractVerifierDslConverter;

import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.RequestEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static org.assertj.core.api.Assertions.assertThat;

@WireMockTest
class DslToWireMockClientConverterTests {

	@TempDir
	Path tmpDir;

	@Test
	void should_convert_dsl_file_to_wiremock_json(WireMockRuntimeInfo wm) throws Exception {
		String url = "http://localhost:" + wm.getHttpPort();
		DslToWireMockClientConverter converter = new DslToWireMockClientConverter();
		Path file = tmpDir.resolve("dsl1.groovy");
		Files.writeString(file, """
				sh.stubborn.contract.spec.Contract.make {
					request {
						method('PUT')
						url $(consumer(~/\\/[0-9]{2}/), producer('/12'))
					}
					response {
						status OK()
					}
				}
				""");
		String json = converter
			.convertContents("Test",
					new ContractMetadata(file, false, 0, null,
							ContractVerifierDslConverter.convertAsCollection(new File("/"), file.toFile())))
			.values()
			.iterator()
			.next();
		JSONAssert.assertEquals("""
				{"request":{"method":"PUT","urlPattern":"/[0-9]{2}"},"response":{"status":200}}
				""", json, false);
		StubMapping mapping = stubMappingIsValidWireMockStub(json);
		wm.getWireMock().register(mapping);
		new TestRestTemplate().exchange(RequestEntity.put(URI.create(url + "/12")).body(""), String.class);
	}

	@Test
	void should_convert_dsl_file_to_wiremock_json_with_byte_arrays(WireMockRuntimeInfo wm) throws Exception {
		String url = "http://localhost:" + wm.getHttpPort();
		DslToWireMockClientConverter converter = new DslToWireMockClientConverter();
		Path file = tmpDir.resolve("dsl1.groovy");
		Files.writeString(file, """
				[
				sh.stubborn.contract.spec.Contract.make {
					request {
						method "POST"
						url "/multipart"
						headers {
							contentType('multipart/form-data')
						}
						multipart(
								file: named(
										name: value(stub(regex('.+')), test('file')),
										content: value(stub(regex('.+')), test([100, 117, 100, 97] as byte[]))
								)
						)
					}
					response {
						status 200
						body "hello"
					}
				}
				]
				""");
		String json = converter
			.convertContents("Test",
					new ContractMetadata(file, false, 0, null,
							ContractVerifierDslConverter.convertAsCollection(new File("/"), file.toFile())))
			.values()
			.iterator()
			.next();
		JSONAssert.assertEquals(
				"""
						{"request":{"url":"/multipart","method":"POST","headers":{"Content-Type":{"matches":"multipart/form-data.*"}},"bodyPatterns":[{"matches" : ".*--(.*)\\r?\\nContent-Disposition: form-data; name=\\"file\\"; filename=\\".+\\"\\r?\\n(Content-Type: .*\\r?\\n)?(Content-Transfer-Encoding: .*\\r?\\n)?(Content-Length: \\\\d+\\r?\\n)?\\r?\\n.+\\r?\\n--.*"}]},"response":{"status":200,"body":"hello","transformers":["response-template", "stubborn-contract" ]}}
						""",
				json, false);
		StubMapping mapping = stubMappingIsValidWireMockStub(json);
		wm.getWireMock().register(mapping);
		MultiValueMap<String, Object> parameters = new LinkedMultiValueMap<>();
		parameters.add("file", new org.springframework.core.io.ByteArrayResource(new byte[] { 100, 117, 100, 97 }) {
			@Override
			public String getFilename() {
				return "file";
			}
		});
		HttpHeaders headers = new HttpHeaders();
		headers.set("Content-Type", "multipart/form-data;boundary=AaB03xssssss");
		headers.set("Accept", "text/plain");
		String result = new TestRestTemplate().postForObject(url + "/multipart",
				new HttpEntity<MultiValueMap<String, Object>>(parameters, headers), String.class);
		assertThat(result).isEqualTo("hello");
	}

	@Test
	void should_convert_dsl_file_with_list_of_contracts_to_wiremock_jsons(WireMockRuntimeInfo wm) throws Exception {
		String url = "http://localhost:" + wm.getHttpPort();
		DslToWireMockClientConverter converter = new DslToWireMockClientConverter();
		Path file = tmpDir.resolve("dsl1_list.groovy");
		Files.writeString(file, """
				(1..2).collect { int index ->
					sh.stubborn.contract.spec.Contract.make {
						request {
							method(PUT())
							headers {
								contentType(applicationJson())
							}
							url "/${index}"
						}
						response {
							status OK()
						}
					}
				}
				""");
		Map<Contract, String> convertedContents = converter.convertContents("Test", new ContractMetadata(file, false, 0,
				null, ContractVerifierDslConverter.convertAsCollection(new File("/"), file.toFile())));
		assertThat(convertedContents).hasSize(2);
		List<String> values = List.copyOf(convertedContents.values());
		JSONAssert.assertEquals(jsonResponse(1), values.get(0), false);
		JSONAssert.assertEquals(jsonResponse(2), values.get(1), false);
		StubMapping mapping = stubMappingIsValidWireMockStub(values.get(0));
		StubMapping mapping2 = stubMappingIsValidWireMockStub(values.get(1));
		wm.getWireMock().register(mapping);
		wm.getWireMock().register(mapping2);
		new TestRestTemplate().exchange(
				RequestEntity.put(URI.create(url + "/1")).header("Content-Type", "application/json").body(""),
				String.class);
		new TestRestTemplate().exchange(
				RequestEntity.put(URI.create(url + "/2")).header("Content-Type", "application/json").body(""),
				String.class);
	}

	private String jsonResponse(int index) {
		return "{\"request\":{\"method\":\"PUT\",\"url\":\"/" + index + "\"},\"response\":{\"status\":200}}";
	}

	@Test
	void should_not_convert_if_contract_is_messaging_related(WireMockRuntimeInfo wm) throws Exception {
		DslToWireMockClientConverter converter = new DslToWireMockClientConverter();
		Path file = tmpDir.resolve("dsl1_list.groovy");
		Files.writeString(file, """
				(1..2).collect { int index ->
					sh.stubborn.contract.spec.Contract.make {
						input {
							triggeredBy("foo")
						}
					}
				}
				""");
		Map<Contract, String> convertedContents = converter.convertContents("Test", new ContractMetadata(file, false, 0,
				null, ContractVerifierDslConverter.convertAsCollection(new File("/"), file.toFile())));
		assertThat(convertedContents).isEmpty();
	}

	@Test
	void should_creation_of_delayed_stub_responses_be_possible(WireMockRuntimeInfo wm) throws Exception {
		String url = "http://localhost:" + wm.getHttpPort();
		DslToWireMockClientConverter converter = new DslToWireMockClientConverter();
		Path file = tmpDir.resolve("dsl-delay.groovy");
		Files.writeString(file, """
				sh.stubborn.contract.spec.Contract.make {
					request {
						method 'GET'
						url '/foo'
					}
					response {
						status OK()
						fixedDelayMilliseconds 1000
					}
				}
				""");
		String json = converter
			.convertContents("test",
					new ContractMetadata(file, false, 0, null,
							ContractVerifierDslConverter.convertAsCollection(new File("/"), file.toFile())))
			.values()
			.iterator()
			.next();
		JSONAssert.assertEquals("""
				{"request":{"url" : "/foo","method" : "GET"},"response":{"status":200,"fixedDelayMilliseconds":1000}}
				""", json, false);
		StubMapping mapping = stubMappingIsValidWireMockStub(json);
		wm.getWireMock().register(mapping);
		new TestRestTemplate().exchange(RequestEntity.get(URI.create(url + "/foo")).build(), String.class);
	}

	@Test
	void should_convert_dsl_file_with_a_nested_list_to_wiremock_json(WireMockRuntimeInfo wm) throws Exception {
		String url = "http://localhost:" + wm.getHttpPort();
		DslToWireMockClientConverter converter = new DslToWireMockClientConverter();
		Path file = tmpDir.resolve("dsl2.groovy");
		Files.writeString(file,
				"""
						sh.stubborn.contract.spec.Contract.make {
							request {
								method 'PUT'
								url '/api/12'
								headers {
									header 'Content-Type': 'application/vnd.sh.stubborn.contract.verifier.twitter-places-analyzer.v1+json'
								}
								body '''
							[{
								"created_at": "Sat Jul 26 09:38:57 +0000 2014",
								"id": 492967299297845248,
								"id_str": "492967299297845248",
								"text": "Gonna see you at Warsaw",
								"place":
								{
									"attributes":{},
									"bounding_box":
									{
										"coordinates":
											[[
												[-77.119759,38.791645],
												[-76.909393,38.791645],
												[-76.909393,38.995548],
												[-77.119759,38.995548]
											]],
										"type":"Polygon"
									},
									"country":"United States",
									"country_code":"US",
									"full_name":"Washington, DC",
									"id":"01fbe706f872cb32",
									"name":"Washington",
									"place_type":"city",
									"url": "https://api.twitter.com/1/geo/id/01fbe706f872cb32.json"
								}
							}]
						'''
							}
							response {
								status OK()
							}
						}
						""");
		String json = converter
			.convertContents("Test",
					new ContractMetadata(file, false, 0, null,
							ContractVerifierDslConverter.convertAsCollection(new File("/"), file.toFile())))
			.values()
			.iterator()
			.next();
		JSONAssert.assertEquals("""
				{
				  "request" : {
					"url" : "/api/12",
					"method" : "PUT",
					"headers" : {
					  "Content-Type" : {
						"equalTo" : "application/vnd.sh.stubborn.contract.verifier.twitter-places-analyzer.v1+json"
					  }
					}
				  },
				  "response" : {
					"status" : 200
				  }
				}
				""", json, false);
		StubMapping mapping = stubMappingIsValidWireMockStub(json);
		wm.getWireMock().register(mapping);
		new TestRestTemplate().exchange(RequestEntity.put(URI.create(url + "/api/12"))
			.header("Content-Type", "application/vnd.sh.stubborn.contract.verifier.twitter-places-analyzer.v1+json")
			.body("""
					[{
						"created_at": "Sat Jul 26 09:38:57 +0000 2014",
						"id": 492967299297845248,
						"id_str": "492967299297845248",
						"text": "Gonna see you at Warsaw",
						"place":
						{
							"attributes":{},
							"bounding_box":
							{
								"coordinates":
									[[
										[-77.119759,38.791645],
										[-76.909393,38.791645],
										[-76.909393,38.995548],
										[-77.119759,38.995548]
									]],
								"type":"Polygon"
							},
							"country":"United States",
							"country_code":"US",
							"full_name":"Washington, DC",
							"id":"01fbe706f872cb32",
							"name":"Washington",
							"place_type":"city",
							"url": "https://api.twitter.com/1/geo/id/01fbe706f872cb32.json"
						}
					}]"""), String.class);
	}

	@Test
	void should_create_stub_with_map_inside_list(WireMockRuntimeInfo wm) throws Exception {
		String url = "http://localhost:" + wm.getHttpPort();
		DslToWireMockClientConverter converter = new DslToWireMockClientConverter();
		Path file = tmpDir.resolve("dsl-mapinlist.groovy");
		Files.writeString(file, """
				sh.stubborn.contract.spec.Contract.make {
					request {
						method 'GET'
						urlPath '/foos'
					}
					response {
						status OK()
						body([[id: value(
								consumer('123'),
								producer(regex('[0-9]+'))
						)], [id: value(
								consumer('567'),
								producer(regex('[0-9]+'))
						)]])
						headers {
							header 'Content-Type': 'application/json'
						}
					}
				}
				""");
		String json = converter
			.convertContents("test",
					new ContractMetadata(file, false, 0, null,
							ContractVerifierDslConverter.convertAsCollection(new File("/"), file.toFile())))
			.values()
			.iterator()
			.next();
		JSONAssert.assertEquals(
				"""
						{"request":{"urlPath":"/foos","method":"GET"},"response":{"body":"[{\\"id\\":\\"123\\"},{\\"id\\":\\"567\\"}]"}}
						""",
				json, false);
		StubMapping mapping = stubMappingIsValidWireMockStub(json);
		wm.getWireMock().register(mapping);
		var response = new TestRestTemplate().exchange(RequestEntity.get(URI.create(url + "/foos")).build(),
				String.class);
		assertThat(response.getHeaders().get("Content-Type")).contains("application/json");
		JSONAssert.assertEquals("[ { \"id\":\"123\" }, { \"id\": \"567\" } ]", response.getBody(), false);
	}

	@Test
	void should_create_stub_when_response_has_only_one_side_of_the_dynamic_value(WireMockRuntimeInfo wm)
			throws Exception {
		String url = "http://localhost:" + wm.getHttpPort();
		DslToWireMockClientConverter converter = new DslToWireMockClientConverter();
		Path file = tmpDir.resolve("dsl-dynamic.groovy");
		Files.writeString(file, """
				sh.stubborn.contract.spec.Contract.make {
					request {
						method 'GET'
						urlPath '/foos'
					}
					response {
						status OK()
						body(
							digit: $(producer(regex('[0-9]{1}'))),
							id: $(producer(regex(number())))
						)
					}
				}
				""");
		String json = converter
			.convertContents("test",
					new ContractMetadata(file, false, 0, null,
							ContractVerifierDslConverter.convertAsCollection(new File("/"), file.toFile())))
			.values()
			.iterator()
			.next();
		assertThat(json).isNotNull().doesNotContain("cursor");
		StubMapping mapping = stubMappingIsValidWireMockStub(json);
		wm.getWireMock().register(mapping);
		var response = new TestRestTemplate().exchange(RequestEntity.get(URI.create(url + "/foos")).build(),
				String.class);
		assertThat(response.getBody()).isNotNull();
	}

	@Test
	void should_convert_dsl_to_wiremock_to_show_it_in_the_docs(WireMockRuntimeInfo wm) throws Exception {
		String url = "http://localhost:" + wm.getHttpPort();
		DslToWireMockClientConverter converter = new DslToWireMockClientConverter();
		Path file = tmpDir.resolve("dsl_from_docs.groovy");
		Files.writeString(file,
				"""
						sh.stubborn.contract.spec.Contract.make {
							priority 1
							request {
								method 'POST'
								url '/users/password'
								headers {
									header 'Content-Type': 'application/json'
								}
								body(
										email: $(consumer(optional(regex(email()))), producer('abc@abc.com')),
										callback_url: $(consumer(regex(hostname())), producer('https://partners.com'))
								)
							}
							response {
								status 404
								headers {
									header 'Content-Type': 'application/json'
								}
								body(
										code: value(consumer("123123"), producer(optional("123123"))),
										message: "User not found by email == [${value(producer(regex(email())), consumer('not.existing@user.com'))}]"
								)
							}
						}
						""");
		String json = converter
			.convertContents("Test",
					new ContractMetadata(file, false, 0, null,
							ContractVerifierDslConverter.convertAsCollection(new File("/"), file.toFile())))
			.values()
			.iterator()
			.next();
		JSONAssert.assertEquals(
				"""
						{
						  "request" : {
							"url" : "/users/password",
							"method" : "POST",
							"bodyPatterns" : [ {
							  "matchesJsonPath" : "$[?(@.['email'] =~ /([a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\\\.[a-zA-Z]{2,6})?/)]"
							}, {
							  "matchesJsonPath" : "$[?(@.['callback_url'] =~ /((http[s]?|ftp):\\\\/)\\\\/?([^:\\\\/\\\\s]+)(:[0-9]{1,5})?/)]"
							} ],
							"headers" : {
							  "Content-Type" : {
								"equalTo" : "application/json"
							  }
							}
						  },
						  "response" : {
							"status" : 404,
							"body" : "{\\"code\\":\\"123123\\",\\"message\\":\\"User not found by email == [not.existing@user.com]\\"}",
							"headers" : {
							  "Content-Type" : "application/json"
							}
						  },
						  "priority" : 1
						}
						""",
				json, false);
		StubMapping mapping = stubMappingIsValidWireMockStub(json);
		wm.getWireMock().register(mapping);
		var response = new TestRestTemplate().exchange(RequestEntity.post(URI.create(url + "/users/password"))
			.header("Content-Type", "application/json")
			.body("{\"email\":\"abc@abc.com\", \"callback_url\":\"https://partners.com\"}"), String.class);
		assertThat(response.getHeaders().get("Content-Type")).contains("application/json");
		assertThat(response.getStatusCode().value()).isEqualTo(404);
	}

	@Test
	void should_convert_dsl_to_wiremock_with_stub_matchers(WireMockRuntimeInfo wm) throws Exception {
		String url = "http://localhost:" + wm.getHttpPort();
		DslToWireMockClientConverter converter = new DslToWireMockClientConverter();
		Path file = tmpDir.resolve("dsl_from_docs.groovy");
		Files.writeString(file, """
				sh.stubborn.contract.spec.Contract.make {
					request {
						method 'POST'
						urlPath '/get'
						body([
								duck: 123,
								alpha: "abc",
								number: 123,
								aBoolean: true,
								date: "2017-01-01",
								dateTime: "2017-01-01T01:23:45",
								time: "01:02:34",
								valueWithoutAMatcher: "foo",
								valueWithTypeMatch: "string",
								list: [
									some: [
										nested: [
											json: "with value",
											anothervalue: 4
										]
									],
									someother: [
										nested: [
											json: "with value",
											anothervalue: 4
										]
									]
								],
								valueWithMin: [
									1,2,3
								],
								valueWithMax: [
									1,2,3
								],
								valueWithMinMax: [
									1,2,3
								]
						])
						bodyMatchers {
							jsonPath('$.duck', byRegex("[0-9]{3}"))
							jsonPath('$.duck', byEquality())
							jsonPath('$.alpha', byRegex(onlyAlphaUnicode()))
							jsonPath('$.alpha', byEquality())
							jsonPath('$.number', byRegex(number()))
							jsonPath('$.aBoolean', byRegex(anyBoolean()))
							jsonPath('$.date', byDate())
							jsonPath('$.dateTime', byTimestamp())
							jsonPath('$.time', byTime())
							jsonPath('$.list.some.nested.json', byRegex(".*"))
							jsonPath('$.valueWithMin', byType {
								minOccurrence(1)
							})
							jsonPath('$.valueWithMax', byType {
								maxOccurrence(3)
							})
							jsonPath('$.valueWithMinMax', byType {
								minOccurrence(1)
								maxOccurrence(3)
							})
							jsonPath('$.valueWithOccurrence', byType {
								occurrence(4)
							})
						}
						headers {
							contentType(applicationJson())
						}
					}
					response {
						status OK()
						body([
								duck: 123,
								alpha: "abc",
								number: 123,
								aBoolean: true,
								date: "2017-01-01",
								dateTime: "2017-01-01T01:23:45",
								time: "01:02:34",
								valueWithoutAMatcher: "foo",
								valueWithTypeMatch: "string",
								valueWithMin: [1,2,3],
								valueWithMax: [1,2,3],
								valueWithMinMax: [1,2,3],
								valueWithOccurrence: [1,2,3,4],
						])
						bodyMatchers {
							jsonPath('$.duck', byRegex("[0-9]{3}"))
							jsonPath('$.duck', byEquality())
							jsonPath('$.alpha', byRegex(onlyAlphaUnicode()))
							jsonPath('$.alpha', byEquality())
							jsonPath('$.number', byRegex(number()))
							jsonPath('$.aBoolean', byRegex(anyBoolean()))
							jsonPath('$.date', byDate())
							jsonPath('$.dateTime', byTimestamp())
							jsonPath('$.time', byTime())
							jsonPath('$.valueWithTypeMatch', byType())
							jsonPath('$.valueWithMin', byType {
								minOccurrence(1)
							})
							jsonPath('$.valueWithMax', byType {
								maxOccurrence(3)
							})
							jsonPath('$.valueWithMinMax', byType {
								minOccurrence(1)
								maxOccurrence(3)
							})
							jsonPath('$.valueWithOccurrence', byType {
								occurrence(4)
							})
						}
						headers {
							contentType(applicationJson())
						}
					}
				}
				""");
		String json = converter
			.convertContents("Test",
					new ContractMetadata(file, false, 0, null,
							ContractVerifierDslConverter.convertAsCollection(new File("/"), file.toFile())))
			.values()
			.iterator()
			.next();
		StubMapping mapping = stubMappingIsValidWireMockStub(json);
		wm.getWireMock().register(mapping);
		assertThat(json).isNotNull();
	}

	@Test
	void should_convert_dsl_to_wiremock_with_stub_matchers_with_docs_example(WireMockRuntimeInfo wm) throws Exception {
		String url = "http://localhost:" + wm.getHttpPort();
		DslToWireMockClientConverter converter = new DslToWireMockClientConverter();
		Path file = tmpDir.resolve("dsl_from_docs.groovy");
		Files.writeString(file, """
				sh.stubborn.contract.spec.Contract.make {
					priority 1
					request {
						method 'POST'
						url '/users/password2'
						headers {
							header 'Content-Type': 'application/json'
						}
						body(
							email: 'abc@abc.com',
							callback_url: 'https://partners.com'
						)
						bodyMatchers {
							jsonPath('$.[\\'email\\']', byRegex(email()))
							jsonPath('$.[\\'callback_url\\']', byRegex(hostname()))
						}
					}
					response {
						status 404
						headers {
							header 'Content-Type': 'application/json'
						}
						body(
							code: "123123",
							message: "User not found by email == [not.existing@user.com]"
						)
						bodyMatchers {
							jsonPath('$.code', byRegex("123123"))
							jsonPath('$.message', byRegex("User not found by email == ${email()}"))
						}
					}
				}
				""");
		String json = converter
			.convertContents("Test",
					new ContractMetadata(file, false, 0, null,
							ContractVerifierDslConverter.convertAsCollection(new File("/"), file.toFile())))
			.values()
			.iterator()
			.next();
		JSONAssert.assertEquals(
				"""
						{
						  "request" : {
							"url" : "/users/password2",
							"method" : "POST",
							"bodyPatterns" : [ {
							  "matchesJsonPath" : "$[?(@.['email'] =~ /([a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\\\.[a-zA-Z]{2,6})/)]"
							}, {
							  "matchesJsonPath" : "$[?(@.['callback_url'] =~ /(((http[s]?|ftp):\\\\/)\\\\/?([^:\\\\/\\\\s]+)(:[0-9]{1,5})?)/)]"
							} ],
							"headers" : {
							  "Content-Type" : {
								"equalTo" : "application/json"
							  }
							}
						  },
						  "response" : {
							"status" : 404,
							"body" : "{\\"code\\":\\"123123\\",\\"message\\":\\"User not found by email == [not.existing@user.com]\\"}",
							"headers" : {
							  "Content-Type" : "application/json"
							}
						  },
						  "priority" : 1
						}
						""",
				json, false);
		StubMapping mapping = stubMappingIsValidWireMockStub(json);
		wm.getWireMock().register(mapping);
		var response = new TestRestTemplate().exchange(RequestEntity.post(URI.create(url + "/users/password2"))
			.header("Content-Type", "application/json")
			.body("{\"email\":\"abc@abc.com\", \"callback_url\":\"https://partners.com\"}"), String.class);
		assertThat(response.getHeaders().get("Content-Type")).contains("application/json");
		assertThat(response.getStatusCode().value()).isEqualTo(404);
	}

	@Test
	void should_not_escape_any_java_chars_in_the_javascript_wiremock_stub(WireMockRuntimeInfo wm) throws Exception {
		String url = "http://localhost:" + wm.getHttpPort();
		DslToWireMockClientConverter converter = new DslToWireMockClientConverter();
		Path file = tmpDir.resolve("dsl_from_docs.groovy");
		Files.writeString(file,
				"""
						sh.stubborn.contract.spec.Contract.make {
							priority 1
							request {
								method 'POST'
								url '/users/password2'
								headers {
									header 'Content-Type': 'application/json'
								}
								body(
									email: 'abc@abc.com',
									callback_url: 'https://partners.com'
								)
								bodyMatchers {
									jsonPath('$.[\\'email\\']', byRegex(email()))
									jsonPath('$.[\\'callback_url\\']', byRegex(hostname()))
								}
							}
							response {
								status 400
								headers {
									header 'CorrelationID': '11111111-1111-1111-1111-111111111111'
									header 'Content-Type': value(test(regex('application/json(;.*)?')), stub('application/json;charset=UTF-8'))
								}
								body(
										[
												subject: [
														'@type'	:'ErrorSubject',
														'oid'		:'8.2',
														'description':'Profile'
												],
												reason : [
														'@type'	:'ErrorReason',
														'oid'		:'3.7',
														'description':'Bad Request',
														'httpCode':'400'
												],
												message: '[8.2 Profile/3.7 Bad Request]'
										]
								)
							}
						}
						""");
		String json = converter
			.convertContents("Test",
					new ContractMetadata(file, false, 0, null,
							ContractVerifierDslConverter.convertAsCollection(new File("/"), file.toFile())))
			.values()
			.iterator()
			.next();
		JSONAssert.assertEquals(
				"""
						{
						  "request" : {
							"url" : "/users/password2",
							"method" : "POST",
							"headers" : {
							  "Content-Type" : {
								"equalTo" : "application/json"
							  }
							},
							"bodyPatterns" : [ {
							  "matchesJsonPath" : "$[?(@.['email'] =~ /([a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\\\.[a-zA-Z]{2,6})/)]"
							}, {
							  "matchesJsonPath" : "$[?(@.['callback_url'] =~ /(((http[s]?|ftp):\\\\/)\\\\/?([^:\\\\/\\\\s]+)(:[0-9]{1,5})?)/)]"
							} ]
						  },
						  "response" : {
							"status" : 400,
							"body" : "{\\"subject\\":{\\"@type\\":\\"ErrorSubject\\",\\"oid\\":\\"8.2\\",\\"description\\":\\"Profile\\"},\\"reason\\":{\\"@type\\":\\"ErrorReason\\",\\"oid\\":\\"3.7\\",\\"description\\":\\"Bad Request\\",\\"httpCode\\":\\"400\\"},\\"message\\":\\"[8.2 Profile/3.7 Bad Request]\\"}",
							"headers" : {
							  "CorrelationID" : "11111111-1111-1111-1111-111111111111",
							  "Content-Type" : "application/json;charset=UTF-8"
							},
							"transformers" : [ "response-template", "stubborn-contract" ]
						  },
						  "priority" : 1
						}
						""",
				json, false);
		StubMapping mapping = stubMappingIsValidWireMockStub(json);
		wm.getWireMock().register(mapping);
		var response = new TestRestTemplate().exchange(RequestEntity.post(URI.create(url + "/users/password2"))
			.header("Content-Type", "application/json")
			.body("{\"email\":\"abc@abc.com\", \"callback_url\":\"https://partners.com\"}"), String.class);
		assertThat(response.getHeaders().get("Content-Type")).contains("application/json;charset=UTF-8");
		assertThat(response.getStatusCode().value()).isEqualTo(400);
	}

	@Test
	void should_properly_convert_regex_for_headers(WireMockRuntimeInfo wm) throws Exception {
		String url = "http://localhost:" + wm.getHttpPort();
		DslToWireMockClientConverter converter = new DslToWireMockClientConverter();
		Path file = tmpDir.resolve("dsl_from_docs.groovy");
		Files.writeString(file, """
				sh.stubborn.contract.spec.Contract.make {
					request {
						method 'GET'
						urlPath($(
								consumer(regex('/v1/communities/(.+)/channels/[0-9]+')),
								producer('/v1/communities/contract/channels/1')))
						headers {
							header("X-Smartup-Test",
									$(
											consumer(regex(nonEmpty())),
											producer(1)))
						}
					}
					response {
						status 204
					}
				}
				""");
		String json = converter
			.convertContents("Test",
					new ContractMetadata(file, false, 0, null,
							ContractVerifierDslConverter.convertAsCollection(new File("/"), file.toFile())))
			.values()
			.iterator()
			.next();
		JSONAssert.assertEquals("""
				{
				"request" : {
				"urlPathPattern" : "/v1/communities/(.+)/channels/[0-9]+",
				"method" : "GET",
				"headers" : {
				  "X-Smartup-Test" : {
					"matches" : "[\\\\S\\\\s]+"
				  }
				}
				 },
				 "response" : {
				"status" : 204
				 }
				 }
				""", json, false);
		StubMapping mapping = stubMappingIsValidWireMockStub(json);
		wm.getWireMock().register(mapping);
		var response = new TestRestTemplate()
			.exchange(RequestEntity.get(URI.create(url + "/v1/communities/abc/channels/123"))
				.header("X-Smartup-Test", "asd123")
				.build(), String.class);
		assertThat(response.getStatusCode().value()).isEqualTo(204);
	}

	@Test
	void should_create_wiremock_json_with_no_duplicate_metadata_fields(WireMockRuntimeInfo wm) throws Exception {
		DslToWireMockClientConverter converter = new DslToWireMockClientConverter();
		Path file = tmpDir.resolve("stub_mapping_duplicate.groovy");
		Files.writeString(file, """
				sh.stubborn.contract.spec.Contract.make {
					request {
						method 'POST'
						url ('/ping')
					}
					response {
						status 200
					}
					metadata([wiremock: [
					stubMapping: '''  {
					    "postServeActions" :{
						    "webhook" : {
							    "url" : "/pong",
							    "method" : "PATCH"
						    }
					    }
				        }''']])
				}
				""");
		String json = converter
			.convertContents("Test",
					new ContractMetadata(file, false, 0, null,
							ContractVerifierDslConverter.convertAsCollection(new File("/"), file.toFile())))
			.values()
			.iterator()
			.next();
		JSONAssert.assertEquals("""
				{
				  "request" : {
					"url" : "/ping",
					"method" : "POST"
				  },
				  "response" : {
					"status" : 200
				  },
				  "postServeActions" : [ {
					"name" : "webhook",
					"parameters" : {
					  "url" : "/pong",
					  "method" : "PATCH"
					}
				  } ]
				}""", json, false);
	}

	@Test
	void should_convert_dsl_file_to_wiremock_json_with_array(WireMockRuntimeInfo wm) throws Exception {
		DslToWireMockClientConverter converter = new DslToWireMockClientConverter();
		Path file = tmpDir.resolve("dsl1656.groovy");
		Files.writeString(file, """
				sh.stubborn.contract.spec.Contract.make {
					request {
						method "GET"
						url "/api/foo/61923376"
						headers {
							accept 'application/json'
						}
					}
					response {
						status OK()
						headers {
							contentType(applicationJson())
						}
						body(
							\"\"\"
							[
							813146,
							814952,
							813102,
							813282
							]
							\"\"\"
						)
					}
				}
				""");
		String json = converter
			.convertContents("Test",
					new ContractMetadata(file, false, 0, null,
							ContractVerifierDslConverter.convertAsCollection(new File("/"), file.toFile())))
			.values()
			.iterator()
			.next();
		JSONAssert.assertEquals("""
				{
					"request" : {
						"url" : "/api/foo/61923376",
						"method" : "GET",
						"headers" : {
							"Accept" : {
								"matches" : "application/json.*"
							}
						}
					},
					"response" : {
						"status" : 200,
						"body" : "[813146,814952,813102,813282]",
						"headers" : {
							"Content-Type" : "application/json"
						},
						"transformers" : [ "response-template", "stubborn-contract" ]
					}
				}""", json, false);
	}

	private StubMapping stubMappingIsValidWireMockStub(String mappingDefinition) {
		StubMapping stubMapping = WireMockStubMapping.buildFrom(mappingDefinition);
		if (stubMapping.getRequest().getBodyPatterns() != null) {
			stubMapping.getRequest()
				.getBodyPatterns()
				.stream()
				.filter(p -> p instanceof RegexPattern)
				.map(p -> (RegexPattern) p)
				.forEach(p -> Pattern.compile(p.getValue()));
		}
		assertThat(mappingDefinition).doesNotContain("sh.stubborn.contract.spec.internal");
		return stubMapping;
	}

}
