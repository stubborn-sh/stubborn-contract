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

import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import org.junit.jupiter.api.Test;
import sh.stubborn.contract.spec.Contract;
import sh.stubborn.contract.spec.internal.RegexPatterns;
import sh.stubborn.contract.verifier.file.ContractMetadata;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link WireMockXmlStubStrategy}.
 *
 * @author Olga Maciaszek-Sharma
 */
class WireMockXmlStubStrategyTests implements WireMockStubVerifier {

	@Test
	void should_generate_stubs_with_plain_xml_request_body() {
		Contract contractDsl = Contract.make((c) -> {
			c.request((r) -> {
				r.method("GET");
				r.urlPath("/get");
				r.body("\n<test>\n<duck type='xtype'>123</duck>\n<alpha>abc</alpha>\n<list>\n"
						+ "<elem>abc</elem>\n<elem>def</elem>\n<elem>ghi</elem>\n</list>\n"
						+ "<number>123</number>\n<aBoolean>true</aBoolean>\n<date>2017-01-01</date>\n"
						+ "<dateTime>2017-01-01T01:23:45</dateTime>\n<time>01:02:34</time>\n"
						+ "<valueWithoutAMatcher>foo</valueWithoutAMatcher>\n"
						+ "<valueWithTypeMatch>string</valueWithTypeMatch>\n"
						+ "<key><complex>foo</complex></key>\n</test>");
				r.headers((h) -> h.contentType(h.applicationXml()));
			});
			c.response((r) -> {
				r.status(r.OK());
				r.headers((h) -> h.contentType(h.applicationXml()));
			});
		});
		StubMapping wireMockStub = new WireMockStubStrategy("Test",
				new ContractMetadata(null, false, 0, null, contractDsl), contractDsl)
			.toWireMockClientStub();
		stubMappingIsValidWireMockStub(wireMockStub);
		String stub = wireMockStub.toString().replaceAll("\n", "").replaceAll("\r", "").replaceAll(" ", "");
		assertThat(stub).contains("""
				"bodyPatterns": [
				    {
				      "matchesXPath": {
				        "expression": "/test/duck/text()",
				        "equalTo": "123"
				      }
				    },
				    {
				      "matchesXPath": {
				        "expression": "/test/alpha/text()",
				        "equalTo": "abc"
				      }
				    },
				    {
				      "matchesXPath": {
				        "expression": "/test/list/elem/text()",
				        "equalTo": "abc"
				      }
				    },
				    {
				      "matchesXPath": {
				        "expression": "/test/list/elem[2]/text()",
				        "equalTo": "def"
				      }
				    },
				    {
				      "matchesXPath": {
				        "expression": "/test/list/elem[3]/text()",
				        "equalTo": "ghi"
				      }
				    },
				    {
				      "matchesXPath": {
				        "expression": "/test/number/text()",
				        "equalTo": "123"
				      }
				    },
				    {
				      "matchesXPath": {
				        "expression": "/test/aBoolean/text()",
				        "equalTo": "true"
				      }
				    },
				    {
				      "matchesXPath": {
				        "expression": "/test/date/text()",
				        "equalTo": "2017-01-01"
				      }
				    },
				    {
				      "matchesXPath": {
				        "expression": "/test/dateTime/text()",
				        "equalTo": "2017-01-01T01:23:45"
				      }
				    },
				    {
				      "matchesXPath": {
				        "expression": "/test/time/text()",
				        "equalTo": "01:02:34"
				      }
				    },
				    {
				      "matchesXPath": {
				        "expression": "/test/valueWithoutAMatcher/text()",
				        "equalTo": "foo"
				      }
				    },
				    {
				      "matchesXPath": {
				        "expression": "/test/valueWithTypeMatch/text()",
				        "equalTo": "string"
				      }
				    },
				    {
				      "matchesXPath": {
				        "expression": "/test/key/complex/text()",
				        "equalTo": "foo"
				      }
				    },
				    {
				      "matchesXPath": {
				        "expression": "/test/duck/@type",
				        "equalTo": "xtype"
				      }
				      }]
				""".replaceAll("\n", "").replaceAll("\r", "").replaceAll(" ", ""));
	}

