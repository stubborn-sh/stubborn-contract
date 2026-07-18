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

package sh.stubborn.contract.verifier.builder;

import java.io.File;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import sh.stubborn.contract.spec.Contract;
import sh.stubborn.contract.spec.internal.RegexPatterns;
import sh.stubborn.contract.verifier.config.ContractVerifierConfigProperties;
import sh.stubborn.contract.verifier.config.TestFramework;
import sh.stubborn.contract.verifier.config.TestMode;
import sh.stubborn.contract.verifier.file.ContractMetadata;
import sh.stubborn.contract.verifier.util.SyntaxChecker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Olga Maciaszek-Sharma
 * @author Chris Bono
 * @since 2.1.0
 */
class XmlMethodBodyBuilderTests {

	ContractVerifierConfigProperties properties;

	final GeneratedClassDataForMethod generatedClassDataForMethod = new GeneratedClassDataForMethod(
			new SingleTestGenerator.GeneratedClassData("foo", "bar", new File(".").toPath()), "method");

	final SingleTestGenerator.GeneratedClassData generatedClassData = new SingleTestGenerator.GeneratedClassData("foo",
			"com.example", new File(".").toPath());

	@BeforeEach
	void setup() {
		properties = new ContractVerifierConfigProperties();
		properties.setAssertJsonSize(true);
	}

	private String singleTestGenerator(Contract contractDsl) {
		return new JavaTestGenerator() {
			@Override
			ClassBodyBuilder classBodyBuilder(BlockBuilder builder, GeneratedClassMetaData metaData,
					SingleMethodBuilder methodBuilder) {
				return super.classBodyBuilder(builder, metaData, methodBuilder).field(new Field() {
					@Override
					public boolean accept() {
						return metaData.configProperties.getTestMode() == TestMode.JAXRSCLIENT;
					}

					@Override
					public Field call() {
						builder.addLine("WebTarget webTarget");
						return this;
					}
				});
			}
		}.buildClass(properties, List.of(contractMetadata(contractDsl)), "foo", generatedClassData);
	}

	private ContractMetadata contractMetadata(Contract contractDsl) {
		return new ContractMetadata(new File(".").toPath(), false, 0, null, contractDsl);
	}

	private Contract xmlWithBodyMatchersContract() {
		// tag::xmlgroovy[]
		return Contract.make((contract) -> {
			contract.request((request) -> {
				request.method(request.GET());
				request.urlPath("/get");
				request.headers((headers) -> headers.contentType(headers.applicationXml()));
			});
			contract.response((response) -> {
				response.status(response.OK());
				response.headers((headers) -> headers.contentType(headers.applicationXml()));
				response.body("\n<test>\n<duck type='xtype'>123</duck>\n<alpha>abc</alpha>\n<list>\n"
						+ "<elem>abc</elem>\n<elem>def</elem>\n<elem>ghi</elem>\n</list>\n"
						+ "<number>123</number>\n<aBoolean>true</aBoolean>\n<date>2017-01-01</date>\n"
						+ "<dateTime>2017-01-01T01:23:45</dateTime>\n<time>01:02:34</time>\n"
						+ "<valueWithoutAMatcher>foo</valueWithoutAMatcher>\n"
						+ "<key><complex>foo</complex></key>\n</test>");
				response.bodyMatchers((matchers) -> {
					matchers.xPath("/test/duck/text()", matchers.byRegex("[0-9]{3}"));
					matchers.xPath("/test/duck/text()", matchers.byCommand("equals($it)"));
					matchers.xPath("/test/duck/xxx", matchers.byNull());
					matchers.xPath("/test/duck/text()", matchers.byEquality());
					matchers.xPath("/test/alpha/text()", matchers.byRegex(RegexPatterns.onlyAlphaUnicode()));
					matchers.xPath("/test/alpha/text()", matchers.byEquality());
					matchers.xPath("/test/number/text()", matchers.byRegex(RegexPatterns.number()));
					matchers.xPath("/test/date/text()", matchers.byDate());
					matchers.xPath("/test/dateTime/text()", matchers.byTimestamp());
					matchers.xPath("/test/time/text()", matchers.byTime());
					matchers.xPath("/test/*/complex/text()", matchers.byEquality());
					matchers.xPath("/test/duck/@type", matchers.byEquality());
				});
			});
		});
		// end::xmlgroovy[]
	}

