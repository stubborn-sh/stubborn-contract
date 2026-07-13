package com.toomuchcoding.xmlassert;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.wst.xml.xpath2.processor.DOMLoader;
import org.eclipse.wst.xml.xpath2.processor.XercesLoader;
import org.w3c.dom.Document;

/**
 * Entry point for assertions. Use the static factory method and you're ready to go!
 *
 * @author Marcin Grzejszczak
 * @since 0.0.1
 * @see XmlVerifiable
 */
public class XmlAssertion {

	private final XmlCachedObjects cachedObjects;

	private final LinkedList<String> xPathBuffer = new LinkedList<String>();

	private final LinkedList<String> specialCaseXPathBuffer = new LinkedList<String>();

	private final XmlAsserterConfiguration xmlAsserterConfiguration = new XmlAsserterConfiguration();

	private static final Map<String, XmlCachedObjects> CACHE = new ConcurrentHashMap<String, XmlCachedObjects>();

	private XmlAssertion(Document parsedXml) {
		this.cachedObjects = new XmlCachedObjects(parsedXml);
	}

	private XmlAssertion(String xml) {
		XmlCachedObjects cachedObjects = CACHE.get(xml);
		if (cachedObjects == null && !empty(xml)) {
			try {
				InputStream inputXml = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
				DOMLoader loader = new XercesLoader();
				Document document = loader.load(inputXml);
				cachedObjects = new XmlCachedObjects(document, xml);
			}
			catch (Exception e) {
				throw new IllegalStateException("Exception occurred while trying to parse the XML", e);
			}
			CACHE.put(xml, cachedObjects);
		}
		this.cachedObjects = cachedObjects;
	}

	private boolean empty(String text) {
		return text == null || text.length() == 0 || text.matches("^\\s*$");
	}

	/**
	 * Starts assertions for the XML provided as {@link String}
	 */
	public static XmlVerifiable assertThat(String xml) {
		return new XmlAssertion(xml).root();
	}

	/**
	 * Starts assertions for the XML provided as {@link Document}
	 */
	public static XmlVerifiable assertThat(Document parsedXml) {
		return new XmlAssertion(parsedXml).root();
	}

	/**
	 * Helper method so that there are no clashes with other static methods of that name
	 *
	 * @see XmlAssertion#assertThat(String)
	 */
	public static XmlVerifiable assertThatXml(String body) {
		return assertThat(body);
	}

	/**
	 * Helper method so that there are no clashes with other static methods of that name
	 *
	 * @see XmlAssertion#assertThat(Document)
	 */
	public static XmlVerifiable assertThatXml(Document parsedXml) {
		return assertThat(parsedXml);
	}

	private XmlVerifiable root() {
		return new FieldAssertion(cachedObjects, xPathBuffer, specialCaseXPathBuffer, "", xmlAsserterConfiguration)
			.node("");
	}

}
