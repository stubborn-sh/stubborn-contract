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

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Mutation-focused tests for {@link DOMNamespaceContext}.
 */
class DOMNamespaceContextMutationTests {

	private static DOMNamespaceContext contextFor(String xml) throws Exception {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document document = builder.parse(new InputSource(new StringReader(xml)));
		return new DOMNamespaceContext(document.getDocumentElement());
	}

	@Test
	void resolves_namespaces_declared_on_root_and_child() throws Exception {
		String xml = "<root xmlns:ns1=\"http://a\"><child xmlns:ns2=\"http://b\"/></root>";
		DOMNamespaceContext context = contextFor(xml);
		assertThat(context.getNamespaceURI("ns1")).isEqualTo("http://a");
		assertThat(context.getNamespaceURI("ns2")).isEqualTo("http://b");
		assertThat(context.getNamespaceURI("missing")).isNull();
	}

	@Test
	void getPrefix_returns_matching_prefix_or_null() throws Exception {
		String xml = "<root xmlns:ns1=\"http://a\"/>";
		DOMNamespaceContext context = contextFor(xml);
		assertThat(context.getPrefix("http://a")).isEqualTo("ns1");
		assertThat(context.getPrefix("http://unknown")).isNull();
	}

	@Test
	void getPrefixes_iterates_declared_prefixes() throws Exception {
		String xml = "<root xmlns:ns1=\"http://a\"/>";
		DOMNamespaceContext context = contextFor(xml);
		Iterator<String> prefixes = context.getPrefixes("anything");
		List<String> collected = new ArrayList<>();
		prefixes.forEachRemaining(collected::add);
		assertThat(collected).containsExactly("ns1");
	}

	@Test
	void document_without_namespaces_resolves_nothing() throws Exception {
		String xml = "<root><child/></root>";
		DOMNamespaceContext context = contextFor(xml);
		assertThat(context.getNamespaceURI("ns1")).isNull();
		assertThat(context.getPrefix("http://a")).isNull();
		assertThat(context.getPrefixes("x").hasNext()).isFalse();
	}

}