	private Contract xmlWithNamespaceContract() {
		return Contract.make((contract) -> {
			contract.request((request) -> {
				request.method(request.GET());
				request.urlPath("/get");
				request.headers((headers) -> headers.contentType(headers.applicationXml()));
			});
			contract.response((response) -> {
				response.status(response.OK());
				response.headers((headers) -> headers.contentType(headers.applicationXml()));
				response.body("\n<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"
						+ "        <soap:Body>\n"
						+ "            <ns2:Res xmlns:ns2=\"http://*******/****/****/******/schema\">\n"
						+ "                <ns2:ID>1</ns2:ID>\n" + "            </ns2:Res>\n" + "        </soap:Body>\n"
						+ "    </soap:Envelope>");
			});
		});
	}

	private Contract namedXmlWithBodyMatchersContract() {
		// tag::xmlgroovy[]
		return Contract.make((contract) -> {
			contract.request((request) -> {
				request.method(request.GET());
				request.urlPath("/get");
				request.headers((headers) -> headers.contentType(headers.applicationXml()));
			});
			contract.response((response) -> {
				response.status(response.OK());
				response.headers((headers) -> headers.contentType(headers.applicationXml()));
				response.body("\n<ns1:test xmlns:ns1=\"http://demo.com/testns\">\n" + " <ns1:header>\n"
						+ "    <duck-bucket type='bigbucket'>\n" + "      <duck>duck5150</duck>\n"
						+ "    </duck-bucket>\n" + "</ns1:header>\n" + "</ns1:test>\n");
				response.bodyMatchers((matchers) -> {
					matchers.xPath("/test/duck/text()", matchers.byRegex("[0-9]{3}"));
					matchers.xPath("/test/duck/text()", matchers.byCommand("equals($it)"));
					matchers.xPath("/test/duck/xxx", matchers.byNull());
					matchers.xPath("/test/duck/text()", matchers.byEquality());
					matchers.xPath("/test/alpha/text()", matchers.byRegex(RegexPatterns.onlyAlphaUnicode()));
					matchers.xPath("/test/alpha/text()", matchers.byEquality());
					matchers.xPath("/test/number/text()", matchers.byRegex(RegexPatterns.number()));
					matchers.xPath("/test/date/text()", matchers.byDate());
					matchers.xPath("/test/dateTime/text()", matchers.byTimestamp());
					matchers.xPath("/test/time/text()", matchers.byTime());
					matchers.xPath("/test/duck/@type", matchers.byEquality());
				});
			});
		});
		// end::xmlgroovy[]
	}

	private Contract complexNamedXmlWithBodyMatchersContract() {
		// tag::xmlgroovy[]
		return Contract.make((contract) -> {
			contract.request((request) -> {
				request.method(request.GET());
				request.urlPath("/get");
				request.headers((headers) -> headers.contentType(headers.applicationXml()));
			});
			contract.response((response) -> {
				response.status(response.OK());
				response.headers((headers) -> headers.contentType(headers.applicationXml()));
				response.body("\n<SOAP-ENV:Envelope xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"
						+ "   <SOAP-ENV:Header>\n"
						+ "      <RsHeader xmlns=\"http://schemas.xmlsoap.org/soap/custom\">\n"
						+ "         <MsgSeqId>1234</MsgSeqId>\n" + "      </RsHeader>\n" + "   </SOAP-ENV:Header>\n"
						+ "</SOAP-ENV:Envelope>\n");
				response.bodyMatchers((matchers) -> matchers.xPath(
						"//*[local-name()='RsHeader' and namespace-uri()='http://schemas.xmlsoap.org/soap/custom']/*[local-name()='MsgSeqId']/text()",
						matchers.byEquality()));
			});
		});
		// end::xmlgroovy[]
	}

