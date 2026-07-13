package com.toomuchcoding.xmlassert;

/**
 * Builder of XPaths.
 *
 * @author Marcin Grzejszczak
 * @since 0.0.1
 * @see XmlVerifiable
 * @see XmlAssertion
 */
public class XPathBuilder {

	/**
	 * Returns a builder of {@link XmlVerifiable} with which you can build your XPath.
	 * Once finished just call {@link XmlVerifiable#xPath()} to get XPath as String.
	 */
	public static XmlVerifiable builder() {
		return XmlAssertion.assertThat("").withoutThrowingException();
	}

	/**
	 * Using a XPath builder for the given XML you can read its value.
	 */
	public static XmlVerifiable builder(String xml) {
		return XmlAssertion.assertThat(xml).withoutThrowingException();
	}

}
