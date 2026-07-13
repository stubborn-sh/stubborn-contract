package com.toomuchcoding.xmlassert;

import org.assertj.core.api.BDDAssertions;
import org.w3c.dom.Document;

/**
 * Entry point for {@link BDDAssertions}
 *
 * @author Marcin Grzejszczak
 * @since 0.0.1
 */
public class BDDXmlAssertions extends BDDAssertions {

	public static XPathAssert then(XmlAsString actual) {
		return new XPathAssert(actual);
	}

	public static XPathAssert then(Document actual) {
		return new XPathAssert(actual);
	}

	public static XPathAssert then(XmlVerifiable actual) {
		return new XPathAssert(actual);
	}

}