	private Contract veryComplexNamedXmlWithoutBodyMatchersContract() {
		// tag::xmlgroovy[]
		return Contract.make((contract) -> {
			contract.request((request) -> {
				request.method(request.GET());
				request.urlPath("/get");
				request.headers((headers) -> headers.contentType(headers.applicationXml()));
			});
			contract.response((response) -> {
				response.status(response.OK());
				response.headers((headers) -> headers.contentType(headers.applicationXml()));
				response.body(
						"\n<ns1:customer xmlns:ns1=\"http://demo.com/customer\" xmlns:addr=\"http://demo.com/address\">\n"
								+ "\t<email>customer@test.com</email>\n"
								+ "\t<contact-info xmlns=\"http://demo.com/contact-info\">\n"
								+ "\t\t<name>Krombopulous</name>\n" + "\t\t<address>\n" + "\t\t\t<addr:gps>\n"
								+ "\t\t\t\t<lat>51</lat>\n" + "\t\t\t\t<addr:lon>50</addr:lon>\n"
								+ "\t\t\t</addr:gps>\n" + "\t\t</address>\n" + "\t</contact-info>\n"
								+ "</ns1:customer>\n");
			});
		});
		// end::xmlgroovy[]
	}

	private static Stream<Arguments> methodBuilderVariants() {
		return Stream.of(Arguments.of("spock", TestFramework.SPOCK, null),
				Arguments.of("testng", TestFramework.TESTNG, null), Arguments.of("junit", null, TestMode.MOCKMVC),
				Arguments.of("jaxrs-spock", TestFramework.SPOCK, TestMode.JAXRSCLIENT),
				Arguments.of("jaxrs", TestFramework.JUNIT5, TestMode.JAXRSCLIENT),
				Arguments.of("webclient", null, TestMode.WEBTESTCLIENT));
	}

	private void applyMethodBuilder(TestFramework framework, TestMode mode) {
		if (framework != null) {
			properties.setTestFramework(framework);
		}
		if (mode != null) {
			properties.setTestMode(mode);
		}
	}

	@ParameterizedTest(name = "should generate correct verification from xml with body matchers [{0}]")
	@MethodSource("methodBuilderVariants")
	void shouldGenerateCorrectVerificationFromXmlWithBodyMatchers(String methodBuilderName, TestFramework framework,
			TestMode mode) {
		applyMethodBuilder(framework, mode);
		Contract contractDsl = xmlWithBodyMatchersContract();
		String test = singleTestGenerator(contractDsl);
		assertThat(test)
			.contains("assertThat(valueFromXPath(parsedXml, \"/test/list/elem/text()\")).isEqualTo(\"abc\")")
			.contains("assertThat(valueFromXPath(parsedXml, \"/test/list/elem[2]/text()\")).isEqualTo(\"def\")")
			.contains("assertThat(valueFromXPath(parsedXml, \"/test/list/elem[3]/text()\")).isEqualTo(\"ghi\")")
			.contains("assertThat(valueFromXPath(parsedXml, \"/test/aBoolean/text()\")).isEqualTo(\"true\")")
			.contains("assertThat(valueFromXPath(parsedXml, \"/test/valueWithoutAMatcher/text()\")).isEqualTo(\"foo\")")
			.contains("assertThat(valueFromXPath(parsedXml, \"/test/duck/text()\")).matches(\"[0-9]{3}\")")
			.contains("equals(\"123\")")
			.contains("assertThat(nodeFromXPath(parsedXml, \"/test/duck/xxx\")).isNull()")
			.contains("assertThat(valueFromXPath(parsedXml, \"/test/duck/text()\")).isEqualTo(\"123\")")
			.contains("assertThat(valueFromXPath(parsedXml, \"/test/alpha/text()\")).matches(\"[\\\\p{L}]*\")")
			.contains("assertThat(valueFromXPath(parsedXml, \"/test/alpha/text()\")).isEqualTo(\"abc\")")
			.contains(
					"assertThat(valueFromXPath(parsedXml, \"/test/number/text()\")).matches(\"-?(\\\\d*\\\\.\\\\d+|\\\\d+)\")")
			.contains(
					"assertThat(valueFromXPath(parsedXml, \"/test/date/text()\")).matches(\"(\\\\d\\\\d\\\\d\\\\d)-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])\")")
			.contains(
					"assertThat(valueFromXPath(parsedXml, \"/test/dateTime/text()\")).matches(\"([0-9]{4})-(1[0-2]|0[1-9])-(3[01]|0[1-9]|[12][0-9])T(2[0-3]|[01][0-9]):([0-5][0-9]):([0-5][0-9])\")")
			.contains(
					"assertThat(valueFromXPath(parsedXml, \"/test/time/text()\")).matches(\"(2[0-3]|[01][0-9]):([0-5][0-9]):([0-5][0-9])\")")
			.contains("assertThat(valueFromXPath(parsedXml, \"/test/*/complex/text()\")).isEqualTo(\"foo\")")
			.contains("assertThat(valueFromXPath(parsedXml, \"/test/duck/@type\")).isEqualTo(\"xtype\")");
		SyntaxChecker.tryToCompile(methodBuilderName, test);
	}

