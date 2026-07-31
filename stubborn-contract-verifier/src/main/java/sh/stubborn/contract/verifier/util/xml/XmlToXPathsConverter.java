/*
 * Copyright 2018-present the original author or authors.
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

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.IntFunction;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import sh.stubborn.contract.spec.internal.BodyMatcher;
import sh.stubborn.contract.spec.internal.BodyMatchers;
import sh.stubborn.contract.spec.internal.MatchingType;
import sh.stubborn.contract.spec.internal.MatchingTypeValue;
import sh.stubborn.contract.spec.internal.PathBodyMatcher;

import static java.util.stream.Collectors.toList;
import static javax.xml.xpath.XPathConstants.NODESET;

/**
 * @author Olga Maciaszek-Sharma
 * @author Chris Bono
 * @since 2.1.0
 */
public class XmlToXPathsConverter {

	public static Object removeMatchingXPaths(Object body, @Nullable BodyMatchers bodyMatchers) {
		try {
			XPath xPath = XPathFactory.newInstance().newXPath();
			DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
			builderFactory.setNamespaceAware(true);
			DocumentBuilder documentBuilder = builderFactory.newDocumentBuilder();
			Document parsedXml = documentBuilder.parse(new InputSource(new StringReader(body.toString())));
			Node documentElement = parsedXml.getDocumentElement();
			xPath.setNamespaceContext(new DOMNamespaceContext(documentElement));
			// Groovy: bodyMatchers?.matchers()?.each { ... } — skip when either is null.
			List<BodyMatcher> matchers = (bodyMatchers != null) ? bodyMatchers.matchers() : null;
			if (matchers != null) {
				for (BodyMatcher matcher : matchers) {
					NodeList nodes = (NodeList) xPath.evaluate(matcher.path(), documentElement, NODESET);
					for (int i = 0; i < nodes.getLength(); i++) {
						removeNode(nodes.item(i));
					}
				}
			}
			parsedXml.normalizeDocument();
			return xmlToString(parsedXml);
		}
		catch (ParserConfigurationException | SAXException | IOException | XPathExpressionException ex) {
			throw sneakyThrow(ex);
		}
	}

	public static String retrieveValue(BodyMatcher matcher, Object body) {
		Object value = matcher.value();
		// Groovy `!matcher.value()` used Groovy truthiness (null / empty / zero / false).
		if (matcher.matchingType() == MatchingType.EQUALITY || isFalsy(value)) {
			return retrieveValueFromBody(matcher.path(), body);
		}
		return Objects.requireNonNull(value).toString();
	}

	/**
	 * Reproduces Groovy truthiness for the values a {@link BodyMatcher} may hold: null,
	 * an empty {@link CharSequence}/{@link Collection}/{@link Map}, a zero
	 * {@link Number}, or {@link Boolean#FALSE} are all "falsy".
	 */
	private static boolean isFalsy(@Nullable Object value) {
		if (value == null) {
			return true;
		}
		if (value instanceof CharSequence text) {
			return text.length() == 0;
		}
		if (value instanceof Collection<?> collection) {
			return collection.isEmpty();
		}
		if (value instanceof Map<?, ?> map) {
			return map.isEmpty();
		}
		if (value instanceof Number number) {
			return number.doubleValue() == 0;
		}
		if (value instanceof Boolean bool) {
			return !bool;
		}
		return false;
	}

	public static String retrieveValueFromBody(String path, Object body) {
		return getNodeValue(path, body);
	}

	private static String getNodeValue(String path, Object body) {
		try {
			XPath xPath = XPathFactory.newInstance().newXPath();
			DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
			builderFactory.setNamespaceAware(true);
			DocumentBuilder documentBuilder = builderFactory.newDocumentBuilder();
			Document parsedXml = documentBuilder.parse(new InputSource(new StringReader(body.toString())));
			Node documentElement = parsedXml.getDocumentElement();
			xPath.setNamespaceContext(new DOMNamespaceContext(documentElement));
			return xPath.evaluate(path, documentElement);
		}
		catch (ParserConfigurationException | SAXException | IOException | XPathExpressionException ex) {
			throw sneakyThrow(ex);
		}
	}

	private static void removeNode(@Nullable Node node) {
		if (node != null) {
			if (isValueNode(node)) {
				Node parentNode = node.getParentNode();
				if (parentNode != null) {
					parentNode.removeChild(node);
				}
			}
			else {
				removeNode(node.getParentNode());
			}
		}
	}

	private static boolean isValueNode(Node node) {
		short nodeType = node.getNodeType();
		return nodeType == Node.TEXT_NODE || nodeType == Node.CDATA_SECTION_NODE || nodeType == Node.COMMENT_NODE
				|| nodeType == Node.DOCUMENT_TYPE_NODE || nodeType == Node.PROCESSING_INSTRUCTION_NODE
				|| nodeType == Node.NOTATION_NODE;
	}

	private static boolean isAttributeNode(Node node) {
		return Node.ATTRIBUTE_NODE == node.getNodeType();
	}