	@Test
	void should_generate_stubs_with_request_body_matchers() {
		Contract contractDsl = Contract.make((c) -> {
			c.request((r) -> {
				r.method("GET");
				r.urlPath("/get");
				r.body("\n<test>\n<duck type='xtype'>123</duck>\n<alpha>abc</alpha>\n<number>123</number>\n"
						+ "<aBoolean>true</aBoolean>\n<date>2017-01-01</date>\n"
						+ "<dateTime>2017-01-01T01:23:45</dateTime>\n<time>01:02:34</time>\n"
						+ "<valueWithoutAMatcher>foo</valueWithoutAMatcher>\n"
						+ "<valueWithTypeMatch>string</valueWithTypeMatch>\n"
						+ "<key><complex>foo</complex></key>\n</test>");
				r.bodyMatchers((m) -> {
					m.xPath("/test/duck/text()", m.byRegex("[0-9]{3}"));
					m.xPath("/test/duck/text()", m.byEquality());
					m.xPath("/test/alpha/text()", m.byRegex(RegexPatterns.onlyAlphaUnicode()));
					m.xPath("/test/alpha/text()", m.byEquality());
					m.xPath("/test/number/text()", m.byRegex(RegexPatterns.number()));
					m.xPath("/test/aBoolean/text()", m.byRegex(RegexPatterns.anyBoolean()));
					m.xPath("/test/date/text()", m.byDate());
					m.xPath("/test/dateTime/text()", m.byTimestamp());
					m.xPath("/test/time/text()", m.byTime());
					m.xPath("/test/*/complex/text()", m.byEquality());
					m.xPath("/test/duck/@type", m.byEquality());
				});
				r.headers((h) -> h.contentType(h.applicationXml()));
			});
			c.response((r) -> {
				r.status(r.OK());
				r.headers((h) -> h.contentType(h.applicationXml()));
			});
		});
		StubMapping wireMockStub = new WireMockStubStrategy("Test",
				new ContractMetadata(null, false, 0, null, contractDsl), contractDsl)
			.toWireMockClientStub();
		stubMappingIsValidWireMockStub(wireMockStub);
		assertThat(wireMockStub.toString().replaceAll("\n", "").replaceAll("\r", "").replaceAll(" ", "")).contains(
				"""
						  matchesXPath" : {
						    "expression" : "/test/duck/text()",
						    "matches" : "[0-9]{3}"
						  }
						}, {
						  "matchesXPath" : {
						    "expression" : "/test/duck/text()",
						    "equalTo" : "123"
						  }
						}, {
						  "matchesXPath" : {
						    "expression" : "/test/alpha/text()",
						    "matches" : "[\\\\p{L}]*"
						  }
						}, {
						  "matchesXPath" : {
						    "expression" : "/test/alpha/text()",
						    "equalTo" : "abc"
						  }
						}, {
						  "matchesXPath" : {
						    "expression" : "/test/number/text()",
						    "matches" : "-?(\\\\d*\\\\.\\\\d+|\\\\d+)"
						  }
						}, {
						  "matchesXPath" : {
						    "expression" : "/test/aBoolean/text()",
						    "matches" : "(true|false)"
						  }
						}, {
						  "matchesXPath" : {
						    "expression" : "/test/date/text()",
						    "matches" : "(\\\\d\\\\d\\\\d\\\\d)-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])"
						  }
						}, {
						  "matchesXPath" : {
						    "expression" : "/test/dateTime/text()",
						    "matches" : "([0-9]{4})-(1[0-2]|0[1-9])-(3[01]|0[1-9]|[12][0-9])T(2[0-3]|[01][0-9]):([0-5][0-9]):([0-5][0-9])"
						  }
						}, {
						  "matchesXPath" : {
						    "expression" : "/test/time/text()",
						    "matches" : "(2[0-3]|[01][0-9]):([0-5][0-9]):([0-5][0-9])"
						  }
						}, {
						  "matchesXPath" : {
						    "expression" : "/test/*/complex/text()",
						    "equalTo" : "foo"
						  }
						}, {
						  "matchesXPath" : {
						    "expression" : "/test/duck/@type",
						    "equalTo" : "xtype"
						  }
						}"""
					.replaceAll("\n", "")
					.replaceAll("\r", "")
					.replaceAll(" ", ""));
	}

