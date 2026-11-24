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

package sh.stubborn.contract.verifier.util.xml;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Parsing an XML string must not resolve external entities (XXE).
 */
class XmlAssertionXxeTests {

	@Test
	void should_reject_doctype_with_external_entity(@TempDir Path dir) throws IOException {
		Path secret = Files.writeString(dir.resolve("secret.txt"), "SECRET-XXE-MARKER");
		String xxe = "<?xml version=\"1.0\"?><!DOCTYPE foo [<!ENTITY xxe SYSTEM \"" + secret.toUri() + "\">]>"
				+ "<root><foo>&xxe;</foo></root>";

		assertThatThrownBy(() -> XmlAssertion.assertThat(xxe).node("root").node("foo").isEqualTo("SECRET-XXE-MARKER"))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("parse the XML");
	}

	@Test
	void should_parse_plain_xml_and_assert_on_it() {
		String xml = "<root><foo>bar</foo></root>";

		XmlAssertion.assertThat(xml).node("root").node("foo").isEqualTo("bar");
		assertThatThrownBy(() -> XmlAssertion.assertThat(xml).node("root").node("foo").isEqualTo("baz"))
			.isInstanceOf(IllegalStateException.class);
	}

	@Test
	void should_reject_malformed_xml() {
		assertThatThrownBy(() -> XmlAssertion.assertThat("<root><foo></root>"))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("parse the XML");
	}

	@Test
	void should_assert_on_an_already_parsed_document() throws Exception {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		Document document = factory.newDocumentBuilder()
			.parse(new InputSource(new StringReader("<root><foo>bar</foo></root>")));

		XmlAssertion.assertThat(document).node("root").node("foo").isEqualTo("bar");
		XmlAssertion.assertThatXml(document).node("root").node("foo").isEqualTo("bar");
		assertThatThrownBy(() -> XmlAssertion.assertThatXml(document).node("root").node("foo").isEqualTo("baz"))
			.isInstanceOf(IllegalStateException.class);
	}

	@Test
	void secure_loader_builds_a_namespace_aware_document() throws Exception {
		Document document = SecureXmlLoader.load(new ByteArrayInputStream(
				"<n:root xmlns:n=\"urn:x\"><n:foo>bar</n:foo></n:root>".getBytes(StandardCharsets.UTF_8)));

		assertThat(document.getDocumentElement().getLocalName()).isEqualTo("root");
		assertThat(document.getDocumentElement().getNamespaceURI()).isEqualTo("urn:x");
	}

}
