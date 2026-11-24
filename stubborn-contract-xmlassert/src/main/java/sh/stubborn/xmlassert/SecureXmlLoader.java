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

import java.io.InputStream;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXParseException;

/**
 * Loads XML into the PSVI-enabled Xerces DOM the XPath 2 engine works on, with the same
 * settings as PsychoPath's {@code XercesLoader} plus protection against XML External
 * Entity (XXE) attacks. {@code XercesLoader} only switches off loading of an external
 * DTD; an internal-subset {@code <!ENTITY x SYSTEM "file:...">} was still resolved, so
 * asserting on an untrusted response body could read local files or make network calls.
 *
 * @author Marcin Grzejszczak
 * @since 0.2.5
 */
final class SecureXmlLoader {

	private SecureXmlLoader() {
	}

	static Document load(InputStream inputStream) throws Exception {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		factory.setValidating(false);
		factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
		factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
		factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
		factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
		factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
		factory.setXIncludeAware(false);
		factory.setExpandEntityReferences(false);
		// Same DOM settings as XercesLoader, so the XPath 2 engine sees the same tree.
		factory.setAttribute("http://apache.org/xml/features/validation/schema", Boolean.FALSE);
		factory.setAttribute("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", Boolean.FALSE);
		factory.setAttribute("http://apache.org/xml/properties/dom/document-class-name",
				"org.apache.xerces.dom.PSVIDocumentImpl");
		DocumentBuilder builder = factory.newDocumentBuilder();
		builder.setErrorHandler(new ErrorHandler() {
			@Override
			public void warning(SAXParseException exception) {
			}

			@Override
			public void error(SAXParseException exception) throws SAXParseException {
				throw exception;
			}

			@Override
			public void fatalError(SAXParseException exception) throws SAXParseException {
				throw exception;
			}
		});
		return builder.parse(inputStream);
	}

}