	@ParameterizedTest(name = "should generate correct verification from xml with namespace [{0}]")
	@MethodSource("methodBuilderVariants")
	void shouldGenerateCorrectVerificationFromXmlWithNamespace(String methodBuilderName, TestFramework framework,
			TestMode mode) {
		applyMethodBuilder(framework, mode);
		Contract contractDsl = xmlWithNamespaceContract();
		String test = singleTestGenerator(contractDsl);
		assertThat(test).contains(
				"assertThat(valueFromXPath(parsedXml, \"/soap:Envelope/soap:Body/ns2:Res/ns2:ID/text()\")).isEqualTo(\"1\")")
			.contains(
					"assertThat(valueFromXPath(parsedXml, \"/soap:Envelope/namespace::soap\")).isEqualTo(\"http://schemas.xmlsoap.org/soap/envelope/\")")
			.contains(
					"valueFromXPath(parsedXml, \"/soap:Envelope/soap:Body/ns2:Res/namespace::ns2\")).isEqualTo(\"http://*******/****/****/******/schema\"");
		SyntaxChecker.tryToCompile(methodBuilderName, test);
	}

	@ParameterizedTest(name = "should generate correct verification from named xml with body matchers [{0}]")
	@MethodSource("methodBuilderVariants")
	void shouldGenerateCorrectVerificationFromNamedXmlWithBodyMatchers(String methodBuilderName,
			TestFramework framework, TestMode mode) {
		applyMethodBuilder(framework, mode);
		Contract contractDsl = namedXmlWithBodyMatchersContract();
		String test = singleTestGenerator(contractDsl);
		assertThat(test).contains(
				"assertThat(valueFromXPath(parsedXml, \"/ns1:test/ns1:header/duck-bucket/duck/text()\")).isEqualTo(\"duck5150\")")
			.contains(
					"assertThat(valueFromXPath(parsedXml, \"/ns1:test/namespace::ns1\")).isEqualTo(\"http://demo.com/testns\")")
			.contains(
					"assertThat(valueFromXPath(parsedXml, \"/ns1:test/ns1:header/duck-bucket/@type\")).isEqualTo(\"bigbucket\")");
		SyntaxChecker.tryToCompile(methodBuilderName, test);
	}

