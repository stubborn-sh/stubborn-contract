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

package sh.stubborn.xmlassert;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Targets the {@link XmlAssertion} entry points (String cache/empty branches and the
 * {@link Document} overloads), {@link XmlCachedObjects}, {@link XPathBuilder} and the
 * {@link XmlAssertions}/{@link BDDXmlAssertions} {@link Document} entry methods.
 */
class XmlAssertionEntryTests {

	static final String XML2 = "<root><property1>a</property1><property2>b</property2></root>";

	private static Document toDocument(String xml) throws Exception {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		return factory.newDocumentBuilder().parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
	}

	@Test
	void cachedObjectsSerializesDocumentWhenNoStringGiven() throws Exception {
		Document document = toDocument("<root><a>1</a></root>");
		XmlCachedObjects cached = new XmlCachedObjects(document);
		assertThat(cached.document).isSameAs(document);
		assertThat(cached.xpathBuilder).isNotNull();
		assertThat(cached.xmlAsString).contains("<root>").contains("<a>1</a>").contains("</root>");
		assertThat(cached.xmlAsString).doesNotContain("\n").doesNotContain("\r");
		// OMIT_XML_DECLARATION is set, so the serialized form must not carry the prolog.
		assertThat(cached.xmlAsString).doesNotContain("<?xml");
	}

	@Test
	void cachedObjectsKeepsProvidedString() throws Exception {
		Document document = toDocument("<root><a>1</a></root>");
		XmlCachedObjects cached = new XmlCachedObjects(document, "explicit-xml");
		assertThat(cached.document).isSameAs(document);
		assertThat(cached.xpathBuilder).isNotNull();
		assertThat(cached.xmlAsString).isEqualTo("explicit-xml");
	}

	@Test
	void assertThatDocumentBuildsXPath() throws Exception {
		Document document = toDocument(XML2);
		XmlVerifiable verifiable = XmlAssertion.assertThat(document);
		assertThat(verifiable).isNotNull();
		assertThat(verifiable.node("root").node("property1").xPath()).isEqualTo("/root/property1");
	}

	@Test
	void assertThatXmlDocumentBuildsXPath() throws Exception {
		Document document = toDocument(XML2);
		XmlVerifiable verifiable = XmlAssertion.assertThatXml(document);
		assertThat(verifiable).isNotNull();
		assertThat(verifiable.node("root").node("property2").xPath()).isEqualTo("/root/property2");
	}

	@Test
	void xmlAssertionsDocumentEntry() throws Exception {
		Document document = toDocument(XML2);
		XPathAssert result = XmlAssertions.assertThat(document).node("root").node("property1");
		assertThat(result).isNotNull();
	}

	@Test
	void bddXmlAssertionsDocumentEntry() throws Exception {
		Document document = toDocument(XML2);
		XPathAssert result = BDDXmlAssertions.then(document).node("root").node("property2");
		assertThat(result).isNotNull();
	}

	@Test
	void bddXmlAssertionsVerifiableEntry() {
		XmlVerifiable verifiable = XmlAssertion.assertThat(XML2);
		XPathAssert result = BDDXmlAssertions.then(verifiable);
		assertThat(result).isNotNull();
	}

	@Test
	void assertThatStringParsesOnCacheMissThenReusesOnCacheHit() {
		// Deliberately unique so the FIRST call is a guaranteed cache miss: the document
		// must actually be parsed (cachedObjects == null && !empty(xml)) for the
		// following
		// matchesXPath to evaluate. If either condition in assertThat(String), or the
		// empty() predicate, is mutated, the parse is skipped and matchesXPath throws.
		String xml = "<cacheEmptyProbe><child>value</child></cacheEmptyProbe>";
		XmlAssertion.assertThat(xml).matchesXPath("/cacheEmptyProbe[child='value']"); // cache
																						// miss
																						// ->
																						// parses
		XmlAssertion.assertThat(xml).matchesXPath("/cacheEmptyProbe[child='value']"); // cache
																						// hit
																						// ->
																						// reuses
	}

	@Test
	void assertThatEmptyStringActsAsBuilder() {
		XmlVerifiable verifiable = XmlAssertion.assertThat("").node("a");
		assertThat(verifiable.xPath()).isEqualTo("/a");
	}

	@Test
	void assertThatBlankStringActsAsBuilder() {
		XmlVerifiable verifiable = XmlAssertion.assertThat("   \n  ").node("a");
		assertThat(verifiable.xPath()).isEqualTo("/a");
	}

	@Test
	void malformedXmlSurfacesRuntimeExceptionOnEvaluation() {
		// Parsing is tolerant/deferred, so building the assertion does not fail; the
		// error
		// only surfaces once an XPath is actually evaluated against the document.
		org.assertj.core.api.Assertions
			.assertThatThrownBy(
					() -> XmlAssertion.assertThat("<root><unclosed></root>").matchesXPath("/no/such/path/exists"))
			.isInstanceOf(RuntimeException.class);
	}

	@Test
	void xPathBuilderNoArgBuildsXPath() {
		String xPath = XPathBuilder.builder().node("some").node("nested").node("value").isEqualTo("x").xPath();
		assertThat(xPath).isEqualTo("/some/nested[value='x']");
	}

	@Test
	void xPathBuilderWithXmlReadsValue() {
		String value = XPathBuilder.builder(XML2).node("root").node("property1").read();
		assertThat(value).isEqualTo("a");
	}

}
