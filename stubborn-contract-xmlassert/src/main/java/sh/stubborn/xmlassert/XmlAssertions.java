package sh.stubborn.xmlassert;

import org.assertj.core.api.Assertions;
import org.w3c.dom.Document;

/**
 * Entry point for {@link Assertions}
 *
 * @author Marcin Grzejszczak
 * @since 1.0.0
 */
public class XmlAssertions extends Assertions {

	public static XPathAssert assertThat(XmlAsString actual) {
		return new XPathAssert(actual);
	}

	public static XPathAssert assertThat(Document actual) {
		return new XPathAssert(actual);
	}

	public static XPathAssert assertThat(XmlVerifiable actual) {
		return new XPathAssert(actual);
	}

}
