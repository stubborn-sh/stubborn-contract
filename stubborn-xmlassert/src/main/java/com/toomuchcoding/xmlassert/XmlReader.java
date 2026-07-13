package com.toomuchcoding.xmlassert;

/**
 * Contract to read the value from a XML basing on it.
 *
 * @author Marcin Grzejszczak
 * @since 0.0.1
 */
public interface XmlReader {

	/**
	 * Returns the value from the XML, based on the created XPath.
	 */
	String read();

}