	private static String xmlToString(Node parsedXml) {
		try {
			Transformer transformer = TransformerFactory.newInstance().newTransformer();
			StringWriter writer = new StringWriter();
			StreamResult result = new StreamResult(writer);
			transformer.transform(new DOMSource(parsedXml), result);
			return writer.toString();
		}
		catch (TransformerException ex) {
			throw sneakyThrow(ex);
		}
	}

	public static List<BodyMatcher> mapToMatchers(Object xml) {
		try {
			DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
			builderFactory.setNamespaceAware(true);
			DocumentBuilder documentBuilder = builderFactory.newDocumentBuilder();
			Document parsedXml = documentBuilder.parse(new InputSource(new StringReader(xml.toString())));
			List<List<Node>> valueNodes = getValueNodesWithParents(parsedXml);
			List<BodyMatcher> matchers = new ArrayList<>();
			List<NodePath> valueNodePaths = transformListEntries(valueNodes);
			for (NodePath nodePath : valueNodePaths) {
				if (!isPathToDefaultXmlnsAttributeNode(nodePath)) {
					matchers.add(new PathBodyMatcher(buildXPath(nodePath.fromChildToParents(), nodePath.index),
							new MatchingTypeValue(MatchingType.EQUALITY, nodePath.path.get(0).getNodeValue())));
				}
			}
			return matchers;
		}
		catch (ParserConfigurationException | SAXException | IOException ex) {
			throw sneakyThrow(ex);
		}
	}

	public static boolean isPathToDefaultXmlnsAttributeNode(@Nullable NodePath nodePath) {
		if (nodePath == null) {
			return false;
		}
		Node node = nodePath.path.get(0);
		return isAttributeNode(node) && "xmlns".equals(node.getNodeName());
	}

	public static List<NodePath> transformListEntries(List<List<Node>> nodeLists) {
		List<PathOccurrenceCounter> pathOccurrenceCounters = new ArrayList<>();
		List<NodePath> nodePaths = new ArrayList<>();
		for (List<Node> nodeList : nodeLists) {
			List<Node> parentNodesList = nodeList.subList(1, nodeList.size());
			int elementIndex = pathOccurrenceCounters.stream()
				.filter(counter -> nodeNames(counter.path).equals(nodeNames(parentNodesList)))
				.findFirst()
				.map(counter -> ++counter.counter)
				.orElseGet(() -> {
					PathOccurrenceCounter pathCounter = new PathOccurrenceCounter(parentNodesList);
					pathOccurrenceCounters.add(pathCounter);
					return pathCounter.counter;
				});
			nodePaths.add(new NodePath(nodeList, elementIndex));
		}
		return nodePaths;
	}

	private static List<String> nodeNames(List<Node> nodes) {
		return nodes.stream().map(Node::getNodeName).collect(toList());
	}

	public static String buildXPath(@Nullable List<Node> nodes) {
		return buildXPath(nodes, 1);
	}

	public static String buildXPath(@Nullable List<Node> nodes, int index) {
		XmlVerifiable xmlVerifiable = XPathBuilder.builder();
		// Groovy `if (!nodes)` was truthy-false for both null and empty.
		if (nodes == null || nodes.isEmpty()) {
			return xmlVerifiable.xPath();
		}
		for (Node node : nodes.subList(0, nodes.size() - 1)) {
			if (node instanceof Attr attribute) {
				xmlVerifiable = processNode(xmlVerifiable, attribute);
			}
			else {
				xmlVerifiable = processNode(xmlVerifiable, node);
			}
		}
		Node closingNode = nodes.get(nodes.size() - 1);
		if (closingNode instanceof Attr attribute) {
			xmlVerifiable = processClosingNode(xmlVerifiable, attribute, index);
		}
		else {
			xmlVerifiable = processClosingNode(xmlVerifiable, closingNode, index);
		}
		return xmlVerifiable.xPath();
	}

	private static XmlVerifiable processNode(XmlVerifiable xmlVerifiable, Node node) {
		// If node has explicit namespace (eg '<prefix:name>') no special processing
		// needed
		if (nodeUsesExplicitNamespace(node)) {
			return xmlVerifiable.node(node.getNodeName());
		}
		// If node directly declares default ns (eg. 'xmlns=<namespace_uri>') then use
		// local
		// name and namespace uri syntax
		String defaultXmlns = getDefaultXmlnsDeclarationOnNodeIfExists(node);
		if (defaultXmlns != null) {
			return xmlVerifiable.nodeWithDefaultNamespace(node.getNodeName(), defaultXmlns);
		}
		// If node indirectly declares (via ancestor) default ns then use local name
		// syntax
		if (getDefaultXmlnsDeclarationOnAncestorsIfExists(node) != null) {
			return xmlVerifiable.nodeWithDefaultNamespace(node.getNodeName(), "");
		}
		return xmlVerifiable.node(node.getNodeName());
	}

	private static boolean nodeUsesExplicitNamespace(Node node) {
		String nodeName = node.getNodeName();
		return nodeName != null && nodeName.contains(":");
	}

