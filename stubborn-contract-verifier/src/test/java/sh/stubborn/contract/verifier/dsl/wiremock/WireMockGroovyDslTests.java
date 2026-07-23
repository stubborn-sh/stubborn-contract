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
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.extension.TemplateHelperProviderExtension;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import org.junit.jupiter.api.Test;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.cloud.test.TestSocketUtils;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import sh.stubborn.contract.spec.Contract;
import sh.stubborn.contract.spec.internal.RegexPatterns;
import sh.stubborn.contract.verifier.builder.handlebars.HandlebarsEscapeHelper;
import sh.stubborn.contract.verifier.builder.handlebars.HandlebarsJsonPathHelper;
import sh.stubborn.contract.verifier.converter.YamlContractConverter;
import sh.stubborn.contract.verifier.file.ContractMetadata;
import sh.stubborn.contract.verifier.util.AssertionUtil;
import sh.stubborn.contract.verifier.util.ContractVerifierDslConverter;
import com.github.jknack.handlebars.Helper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WireMockGroovyDslTests implements WireMockStubVerifier {

	private String toWireMockClientJsonStub(Contract groovyDsl) {
		StubMapping result = new WireMockStubStrategy("Test", new ContractMetadata(null, false, 0, null, groovyDsl),
				groovyDsl)
			.toWireMockClientStub();
		return result != null ? result.toString() : null;
	}

	private WireMockConfiguration config() {
		return new WireMockConfiguration().extensions(new TemplateHelperProviderExtension() {
			@Override
			public String getName() {
				return "custom-helpers";
			}

			@Override
			public Map<String, Helper<?>> provideTemplateHelpers() {
				return Map.of(HandlebarsJsonPathHelper.NAME, new HandlebarsJsonPathHelper(),
						HandlebarsEscapeHelper.NAME, new HandlebarsEscapeHelper());
			}
		});
	}

	private ResponseEntity<String> call(int port) {
		return new TestRestTemplate()
			.exchange(RequestEntity.post(URI.create("http://localhost:" + port + "/api/v1/xxxx?foo=bar&foo=bar2"))
				.header("Authorization", "secret")
				.header("Authorization", "secret2")
				.header("Cookie", "foo=bar")
				.body("{\"foo\":\"bar\",\"baz\":5}"), String.class);
	}

	private ResponseEntity<String> callWithOptionalAndEmpty(int port) {
		return new TestRestTemplate().exchange(RequestEntity.post(URI.create("http://localhost:" + port + "/api/user"))
			.header("Content-Type", "application/json")
			.body("{\"foo\":null,\"name\":\"\"}"), String.class);
	}

	private ResponseEntity<String> callForStream(int port) {
		return new TestRestTemplate()
			.exchange(RequestEntity.get(URI.create("http://localhost:" + port + "/api/v1/entities"))
				.header("Content-Type", "application/stream+json")
				.build(), String.class);
	}

	private ResponseEntity<byte[]> callBytes(int port, File request) {
		return new TestRestTemplate().exchange(
				RequestEntity.put(URI.create("http://localhost:" + port + "/1"))
					.header("Content-Type", "application/octet-stream")
					.body(request.getAbsoluteFile().toPath().toFile().exists() ? readBytes(request) : new byte[0]),
				byte[].class);
	}

	private byte[] readBytes(File file) {
		try {
			return java.nio.file.Files.readAllBytes(file.toPath());
		}
		catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	private ResponseEntity<String> callApiCategories(int port) {
		return new TestRestTemplate()
			.exchange(
					RequestEntity.post(URI.create("http://localhost:" + port + "/api/categories"))
						.header("Content-Type", "application/json;charset=UTF-8")
						.body("[[\"Programming\",\"Java\"],[\"Programming\",\"Java\",\"Spring\",\"Boot\"]]"),
					String.class);
	}

	// TESTS BELOW

	@Test
	void should_convert_groovy_dsl_stub_to_wireMock_stub_for_the_client_side() {
		Contract groovyDsl = Contract.make((c) -> {
			c.request((r) -> {
				r.method("GET");
				r.url(r.$(r.consumer(r.regex("/[0-9]{2}")), r.producer("/12")));
			});
			c.response((r) -> {
				r.status(r.OK());
				LinkedHashMap<String, Object> body1 = new LinkedHashMap<>();
				body1.put("id", r.value(r.consumer("123"), r.producer(r.regex("[0-9]+"))));
				body1.put("surname", r.$(r.consumer("Kowalsky"), r.producer(r.regex("[a-zA-Z]+"))));
				body1.put("name", "Jan");
				body1.put("created", r.$(r.consumer("2014-02-02 12:23:43"), r.producer(r.execute("currentDate($it)"))));
				r.body(body1);
				r.headers((h) -> h.header("Content-Type", "application/json"));
			});
		});
		String wireMockStub = new WireMockStubStrategy("Test", new ContractMetadata(null, false, 0, null, groovyDsl),
				groovyDsl)
			.toWireMockClientStub()
			.toString();
		AssertionUtil.assertThatJsonsAreEqual(
				"""
						{
						  "request" : {
						    "urlPattern" : "/[0-9]{2}",
						    "method" : "GET"
						  },
						  "response" : {
						    "status" : 200,
						    "body" : "{\\"id\\":\\"123\\",\\"surname\\":\\"Kowalsky\\",\\"name\\":\\"Jan\\",\\"created\\":\\"2014-02-02 12:23:43\\"}",
						    "headers" : {
						      "Content-Type" : "application/json"
						    },
						    "transformers" : [ "response-template", "foo-transformer" ]
						  }
						}
						""",
				wireMockStub);
		stubMappingIsValidWireMockStub(wireMockStub);
	}

	@Test
	void should_convert_groovy_dsl_stub_to_wireMock_stub_for_the_client_side_with_a_body_containing_a_map() {
		Contract groovyDsl = Contract.make((c) -> {
			c.request((r) -> {
				r.method("GET");
				r.url("/ingredients");
				r.headers((h) -> h.header("Content-Type", "application/vnd.pl.devoxx.aggregatr.v1+json"));
			});
			c.response((r) -> {
				r.status(r.OK());
				LinkedHashMap<String, Object> malt = new LinkedHashMap<>();
				malt.put("quantity", 100);
				malt.put("type", "MALT");
				LinkedHashMap<String, Object> water = new LinkedHashMap<>();
				water.put("quantity", 200);
				water.put("type", "WATER");
				LinkedHashMap<String, Object> hop = new LinkedHashMap<>();
				hop.put("quantity", 300);
				hop.put("type", "HOP");
				LinkedHashMap<String, Object> yiest = new LinkedHashMap<>();
				yiest.put("quantity", 400);
				yiest.put("type", "YIEST");
				r.body(Map.of("ingredients", List.of(malt, water, hop, yiest)));
			});
		});
		String wireMockStub = new WireMockStubStrategy("Test", new ContractMetadata(null, false, 0, null, groovyDsl),
				groovyDsl)
			.toWireMockClientStub()
			.toString();
		AssertionUtil.assertThatJsonsAreEqual(
				"""
						{
						  "request" : {
						    "url" : "/ingredients",
						    "method" : "GET",
						    "headers" : {
						      "Content-Type" : {
						        "equalTo" : "application/vnd.pl.devoxx.aggregatr.v1+json"
						      }
						    }
						  },
						  "response" : {
						    "status" : 200,
						    "body" : "{\\"ingredients\\":[{\\"quantity\\":100,\\"type\\":\\"MALT\\"},{\\"quantity\\":200,\\"type\\":\\"WATER\\"},{\\"quantity\\":300,\\"type\\":\\"HOP\\"},{\\"quantity\\":400,\\"type\\":\\"YIEST\\"}]}",
						    "transformers" : [ "response-template", "foo-transformer" ]
						  }
						}
						""",
				wireMockStub);
		stubMappingIsValidWireMockStub(wireMockStub);
	}

	@Test
	void should_convert_groovy_dsl_stub_with_GString_and_regexp() {
		Contract groovyDsl = Contract.make((c) -> {
			c.request((r) -> {
				r.method("POST");
				r.url("/ws/payments");
				r.headers((h) -> h.header("Content-Type", "application/x-www-form-urlencoded"));
				r.body(r.$(r.consumer(Pattern.compile(
						"paymentType=INCOMING&transferType=BANK&amount=[0-9]{3}\\.[0-9]{2}&bookingDate=[0-9]{4}-(0[1-9]|1[0-2])-(0[1-9]|[1-2][0-9]|3[0-1])")),
						r.producer("paymentType=INCOMING&transferType=BANK&amount=500.00&bookingDate=2015-05-18")));
			});
			c.response((r) -> {
				r.status(204);
				LinkedHashMap<String, Object> responseBody = new LinkedHashMap<>();
				responseBody.put("paymentId", r.value(r.consumer("4"), r.producer(r.regex("[1-9][0-9]*"))));
				responseBody.put("foundExistingPayment", false);
				r.body(responseBody);
			});
		});
		String wireMockStub = new WireMockStubStrategy("Test", new ContractMetadata(null, false, 0, null, groovyDsl),
				groovyDsl)
			.toWireMockClientStub()
			.toString();
		AssertionUtil.assertThatJsonsAreEqual(
				"""
						{
						  "request": {
						    "method": "POST",
						    "headers": {
						      "Content-Type": {
						        "equalTo": "application/x-www-form-urlencoded"
						      }
						    },
						    "url": "/ws/payments",
						    "bodyPatterns": [
						      {
						        "matches": "paymentType=INCOMING&transferType=BANK&amount=[0-9]{3}\\\\.[0-9]{2}&bookingDate=[0-9]{4}-(0[1-9]|1[0-2])-(0[1-9]|[1-2][0-9]|3[0-1])"
						      }
						    ]
						  },
						  "response": {
						    "status": 204,
						    "body": "{\\"paymentId\\":\\"4\\",\\"foundExistingPayment\\":false}",
						    "transformers" : [ "response-template", "foo-transformer" ]
						  }
						}
						""",
				wireMockStub);
		stubMappingIsValidWireMockStub(wireMockStub);
	}

	@Test
	void should_convert_groovy_dsl_stub_with_Body_as_String_to_wireMock_stub_for_the_client_side() {
		Contract groovyDsl = Contract.make((c) -> {
			c.request((r) -> {
				r.method("GET");
				r.url(r.$(r.consumer(r.regex("/[0-9]{2}")), r.producer("/12")));
			});
			c.response((r) -> {
				r.status(r.OK());
				r.body("{\n\t\"id\": \"" + r.value(r.consumer("123"), r.producer("321")) + "\",\n\t\"surname\": \""
						+ r.value(r.consumer("Kowalsky"), r.producer(r.regex("[a-zA-Z]+")))
						+ "\",\n\t\"name\": \"Jan\",\n\t\"created\" : \""
						+ r.$(r.consumer("2014-02-02 12:23:43"), r.producer("2999-09-09 01:23:45")) + "\"\n}");
				r.headers((h) -> h.header("Content-Type", "application/json"));
			});
		});
		String wireMockStub = new WireMockStubStrategy("Test", new ContractMetadata(null, false, 0, null, groovyDsl),
				groovyDsl)
			.toWireMockClientStub()
			.toString();
		assertThat(wireMockStub).contains("\"urlPattern\" : \"/[0-9]{2}\"");
		assertThat(wireMockStub).contains("\"method\" : \"GET\"");
		assertThat(wireMockStub).contains("\"status\" : 200");
		assertThat(wireMockStub).contains("\"Content-Type\" : \"application/json\"");
		stubMappingIsValidWireMockStub(wireMockStub);
	}

	@Test
	void should_convert_groovy_dsl_stub_with_simple_Body_as_String_to_wireMock_stub_for_the_client_side() {
		Contract groovyDsl = Contract.make((c) -> {
			c.request((r) -> {
				r.method("GET");
				r.url(r.$(r.consumer(r.regex("/[0-9]{2}")), r.producer("/12")));
				r.body("\n{\n\t\"name\": \"Jan\"\n}\n");
			});
			c.response((r) -> {
				r.status(r.OK());
				r.body("\n{\n\t\"name\": \"Jan\"\n}\n");
				r.headers((h) -> h.header("Content-Type", "application/json"));
			});
		});
		String wireMockStub = new WireMockStubStrategy("Test", new ContractMetadata(null, false, 0, null, groovyDsl),
				groovyDsl)
			.toWireMockClientStub()
			.toString();
		AssertionUtil.assertThatJsonsAreEqual("""
				{
				  "request" : {
				    "urlPattern" : "/[0-9]{2}",
				    "method" : "GET",
				    "bodyPatterns" : [ {
				      "matchesJsonPath" : "$[?(@.['name'] == 'Jan')]"
				    } ]
				  },
				  "response" : {
				    "status" : 200,
				    "body" : "{\\"name\\":\\"Jan\\"}",
				    "headers" : {
				      "Content-Type" : "application/json"
				    },
				    "transformers" : [ "response-template", "foo-transformer" ]
				  }
				}
				""", wireMockStub);
		stubMappingIsValidWireMockStub(wireMockStub);
	}

	@Test
	void should_use_equalToJson_when_body_match_is_defined_as_map() {
		Contract groovyDsl = Contract.make((c) -> {
			c.request((r) -> {
				r.method("GET");
				r.url(r.$(r.consumer(r.regex("/[0-9]{2}")), r.producer("/12")));
				r.body(Map.ofEntries(Map.entry("id", r.value(r.consumer(r.regex("[0-9]+")), r.producer("123"))),
						Map.entry("surname", r.$(r.consumer(r.regex("[a-zA-Z]+")), r.producer("Kowalsky"))),
						Map.entry("name", "Jan"), Map.entry("created",
								r.$(r.consumer("2014-02-02 12:23:43"), r.producer(r.execute("currentDate($it)"))))));
			});
			c.response((r) -> r.status(r.OK()));
		});
		String wireMockStub = new WireMockStubStrategy("Test", new ContractMetadata(null, false, 0, null, groovyDsl),
				groovyDsl)
			.toWireMockClientStub()
			.toString();
		AssertionUtil.assertThatJsonsAreEqual("""
				{
				  "request" : {
				    "urlPattern" : "/[0-9]{2}",
				    "method" : "GET",
				    "bodyPatterns" : [ {
				      "matchesJsonPath" : "$[?(@.['created'] == '2014-02-02 12:23:43')]"
				    }, {
				      "matchesJsonPath" : "$[?(@.['surname'] =~ /[a-zA-Z]+/)]"
				    }, {
				      "matchesJsonPath" : "$[?(@.['name'] == 'Jan')]"
				    }, {
				      "matchesJsonPath" : "$[?(@.['id'] =~ /[0-9]+/)]"
				    } ]
				  },
				  "response" : {
				    "status" : 200,
				    "transformers" : [ "response-template", "foo-transformer" ]
				  }
				}
				""", wireMockStub);
		stubMappingIsValidWireMockStub(wireMockStub);
	}

	@Test
	void should_use_equalToJson_when_content_type_ends_with_json() {
		Contract groovyDsl = Contract.make((c) -> {
			c.request((r) -> {
				r.method("GET");
				r.url("/users");
				r.headers((h) -> h.header("Content-Type", "customtype/json"));
				r.body("\n{\n\t\"name\": \"Jan\"\n}\n");
			});
			c.response((r) -> r.status(r.OK()));
		});
		String json = toWireMockClientJsonStub(groovyDsl);
		AssertionUtil.assertThatJsonsAreEqual("""
				{
				  "request" : {
				    "url" : "/users",
				    "method" : "GET",
				    "bodyPatterns" : [ {
				      "matchesJsonPath" : "$[?(@.['name'] == 'Jan')]"
				    } ],
				    "headers" : {
				      "Content-Type" : {
				        "equalTo" : "customtype/json"
				      }
				    }
				  },
				  "response" : {
				    "status" : 200,
				    "transformers" : [ "response-template", "foo-transformer" ]
				  }
				}
				""", json);
		stubMappingIsValidWireMockStub(json);
	}

	@Test
	void should_use_xml_matchers_when_content_type_ends_with_xml() {
		Contract groovyDsl = Contract.make((c) -> {
			c.request((r) -> {
				r.method("GET");
				r.url("/users");
				r.headers((h) -> h.header("Content-Type", "customtype/xml"));
				r.body(r.$(r.consumer("<foo><name>Jozo</name><jobId>&lt;test&gt;</jobId></foo>"),
						r.producer("<foo><name>Denis</name><jobId>1234567890</jobId></foo>")));
			});
			c.response((r) -> r.status(r.OK()));
		});
		String json = toWireMockClientJsonStub(groovyDsl);
		AssertionUtil.assertThatJsonsAreEqual("""
				{
				  "request": {
				    "url": "/users",
				    "method": "GET",
				    "headers": {
				      "Content-Type": {
				        "equalTo": "customtype/xml"
				      }
				    },
				    "bodyPatterns": [
				      {
				        "matchesXPath": {
				          "expression": "/foo/name/text()",
				          "equalTo": "Jozo"
				        }
				      },
				      {
				        "matchesXPath": {
				          "expression": "/foo/jobId/text()",
				          "equalTo": "<test>"
				        }
				      }
				    ]
				  },
				  "response": {
				    "status": 200,
				    "transformers": [
				      "response-template",
				      "foo-transformer"
				    ]
				  }
				}
				""", json);
		stubMappingIsValidWireMockStub(json);
	}

	@Test
	void should_use_xml_matchers_when_content_type_is_parsable_xml() {
		Contract groovyDsl = Contract.make((c) -> {
			c.request((r) -> {
				r.method("GET");
				r.url("/users");
				r.body(r.$(r.consumer("<user><name>Jozo</name><jobId>&lt;test&gt;</jobId></user>"),
						r.producer("<user><name>Denis</name><jobId>1234567890</jobId></user>")));
			});
			c.response((r) -> r.status(r.OK()));
		});
		String json = toWireMockClientJsonStub(groovyDsl);
		AssertionUtil.assertThatJsonsAreEqual("""
				{
				  "request": {
				    "url": "/users",
				    "method": "GET",
				    "bodyPatterns": [
				      {
				        "matchesXPath": {
				          "expression": "/user/name/text()",
				          "equalTo": "Jozo"
				        }
				      },
				      {
				        "matchesXPath": {
				          "expression": "/user/jobId/text()",
				          "equalTo": "<test>"
				        }
				      }
				    ]
				  },
				  "response": {
				    "status": 200,
				    "transformers": [ "response-template", "foo-transformer" ]
				  }
				}
				""", json);
		stubMappingIsValidWireMockStub(json);
	}

	@Test
	void should_support_xml_as_a_response_body() {
		Contract groovyDsl = Contract.make((c) -> {
			c.request((r) -> {
				r.method("GET");
				r.url("/users");
			});
			c.response((r) -> {
				r.status(r.OK());
				r.body(r.$(r.consumer("<user><name>Jozo</name><jobId>&lt;test&gt;</jobId></user>"),
						r.producer("<user><name>Denis</name><jobId>1234567890</jobId></user>")));
			});
		});
		String json = toWireMockClientJsonStub(groovyDsl);
		AssertionUtil.assertThatJsonsAreEqual("""
				{
				  "request": {
				    "method": "GET",
				    "url": "/users"
				  },
				  "response": {
				    "status": 200,
				    "body":"<user><name>Jozo</name><jobId>&lt;test&gt;</jobId></user>",
				    "transformers" : [ "response-template", "foo-transformer" ]
				  }
				}
				""", json);
		stubMappingIsValidWireMockStub(json);
	}

	@Test
	void should_use_equalToJson() {
		Contract groovyDsl = Contract.make((c) -> {
			c.request((r) -> {
				r.method("GET");
				r.url("/users");
				r.body(r.equalToJson("{\"name\":\"Jan\"}"));
			});
			c.response((r) -> r.status(r.OK()));
		});
		String json = toWireMockClientJsonStub(groovyDsl);
		AssertionUtil.assertThatJsonsAreEqual("""
				{
				  "request": {
				    "method": "GET",
				    "url": "/users",
				    "bodyPatterns": [
				      {
				        "equalToJson":"{\\"name\\":\\"Jan\\"}"
				      }
				    ]
				  },
				  "response": {
				    "status": 200,
				    "transformers" : [ "response-template", "foo-transformer" ]
				  }
				}
				""", json);
		stubMappingIsValidWireMockStub(json);
	}

	@Test
	void should_use_equalToJson_and_bodyMatchers_with_json_content_type() {
		Contract groovyDsl = Contract.make((c) -> {
			c.request((r) -> {
				r.method("GET");
				r.url("/users");
				r.headers((h) -> h.header("Content-Type", "application/json"));
				r.body(r.equalToJson("{\"name\":\"Jan\"}"));
				r.bodyMatchers((bm) -> bm.jsonPath("$.name", bm.byRegex("[A-Z]{3}")));
			});
			c.response((r) -> r.status(r.OK()));
		});
		String json = toWireMockClientJsonStub(groovyDsl);
		AssertionUtil.assertThatJsonsAreEqual("""
				{
				  "request": {
				    "method": "GET",
				    "url": "/users",
				    "bodyPatterns": [
				      {
				        "equalToJson":"{\\"name\\":\\"Jan\\"}"
				      },
				      {
				        "matchesJsonPath" : "$[?(@.name =~ /([A-Z]{3})/)]"
				      }
				    ]
				  },
				  "response": {
				    "status": 200,
				    "transformers" : [ "response-template", "foo-transformer" ]
				  }
				}
				""", json);
		stubMappingIsValidWireMockStub(json);
	}

	@Test
	void should_use_equalToXml() {
		Contract groovyDsl = Contract.make((c) -> {
			c.request((r) -> {
				r.method("GET");
				r.url("/users");
				r.body(r.equalToXml("<foo><name>Jozo</name><jobId>&lt;test&gt;</jobId></foo>"));
			});
			c.response((r) -> r.status(r.OK()));
		});
		String json = toWireMockClientJsonStub(groovyDsl);
		AssertionUtil.assertThatJsonsAreEqual("""
				{
				  "request": {
				    "method": "GET",
				    "url": "/users",
				    "bodyPatterns": [
				      {
				        "equalToXml":"<foo><name>Jozo</name><jobId>&lt;test&gt;</jobId></foo>"
				      }
				    ]
				  },
				  "response": {
				    "status": 200,
				    "transformers" : [ "response-template", "foo-transformer" ]
				  }
				}
				""", json);
		stubMappingIsValidWireMockStub(json);
	}

	@Test
	void should_use_equalToXml_and_bodyMatchers_with_xml_content_type() {
		Contract groovyDsl = Contract.make((c) -> {
			c.request((r) -> {
				r.method("GET");
				r.url("/users");
				r.headers((h) -> h.header("Content-Type", "customtype/xml"));
				r.body(r.equalToXml("<foo><name>Jozo</name><jobId>1234567890</jobId></foo>"));
				r.bodyMatchers((bm) -> bm.xPath("/foo/jobId/text()", bm.byRegex("[0-9]{10}")));
			});
			c.response((r) -> r.status(r.OK()));
		});
		String json = toWireMockClientJsonStub(groovyDsl);
		AssertionUtil.assertThatJsonsAreEqual("""
				{
				  "request": {
				    "method": "GET",
				    "url": "/users",
				    "bodyPatterns" : [ {
				        "equalToXml" : "<foo><name>Jozo</name><jobId>1234567890</jobId></foo>"
				      }, {
				        "matchesXPath" : {
				          "expression" : "/foo/jobId/text()",
				          "matches" : "[0-9]{10}"
				        }
				      } ]
				  },
				  "response": {
				    "status": 200,
				    "transformers" : [ "response-template", "foo-transformer" ]
				  }
				}
				""", json);
		stubMappingIsValidWireMockStub(json);
	}

	@Test
	void should_create_stub_with_body_from_the_file() {
		Contract groovyDsl = Contract.make((c) -> {
			c.request((r) -> {
				r.method("GET");
				r.url("/users");
				r.headers((h) -> h.header("Content-Type", "application/xml"));
				r.body(r.file("classpath/request.xml"));
			});
			c.response((r) -> r.status(r.OK()));
		});
		String json = toWireMockClientJsonStub(groovyDsl);
		AssertionUtil.assertThatJsonsAreEqual("""
				{
				  "request": {
				    "method": "GET",
				    "url": "/users",
				    "bodyPatterns": [
				        {
				          "matchesXPath": {
				            "expression": "/foo/name/text()",
				            "equalTo": "Jozo"
				          }
				        },
				        {
				          "matchesXPath": {
				            "expression": "/foo/jobId/text()",
				            "equalTo": "123"
				          }
				        }
				      ]
				  },
				  "response": {
				    "status": 200,
				    "transformers" : [ "response-template", "foo-transformer" ]
				  }
				}
				""", json);
		stubMappingIsValidWireMockStub(json);
	}

	@Test
	void should_convert_groovy_dsl_stub_with_regexp_Body_as_String_to_wireMock_stub_for_the_client_side() {
		Contract groovyDsl = Contract.make((c) -> {
			c.request((r) -> {
				r.method("GET");
				r.url(r.$(r.consumer(r.regex("/[0-9]{2}")), r.producer("/12")));
				r.body(Map.of("personalId", r.value(r.consumer(r.regex("^[0-9]{11}$")), r.producer("57593728525"))));
			});
			c.response((r) -> {
				r.status(r.OK());
				r.body("\n{\n\t\"name\": \"Jan\"\n}\n");
				r.headers((h) -> h.header("Content-Type", "application/json"));
			});
		});
		String wireMockStub = new WireMockStubStrategy("Test", new ContractMetadata(null, false, 0, null, groovyDsl),
				groovyDsl)
			.toWireMockClientStub()
			.toString();
		AssertionUtil.assertThatJsonsAreEqual("""
				{
				  "request" : {
				    "urlPattern" : "/[0-9]{2}",
				    "method" : "GET",
				    "bodyPatterns" : [ {
				      "matchesJsonPath" : "$[?(@.['personalId'] =~ /^[0-9]{11}$/)]"
				    } ]
				  },
				  "response" : {
				    "status" : 200,
				    "body" : "{\\"name\\":\\"Jan\\"}",
				    "headers" : {
				      "Content-Type" : "application/json"
				    },
				    "transformers" : [ "response-template", "foo-transformer" ]
				  }
				}
				""", wireMockStub);
		stubMappingIsValidWireMockStub(wireMockStub);
	}

	@Test
	void should_convert_groovy_dsl_stub_with_a_regexp_and_an_integer_in_request_body() {
		Contract groovyDsl = Contract.make((c) -> {
			c.request((r) -> {
				r.method("PUT");
				r.url("/fraudcheck");
				LinkedHashMap<String, Object> reqBody = new LinkedHashMap<>();
				reqBody.put("clientPesel", r.value(r.consumer(r.regex("[0-9]{10}")), r.producer("1234567890")));
				reqBody.put("loanAmount", 123.123);
				r.body(reqBody);
				r.headers((h) -> h.contentType("application/vnd.fraud.v1+json"));
			});
			c.response((r) -> {
				r.status(r.OK());
				LinkedHashMap<String, Object> respBody = new LinkedHashMap<>();
				respBody.put("fraudCheckStatus", "OK");
				respBody.put("rejectionReason",
						r.$(r.consumer((Object) null), r.producer(r.execute("assertThatRejectionReasonIsNull($it)"))));
				r.body(respBody);
				r.headers((h) -> h.header("Content-Type", "application/vnd.fraud.v1+json"));
			});
		});
		String wireMockStub = new WireMockStubStrategy("Test", new ContractMetadata(null, false, 0, null, groovyDsl),
				groovyDsl)
			.toWireMockClientStub()
			.toString();
		AssertionUtil.assertThatJsonsAreEqual("""
				{
				  "request" : {
				    "url" : "/fraudcheck",
				    "method" : "PUT",
				    "bodyPatterns" : [ {
				      "matchesJsonPath" : "$[?(@.['clientPesel'] =~ /[0-9]{10}/)]"
				    }, {
				      "matchesJsonPath" : "$[?(@.['loanAmount'] == 123.123)]"
				    } ],
				    "headers" : {
				      "Content-Type" : {
				        "matches" : "application/vnd\\\\.fraud\\\\.v1\\\\+json.*"
				      }
				    }
				  },
				  "response" : {
				    "status" : 200,
				    "body" : "{\\"fraudCheckStatus\\":\\"OK\\",\\"rejectionReason\\":null}",
				    "headers" : {
				      "Content-Type" : "application/vnd.fraud.v1+json"
				    },
				    "transformers" : [ "response-template", "foo-transformer" ]
				  }
				}
				""", wireMockStub);
		stubMappingIsValidWireMockStub(wireMockStub);
	}

	@Test
	void should_generate_request_with_urlPath_and_queryParameters_for_client_side() {
		Contract groovyDsl = Contract.make((c) -> {
			c.request((r) -> {
				r.method("GET");
				r.urlPath(r.$(r.consumer("users"), r.producer("items")), (u) -> {
					u.queryParameters((q) -> {
						q.parameter("limit", r.$(r.consumer(r.equalTo("20")), r.producer("10")));
						q.parameter("offset", r.$(r.consumer(r.containing("10")), r.producer("10")));
						q.parameter("filter", "email");
						q.parameter("sort", r.$(r.consumer(r.regex("^[0-9]{10}$")), r.producer("1234567890")));
						q.parameter("search", r.$(r.consumer(r.notMatching("^/[0-9]{2}$")), r.producer("10")));
						q.parameter("age", r.$(r.consumer(r.notMatching("^\\w*$")), r.producer(10)));
						q.parameter("name", r.$(r.consumer(r.matching("Denis.*")), r.producer("Denis")));
						q.parameter("credit", r.absent());
					});
				});
			});
			c.response((r) -> r.status(r.OK()));
		});
		String json = toWireMockClientJsonStub(groovyDsl);
		AssertionUtil.assertThatJsonsAreEqual("""
				{
				  "request": {
				    "method": "GET",
				    "urlPath": "users",
				    "queryParameters": {
				      "offset": {
				        "contains": "10"
				      },
				      "limit": {
				        "equalTo": "20"
				      },
				      "filter": {
				        "equalTo": "email"
				      },
				      "sort": {
				        "matches": "^[0-9]{10}$"
				      },
				      "search": {
				        "doesNotMatch": "^/[0-9]{2}$"
				      },
				      "age": {
				        "doesNotMatch": "^\\\\w*$"
				      },
				      "name": {
				        "matches": "Denis.*"
				      },
				      "credit": {
				        "absent": true
				      }
				    }
				  },
				  "response": {
				    "status": 200,
				    "transformers" : [ "response-template", "foo-transformer" ]
				  }
				}
				""", json);
		stubMappingIsValidWireMockStub(json);
	}

	@Test
	void should_generate_request_with_absent_header_for_client_side() {
		Contract groovyDsl = Contract.make((c) -> {
			c.request((r) -> {
				r.method("GET");
				r.urlPath("/some/path/*");
				r.headers((h) -> h.header("Authentication", r.absent()));
			});
			c.response((r) -> r.status(r.OK()));
		});
		String json = toWireMockClientJsonStub(groovyDsl);
		AssertionUtil.assertThatJsonsAreEqual("""
				{
				  "request" : {
				    "urlPath" : "/some/path/*",
				    "method" : "GET",
				    "headers" : {
				      "Authentication" : {
				        "absent" : true
				      }
				    }
				  },
				  "response" : {
				    "status" : 200,
				    "transformers" : [ "response-template", "foo-transformer" ]
				  }
				}
				""", json);
		stubMappingIsValidWireMockStub(json);
	}

	@Test
	void should_generate_request_with_urlPathPattern_and_queryParameters_for_client_side_when_both_contains_regular_expressions() {
		Contract groovyDsl = Contract.make((c) -> {
			c.request((r) -> {
				r.method("GET");
				r.urlPath(r.$(r.consumer(r.regex("/users/[0-9]+")), r.producer("/users/1")), (u) -> {
					u.queryParameters((q) -> q.parameter("search",
							r.$(r.consumer(r.notMatching("^/[0-9]{2}$")), r.producer("10"))));
				});
			});
			c.response((r) -> r.status(r.OK()));
		});
		String json = toWireMockClientJsonStub(groovyDsl);
		AssertionUtil.assertThatJsonsAreEqual("""
				{
				  "request": {
				    "method": "GET",
				    "urlPathPattern": "/users/[0-9]+",
				    "queryParameters": {
				      "search": {
				        "doesNotMatch": "^/[0-9]{2}$"
				      }
				    }
				  },
				  "response": {
				    "status": 200,
				    "transformers" : [ "response-template", "foo-transformer" ]
				  }
				}
				""", json);
		stubMappingIsValidWireMockStub(json);
	}

	@Test
	void should_generate_request_with_urlPath_for_client_side() {
		Contract groovyDsl = Contract.make((c) -> {
			c.request((r) -> {
				r.method("GET");
				r.urlPath(r.$(r.consumer("boxes"), r.producer("items")));
			});
			c.response((r) -> r.status(r.OK()));
		});
		String json = toWireMockClientJsonStub(groovyDsl);
		AssertionUtil.assertThatJsonsAreEqual("""
				{
				  "request": {
				    "method": "GET",
				    "urlPath": "boxes"
				  },
				  "response": {
				    "status": 200,
				    "transformers" : [ "response-template", "foo-transformer" ]
				  }
				}
				""", json);
		stubMappingIsValidWireMockStub(json);
	}

	@Test
	void should_generate_simple_request_with_urlPath_for_client_side() {
		Contract groovyDsl = Contract.make((c) -> {
			c.request((r) -> {
				r.method("GET");
				r.urlPath("boxes");
			});
			c.response((r) -> r.status(r.OK()));
		});
		String json = toWireMockClientJsonStub(groovyDsl);
		AssertionUtil.assertThatJsonsAreEqual("""
				{
				  "request": {
				    "method": "GET",
				    "urlPath": "boxes"
				  },
				  "response": {
				    "status": 200,
				    "transformers" : [ "response-template", "foo-transformer" ]
				  }
				}
				""", json);
		stubMappingIsValidWireMockStub(json);
	}

	@Test
	void should_not_allow_not_matching_query_param_for_server_value() {
		assertThatThrownBy(() -> Contract.make((c) -> {
			c.request((r) -> {
				r.method("GET");
				r.url(r.regex("users/[0-9]*"), (u) -> {
					u.queryParameters((q) -> {
						q.parameter("age", r.notMatching("^\\w*$"));
						q.parameter("name", r.matching("Denis.*"));
					});
				});
			});
			c.response((r) -> r.status(r.OK()));
		})).isInstanceOf(IllegalStateException.class)
			.hasMessageContaining(
					"Query parameter 'age' can't be of a matching type: NOT_MATCHING for the server side");
	}

	@Test
	void should_not_allow_regexp_in_query_parameter_for_server_value() {
		assertThatThrownBy(() -> Contract.make((c) -> {
			c.request((r) -> {
				r.method("GET");
				r.url("abc", (u) -> {
					u.queryParameters((q) -> q.parameter("age",
							r.$(r.consumer(r.notMatching("^\\w*$")), r.producer(r.regex(".*")))));
				});
			});
			c.response((r) -> r.status(r.OK()));
		})).isInstanceOf(IllegalStateException.class);
	}

	@Test
	void should_not_allow_query_parameter_unresolvable_for_a_server_value() {
		assertThatThrownBy(() -> Contract.make((c) -> {
			c.request((r) -> {
				r.method("GET");
				r.urlPath("users", (u) -> {
					u.queryParameters((q) -> {
						q.parameter("age", r.notMatching("^\\w*$"));
						q.parameter("name", r.matching("Denis.*"));
					});
				});
			});
			c.response((r) -> r.status(r.OK()));
		})).isInstanceOf(IllegalStateException.class)
			.hasMessageContaining(
					"Query parameter 'age' can't be of a matching type: NOT_MATCHING for the server side");
	}

	@Test
	void should_generate_request_with_url_and_queryParameters_for_client_side() {
		Contract groovyDsl = Contract.make((c) -> {
			c.request((r) -> {
				r.method("GET");
				r.url(r.$(r.consumer(r.regex("users/[0-9]*")), r.producer("users/123")), (u) -> {
					u.queryParameters((q) -> {
						q.parameter("age", r.$(r.consumer(r.notMatching("^\\w*$")), r.producer(10)));
						q.parameter("name", r.$(r.consumer(r.matching("Denis.*")), r.producer("Denis")));
					});
				});
			});
			c.response((r) -> r.status(r.OK()));
		});
		String json = toWireMockClientJsonStub(groovyDsl);
		AssertionUtil.assertThatJsonsAreEqual("""
				{
				  "request": {
				    "method": "GET",
				    "urlPathPattern": "users/[0-9]*",
				    "queryParameters": {
				      "age": {
				        "doesNotMatch": "^\\\\w*$"
				      },
				      "name": {
				        "matches": "Denis.*"
				      }
				    }
				  },
				  "response": {
				    "status": 200,
				    "transformers" : [ "response-template", "foo-transformer" ]
				  }
				}
				""", json);
		stubMappingIsValidWireMockStub(json);
	}

	@Test
	void should_convert_groovy_dsl_stub_with_rich_tree_Body_as_String_to_wireMock_stub_for_the_client_side() {
		Contract groovyDsl = Contract.make((c) -> {
			c.request((r) -> {
				r.method("GET");
				r.url(r.$(r.consumer(r.regex("/[0-9]{2}")), r.producer("/12")));
				r.body(Map.ofEntries(
						Map.entry("personalId", r.value(r.consumer(r.regex("[0-9]{11}")), r.producer("57593728525"))),
						Map.entry("firstName", r.value(r.consumer(r.regex(".*")), r.producer("Bruce"))),
						Map.entry("lastName", r.value(r.consumer(r.regex(".*")), r.producer("Lee"))),
						Map.entry("birthDate",
								r.value(r.consumer(r.regex("[0-9]{4}-[0-9]{2}-[0-9]{2}")), r.producer("1985-12-12"))),
						Map.entry("errors", List.of(
								Map.of("propertyName", r.value(r.consumer(r.regex("[0-9]{2}")), r.producer("04")),
										"providerValue", "Test"),
								Map.of("propertyName", r.value(r.consumer(r.regex("[0-9]{2}")), r.producer("08")),
										"providerValue", "Test")))));
			});
			c.response((r) -> {
				r.status(r.OK());
				r.body("\n{\n\t\"name\": \"Jan\"\n}\n");
				r.headers((h) -> h.header("Content-Type", "application/json"));
			});
		});
		String wireMockStub = new WireMockStubStrategy("Test", new ContractMetadata(null, false, 0, null, groovyDsl),
				groovyDsl)
			.toWireMockClientStub()
			.toString();
		AssertionUtil.assertThatJsonsAreEqual("""
				{
				  "request" : {
				    "urlPattern" : "/[0-9]{2}",
				    "method" : "GET",
				    "bodyPatterns" : [ {
				      "matchesJsonPath" : "$.['errors'][*][?(@.['propertyName'] =~ /[0-9]{2}/)]"
				    }, {
				      "matchesJsonPath" : "$.['errors'][*][?(@.['providerValue'] == 'Test')]"
				    }, {
				      "matchesJsonPath" : "$[?(@.['lastName'] =~ /.*/)]"
				    }, {
				      "matchesJsonPath" : "$[?(@.['firstName'] =~ /.*/)]"
				    }, {
				      "matchesJsonPath" : "$[?(@.['birthDate'] =~ /[0-9]{4}-[0-9]{2}-[0-9]{2}/)]"
				    }, {
				      "matchesJsonPath" : "$[?(@.['personalId'] =~ /[0-9]{11}/)]"
				    }]
				  },
				  "response" : {
				    "status" : 200,
				    "body" : "{\\"name\\":\\"Jan\\"}",
				    "headers" : {
				      "Content-Type" : "application/json"
				    },
				    "transformers" : [ "response-template", "foo-transformer" ]
				  }
				}
				""", wireMockStub);
	}

	@Test
	void should_use_regexp_matches_when_request_body_match_is_defined_using_a_map_with_a_pattern() {
		Contract groovyDsl = Contract.make((c) -> {
			c.request((r) -> {
				r.method("POST");
				r.url("/reissue-payment-order");
				r.body(Map.ofEntries(Map.entry("loanNumber", "999997001"),
						Map.entry("amount", r.value(r.consumer(r.regex("[0-9.]+")), r.producer("100.00"))),
						Map.entry("currency", "DKK"),
						Map.entry("applicationName", r.value(r.consumer(r.regex(".*")), r.producer("Auto-Repayments"))),
						Map.entry("username", r.value(r.consumer(r.regex(".*")), r.producer("scheduler"))),
						Map.entry("cardId", 1)));
			});
			c.response((r) -> {
				r.status(r.OK());
				r.body("\n{\n\"status\": \"OK\"\n}\n");
				r.headers((h) -> h.header("Content-Type", "application/json"));
			});
		});
		String json = toWireMockClientJsonStub(groovyDsl);
		AssertionUtil.assertThatJsonsAreEqual("""
				{
				  "request" : {
				    "url" : "/reissue-payment-order",
				    "method" : "POST",
				    "bodyPatterns" : [ {
				      "matchesJsonPath" : "$[?(@.['loanNumber'] == '999997001')]"
				    }, {
				      "matchesJsonPath" : "$[?(@.['username'] =~ /.*/)]"
				    }, {
				      "matchesJsonPath" : "$[?(@.['amount'] =~ /[0-9.]+/)]"
				    }, {
				      "matchesJsonPath" : "$[?(@.['cardId'] == 1)]"
				    }, {
				      "matchesJsonPath" : "$[?(@.['currency'] == 'DKK')]"
				    }, {
				      "matchesJsonPath" : "$[?(@.['applicationName'] =~ /.*/)]"
				    } ]
				  },
				  "response" : {
				    "status" : 200,
				    "body" : "{\\"status\\":\\"OK\\"}",
				    "headers" : {
				      "Content-Type" : "application/json"
				    },
				    "transformers" : [ "response-template", "foo-transformer" ]
				  }
				}
				""", json);
	}

	@Test
	void should_generate_stub_for_empty_body() {
		Contract groovyDsl = Contract.make((c) -> {
			c.request((r) -> {
				r.method("POST");
				r.url("test");
				r.body("");
			});
			c.response((r) -> r.status(406));
		});
		String json = toWireMockClientJsonStub(groovyDsl);
		AssertionUtil.assertThatJsonsAreEqual("""
				{
				  "request": {
				    "method": "POST",
				    "url": "test",
				    "bodyPatterns": [
				      {
				        "equalTo": ""
				      }
				    ]
				  },
				  "response": {
				    "status": 406,
				    "transformers" : [ "response-template", "foo-transformer" ]
				  }
				}
				""", json);
	}

	@Test
	void should_generate_stub_with_priority() {
		Contract groovyDsl = Contract.make((c) -> {
			c.priority(9);
			c.request((r) -> {
				r.method("POST");
				r.url("test");
			});
			c.response((r) -> r.status(406));
		});
		String json = toWireMockClientJsonStub(groovyDsl);
		AssertionUtil.assertThatJsonsAreEqual("""
				{
				  "priority": 9,
				  "request": {
				    "method": "POST",
				    "url": "test"
				  },
				  "response": {
				    "status": 406,
				    "transformers" : [ "response-template", "foo-transformer" ]
				  }
				}
				""", json);
	}

	@Test
	void should_use_test_as_an_alias_for_server() {
		Contract groovyDsl = Contract.make((c) -> {
			c.request((r) -> {
				r.method("POST");
				r.url("foo");
				r.body(Map.of("property", r.value(r.consumer("value"), r.producer("value"))));
			});
			c.response((r) -> r.status(r.OK()));
		});
		String wireMockStub = new WireMockStubStrategy("Test", new ContractMetadata(null, false, 0, null, groovyDsl),
				groovyDsl)
			.toWireMockClientStub()
			.toString();
		AssertionUtil.assertThatJsonsAreEqual("""
				{
				  "request" : {
				    "method" : "POST",
				    "bodyPatterns" : [ {
				      "matchesJsonPath" : "$[?(@.['property'] == 'value')]"
				    } ]
				  },
				  "response" : {
				    "status" : 200,
				    "transformers" : [ "response-template", "foo-transformer" ]
				  }
				}
				""", wireMockStub);
		stubMappingIsValidWireMockStub(wireMockStub);
	}

	@Test
	void should_generate_stub_with_empty_list_as_a_value_of_a_field() {
		Contract groovyDsl = Contract.make((c) -> {
			c.request((r) -> {
				r.method("POST");
				r.url("foo");
				r.body(Map.of("values", List.of()));
			});
			c.response((r) -> r.status(r.OK()));
		});
		String wireMockStub = new WireMockStubStrategy("Test", new ContractMetadata(null, false, 0, null, groovyDsl),
				groovyDsl)
			.toWireMockClientStub()
			.toString();
		AssertionUtil.assertThatJsonsAreEqual("""
				{
				  "request": {
				    "method": "POST",
				    "bodyPatterns": [
				      {
				        "equalToJson": "{\\"values\\":[]}"
				      }
				    ]
				  },
				  "response": {
				    "status": 200,
				    "transformers" : [ "response-template", "foo-transformer" ]
				  }
				}
				""", wireMockStub);
		stubMappingIsValidWireMockStub(wireMockStub);
	}

	@Test
	void should_generate_stub_properly_resolving_GString_with_regular_expression() {
		Contract groovyDsl = Contract.make((c) -> {
			c.priority(1);
			c.request((r) -> {
				r.method("POST");
				r.url("/users/password");
				r.headers((h) -> h.header("Content-Type", "application/json"));
				r.body(Map.of("email",
						r.$(r.consumer(r.regex(RegexPatterns.email())), r.producer("not.existing@user.com")),
						"callback_url",
						r.$(r.consumer(r.regex(RegexPatterns.hostname())), r.producer("https://partners.com"))));
			});
			c.response((r) -> {
				r.status(404);
				r.headers((h) -> h.header("Content-Type", "application/json"));
				LinkedHashMap<String, Object> responseBody1 = new LinkedHashMap<>();
				responseBody1.put("code", 4);
				responseBody1.put("message", "User not found by email = [not.existing@user.com]");
				r.body(responseBody1);
			});
		});
		String wireMockStub = new WireMockStubStrategy("Test", new ContractMetadata(null, false, 0, null, groovyDsl),
				groovyDsl)
			.toWireMockClientStub()
			.toString();
		AssertionUtil.assertThatJsonsAreEqual(
				"""
						{
						  "request" : {
						    "url" : "/users/password",
						    "method" : "POST",
						    "bodyPatterns" : [ {
						      "matchesJsonPath" : "$[?(@.['callback_url'] =~ /((http[s]?|ftp):\\\\/)\\\\/?([^:\\\\/\\\\s]+)(:[0-9]{1,5})?/)]"
						    }, {
						      "matchesJsonPath" : "$[?(@.['email'] =~ /[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\\\.[a-zA-Z]{2,6}/)]"
						    } ],
						    "headers" : {
						      "Content-Type" : {
						        "equalTo" : "application/json"
						      }
						    }
						  },
						  "response" : {
						    "status" : 404,
						    "body" : "{\\"code\\":4,\\"message\\":\\"User not found by email = [not.existing@user.com]\\"}",
						    "headers" : {
						      "Content-Type" : "application/json"
						    },
						    "transformers" : [ "response-template", "foo-transformer" ]
						  },
						  "priority" : 1
						}
						""",
				wireMockStub);
		stubMappingIsValidWireMockStub(wireMockStub);
	}

	@Test
	void should_generate_stub_properly_resolving_GString_with_regular_expression_in_url() {
		Contract groovyDsl = Contract.make((c) -> {
			c.request((r) -> {
				r.method("PUT");
				r.url(r.$(r.consumer(r.regex("/partners/[0-9]*/agents/11/customers/09665703Z")),
						r.producer("/partners/11/agents/11/customers/09665703Z")));
				r.headers((h) -> h.header("Content-Type", "application/json"));
				r.body(Map.of("first_name", "Josef"));
			});
			c.response((r) -> r.status(422));
		});
		String wireMockStub = new WireMockStubStrategy("Test", new ContractMetadata(null, false, 0, null, groovyDsl),
				groovyDsl)
			.toWireMockClientStub()
			.toString();
		AssertionUtil.assertThatJsonsAreEqual("""
				{
				  "request" : {
				    "urlPattern" : "/partners/[0-9]*/agents/11/customers/09665703Z",
				    "method" : "PUT",
				    "bodyPatterns" : [ {
				      "matchesJsonPath" : "$[?(@.['first_name'] == 'Josef')]"
				    } ],
				    "headers" : {
				      "Content-Type" : {
				        "equalTo" : "application/json"
				      }
				    }
				  },
				  "response" : {
				    "status" : 422,
				    "transformers" : [ "response-template", "foo-transformer" ]
				  }
				}
				""", wireMockStub);
		stubMappingIsValidWireMockStub(wireMockStub);
	}

	@Test
	void should_generate_stub_without_optional_parameters() {
		// Two contract variants – test both
		Contract contractDsl1 = Contract.make((c) -> {
			c.priority(1);
			c.request((r) -> {
				r.method("POST");
				r.url("/users/password");
				r.headers((h) -> h.header("Content-Type", "application/json"));
				r.body(Map.of("email",
						r.$(r.consumer(r.optional(r.regex(RegexPatterns.email()))), r.producer("abc@abc.com")),
						"callback_url",
						r.$(r.consumer(r.regex(RegexPatterns.hostname())), r.producer("https://partners.com"))));
			});
			c.response((r) -> {
				r.status(404);
				r.headers((h) -> h.header("Content-Type", "application/json"));
				LinkedHashMap<String, Object> optRespBody = new LinkedHashMap<>();
				optRespBody.put("code", r.$(r.consumer("123123"), r.producer(r.optional("123123"))));
				optRespBody.put("message", "User not found by email = [not.existing@user.com]");
				r.body(optRespBody);
			});
		});
		String wireMockStub = new WireMockStubStrategy("Test", new ContractMetadata(null, false, 0, null, contractDsl1),
				contractDsl1)
			.toWireMockClientStub()
			.toString();
		AssertionUtil.assertThatJsonsAreEqual(
				"""
						{
						  "request" : {
						    "url" : "/users/password",
						    "method" : "POST",
						    "bodyPatterns" : [ {
						      "matchesJsonPath" : "$[?(@.['callback_url'] =~ /((http[s]?|ftp):\\\\/)\\\\/?([^:\\\\/\\\\s]+)(:[0-9]{1,5})?/)]"
						    }, {
						      "matchesJsonPath" : "$[?(@.['email'] =~ /([a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\\\.[a-zA-Z]{2,6})?/)]"
						    } ],
						    "headers" : {
						      "Content-Type" : {
						        "equalTo" : "application/json"
						      }
						    }
						  },
						  "response" : {
						    "status" : 404,
						    "body" : "{\\"code\\":\\"123123\\",\\"message\\":\\"User not found by email = [not.existing@user.com]\\"}",
						    "headers" : {
						      "Content-Type" : "application/json"
						    },
						    "transformers" : [ "response-template", "foo-transformer" ]
						  },
						  "priority" : 1
						}
						""",
				wireMockStub);
		stubMappingIsValidWireMockStub(wireMockStub);
	}

	@Test
	void should_generate_stub_with_multipart_parameters() {
		Contract contractDsl = Contract.make((c) -> {
			c.request((r) -> {
				r.method("PUT");
				r.url("/multipart");
				r.headers((h) -> h.contentType("multipart/form-data;boundary=AaB03x"));
				r.multipart(Map.ofEntries(
						Map.entry("formParameter", r.$(r.c(r.regex("\".+\"")), r.p("\"formParameterValue\""))),
						Map.entry("someBooleanParameter", r.$(r.c(r.regex(RegexPatterns.anyBoolean())), r.p("true"))),
						Map.entry("file",
								r.named(Map.of("name", r.$(r.c(r.regex(RegexPatterns.nonEmpty())), r.p("filename.csv")),
										"content",
										r.$(r.c(r.regex(RegexPatterns.nonEmpty())), r.p("file content")))))));
			});
			c.response((r) -> r.status(r.OK()));
		});
		String wireMockStub = new WireMockStubStrategy("Test", new ContractMetadata(null, false, 0, null, contractDsl),
				contractDsl)
			.toWireMockClientStub()
			.toString();
		AssertionUtil.assertThatJsonsAreEqual(
				"""
						{
						  "request" : {
						    "url" : "/multipart",
						    "method" : "PUT",
						    "headers" : {
						      "Content-Type" : {
						        "matches" : "multipart/form-data;boundary=AaB03x.*"
						      }
						    },
						    "bodyPatterns" : [ {
						        "matches" : ".*--(.*)\\r?\\nContent-Disposition: form-data; name=\\"formParameter\\"\\r?\\n(Content-Type: .*\\r?\\n)?(Content-Transfer-Encoding: .*\\r?\\n)?(Content-Length: \\\\d+\\r?\\n)?\\r?\\n\\".+\\"\\r?\\n--.*"
						    }, {
						        "matches" : ".*--(.*)\\r?\\nContent-Disposition: form-data; name=\\"someBooleanParameter\\"\\r?\\n(Content-Type: .*\\r?\\n)?(Content-Transfer-Encoding: .*\\r?\\n)?(Content-Length: \\\\d+\\r?\\n)?\\r?\\n(true|false)\\r?\\n--.*"
						    }, {
						      "matches" : ".*--(.*)\\r?\\nContent-Disposition: form-data; name=\\"file\\"; filename=\\"[\\\\S\\\\s]+\\"\\r?\\n(Content-Type: .*\\r?\\n)?(Content-Transfer-Encoding: .*\\r?\\n)?(Content-Length: \\\\d+\\r?\\n)?\\r?\\n[\\\\S\\\\s]+\\r?\\n--.*"
						    } ]
						  },
						  "response" : {
						    "status" : 200,
						    "transformers" : [ "response-template", "foo-transformer" ]
						  }
						}
						""",
				wireMockStub);
		stubMappingIsValidWireMockStub(wireMockStub);
	}

	@Test
	void should_generate_request_with_an_optional_queryParameter_for_client_side() {
		Contract groovyDsl = Contract.make((c) -> {
			c.request((r) -> {
				r.method("GET");
				r.urlPath("/some/api", (u) -> {
					u.queryParameters((q) -> {
						q.parameter("size", r.value(r.consumer(r.regex("[0-9]+")), r.producer(1)));
						q.parameter("page", r.value(r.consumer(r.regex("[0-9]+")), r.producer(0)));
						q.parameter("sort", r.value(r.consumer(r.optional(r.regex("^[a-z]+$"))), r.producer("id")));
					});
				});
			});
			c.response((r) -> {
				r.status(r.OK());
				LinkedHashMap<String, Object> contentItem = new LinkedHashMap<>();
				contentItem.put("id", "00000000-0000-0000-0000-000000000000");
				contentItem.put("type", "Extraordinary");
				contentItem.put("state", "ACTIVE");
				LinkedHashMap<String, Object> sortItem = new LinkedHashMap<>();
				sortItem.put("direction", "ASC");
				sortItem.put("property", "id");
				sortItem.put("ignoreCase", false);
				sortItem.put("nullHandling", "NATIVE");
				sortItem.put("ascending", true);
				LinkedHashMap<String, Object> optionalQueryBody = new LinkedHashMap<>();
				optionalQueryBody.put("content", List.of(contentItem));
				optionalQueryBody.put("totalPages", 1);
				optionalQueryBody.put("totalElements", 1);
				optionalQueryBody.put("last", true);
				optionalQueryBody.put("sort", List.of(sortItem));
				optionalQueryBody.put("first", true);
				optionalQueryBody.put("numberOfElements", 1);
				optionalQueryBody.put("size", 1);
				optionalQueryBody.put("number", 0);
				r.body(optionalQueryBody);
			});
		});
		String json = toWireMockClientJsonStub(groovyDsl);
		AssertionUtil.assertThatJsonsAreEqual(
				"""
						{
						  "request" : {
						    "urlPath" : "/some/api",
						    "method" : "GET",
						    "queryParameters" : {
						      "size" : {
						        "matches" : "[0-9]+"
						      },
						      "page" : {
						        "matches" : "[0-9]+"
						      },
						      "sort" : {
						        "matches" : "(^[a-z]+$)?"
						      }
						    }
						  },
						  "response" : {
						    "status" : 200,
						    "body" : "{\\"content\\":[{\\"id\\":\\"00000000-0000-0000-0000-000000000000\\",\\"type\\":\\"Extraordinary\\",\\"state\\":\\"ACTIVE\\"}],\\"totalPages\\":1,\\"totalElements\\":1,\\"last\\":true,\\"sort\\":[{\\"direction\\":\\"ASC\\",\\"property\\":\\"id\\",\\"ignoreCase\\":false,\\"nullHandling\\":\\"NATIVE\\",\\"ascending\\":true}],\\"first\\":true,\\"numberOfElements\\":1,\\"size\\":1,\\"number\\":0}",
						    "transformers" : [ "response-template", "foo-transformer" ]
						  }
						}
						""",
				json);
		stubMappingIsValidWireMockStub(json);
	}

	@Test
	void should_not_create_a_stub_for_a_skipped_contract() {
		Contract groovyDsl = Contract.make((c) -> {
			c.ignored();
			c.request((r) -> {
				r.method("GET");
				r.urlPath("/some/api", (u) -> u.queryParameters(
						(q) -> q.parameter("size", r.value(r.consumer(r.regex("[0-9]+")), r.producer(1)))));
			});
			c.response((r) -> {
				r.status(r.OK());
				r.body("");
			});
		});
		String json = toWireMockClientJsonStub(groovyDsl);
		assertThat(json).isNull();
	}

	@Test
	void should_not_create_a_stub_for_a_contract_matching_ignored_pattern() {
		Contract groovyDsl = Contract.make((c) -> {
			c.request((r) -> {
				r.method("GET");
				r.urlPath("/some/api", (u) -> u.queryParameters(
						(q) -> q.parameter("size", r.value(r.consumer(r.regex("[0-9]+")), r.producer(1)))));
			});
			c.response((r) -> {
				r.status(r.OK());
				r.body("");
			});
		});
		StubMapping result = new WireMockStubStrategy("Test", new ContractMetadata(null, true, 0, null, groovyDsl),
				groovyDsl)
			.toWireMockClientStub();
		assertThat(result).isNull();
	}

	@Test
	void should_work_with_legacy_mappings() {
		String oldJsonMapping = """
				{
				  "request" : {
				    "urlPath" : "/api/v1/xxxx",
				    "method" : "POST",
				    "headers" : {
				      "Authorization" : {
				        "equalTo" : "secret2"
				      }
				    },
				    "cookies" : {
				      "foo" : {
				        "equalTo" : "bar"
				      }
				    },
				    "queryParameters" : {
				      "foo" : {
				        "equalTo" : "bar2"
				      }
				    },
				    "bodyPatterns" : [ {
				      "matchesJsonPath" : "$[?(@.['baz'] == 5)]"
				    }, {
				      "matchesJsonPath" : "$[?(@.['foo'] == 'bar')]"
				    } ]
				  },
				  "response" : {
				    "status" : 200,
				    "body" : "{\\"authorization\\":\\"{{{request.headers.Authorization.[0]}}}\\",\\"path\\":\\"{{{request.path}}}\\",\\"responseBaz\\":{{{jsonpath this '$.baz'}}} ,\\"param\\":\\"{{{request.query.foo.[0]}}}\\",\\"pathIndex\\":\\"{{{request.path.[1]}}}\\",\\"responseBaz2\\":\\"Bla bla {{{jsonpath this '$.foo'}}} bla bla\\",\\"responseFoo\\":\\"{{{jsonpath this '$.foo'}}}\\",\\"authorization2\\":\\"{{{request.headers.Authorization.[1]}}}\\",\\"fullBody\\":\\"{{{escapejsonbody}}}\\",\\"url\\":\\"{{{request.url}}}\\",\\"paramIndex\\":\\"{{{request.query.foo.[1]}}}\\"}",
				    "headers" : {
				      "Authorization" : "{{{request.headers.Authorization.[0]}}};foo"
				    },
				    "transformers" : [ "response-template", "foo-transformer" ]
				  }
				}
				""";
		int port = TestSocketUtils.findAvailableTcpPort();
		WireMockServer server = new WireMockServer(config().port(port));
		server.start();
		try {
			server.addStubMapping(WireMockStubMapping.buildFrom(oldJsonMapping));
			ResponseEntity<String> entity = call(port);
			assertThat(entity.getHeaders().toSingleValueMap())
				.anySatisfy((k, v) -> assertThat(k).isEqualToIgnoringCase("authorization"));
			AssertionUtil.assertThatJsonsAreEqual("""
					{
					  "url" : "/api/v1/xxxx?foo=bar&foo=bar2",
					  "param" : "bar",
					  "paramIndex" : "bar2",
					  "authorization" : "secret",
					  "authorization2" : "secret2",
					  "fullBody" : "{\\"foo\\":\\"bar\\",\\"baz\\":5}",
					  "responseFoo" : "bar",
					  "responseBaz" : 5,
					  "responseBaz2" : "Bla bla bar bla bla"
					}
					""", entity.getBody());
		}
		finally {
			server.shutdown();
		}
	}

	@Test
	void should_work_with_byte_arrays() {
		File request = new File(WireMockGroovyDslTests.class.getResource("/body_builder/request.pdf").getFile());
		File response = new File(WireMockGroovyDslTests.class.getResource("/body_builder/response.pdf").getFile());
		File file = new File(WireMockGroovyDslTests.class.getResource("/body_builder/worksWithPdf.groovy").getFile());
		Contract contract = ContractVerifierDslConverter.convertAsCollection(file.getParentFile(), file)
			.iterator()
			.next();
		String json = toWireMockClientJsonStub(contract);
		int port = TestSocketUtils.findAvailableTcpPort();
		WireMockServer server = new WireMockServer(config().port(port));
		server.start();
		try {
			server.addStubMapping(WireMockStubMapping.buildFrom(json));
			ResponseEntity<byte[]> entity = callBytes(port, request);
			assertThat(entity.getStatusCode().value()).isEqualTo(200);
			assertThat(entity.getBody()).isEqualTo(readBytes(response));
		}
		finally {
			server.shutdown();
		}
	}

	@Test
	void should_generate_a_stub_with_standard_WireMock_request_template() {
		Contract groovyDsl = Contract.make((c) -> {
			c.request((r) -> {
				r.method("POST");
				r.url("/api/v1/xxxx", (u) -> {
					u.queryParameters((q) -> {
						q.parameter("foo", "bar");
						q.parameter("foo", "bar2");
					});
				});
				r.headers((h) -> {
					h.header(h.authorization(), "secret");
					h.header(h.authorization(), "secret2");
				});
				r.cookies((ck) -> ck.cookie("foo", "bar"));
				r.body(Map.of("foo", "bar", "baz", 5));
			});
			c.response((r) -> {
				r.status(r.OK());
				r.headers((h) -> h.header(h.authorization(), "{{{request.headers.Authorization.[0]}}};foo"));
				LinkedHashMap<String, Object> stdRequestBody = new LinkedHashMap<>();
				stdRequestBody.put("url", r.fromRequest().url());
				stdRequestBody.put("path", r.fromRequest().path());
				stdRequestBody.put("pathIndex", r.fromRequest().path(1));
				stdRequestBody.put("param", r.fromRequest().query("foo"));
				stdRequestBody.put("paramIndex", r.fromRequest().query("foo", 1));
				stdRequestBody.put("authorization", r.fromRequest().header("Authorization"));
				stdRequestBody.put("authorization2", r.fromRequest().header("Authorization", 1));
				stdRequestBody.put("fullBody", r.fromRequest().body());
				stdRequestBody.put("responseFoo", r.fromRequest().body("$.foo"));
				stdRequestBody.put("responseBaz", r.fromRequest().body("$.baz"));
				stdRequestBody.put("responseBaz2", "Bla bla {{{jsonPath request.body '$.foo'}}} bla bla");
				stdRequestBody.put("rawUrl", r.fromRequest().rawUrl());
				stdRequestBody.put("rawPath", r.fromRequest().rawPath());
				stdRequestBody.put("rawPathIndex", r.fromRequest().rawPath(1));
				stdRequestBody.put("rawParam", r.fromRequest().rawQuery("foo"));
				stdRequestBody.put("rawParamIndex", r.fromRequest().rawQuery("foo", 1));
				stdRequestBody.put("rawAuthorization", r.fromRequest().rawHeader("Authorization"));
				stdRequestBody.put("rawAuthorization2", r.fromRequest().rawHeader("Authorization", 1));
				stdRequestBody.put("rawResponseFoo", r.fromRequest().rawBody("$.foo"));
				stdRequestBody.put("rawResponseBaz", r.fromRequest().rawBody("$.baz"));
				stdRequestBody.put("rawResponseBaz2", "Bla bla {{jsonPath request.body '$.foo'}} bla bla");
				r.body(stdRequestBody);
			});
		});
		String json = toWireMockClientJsonStub(groovyDsl);
		AssertionUtil.assertThatJsonsAreEqual(
				"""
						{
						  "request" : {
						    "urlPath" : "/api/v1/xxxx",
						    "method" : "POST",
						    "headers" : {
						      "Authorization" : {
						        "equalTo" : "secret2"
						      }
						    },
						    "cookies" : {
						      "foo" : {
						        "equalTo" : "bar"
						      }
						    },
						    "queryParameters" : {
						      "foo" : {
						        "equalTo" : "bar2"
						      }
						    },
						    "bodyPatterns" : [ {
						      "matchesJsonPath" : "$[?(@.['baz'] == 5)]"
						    }, {
						      "matchesJsonPath" : "$[?(@.['foo'] == 'bar')]"
						    } ]
						  },
						  "response" : {
						    "status" : 200,
						    "body" : "{\\"url\\":\\"{{{request.url}}}\\",\\"path\\":\\"{{{request.path}}}\\",\\"pathIndex\\":\\"{{{request.path.[1]}}}\\",\\"param\\":\\"{{{request.query.foo.[0]}}}\\",\\"paramIndex\\":\\"{{{request.query.foo.[1]}}}\\",\\"authorization\\":\\"{{{request.headers.Authorization.[0]}}}\\",\\"authorization2\\":\\"{{{request.headers.Authorization.[1]}}}\\",\\"fullBody\\":\\"{{{escapejsonbody}}}\\",\\"responseFoo\\":\\"{{{jsonPath request.body '$.foo'}}}\\",\\"responseBaz\\":{{{jsonPath request.body '$.baz'}}} ,\\"responseBaz2\\":\\"Bla bla {{{jsonPath request.body '$.foo'}}} bla bla\\",\\"rawUrl\\":\\"{{request.url}}\\",\\"rawPath\\":\\"{{request.path}}\\",\\"rawPathIndex\\":\\"{{request.path.[1]}}\\",\\"rawParam\\":\\"{{request.query.foo.[0]}}\\",\\"rawParamIndex\\":\\"{{request.query.foo.[1]}}\\",\\"rawAuthorization\\":\\"{{request.headers.Authorization.[0]}}\\",\\"rawAuthorization2\\":\\"{{request.headers.Authorization.[1]}}\\",\\"rawResponseFoo\\":\\"{{jsonPath request.body '$.foo'}}\\",\\"rawResponseBaz\\":{{jsonPath request.body '$.baz'}} ,\\"rawResponseBaz2\\":\\"Bla bla {{jsonPath request.body '$.foo'}} bla bla\\"}",
						    "headers" : {
						      "Authorization" : "{{{request.headers.Authorization.[0]}}};foo"
						    },
						    "transformers" : [ "response-template", "foo-transformer" ]
						  }
						}
						""",
				json);
		stubMappingIsValidWireMockStub(json);
		int port = TestSocketUtils.findAvailableTcpPort();
		WireMockServer server = new WireMockServer(config().port(port));
		server.start();
		try {
			server.addStubMapping(WireMockStubMapping.buildFrom(json));
			ResponseEntity<String> entity = call(port);
			assertThat(entity.getHeaders().toSingleValueMap())
				.anySatisfy((k, v) -> assertThat(k).isEqualToIgnoringCase("authorization"));
			AssertionUtil.assertThatJsonsAreEqual("""
					{
					   "rawAuthorization2":"secret2",
					   "responseBaz":5,
					   "pathIndex":"v1",
					   "rawAuthorization":"secret",
					   "authorization2":"secret2",
					   "rawParam":"bar",
					   "url":"/api/v1/xxxx?foo=bar&foo=bar2",
					   "paramIndex":"bar2",
					   "authorization":"secret",
					   "path":"/api/v1/xxxx",
					   "rawUrl":"/api/v1/xxxx?foo=bar&foo=bar2",
					   "rawPath":"/api/v1/xxxx",
					   "rawResponseBaz2":"Bla bla bar bla bla",
					   "param":"bar",
					   "rawResponseBaz":5,
					   "responseBaz2":"Bla bla bar bla bla",
					   "rawResponseFoo":"bar",
					   "responseFoo":"bar",
					   "rawPathIndex":"v1",
					   "fullBody":"{\\"foo\\":\\"bar\\",\\"baz\\":5}",
					   "rawParamIndex":"bar2"
					}
					""", entity.getBody());
		}
		finally {
			server.shutdown();
		}
	}

	@Test
	void should_generate_a_stub_for_a_request_with_form_parameters() {
		Contract groovyDsl = Contract.make((c) -> {
			c.request((r) -> {
				r.method("POST");
				r.urlPath("/oauth/token");
				r.headers((h) -> {
					h.header(h.authorization(), r.anyNonBlankString());
					h.header(h.contentType(), h.applicationFormUrlencoded());
					h.header(h.accept(), h.applicationJson());
				});
				LinkedHashMap<String, Object> formBody = new LinkedHashMap<>();
				formBody.put("username", "user");
				formBody.put("password", "password");
				formBody.put("grant_type", "password");
				r.body(formBody);
			});
			c.response((r) -> {
				r.status(200);
				r.headers((h) -> h.header(h.contentType(), h.applicationJson()));
				LinkedHashMap<String, Object> userBody = new LinkedHashMap<>();
				userBody.put("id", 1);
				userBody.put("username", "user");
				userBody.put("name", "User");
				LinkedHashMap<String, Object> formRespBody = new LinkedHashMap<>();
				formRespBody.put("refresh_token", "RANDOM_REFRESH_TOKEN");
				formRespBody.put("access_token", "RANDOM_ACCESS_TOKEN");
				formRespBody.put("token_type", "bearer");
				formRespBody.put("expires_in", 3600);
				formRespBody.put("scope", List.of("task"));
				formRespBody.put("user", userBody);
				r.body(formRespBody);
			});
		});
		String json = toWireMockClientJsonStub(groovyDsl);
		AssertionUtil.assertThatJsonsAreEqual(
				"""
						{
						  "request" : {
						    "urlPath" : "/oauth/token",
						    "method" : "POST",
						    "headers" : {
						      "Authorization" : {
						        "matches" : "^\\\\s*\\\\S[\\\\S\\\\s]*"
						      },
						      "Content-Type" : {
						        "equalTo" : "application/x-www-form-urlencoded"
						      },
						      "Accept" : {
						        "equalTo" : "application/json"
						      }
						    },
						    "bodyPatterns" : [ {
						      "equalTo" : "username=user&password=password&grant_type=password"
						    } ]
						  },
						  "response" : {
						    "status" : 200,
						    "body" : "{\\"refresh_token\\":\\"RANDOM_REFRESH_TOKEN\\",\\"access_token\\":\\"RANDOM_ACCESS_TOKEN\\",\\"token_type\\":\\"bearer\\",\\"expires_in\\":3600,\\"scope\\":[\\"task\\"],\\"user\\":{\\"id\\":1,\\"username\\":\\"user\\",\\"name\\":\\"User\\"}}",
						    "headers" : {
						      "Content-Type" : "application/json"
						    },
						    "transformers" : [ "response-template", "foo-transformer" ]
						  }
						}
						""",
				json);
		stubMappingIsValidWireMockStub(json);
	}

	@Test
	void should_work_with_array_of_arrays() {
		Contract groovyDsl = Contract.make((c) -> {
			c.request((r) -> {
				r.method("POST");
				r.urlPath("/api/categories");
				r.body(List.of(List.of("Programming", "Java"), List.of("Programming", "Java", "Spring", "Boot")));
				r.headers((h) -> h.header("Content-Type", "application/json;charset=UTF-8"));
			});
			c.response((r) -> {
				r.status(200);
				r.body(List.of(List.of("Programming", "Java"), List.of("Programming", "Java", "Spring", "Boot")));
				r.headers((h) -> h.header("Content-Type", "application/json;charset=UTF-8"));
			});
		});
		String json = toWireMockClientJsonStub(groovyDsl);
		AssertionUtil.assertThatJsonsAreEqual(
				"""
						{
						  "request" : {
						    "urlPath" : "/api/categories",
						    "method" : "POST",
						    "headers" : {
						      "Content-Type" : {
						        "equalTo" : "application/json;charset=UTF-8"
						      }
						    },
						    "bodyPatterns" : [ {
						      "matchesJsonPath" : "$[*][?(@ == 'Spring')]"
						    }, {
						      "matchesJsonPath" : "$[*][?(@ == 'Boot')]"
						    }, {
						      "matchesJsonPath" : "$[*][?(@ == 'Programming')]"
						    }, {
						      "matchesJsonPath" : "$[*][?(@ == 'Java')]"
						    } ]
						  },
						  "response" : {
						    "status" : 200,
						    "body" : "[\\"[\\\\\\"Programming\\\\\\",\\\\\\"Java\\\\\\"]\\",\\"[\\\\\\"Programming\\\\\\",\\\\\\"Java\\\\\\",\\\\\\"Spring\\\\\\",\\\\\\"Boot\\\\\\"]\\"]",
						    "headers" : {
						      "Content-Type" : "application/json;charset=UTF-8"
						    },
						    "transformers" : [ "response-template", "foo-transformer" ]
						  }
						}
						""",
				json);
		stubMappingIsValidWireMockStub(json);
		int port = TestSocketUtils.findAvailableTcpPort();
		WireMockServer server = new WireMockServer(config().port(port));
		server.start();
		try {
			server.addStubMapping(WireMockStubMapping.buildFrom(json));
			ResponseEntity<String> entity = callApiCategories(port);
			AssertionUtil.assertThatJsonsAreEqual("""
					["[\\"Programming\\",\\"Java\\"]","[\\"Programming\\",\\"Java\\",\\"Spring\\",\\"Boot\\"]"]
					""", entity.getBody());
		}
		finally {
			server.shutdown();
		}
	}

	@Test
	void should_create_a_stub_for_dot_separated_keys() {
		Contract groovyDsl = Contract.make((c) -> {
			c.request((r) -> {
				r.method("PUT");
				r.url("/fraudcheck");
				r.body(Map.of("client.id", r.$(r.regex("[0-9]{10}")), "loanAmount", 99999));
				r.headers((h) -> h.contentType("application/vnd.fraud.v1+json"));
			});
			c.response((r) -> r.status(r.OK()));
		});
		String wireMockStub = new WireMockStubStrategy("Test", new ContractMetadata(null, false, 0, null, groovyDsl),
				groovyDsl)
			.toWireMockClientStub()
			.toString();
		AssertionUtil.assertThatJsonsAreEqual("""
				{
				  "request" : {
				    "url" : "/fraudcheck",
				    "method" : "PUT",
				    "headers" : {
				      "Content-Type" : {
				        "matches" : "application/vnd\\\\.fraud\\\\.v1\\\\+json.*"
				      }
				    },
				    "bodyPatterns" : [ {
				      "matchesJsonPath" : "$[?(@.['loanAmount'] == 99999)]"
				    }, {
				      "matchesJsonPath" : "$[?(@.['client.id'] =~ /[0-9]{10}/)]"
				    } ]
				  },
				  "response" : {
				    "status" : 200
				  }
				}
				""", wireMockStub);
		stubMappingIsValidWireMockStub(wireMockStub);
	}

	@Test
	void should_not_fail_to_generate_a_stub_when_arrays_are_there_in_the_request() {
		Contract groovyDsl = Contract.make((c) -> {
			c.request((r) -> {
				r.method("POST");
				r.urlPath("/batch/persons");
				r.body("[{\n\t\"fruitsILike\": [\n\t  \"apple\"\n\t]\n},\n{\n\t\"fruitsILike\": [\n\t]\n},\n{\n\t\"fruitsILike\": [\n\t\t\"orange\"\n\t]\n}]");
			});
			c.response((r) -> {
				r.status(201);
				r.headers((h) -> h.contentType(h.applicationJsonUtf8()));
				r.body("{  \n\t\t\t\t\t\t\t\"id\": \"foo\"  \n\t\t\t\t\t\t}\"  \n\t\t\t\t\t");
			});
		});
		String wireMockStub = new WireMockStubStrategy("Test", new ContractMetadata(null, false, 0, null, groovyDsl),
				groovyDsl)
			.toWireMockClientStub()
			.toString();
		AssertionUtil.assertThatJsonsAreEqual(
				"""
						{
						  "request" : {
						    "urlPath" : "/batch/persons",
						    "method" : "POST",
						    "bodyPatterns" : [ {
						      "matchesJsonPath" : "$[*].['fruitsILike'][?(@ == 'orange')]"
						    }, {
						      "matchesJsonPath" : "$[*].['fruitsILike'][?(@ == 'apple')]"
						    } ]
						  },
						  "response" : {
						    "status" : 201,
						    "body" : "{  \\n\\t\\t\\t\\t\\t\\t\\t\\"id\\": \\"foo\\"  \\n\\t\\t\\t\\t\\t\\t}\\"  \\n\\t\\t\\t\\t\\t",
						    "headers" : {
						      "Content-Type" : "application/json;charset=UTF-8"
						    }
						  }
						}
						""",
				wireMockStub);
		stubMappingIsValidWireMockStub(wireMockStub);
	}

	@Test
	void should_not_escape_unicode_characters() {
		Contract groovyDsl = Contract.make((c) -> {
			c.request((r) -> {
				r.method("PUT");
				r.url("/fraudcheck");
				r.body(Map.of("client.id", r.$(r.regex("[0-9]{10}")), "loanAmount", 99999));
				r.headers((h) -> h.contentType("application/vnd.fraud.v1+json"));
			});
			c.response((r) -> {
				r.status(r.OK());
				r.body("{\n\t\"code\": 91015,\n\t\"description\": \"订单已失效\",\n\t\"lastUpdateTime\": \"0\",\n\t\"payload\": null\n}");
			});
		});
		String wireMockStub = new WireMockStubStrategy("Test", new ContractMetadata(null, false, 0, null, groovyDsl),
				groovyDsl)
			.toWireMockClientStub()
			.toString();
		AssertionUtil.assertThatJsonsAreEqual(
				"""
						{
						  "request" : {
						    "url" : "/fraudcheck",
						    "method" : "PUT",
						    "headers" : {
						      "Content-Type" : {
						        "matches" : "application/vnd\\\\.fraud\\\\.v1\\\\+json.*"
						      }
						    },
						    "bodyPatterns" : [ {
						      "matchesJsonPath" : "$[?(@.['client.id'] =~ /[0-9]{10}/)]"
						    }, {
						      "matchesJsonPath" : "$[?(@.['loanAmount'] == 99999)]"
						    } ]
						  },
						  "response" : {
						    "status" : 200,
						    "body" : "{\\"code\\":91015,\\"description\\":\\"订单已失效\\",\\"lastUpdateTime\\":\\"0\\",\\"payload\\":null}",
						    "transformers" : [ "response-template", "foo-transformer" ]
						  }
						}
						""",
				wireMockStub);
		stubMappingIsValidWireMockStub(wireMockStub);
	}

	@Test
	void should_not_escape_ampersand() {
		Contract groovyDsl = Contract.make((c) -> {
			c.request((r) -> {
				r.method("POST");
				r.urlPath("/oauth/token");
				r.headers((h) -> {
					h.header(h.authorization(), r.anyNonBlankString());
					h.header(h.contentType(), "application/x-www-form-urlencoded; charset=UTF-8");
					h.header(h.accept(), r.anyNonBlankString());
				});
				r.body("username=user&password=password&grant_type=password");
			});
			c.response((r) -> {
				r.status(200);
				r.headers((h) -> h.header(h.contentType(), h.applicationJsonUtf8()));
				LinkedHashMap<String, Object> oauthUser = new LinkedHashMap<>();
				oauthUser.put("id", 1);
				oauthUser.put("username", "user");
				oauthUser.put("name", "User");
				LinkedHashMap<String, Object> oauthResp = new LinkedHashMap<>();
				oauthResp.put("refresh_token", "RANDOM_REFRESH_TOKEN");
				oauthResp.put("access_token", "RANDOM_ACCESS_TOKEN");
				oauthResp.put("token_type", "bearer");
				oauthResp.put("expires_in", 3600);
				oauthResp.put("scope", List.of("task"));
				oauthResp.put("user", oauthUser);
				r.body(oauthResp);
			});
		});
		String wireMockStub = new WireMockStubStrategy("Test", new ContractMetadata(null, false, 0, null, groovyDsl),
				groovyDsl)
			.toWireMockClientStub()
			.toString();
		assertThat(wireMockStub).doesNotContain("&amp;");
		AssertionUtil.assertThatJsonsAreEqual(
				"""
						{
						  "request" : {
						    "urlPath" : "/oauth/token",
						    "method" : "POST",
						    "headers" : {
						      "Authorization" : {
						        "matches" : "^\\\\s*\\\\S[\\\\S\\\\s]*"
						      },
						      "Content-Type" : {
						        "equalTo" : "application/x-www-form-urlencoded; charset=UTF-8"
						      },
						      "Accept" : {
						        "matches" : "^\\\\s*\\\\S[\\\\S\\\\s]*"
						      }
						    },
						    "bodyPatterns" : [ {
						      "equalTo" : "username=user&password=password&grant_type=password"
						    } ]
						  },
						  "response" : {
						    "status" : 200,
						    "body" : "{\\"refresh_token\\":\\"RANDOM_REFRESH_TOKEN\\",\\"access_token\\":\\"RANDOM_ACCESS_TOKEN\\",\\"token_type\\":\\"bearer\\",\\"expires_in\\":3600,\\"scope\\":[\\"task\\"],\\"user\\":{\\"id\\":1,\\"username\\":\\"user\\",\\"name\\":\\"User\\"}}",
						    "headers" : {
						      "Content-Type" : "application/json;charset=UTF-8"
						    },
						    "transformers" : [ "response-template", "foo-transformer" ]
						  }
						}
						""",
				wireMockStub);
		stubMappingIsValidWireMockStub(wireMockStub);
	}

	@Test
	void should_not_double_escape_backslashes() {
		Contract groovyDsl = Contract.make((c) -> {
			c.priority(1);
			c.request((r) -> {
				r.method("GET");
				r.urlPath(r.value(
						r.consumer(r.regex("/data/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")),
						r.producer("/data/444d57b2-e309-4576-83cb-5530ee03106a")));
			});
			c.response((r) -> {
				r.status(200);
				r.headers((h) -> h.header("Content-Type", "application/json;charset=UTF-8"));
				r.body(Map.of("jsonString", "{\\\"attribute\\\": \\\"value\\\"}"));
			});
		});
		String wireMockStub = new WireMockStubStrategy("Test", new ContractMetadata(null, false, 0, null, groovyDsl),
				groovyDsl)
			.toWireMockClientStub()
			.toString();
		AssertionUtil.assertThatJsonsAreEqual("""
				{
				  "request" : {
				    "urlPathPattern" : "/data/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}",
				    "method" : "GET"
				  },
				  "response" : {
				    "status" : 200,
				    "body" : "{\\"jsonString\\":\\"{\\\\\\"attribute\\\\\\": \\\\\\"value\\\\\\"}\\"}",
				    "headers" : {
				      "Content-Type" : "application/json;charset=UTF-8"
				    },
				    "transformers" : [ "response-template", "foo-transformer" ]
				  },
				  "priority" : 1
				}
				""", wireMockStub);
		stubMappingIsValidWireMockStub(wireMockStub);
	}

	@Test
	void should_work_with_boolean_body() {
		Contract groovyDsl = Contract.make((c) -> {
			c.request((r) -> {
				r.method("DELETE");
				r.urlPath("/item/1");
			});
			c.response((r) -> {
				r.status(200);
				r.headers((h) -> h.contentType(h.applicationJsonUtf8()));
				r.body("true");
			});
		});
		String wireMockStub = new WireMockStubStrategy("Test", new ContractMetadata(null, false, 0, null, groovyDsl),
				groovyDsl)
			.toWireMockClientStub()
			.toString();
		AssertionUtil.assertThatJsonsAreEqual("""
				{
				  "request" : {
				    "urlPath" : "/item/1",
				    "method" : "DELETE"
				  },
				  "response" : {
				    "status" : 200,
				    "body" : "true",
				    "headers" : {
				      "Content-Type" : "application/json;charset=UTF-8"
				    },
				    "transformers" : [ "response-template", "foo-transformer" ]
				  }
				}
				""", wireMockStub);
		stubMappingIsValidWireMockStub(wireMockStub);
	}

	@Test
	void should_generate_stubs_with_request_body_matchers() {
		Contract contractDsl = Contract.make((c) -> {
			c.request((r) -> {
				r.method("GET");
				r.urlPath("/get");
				r.body(Map.ofEntries(Map.entry("duck", 123), Map.entry("alpha", "abc"), Map.entry("number", 123),
						Map.entry("aBoolean", true), Map.entry("date", "2017-01-01"),
						Map.entry("dateTime", "2017-01-01T01:23:45"), Map.entry("time", "01:02:34"),
						Map.entry("valueWithoutAMatcher", "foo"), Map.entry("valueWithTypeMatch", "string"),
						Map.entry("key", Map.of("complex.key", "foo"))));
				r.bodyMatchers((bm) -> {
					bm.jsonPath("$.duck", bm.byRegex("[0-9]{3}"));
					bm.jsonPath("$.duck", bm.byEquality());
					bm.jsonPath("$.alpha", bm.byRegex(RegexPatterns.onlyAlphaUnicode()));
					bm.jsonPath("$.alpha", bm.byEquality());
					bm.jsonPath("$.number", bm.byRegex(RegexPatterns.number()));
					bm.jsonPath("$.aBoolean", bm.byRegex(RegexPatterns.anyBoolean()));
					bm.jsonPath("$.date", bm.byDate());
					bm.jsonPath("$.dateTime", bm.byTimestamp());
					bm.jsonPath("$.time", bm.byTime());
					bm.jsonPath("$.['key'].['complex.key']", bm.byEquality());
				});
				r.headers((h) -> h.contentType(h.applicationJson()));
			});
			c.response((r) -> {
				r.status(200);
				r.headers((h) -> h.contentType(h.applicationJsonUtf8()));
				r.body("true");
			});
		});
		String wireMockStub = new WireMockStubStrategy("Test", new ContractMetadata(null, false, 0, null, contractDsl),
				contractDsl)
			.toWireMockClientStub()
			.toString();
		stubMappingIsValidWireMockStub(wireMockStub);
	}

	@Test
	void should_not_produce_any_cursors_in_the_stub() {
		Contract contractDsl = Contract.make((c) -> {
			c.request((r) -> {
				r.method("POST");
				r.urlPath(r.$(r.test("/resource/resourceId/another-resource/another-resource-id/sth"),
						r.stub(r.regex("/resource/[\\w\\.]+/another-resource/([\\w+\\.-]|%[a-fA-F0-9]{2})+/sth"))));
				r.body(r.$(r.stub(r.regex(".+")), r.test(r.execute("encrypt('a lot of code')"))));
			});
			c.response((r) -> r.status(201));
			c.priority(1000);
		});
		String wireMockStub = new WireMockStubStrategy("Test", new ContractMetadata(null, false, 0, null, contractDsl),
				contractDsl)
			.toWireMockClientStub()
			.toString();
		stubMappingIsValidWireMockStub(wireMockStub);
	}

	@Test
	void should_not_fail_when_matchers_dont_have_dots() {
		Contract contractDsl = Contract.make((c) -> {
			c.request((r) -> {
				r.method("POST");
				r.url("/example");
				r.body(List.of("123", "234"));
				r.bodyMatchers((bm) -> bm.jsonPath("$[*]", bm.byRegex(RegexPatterns.nonEmpty())));
			});
			c.response((r) -> r.status(201));
		});
		String wireMockStub = new WireMockStubStrategy("Test", new ContractMetadata(null, false, 0, null, contractDsl),
				contractDsl)
			.toWireMockClientStub()
			.toString();
		stubMappingIsValidWireMockStub(wireMockStub);
	}

	@Test
	void should_deal_with_unicode_strings() {
		File file = new File(WireMockGroovyDslTests.class.getResource("/yml/issue1038.yml").getFile());
		Contract contractDsl = new YamlContractConverter().convertFrom(file).iterator().next();
		String wireMockStub = new WireMockStubStrategy("Test", new ContractMetadata(null, false, 0, null, contractDsl),
				contractDsl)
			.toWireMockClientStub()
			.toString();
		assertThat(wireMockStub).contains("日本");
		stubMappingIsValidWireMockStub(wireMockStub);
	}

	@Test
	void should_work_with_complex_objects_in_the_body() {
		Contract contractDsl = Contract.make((c) -> {
			c.description("should migrate spaceship");
			c.request((r) -> {
				r.method("POST");
				r.url("/api/migration");
				r.headers((h) -> {
					h.accept("application/json");
					h.contentType(h.applicationJson());
				});
				LinkedHashMap<String, Object> complexBody = new LinkedHashMap<>();
				complexBody.put("id", 4);
				complexBody.put("foo", 5);
				complexBody.put("whatever", "hello");
				r.body(r.$(r.c(complexBody), r.p(r.execute("hashCode()"))));
			});
			c.response((r) -> {
				r.status(r.OK());
				r.headers((h) -> h.contentType(h.applicationJson()));
				LinkedHashMap<String, Object> complexBody2 = new LinkedHashMap<>();
				complexBody2.put("id", 4);
				complexBody2.put("foo", 5);
				complexBody2.put("whatever", "hello");
				r.body(r.$(r.c(complexBody2), r.p(r.execute("hashCode()"))));
			});
		});
		String wireMockStub = new WireMockStubStrategy("Test", new ContractMetadata(null, false, 0, null, contractDsl),
				contractDsl)
			.toWireMockClientStub()
			.toString();
		assertThat(wireMockStub).contains("$[?(@.['whatever'] == 'hello')]");
		assertThat(wireMockStub).contains("$[?(@.['id'] == 4)]");
		assertThat(wireMockStub).contains("$[?(@.['foo'] == 5)]");
		assertThat(wireMockStub).contains("{\\\"id\\\":4,\\\"foo\\\":5,\\\"whatever\\\":\\\"hello\\\"}");

		stubMappingIsValidWireMockStub(wireMockStub);
	}

	@Test
	void should_work_with_streams() {
		Contract contractDsl = Contract.make((c) -> {
			c.description("should return all entities");
			c.request((r) -> {
				r.method("GET");
				r.url("/api/v1/entities");
			});
			c.response((r) -> {
				r.headers((h) -> h.header("Content-Type", "application/stream+json"));
				r.body("{\n\"id\" : \"1\",\n\"name\" : \"Entity1\",\n\"nested_data\" : {\n\"key1\" : \"value1\"\n}\n},\n{\n\"id\" : \"2\",\n\"name\" : \"Entity2\",\n\"nested_data\" : {\n\"key1\" : \"value1\"\n}\n}");
				r.status(200);
			});
		});
		String wireMockStub = new WireMockStubStrategy("Test", new ContractMetadata(null, false, 0, null, contractDsl),
				contractDsl)
			.toWireMockClientStub()
			.toString();
		assertThat(wireMockStub).contains("Entity1");
		assertThat(wireMockStub).contains("Entity2");
		stubMappingIsValidWireMockStub(wireMockStub);
		int port = TestSocketUtils.findAvailableTcpPort();
		WireMockServer server = new WireMockServer(config().port(port));
		server.start();
		try {
			server.addStubMapping(WireMockStubMapping.buildFrom(wireMockStub));
			ResponseEntity<String> entity = callForStream(port);
			assertThat(entity.getStatusCode().value()).isEqualTo(200);
			assertThat(entity.getBody()).contains("Entity1");
			assertThat(entity.getBody()).contains("Entity2");
		}
		finally {
			server.shutdown();
		}
	}

	@Test
	void should_not_generate_assertions_for_wildcard_when_all_manual_entries_were_passed() {
		Contract contractDsl = Contract.make((c) -> {
			c.request((r) -> {
				r.method("GET");
				r.url("/operations");
				r.body(Map.of("channedlId", "UC", "operations",
						List.of(Map.of("parameters",
								List.of(Map.of("name", "#POID", "value", "70000269814", "type", "DECIMAL"),
										Map.of("name", "#OVID", "value", "3", "type", "DECIMAL"),
										Map.of("name", "#CAMC", "value", "CC2PAY", "type", "CHAR"))))));
				r.bodyMatchers((bm) -> {
					bm.jsonPath("$.operations[0].parameters[0].name", bm.byEquality());
					bm.jsonPath("$.operations[0].parameters[0].value", bm.byRegex("[0-9]{11}"));
					bm.jsonPath("$.operations[0].parameters[0].type", bm.byEquality());
					bm.jsonPath("$.operations[0].parameters[1].name", bm.byEquality());
					bm.jsonPath("$.operations[0].parameters[1].value", bm.byEquality());
					bm.jsonPath("$.operations[0].parameters[1].type", bm.byEquality());
					bm.jsonPath("$.operations[0].parameters[2].name", bm.byEquality());
					bm.jsonPath("$.operations[0].parameters[2].value", bm.byEquality());
					bm.jsonPath("$.operations[0].parameters[2].type", bm.byEquality());
				});
			});
			c.response((r) -> r.status(200));
		});
		String wireMockStub = new WireMockStubStrategy("Test", new ContractMetadata(null, false, 0, null, contractDsl),
				contractDsl)
			.toWireMockClientStub()
			.toString();
		assertThat(wireMockStub).doesNotContain("$.['operations'][*]");
		stubMappingIsValidWireMockStub(wireMockStub);
	}

	@Test
	void should_work_with_null_request_element_on_the_client_side_and_optional_stub_entry() {
		Contract contractDsl = Contract.make((c) -> {
			c.description("Creating user");
			c.name("Create user");
			c.request((r) -> {
				r.method("POST");
				r.url("/api/user");
				r.body(Map.of("address",
						r.$(r.consumer(r.optional(r.regex(RegexPatterns.alphaNumeric()))), r.producer((Object) null)),
						"name", r.$(r.consumer(r.optional(r.regex(RegexPatterns.alphaNumeric()))), r.producer(""))));
				r.headers((h) -> h.contentType(h.applicationJson()));
			});
			c.response((r) -> r.status(201));
		});
		StubMapping wireMockStub = new WireMockStubStrategy("Test",
				new ContractMetadata(null, false, 0, null, contractDsl), contractDsl)
			.toWireMockClientStub();
		int port = TestSocketUtils.findAvailableTcpPort();
		WireMockServer server = new WireMockServer(config().port(port));
		server.start();
		try {
			stubMappingIsValidWireMockStub(wireMockStub);
			server.addStubMapping(wireMockStub);
			ResponseEntity<String> entity = callWithOptionalAndEmpty(port);
			assertThat(entity.getStatusCode().value()).isEqualTo(201);
		}
		finally {
			server.shutdown();
		}
	}

	@Test
	void should_work_with_client_DSL_properties() {
		Contract groovyDsl = Contract.make((c) -> {
			c.request((r) -> {
				r.method("GET");
				r.url("/api/foo");
				r.headers((h) -> h.header("foo", r.$(r.client(r.anyAlphaNumeric()), r.server("123"))));
				r.cookies((ck) -> {
					ck.cookie("cookie1", r.$(r.client("foo"), r.server("bar")));
					ck.cookie("cookie2", r.$(r.client(r.regex("[a-z]+")), r.server("bar")));
					ck.cookie("cookie3", r.$(r.client(r.anyAlphaNumeric()), r.server("bar")));
					ck.cookie("cookie4", r.$(r.anyAlphaNumeric()));
				});
			});
			c.response((r) -> {
				r.status(r.OK());
				r.body("ok");
				r.headers((h) -> h.header("Content-Type", "text/plain"));
			});
		});
		String wireMockStub = new WireMockStubStrategy("Test", new ContractMetadata(null, false, 0, null, groovyDsl),
				groovyDsl)
			.toWireMockClientStub()
			.toString();
		AssertionUtil.assertThatJsonsAreEqual("""
				{
				  "request" : {
				    "url" : "/api/foo",
				    "method" : "GET",
				    "headers" : {
				      "foo" : {
				        "matches" : "[a-zA-Z0-9]+"
				      }
				    },
				    "cookies" : {
				      "cookie1" : {
				        "equalTo" : "foo"
				      },
				      "cookie2" : {
				        "matches" : "[a-z]+"
				      },
				      "cookie3" : {
				        "matches" : "[a-zA-Z0-9]+"
				      },
				      "cookie4" : {
				        "matches" : "[a-zA-Z0-9]+"
				      }
				    }
				  },
				  "response" : {
				    "status" : 200,
				    "body" : "ok",
				    "headers" : {
				      "Content-Type" : "text/plain"
				    },
				    "transformers" : [ "response-template", "foo-transformer" ]
				  }
				}
				""", wireMockStub);
		stubMappingIsValidWireMockStub(wireMockStub);
	}

	@Test
	void should_correctly_process_optional_of_DslProperty_parameters() {
		Contract groovyDsl = Contract.make((c) -> {
			c.request((r) -> {
				r.method("GET");
				r.url("/api/foo");
				r.headers((h) -> {
					h.header("Content-Type", "application/json");
					h.header("Accept", "application/json");
				});
				r.body(Map.ofEntries(
						Map.entry("key1", r.$(r.client(r.optional(r.anyOf("foo", "bar"))), r.server("bar"))),
						Map.entry("key2", r.$(r.client(r.optional(r.anyNonBlankString())), r.server("bar"))),
						Map.entry("key3", r.$(r.client(r.optional(r.anyEmail())), r.server("foo@bar.com"))),
						Map.entry("key4", r.$(r.optional(r.anyNumber())))));
			});
			c.response((r) -> {
				r.status(r.OK());
				r.body("ok");
				r.headers((h) -> h.header("Content-Type", "text/plain"));
			});
		});
		String wireMockStub = new WireMockStubStrategy("Test", new ContractMetadata(null, false, 0, null, groovyDsl),
				groovyDsl)
			.toWireMockClientStub()
			.toString();
		AssertionUtil.assertThatJsonsAreEqual("""
				{
				  "request" : {
				    "url" : "/api/foo",
				    "method" : "GET",
				    "headers" : {
				      "Content-Type" : {
				        "equalTo" : "application/json"
				      },
				      "Accept" : {
				        "equalTo" : "application/json"
				      }
				    },
				    "bodyPatterns" : [ {
				      "matchesJsonPath" : "$[?(@.['key1'] =~ /(^foo$|^bar$)?/)]"
				    }, {
				      "matchesJsonPath" : "$[?(@.['key2'] =~ /(^\\\\s*\\\\S[\\\\S\\\\s]*)?/)]"
				    }, {
				      "matchesJsonPath" : "$[?(@.['key3'] =~ /([a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\\\.[a-zA-Z]{2,6})?/)]"
				    }, {
				      "matchesJsonPath" : "$[?(@.['key4'] =~ /(-?(\\\\d*\\\\.\\\\d+|\\\\d+))?/)]"
				    } ]
				  },
				  "response" : {
				    "status" : 200,
				    "body" : "ok",
				    "headers" : {
				      "Content-Type" : "text/plain"
				    },
				    "transformers" : [ "response-template", "foo-transformer" ]
				  }
				}
				""", wireMockStub);
		stubMappingIsValidWireMockStub(wireMockStub);
	}

}
