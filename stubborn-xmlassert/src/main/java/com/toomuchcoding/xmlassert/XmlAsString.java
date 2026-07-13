package com.toomuchcoding.xmlassert;

/**
 * Wrapper for a string version of an XML
 *
 * @author Marcin Grzejszczak
 * @since 0.0.1
 */
public class XmlAsString {

	public final String xml;

	public XmlAsString(String xml) {
		this.xml = xml;
	}

	public static XmlAsString asXml(String xml) {
		return new XmlAsString(xml);
	}

}
