package com.toomuchcoding.xmlassert;

import org.assertj.core.api.AbstractAssert;
import org.w3c.dom.Document;

/**
 * A AssertJ version of XMLAssert.
 *
 * The methods used by XMLAssert are available as assertions of either {@link XmlAsString}
 * or {@link XmlVerifiable} or {@link org.w3c.dom.Document}.
 *
 * Remember that the order of execution matters since it's building the XPath in the
 * provided sequence.
 *
 * @author Marcin Grzejszczak
 * @since 0.0.1
 */
public class XPathAssert extends AbstractAssert<XPathAssert, XmlVerifiable> {

	public XPathAssert(Document actual) {
		super(XmlAssertion.assertThatXml(actual), XPathAssert.class);
	}

	public XPathAssert(XmlAsString actual) {
		super(XmlAssertion.assertThatXml(actual.xml), XPathAssert.class);
	}

	public XPathAssert(XmlVerifiable actual) {
		super(actual, XPathAssert.class);
	}

	/**
	 * @see XmlVerifiable#node(String)
	 */
	public XPathAssert node(String nodeName) {
		isNotNull();
		return new XPathAssert(actual.node(nodeName));
	}

	/**
	 * @see XmlVerifiable#withAttribute(String, String)
	 */
	public XPathAssert withAttribute(String attribute, String attributeValue) {
		isNotNull();
		return new XPathAssert(actual.withAttribute(attribute, attributeValue));
	}

	/**
	 * @see XmlVerifiable#node(String...)
	 */
	public XPathAssert node(String... nodeNames) {
		isNotNull();
		return new XPathAssert(actual.node(nodeNames));
	}

	/**
	 * @see XmlVerifiable#array(String)
	 */
	public XPathAssert array(String value) {
		isNotNull();
		return new XPathAssert(actual.array(value));
	}

	/**
	 * @see XmlArrayVerifiable#hasSize(int)
	 */
	public XPathAssert hasSize(int size) {
		isNotNull();
		return new XPathAssert(((XmlArrayVerifiable) actual).hasSize(size));
	}

	/**
	 * @see XmlVerifiable#array(String)
	 */
	public XPathAssert contains(String value) {
		isNotNull();
		return new XPathAssert(((XmlArrayVerifiable) actual).contains(value));
	}

	/**
	 * @see XmlVerifiable#isEqualTo(String)
	 */
	public XPathAssert isEqualTo(String value) {
		isNotNull();
		XmlVerifiable xmlVerifiable = null;
		try {
			xmlVerifiable = actual.isEqualTo(value);
		}
		catch (IllegalStateException e) {
			failWithXPathMessage();
		}
		return new XPathAssert(xmlVerifiable);
	}

	private void failWithXPathMessage() {
		failWithMessage("Expected XML to match XPath <%s> but it didn't", actual.xPath());
	}

	/**
	 * @see XmlVerifiable#isEqualTo(Number)
	 */
	public XPathAssert isEqualTo(Number value) {
		isNotNull();
		XmlVerifiable xmlVerifiable = null;
		try {
			xmlVerifiable = actual.isEqualTo(value);
		}
		catch (IllegalStateException e) {
			failWithXPathMessage();
		}
		return new XPathAssert(xmlVerifiable);
	}

	/**
	 * @see XmlVerifiable#matches(String)
	 */
	public XPathAssert matches(String value) {
		isNotNull();
		XmlVerifiable xmlVerifiable = null;
		try {
			xmlVerifiable = actual.matches(value);
		}
		catch (IllegalStateException e) {
			failWithXPathMessage();
		}
		return new XPathAssert(xmlVerifiable);
	}

	/**
	 * @see XmlVerifiable#isEqualTo(Boolean)
	 */
	public XPathAssert isEqualTo(Boolean value) {
		isNotNull();
		XmlVerifiable xmlVerifiable = null;
		try {
			xmlVerifiable = actual.isEqualTo(value);
		}
		catch (IllegalStateException e) {
			failWithXPathMessage();
		}
		return new XPathAssert(xmlVerifiable);
	}

	/**
	 * @see XmlVerifiable#isNull()
	 */
	public void isNull() {
		isNotNull();
		try {
			actual.isNull();
		}
		catch (IllegalStateException e) {
			failWithXPathMessage();
		}
	}

	/**
	 * @see XmlVerifiable#matchesXPath(String)
	 */
	public XPathAssert matchesXPath(String xPath) {
		isNotNull();
		try {
			actual.matchesXPath(xPath);
		}
		catch (IllegalStateException e) {
			failWithMessage("Expected XML [%s] to match XPath <%s> but it didn't",
					((XmlAsserter) actual).cachedObjects.xmlAsString, xPath);
		}
		return this;
	}

}