	@Test
	void should_generate_stubs_with_both_xml_and_body_matchers_in_request() {
		Contract contractDsl = Contract.make((c) -> {
			c.request((r) -> {
				r.method("GET");
				r.urlPath("/get");
				r.body("\n<test>\n<duck type='xtype'>123</duck>\n<alpha>abc</alpha>\n<number>123</number>\n</test>");
				r.bodyMatchers((m) -> {
					m.xPath("/test/duck/text()", m.byEquality());
					m.xPath("/test/number/text()", m.byRegex(RegexPatterns.number()));
				});
				r.headers((h) -> h.contentType(h.applicationXml()));
			});
			c.response((r) -> {
				r.status(r.OK());
				r.headers((h) -> h.contentType(h.applicationXml()));
			});
		});
		StubMapping wireMockStub = new WireMockStubStrategy("Test",
				new ContractMetadata(null, false, 0, null, contractDsl), contractDsl)
			.toWireMockClientStub();
		stubMappingIsValidWireMockStub(wireMockStub);
		assertThat(wireMockStub.toString().replaceAll("\n", "").replaceAll("\r", "").replaceAll(" ", "")).contains("""
				   "bodyPatterns" : [ {
				      "matchesXPath": {
				        "expression": "/test/alpha/text()",
				        "equalTo": "abc"
				      }
				    }, {
				      "matchesXPath" : {
				        "expression" : "/test/duck/@type",
				        "equalTo" : "xtype"
				      }
				    }, {
				      "matchesXPath": {
				        "expression": "/test/duck/text()",
				        "equalTo": "123"
				      }
				    }, {
				      "matchesXPath": {
				        "expression": "/test/number/text()",
				        "matches" : "-?(\\\\d*\\\\.\\\\d+|\\\\d+)"
				      }
				    } ]
				}
				""".replaceAll("\n", "").replaceAll("\r", "").replaceAll(" ", ""));
	}

	@Test
	void should_generate_stubs_with_response_body_matchers() {
		Contract contractDsl = Contract.make((c) -> {
			c.request((r) -> {
				r.method("GET");
				r.urlPath("/get");
				r.headers((h) -> h.contentType(h.applicationXml()));
			});
			c.response((r) -> {
				r.status(r.OK());
				r.headers((h) -> h.contentType(h.applicationXml()));
				r.body("\n<test>\n<duck type='xtype'>123</duck>\n<alpha>abc</alpha>\n<list>\n"
						+ "<elem>abc</elem>\n<elem>def</elem>\n<elem>ghi</elem>\n</list>\n"
						+ "<number>123</number>\n<aBoolean>true</aBoolean>\n<date>2017-01-01</date>\n"
						+ "<dateTime>2017-01-01T01:23:45</dateTime>\n<time>01:02:34</time>\n"
						+ "<valueWithoutAMatcher>foo</valueWithoutAMatcher>\n"
						+ "<valueWithTypeMatch>string</valueWithTypeMatch>\n"
						+ "<key><complex>foo</complex></key>\n</test>");
				r.bodyMatchers((m) -> {
					m.xPath("/test/duck/text()", m.byRegex("[0-9]{3}"));
					m.xPath("/test/duck/text()", m.byEquality());
					m.xPath("/test/alpha/text()", m.byRegex(RegexPatterns.onlyAlphaUnicode()));
					m.xPath("/test/alpha/text()", m.byEquality());
					m.xPath("/test/number/text()", m.byRegex(RegexPatterns.number()));
					m.xPath("/test/aBoolean/text()", m.byRegex(RegexPatterns.anyBoolean()));
					m.xPath("/test/date/text()", m.byDate());
					m.xPath("/test/dateTime/text()", m.byTimestamp());
					m.xPath("/test/time/text()", m.byTime());
					m.xPath("/test/*/complex/text()", m.byEquality());
					m.xPath("/test/duck/@type", m.byEquality());
				});
			});
		});
		StubMapping wireMockStub = new WireMockStubStrategy("Test",
				new ContractMetadata(null, false, 0, null, contractDsl), contractDsl)
			.toWireMockClientStub();
		assertThat(wireMockStub.toString())
			.contains("\\n<test>\\n<duck type='xtype'>123</duck>" + "\\n<alpha>abc</alpha>\\n<list>\\n<elem>abc</elem>"
					+ "\\n<elem>def</elem>" + "\\n<elem>ghi</elem>\\n</list>\\n<number>123</number>"
					+ "\\n<aBoolean>true</aBoolean>\\n<date>2017-01-01</date>"
					+ "\\n<dateTime>2017-01-01T01:23:45</dateTime>\\n<time>01:02:34</time>"
					+ "\\n<valueWithoutAMatcher>foo</valueWithoutAMatcher>"
					+ "\\n<valueWithTypeMatch>string</valueWithTypeMatch>"
					+ "\\n<key><complex>foo</complex></key>\\n</test>");
	}

}
