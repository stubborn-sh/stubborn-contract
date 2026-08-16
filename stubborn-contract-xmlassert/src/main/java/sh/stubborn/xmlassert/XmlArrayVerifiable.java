package sh.stubborn.xmlassert;

/**
 * Contract to match an array in a parsed XML via XPath
 *
 * @author Marcin Grzejszczak
 * @since 1.0.0
 */
public interface XmlArrayVerifiable extends XmlVerifiable {

	/**
	 * When you want to assert a node with a name in an array.
	 */
	XmlArrayVerifiable contains(String nodeName);

	/**
	 * When you want to assert if an array is of given size
	 * @since 1.0.0
	 */
	XmlArrayVerifiable hasSize(int size);

}