	private static @Nullable String getDefaultXmlnsDeclarationOnNodeIfExists(Node node) {
		NamedNodeMap attributes = node.getAttributes();
		if (attributes == null) {
			return null;
		}
		Node namedItem = attributes.getNamedItem("xmlns");
		if (namedItem == null) {
			return null;
		}
		return namedItem.getTextContent();
	}

	private static @Nullable String getDefaultXmlnsDeclarationOnAncestorsIfExists(Node node) {
		Node current = node.getParentNode();
		while (current != null) {
			String defaultXmlns = getDefaultXmlnsDeclarationOnNodeIfExists(current);
			if (defaultXmlns != null) {
				return defaultXmlns;
			}
			current = current.getParentNode();
		}
		return null;
	}

	private static XmlVerifiable processNode(XmlVerifiable xmlVerifiable, Attr attribute) {
		return xmlVerifiable.withAttribute(attribute.getNodeName());
	}

	private static XmlVerifiable processClosingNode(XmlVerifiable xmlVerifiable, Node node, int index) {
		return index != 1 ? xmlVerifiable.index(index).text() : xmlVerifiable.text();
	}

	private static XmlVerifiable processClosingNode(XmlVerifiable xmlVerifiable, Attr attribute, int index) {
		if (index != 1) {
			xmlVerifiable.index(index);
		}
		return processNode(xmlVerifiable, attribute);
	}

	private static List<List<Node>> getValueNodesWithParents(Node node) {
		List<List<Node>> valueNodes = new ArrayList<>();
		List<Node> attributes = new ArrayList<>();
		addValueNodes(node, valueNodes, attributes);
		for (Node attribute : attributes) {
			if (attribute instanceof Attr attr) {
				valueNodes.add(withParents(attr));
			}
			else {
				valueNodes.add(withParents(attribute));
			}
		}
		return valueNodes;
	}

	private static void addValueNodes(Node node, List<List<Node>> valueNodes, List<Node> attributes) {
		for (Node child : getChildNodesAsList(node)) {
			attributes.addAll(getAttributesAsList(child));
			if (isValueNode(child) && !StringUtils.isBlank(child.getNodeValue())) {
				valueNodes.add(withParents(child));
			}
			else {
				addValueNodes(child, valueNodes, attributes);
			}
		}
	}

	private static List<Node> getChildNodesAsList(Node node) {
		NodeList nodeList = node.getChildNodes();
		return getNodeCollectionElements(nodeList);
	}

	private static List<Node> getAttributesAsList(Node node) {
		NamedNodeMap nodeMap = node.getAttributes();
		return nodeMap != null ? getNodeCollectionElements(nodeMap) : new ArrayList<>();
	}

	private static List<Node> getNodeCollectionElements(NodeList nodeCollection) {
		return collectNodes(nodeCollection.getLength(), nodeCollection::item);
	}

	private static List<Node> getNodeCollectionElements(NamedNodeMap nodeCollection) {
		return collectNodes(nodeCollection.getLength(), nodeCollection::item);
	}

	private static List<Node> collectNodes(int length, IntFunction<@Nullable Node> itemFunction) {
		List<Node> nodes = new ArrayList<>();
		for (int i = 0; i < length; i++) {
			Node node = itemFunction.apply(i);
			if (node != null) {
				nodes.add(node);
			}
		}
		return nodes;
	}

	private static List<Node> withParents(Node node) {
		List<Node> nodeList = new ArrayList<>();
		nodeList.add(node);
		return addParents(node, nodeList);
	}

	private static List<Node> withParents(Attr attribute) {
		List<Node> nodeList = new ArrayList<>();
		nodeList.add(attribute);
		Node ownerNode = attribute.getOwnerElement();
		nodeList.add(ownerNode);
		return addParents(ownerNode, nodeList);
	}

	private static List<Node> addParents(Node node, List<Node> nodeList) {
		Node parentNode = node.getParentNode();
		if (parentNode != null && Node.DOCUMENT_NODE != parentNode.getNodeType()) {
			nodeList.add(parentNode);
			return addParents(parentNode, nodeList);
		}
		return nodeList;
	}

	/**
	 * Rethrows a checked exception without wrapping or declaring it, preserving the
	 * original type for callers — matching the Groovy behaviour this class was ported
	 * from (Groovy propagates checked exceptions unchecked).
	 */
	@SuppressWarnings("unchecked")
	private static <T extends Throwable> RuntimeException sneakyThrow(Throwable ex) throws T {
		throw (T) ex;
	}

	private static final class NodePath {

		private final List<Node> path;

		private final int index;

		NodePath(List<Node> path, int index) {
			this.path = path;
			this.index = index;
		}

		List<Node> fromChildToParents() {
			List<Node> reversed = new ArrayList<>(this.path);
			Collections.reverse(reversed);
			return reversed;
		}

	}

	private static final class PathOccurrenceCounter {

		private final List<Node> path;

		private int counter;

		PathOccurrenceCounter(List<Node> path) {
			this.path = path;
			this.counter = 1;
		}

	}

}