	@ParameterizedTest(name = "should generate correct verification from complex named xml with body matchers [{0}]")
	@MethodSource("methodBuilderVariants")
	void shouldGenerateCorrectVerificationFromComplexNamedXmlWithBodyMatchers(String methodBuilderName,
			TestFramework framework, TestMode mode) {
		applyMethodBuilder(framework, mode);
		Contract contractDsl = complexNamedXmlWithBodyMatchersContract();
		String test = singleTestGenerator(contractDsl);
		assertThat(test).contains(
				"assertThat(valueFromXPath(parsedXml, \"//*[local-name()='RsHeader' and namespace-uri()='http://schemas.xmlsoap.org/soap/custom']/*[local-name()='MsgSeqId']/text()\")).isEqualTo(\"1234\")")
			.contains(
					"assertThat(valueFromXPath(parsedXml, \"/SOAP-ENV:Envelope/namespace::SOAP-ENV\")).isEqualTo(\"http://schemas.xmlsoap.org/soap/envelope/\")")
			.doesNotContain(
					"assertThat(valueFromXPath(parsedXml, \"/SOAP-ENV:Envelope/SOAP-ENV:Header/*[local-name()='RsHeader' and namespace-uri()='http://schemas.xmlsoap.org/soap/custom']/@xmlns\")).isEqualTo\"");
		SyntaxChecker.tryToCompile(methodBuilderName, test);
	}

	@ParameterizedTest(
			name = "should generate correct verification from very complex named xml without body matchers [{0}]")
	@MethodSource("methodBuilderVariants")
	void shouldGenerateCorrectVerificationFromVeryComplexNamedXmlWithoutBodyMatchers(String methodBuilderName,
			TestFramework framework, TestMode mode) {
		applyMethodBuilder(framework, mode);
		Contract contractDsl = veryComplexNamedXmlWithoutBodyMatchersContract();
		String test = singleTestGenerator(contractDsl);
		assertThat(test).contains(
				"assertThat(valueFromXPath(parsedXml, \"/ns1:customer/namespace::ns1\")).isEqualTo(\"http://demo.com/customer\")")
			.contains(
					"assertThat(valueFromXPath(parsedXml, \"/ns1:customer/namespace::addr\")).isEqualTo(\"http://demo.com/address\")")
			.contains(
					"assertThat(valueFromXPath(parsedXml, \"/ns1:customer/email/text()\")).isEqualTo(\"customer@test.com\")")
			.contains(
					"assertThat(valueFromXPath(parsedXml, \"/ns1:customer/*[local-name()='contact-info' and namespace-uri()='http://demo.com/contact-info']/*[local-name()='name']/text()\")).isEqualTo(\"Krombopulous\")")
			.contains(
					"assertThat(valueFromXPath(parsedXml, \"/ns1:customer/*[local-name()='contact-info' and namespace-uri()='http://demo.com/contact-info']/*[local-name()='address']/addr:gps/*[local-name()='lat']/text()\")).isEqualTo(\"51\")")
			.contains(
					"assertThat(valueFromXPath(parsedXml, \"/ns1:customer/*[local-name()='contact-info' and namespace-uri()='http://demo.com/contact-info']/*[local-name()='address']/addr:gps/addr:lon/text()\")).isEqualTo(\"50\")")
			.doesNotContain(
					"assertThat(valueFromXPath(parsedXml,\"/ns1:customer/*[local-name()='contact-info' and namespace-uri()='http://demo.com/contact-info']/@xmlns\")).isEqualTo\"");
		SyntaxChecker.tryToCompile(methodBuilderName, test);
	}

	@Test
	void shouldThrowExceptionForVerificationByType() {
		Contract contractDsl = Contract.make((contract) -> {
			contract.request((request) -> {
				request.method(request.GET());
				request.urlPath("/get");
				request.headers((headers) -> headers.contentType(headers.applicationXml()));
			});
			contract.response((response) -> {
				response.status(response.OK());
				response.headers((headers) -> headers.contentType(headers.applicationXml()));
				response.body("\n<test>\n<duck type='xtype'>123</duck>\n</test>");
				response.bodyMatchers((matchers) -> matchers.xPath("/test/duck/text()", matchers.byType()));
			});
		});
		properties.setTestMode(TestMode.MOCKMVC);
		assertThatThrownBy(() -> singleTestGenerator(contractDsl)).isInstanceOf(UnsupportedOperationException.class);
	}

}
