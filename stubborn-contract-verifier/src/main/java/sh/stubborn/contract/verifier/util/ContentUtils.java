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

package sh.stubborn.contract.verifier.util;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;

import groovy.lang.Closure;
import groovy.lang.GString;
import groovy.xml.XmlSlurper;
import org.apache.commons.text.StringEscapeUtils;
import org.codehaus.groovy.runtime.GStringImpl;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import sh.stubborn.contract.spec.internal.DslProperty;
import sh.stubborn.contract.spec.internal.ExecutionProperty;
import sh.stubborn.contract.spec.internal.FromFileProperty;
import sh.stubborn.contract.spec.internal.Header;
import sh.stubborn.contract.spec.internal.Headers;
import sh.stubborn.contract.spec.internal.MatchingStrategy;
import sh.stubborn.contract.spec.internal.NamedProperty;
import sh.stubborn.contract.spec.internal.OptionalProperty;
import sh.stubborn.contract.verifier.template.HandlebarsTemplateProcessor;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * A utility class that can operate on a message body basing on the provided Content Type.
 *
 * @author Marcin Grzejszczak
 * @author Olga Maciaszek-Sharma
 * @author Konstantin Shevchuk
 * @since 1.0.0
 */
public class ContentUtils {

	private static final Logger log = LoggerFactory.getLogger(ContentUtils.class);

	public static final Closure GET_STUB_SIDE = new Closure<Object>(null) {
		public @Nullable Object doCall(Object it) {
			return (it instanceof DslProperty) ? ((DslProperty) it).getClientValue() : it;
		}
	};

	public static final Closure GET_TEST_SIDE = new Closure<Object>(null) {
		public @Nullable Object doCall(Object it) {
			return (it instanceof DslProperty) ? ((DslProperty) it).getServerValue() : it;
		}
	};

	public static final Function GET_STUB_SIDE_FUNCTION = new Function() {
		@Override
		public Object apply(Object it) {
			return GET_STUB_SIDE.call(it);
		}
	};

	public static final Function GET_TEST_SIDE_FUNCTION = new Function() {
		@Override
		public Object apply(Object it) {
			return GET_TEST_SIDE.call(it);
		}
	};

	private static final Pattern TEMPORARY_PATTERN_HOLDER = Pattern.compile(".*REGEXP>>(.*)<<.*");

	private static final Pattern TEMPORARY_EXECUTION_PATTERN_HOLDER = Pattern.compile("[\"]?EXECUTION>>(.*)<<[\"]?");

	private static final Pattern TEMPORARY_OPTIONAL_PATTERN_HOLDER = Pattern.compile("OPTIONAL>>(.*)<<");

	private static final String JSON_VALUE_PATTERN_FOR_REGEX = "REGEXP>>%s<<";

	private static final String JSON_VALUE_PATTERN_FOR_OPTIONAL = "OPTIONAL>>%s<<";

	private static final String JSON_VALUE_PATTERN_FOR_EXECUTION = "\"EXECUTION>>%s<<\"";

	private ContentUtils() {
	}

	/**
	 * Due to the fact that we allow users to have a body with GString and different
	 * values inside we need to be prepared that they pass regexps around both on client
	 * and server side.
	 *
	 * In order to preserve the original JSON structure we need to convert the passed
	 * Regex patterns to a temporary string, then convert all to a legitimate JSON
	 * structure and then finally convert it back from string to a pattern.
	 * @param bodyAsValue - GString with passed values
	 * @param contentType - the content type of the body
	 * @param valueProvider - provider of values either for server or client side
	 * @return the JSON structure with replaced client / server side parts
	 */
	public static Object extractValue(GString bodyAsValue, @Nullable ContentType contentType, Closure valueProvider) {
		String asString = bodyAsValue.toString();
		if (asString == null || asString.isBlank()) {
			return bodyAsValue;
		}
		if (contentType == ContentType.TEXT || contentType == ContentType.FORM) {
			return extractValueForText(bodyAsValue, valueProvider);
		}
		if (ContentType.JSON == contentType) {
			return extractValueForJSON(bodyAsValue, valueProvider);
		}
		if (contentType == ContentType.XML) {
			return extractValueForXML(bodyAsValue, valueProvider);
		}
		// else Brute force :(
		try {
			log.trace("No content type provided so trying to parse as JSON");
			return extractValueForJSON(bodyAsValue, valueProvider);
		}
		catch (JacksonException ex) {
			// Not a JSON format
			log.trace("Failed to parse as JSON - trying to parse as XML", ex);
			try {
				return extractValueForXML(bodyAsValue, valueProvider);
			}
			catch (Exception exception) {
				log.trace("No content type provided and failed to parse as XML - returning the value back to the user",
						exception);
				return extractValueForGString(bodyAsValue, valueProvider);
			}
		}
	}

	public static Object extractValue(GString bodyAsValue, @Nullable ContentType contentType, Function valueProvider) {
		return extractValue(bodyAsValue, contentType, toClosure(valueProvider));
	}

	public static ContentType getClientContentType(GString bodyAsValue) {
		try {
			extractValueForJSON(bodyAsValue, GET_STUB_SIDE);
			return ContentType.JSON;
		}
		catch (JacksonException ex) {
			try {
				getXmlSlurperWithDefaultErrorHandler()
					.parseText(extractValueForXML(bodyAsValue, GET_STUB_SIDE).toString());
				return ContentType.XML;
			}
			catch (Exception ignored) {
				extractValueForGString(bodyAsValue, GET_STUB_SIDE);
				return ContentType.UNKNOWN;
			}
		}
	}

	public static ContentType getClientContentType(String bodyAsValue) {
		try {
			JsonSlurperCompatibility.parse(bodyAsValue);
			return ContentType.JSON;
		}
		catch (JacksonException ex) {
			try {
				getXmlSlurperWithDefaultErrorHandler().parseText(bodyAsValue);
				return ContentType.XML;
			}
			catch (Exception ignored) {
				return ContentType.UNKNOWN;
			}
		}
	}

	public static ContentType getClientContentType(Object bodyAsValue) {
		if (bodyAsValue instanceof GString) {
			return getClientContentType((GString) bodyAsValue);
		}
		else if (bodyAsValue instanceof String) {
			return getClientContentType((String) bodyAsValue);
		}
		else if (bodyAsValue instanceof Map) {
			return getClientContentType((Map) bodyAsValue);
		}
		else if (bodyAsValue instanceof List) {
			return getClientContentType((List) bodyAsValue);
		}
		else if (bodyAsValue instanceof MatchingStrategy) {
			return ContentType.UNKNOWN;
		}
		else if (bodyAsValue instanceof FromFileProperty) {
			return ContentType.UNKNOWN;
		}
		return tryToGuessContentType(bodyAsValue);
	}

	private static ContentType tryToGuessContentType(Object bodyAsValue) {
		try {
			if (log.isDebugEnabled()) {
				log.debug("No content type passed, will try to guess the type of payload");
			}
			return getClientContentType(new JsonMapper().writeValueAsString(bodyAsValue));
		}
		catch (Exception ignored) {
			if (log.isTraceEnabled()) {
				log.trace("Failed to assume that body [" + bodyAsValue + "] is json");
			}
		}
		return ContentType.UNKNOWN;
	}

	public static ContentType getClientContentType(Object bodyAsValue, Headers headers) {
		ContentType contentType = recognizeContentTypeFromHeader(headers);
		if (contentType == ContentType.UNKNOWN) {
			return getClientContentType(bodyAsValue);
		}
		return contentType;
	}

	public static ContentType getClientContentType(Map bodyAsValue) {
		try {
			new JsonMapper().writeValueAsString(bodyAsValue);
			return ContentType.JSON;
		}
		catch (Exception ignore) {
			return ContentType.UNKNOWN;
		}
	}

	public static ContentType getClientContentType(List bodyAsValue) {
		try {
			new JsonMapper().writeValueAsString(bodyAsValue);
			return ContentType.JSON;
		}
		catch (Exception ignore) {
			return ContentType.UNKNOWN;
		}
	}

	public static GStringImpl extractValueForGString(GString bodyAsValue, Closure valueProvider) {
		Object[] values = bodyAsValue.getValues();
		@Nullable String[] transformed = new @Nullable String[values.length];
		for (int i = 0; i < values.length; i++) {
			Object it = values[i];
			Object result = (it instanceof DslProperty) ? valueProvider.call(it) : it;
			transformed[i] = (result != null) ? result.toString() : null;
		}
		return new GStringImpl(transformed, (String[]) CloneUtils.clone(bodyAsValue.getStrings()));
	}

	public static Object extractValue(GString bodyAsValue, Function valueProvider) {
		return extractValue(bodyAsValue, ContentType.UNKNOWN, toClosure(valueProvider));
	}

	public static Object extractValue(GString bodyAsValue, Closure valueProvider) {
		return extractValue(bodyAsValue, ContentType.UNKNOWN, valueProvider);
	}

	private static String extractValueForText(GString bodyAsValue, Closure valueProvider) {
		Object[] values = bodyAsValue.getValues();
		@Nullable String[] transformed = new @Nullable String[values.length];
		for (int i = 0; i < values.length; i++) {
			Object result = valueProvider.call(values[i]);
			transformed[i] = (result != null) ? result.toString() : null;
		}
		GString transformedString = new GStringImpl(transformed, (String[]) CloneUtils.clone(bodyAsValue.getStrings()));
		return transformedString.toString();
	}

	private static Object extractValueForJSON(GString bodyAsValue, Closure valueProvider) {
		Object[] values = bodyAsValue.getValues();
		@Nullable String[] transformed = new @Nullable String[values.length];
		for (int i = 0; i < values.length; i++) {
			Object result = transformJSONStringValue(values[i], valueProvider);
			transformed[i] = (result != null) ? result.toString() : null;
		}
		GString transformedString = new GStringImpl(transformed, (String[]) CloneUtils.clone(bodyAsValue.getStrings()));
		Object parsedJson = JsonSlurperCompatibility.parse(transformedString.toString().replace("\\", "\\\\"));
		return convertAllTemporaryRegexPlaceholdersBackToPatterns(parsedJson);
	}

	private static GStringImpl extractValueForXML(GString bodyAsValue, Closure valueProvider) {
		Object[] values = bodyAsValue.getValues();
		@Nullable String[] transformed = new @Nullable String[values.length];
		for (int i = 0; i < values.length; i++) {
			transformed[i] = transformXMLStringValue(values[i], valueProvider);
		}
		GStringImpl impl = new GStringImpl(transformed, (String[]) CloneUtils.clone(bodyAsValue.getStrings()));
		// try to convert it to XML
		try {
			getXmlSlurperWithDefaultErrorHandler().parseText(impl.toString());
		}
		catch (IOException | SAXException ex) {
			throw new IllegalStateException(ex);
		}
		return impl;
	}

	protected static Object transformJSONStringValue(Object obj, Closure valueProvider) {
		if (obj instanceof DslProperty) {
			return transformJSONStringValue((DslProperty) obj, valueProvider);
		}
		else if (obj instanceof Pattern) {
			return transformJSONStringValue((Pattern) obj, valueProvider);
		}
		else if (obj instanceof OptionalProperty) {
			return transformJSONStringValue((OptionalProperty) obj, valueProvider);
		}
		else if (obj instanceof ExecutionProperty) {
			return transformJSONStringValue((ExecutionProperty) obj, valueProvider);
		}
		return obj;
	}

	protected static Object transformJSONStringValue(DslProperty dslProperty, Closure valueProvider) {
		return transformJSONStringValue(valueProvider.call(dslProperty), valueProvider);
	}

	protected static Object transformJSONStringValue(Pattern pattern, Closure valueProvider) {
		return String.format(JSON_VALUE_PATTERN_FOR_REGEX, pattern.pattern());
	}

	protected static Object transformJSONStringValue(OptionalProperty optional, Closure valueProvider) {
		return String.format(JSON_VALUE_PATTERN_FOR_OPTIONAL, optional.value());
	}

	protected static Object transformJSONStringValue(ExecutionProperty property, Closure valueProvider) {
		return String.format(JSON_VALUE_PATTERN_FOR_EXECUTION, property.getExecutionCommand());
	}

	private static @Nullable String transformXMLStringValue(Object obj, Closure valueProvider) {
		if (obj instanceof DslProperty) {
			// Mirror the original Groovy runtime-dispatch behaviour: resolve the
			// dynamic value through the provider and XML-escape the result, rather
			// than applying the JSON placeholder transformation.
			return transformXMLStringValue(valueProvider.call(obj), valueProvider);
		}
		return StringEscapeUtils.escapeXml11(StringEscapeUtils.unescapeXml(obj.toString()));
	}

	protected static Object convertDslPropsToTemporaryRegexPatterns(Object parsedJson) {
		return convertDslPropsToTemporaryRegexPatterns(parsedJson, MapConverter.JSON_PARSING_CLOSURE);
	}

	protected static Object convertDslPropsToTemporaryRegexPatterns(Object parsedJson, Closure parsingClosure) {
		return MapConverter.transformValues(parsedJson, (value) -> transformJSONStringValue(value, GET_TEST_SIDE),
				(str) -> parsingClosure.call(str));
	}

	protected static Object convertDslPropsToTemporaryRegexPatterns(Object parsedJson, Function parsingFunction) {
		return MapConverter.transformValues(parsedJson, (value) -> transformJSONStringValue(value, GET_TEST_SIDE),
				parsingFunction);
	}

	private static Object convertAllTemporaryRegexPlaceholdersBackToPatterns(Object parsedJson) {
		return MapConverter.transformValues(parsedJson, (value) -> {
			if (value instanceof String) {
				String string = (String) value;
				return returnParsedObject(string);
			}
			return value;
		});
	}

	/**
	 * <p>
	 * If you wonder why there is val[1] without null-check then take a look at this.
	 * </p>
	 * <p>
	 * Example:
	 * </p>
	 * <p>
	 * Our string equals: {@code EXECUTION>>assertThatRejectionReasonIsNull($it)<<} The
	 * matcher matches this group with the pattern {@code EXECUTION>>(.*)<<}
	 * </p>
	 * <p>
	 * So {@code executionMatcher[0]} returns 2 elements:
	 * <ul>
	 * <li>index0: EXECUTION>>assertThatRejectionReasonIsNull($it)<<</li>
	 * <li>index1: assertThatRejectionReasonIsNull($it)<<</li>
	 * </ul>
	 * </p>
	 * <p>
	 * Thus one can safely write {@code executionMatcher[0][1]} to retrieve the matched
	 * group
	 * </p>
	 * @param object to match the regexps against
	 * @return object converted from temporary holders
	 */
	public static Object returnParsedObject(Object object) {
		if (!(object instanceof String)) {
			return object;
		}
		String string = (String) object;
		Matcher matcher = TEMPORARY_PATTERN_HOLDER.matcher(string.trim());
		if (matcher.matches()) {
			return Pattern.compile(patternFromMatchingGroup(matcher));
		}
		Matcher executionMatcher = TEMPORARY_EXECUTION_PATTERN_HOLDER.matcher(string.trim());
		if (executionMatcher.matches()) {
			return new ExecutionProperty(patternFromMatchingGroup(executionMatcher));
		}
		Matcher optionalMatcher = TEMPORARY_OPTIONAL_PATTERN_HOLDER.matcher(string.trim());
		if (optionalMatcher.matches()) {
			String patternToMatch = patternFromMatchingGroup(optionalMatcher);
			return Pattern.compile(new OptionalProperty(patternToMatch).optionalPattern());
		}
		return string;
	}

	private static String patternFromMatchingGroup(Matcher matcher) {
		return matcher.group(1);
	}

	public static ContentType recognizeContentTypeFromHeader(@Nullable Headers headers, Closure<Object> closure) {
		Header header = (headers != null) ? headers.getEntries()
			.stream()
			.filter((it) -> "Content-Type".equals(it.getName()) || "contentType".equals(it.getName()))
			.findFirst()
			.orElse(null) : null;
		Object closureResult = closure.call(header);
		String content = (closureResult != null) ? closureResult.toString() : null;
		if (content != null && content.contains("json")) {
			return ContentType.JSON;
		}
		if (content != null && content.contains("xml")) {
			return ContentType.XML;
		}
		if (content != null && content.contains("text")) {
			return ContentType.TEXT;
		}
		if (content != null && content.contains("form-urlencoded")) {
			return ContentType.FORM;
		}
		if (content != null && content.contains("octet-stream")) {
			return ContentType.UNKNOWN;
		}
		if (content != null && !content.isEmpty() && isNotTemplate(content)) {
			return ContentType.DEFINED;
		}
		return ContentType.UNKNOWN;
	}

	public static ContentType recognizeContentTypeFromHeader(@Nullable Headers headers) {
		return recognizeContentTypeFromHeader(headers, new Closure<Object>(null) {
			public @Nullable Object doCall(Object header) {
				return (header != null) ? ((Header) header).getClientValue() : null;
			}
		});
	}

	public static ContentType recognizeContentTypeFromTestHeader(Headers headers) {
		return recognizeContentTypeFromHeader(headers, new Closure<Object>(null) {
			public @Nullable Object doCall(Object header) {
				return (header != null) ? ((Header) header).getServerValue() : null;
			}
		});
	}

	public static MatchingStrategy.Type getEqualsTypeFromContentType(ContentType contentType) {
		switch (contentType) {
			case JSON:
				return MatchingStrategy.Type.EQUAL_TO_JSON;
			case XML:
				return MatchingStrategy.Type.EQUAL_TO_XML;
			default:
				return MatchingStrategy.Type.EQUAL_TO;
		}
	}

	public static ContentType recognizeContentTypeFromContent(GString gstring) {
		if (isJsonType(gstring)) {
			return ContentType.JSON;
		}
		if (isXmlType(gstring)) {
			return ContentType.XML;
		}
		return ContentType.UNKNOWN;
	}

	public static ContentType recognizeContentTypeFromContent(Map jsonMap) {
		return ContentType.JSON;
	}

	public static ContentType recognizeContentTypeFromContent(byte[] bytes) {
		return ContentType.UNKNOWN;
	}

	public static ContentType recognizeContentTypeFromContent(List jsonList) {
		return ContentType.JSON;
	}

	public static ContentType recognizeContentTypeFromContent(String string) {
		try {
			JsonSlurperCompatibility.parse(string);
			return ContentType.JSON;
		}
		catch (Exception ignored) {
			if (isXmlType(new GStringImpl(new Object[] { string }, new String[] { "", "" }))) {
				return ContentType.XML;
			}
			return ContentType.UNKNOWN;
		}
	}

	public static ContentType recognizeContentTypeFromContent(Number number) {
		return ContentType.TEXT;
	}

	public static ContentType recognizeContentTypeFromContent(@Nullable Object object) {
		if (object instanceof FromFileProperty) {
			FromFileProperty property = (FromFileProperty) object;
			if (property.isJson()) {
				return ContentType.JSON;
			}
			else if (property.isXml()) {
				return ContentType.XML;
			}
			object = property.isByte() ? property.asBytes() : property.asString();
		}
		if (object instanceof GString) {
			return recognizeContentTypeFromContent((GString) object);
		}
		else if (object instanceof Map) {
			return recognizeContentTypeFromContent((Map) object);
		}
		else if (object instanceof byte[]) {
			return recognizeContentTypeFromContent((byte[]) object);
		}
		else if (object instanceof List) {
			return recognizeContentTypeFromContent((List) object);
		}
		else if (object instanceof String) {
			return recognizeContentTypeFromContent((String) object);
		}
		else if (object instanceof Number) {
			return recognizeContentTypeFromContent((Number) object);
		}
		return ContentType.UNKNOWN;
	}

	public static boolean isJsonType(GString gstring) {
		if (gstring.isEmpty()) {
			return false;
		}
		Object[] values = gstring.getValues();
		Object[] transformed = new Object[values.length];
		for (int i = 0; i < values.length; i++) {
			Object it = values[i];
			transformed[i] = (it instanceof String || it instanceof GString) ? it.toString()
					: StringEscapeUtils.escapeJson(it.toString());
		}
		GString stringWithoutValues = new GStringImpl(transformed, (String[]) CloneUtils.clone(gstring.getStrings()));
		try {
			JsonSlurperCompatibility.parse(stringWithoutValues.toString());
			return true;
		}
		catch (JacksonException ex) {
			// Not JSON
		}
		return false;
	}

	public static boolean isXmlType(GString gString) {
		Object[] values = gString.getValues();
		Object[] transformed = new Object[values.length];
		for (int i = 0; i < values.length; i++) {
			Object it = values[i];
			transformed[i] = (it instanceof String || it instanceof GString) ? it.toString()
					: StringEscapeUtils.escapeXml11(it.toString());
		}
		GString stringWithoutValues = new GStringImpl(transformed, (String[]) CloneUtils.clone(gString.getStrings()));
		try {
			getXmlSlurperWithDefaultErrorHandler().parseText(stringWithoutValues.toString());
			return true;
		}
		catch (Exception ignored) {
			// Not XML
		}
		return false;
	}

	public static ContentType recognizeContentTypeFromMatchingStrategy(MatchingStrategy.Type type) {
		switch (type) {
			case EQUAL_TO_XML:
				return ContentType.XML;
			case EQUAL_TO_JSON:
				return ContentType.JSON;
			default:
				return ContentType.UNKNOWN;
		}
	}

	public static String getGroovyMultipartFileParameterContent(String propertyName, NamedProperty propertyValue,
			Closure<String> bytesFromFile) {
		return "'" + propertyName + "', " + namedPropertyName(propertyValue, "'") + ", "
				+ groovyNamedPropertyValue(propertyValue, "'", bytesFromFile)
				+ namedContentTypeNameIfPresent(propertyValue, "'");
	}

	public static String getGroovyMultipartFileParameterContent(String propertyName, NamedProperty propertyValue,
			Function<FromFileProperty, String> bytesFromFile) {
		return "'" + propertyName + "', " + namedPropertyName(propertyValue, "'") + ", "
				+ groovyNamedPropertyValue(propertyValue, "'", new Closure<String>(null) {
					public String doCall(FromFileProperty property) {
						return bytesFromFile.apply(property);
					}
				}) + namedContentTypeNameIfPresent(propertyValue, "'");
	}

	public static String getJavaMultipartFileParameterContent(String propertyName, NamedProperty propertyValue,
			Function<FromFileProperty, String> bytesFromFile) {
		return getJavaMultipartFileParameterContent(propertyName, propertyValue, new Closure<String>(null) {
			public String doCall(FromFileProperty property) {
				return bytesFromFile.apply(property);
			}
		});
	}

	public static String getJavaMultipartFileParameterContent(String propertyName, NamedProperty propertyValue,
			Closure<String> bytesFromFile) {
		return "\"" + StringEscapeUtils.escapeJava(propertyName) + "\", " + namedPropertyName(propertyValue, "\"")
				+ ", " + javaNamedPropertyValue(propertyValue, "\"", bytesFromFile)
				+ namedContentTypeNameIfPresent(propertyValue, "\"");
	}

	public static String namedPropertyName(NamedProperty property, String quote) {
		DslProperty name = Objects.requireNonNull(property.getName());
		Object serverValue = name.getServerValue();
		return (serverValue instanceof ExecutionProperty) ? serverValue.toString()
				: quote + StringEscapeUtils.escapeJava(Objects.requireNonNull(serverValue).toString()) + quote;
	}

	public static String namedContentTypeNameIfPresent(NamedProperty property, String quote) {
		DslProperty contentTypeProperty = property.getContentType();
		if (contentTypeProperty == null) {
			return "";
		}
		Object serverValue = contentTypeProperty.getServerValue();
		String contentType = (serverValue instanceof ExecutionProperty) ? serverValue.toString()
				: quote + StringEscapeUtils.escapeJava(Objects.requireNonNull(serverValue).toString()) + quote;
		return ", " + contentType;
	}

	public static String groovyNamedPropertyValue(NamedProperty property, String quote, Closure<String> bytesFromFile) {
		DslProperty valueProperty = Objects.requireNonNull(property.getValue());
		Object serverValue = valueProperty.getServerValue();
		if (serverValue instanceof ExecutionProperty) {
			return serverValue.toString();
		}
		else if (serverValue instanceof byte[]) {
			byte[] bytes = (byte[]) serverValue;
			return "[" + joinBytes(bytes) + "] as byte[]";
		}
		else if (serverValue instanceof FromFileProperty) {
			FromFileProperty fromFileProperty = (FromFileProperty) serverValue;
			if (fromFileProperty.isByte()) {
				return (String) bytesFromFile.call(fromFileProperty);
			}
			return "[" + joinBytes(fromFileProperty.asBytes()) + "] as byte[]";
		}
		return quote + StringEscapeUtils.escapeJava(Objects.requireNonNull(serverValue).toString()) + quote + ".bytes";
	}

	public static String javaNamedPropertyValue(NamedProperty property, String quote, Closure<String> bytesFromFile) {
		DslProperty valueProperty = Objects.requireNonNull(property.getValue());
		Object serverValue = valueProperty.getServerValue();
		if (serverValue instanceof ExecutionProperty) {
			return serverValue.toString();
		}
		else if (serverValue instanceof byte[]) {
			byte[] bytes = (byte[]) serverValue;
			return "new byte[] {" + joinBytes(bytes) + "}";
		}
		else if (serverValue instanceof FromFileProperty) {
			FromFileProperty fromFileProperty = (FromFileProperty) serverValue;
			if (fromFileProperty.isByte()) {
				return (String) bytesFromFile.call(fromFileProperty);
			}
			return "new byte[] {" + joinBytes(fromFileProperty.asBytes()) + "}";
		}
		return quote + StringEscapeUtils.escapeJava(Objects.requireNonNull(serverValue).toString()) + quote
				+ ".getBytes()";
	}

	private static String joinBytes(byte[] bytes) {
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < bytes.length; i++) {
			if (i > 0) {
				builder.append(", ");
			}
			builder.append(bytes[i]);
		}
		return builder.toString();
	}

	public static ContentType evaluateClientSideContentType(@Nullable Headers contractHeaders, @Nullable Object body) {
		ContentType contentType = recognizeContentTypeFromHeader(contractHeaders);
		if (ContentType.UNKNOWN == contentType) {
			contentType = recognizeContentTypeFromContent(body);
		}
		return contentType;
	}

	public static ContentType evaluateServerSideContentType(Headers contractHeaders, Object body) {
		ContentType contentType = recognizeContentTypeFromTestHeader(contractHeaders);
		if (ContentType.UNKNOWN == contentType) {
			contentType = recognizeContentTypeFromContent(body);
		}
		return contentType;
	}

	/**
	 * Creates new {@link XmlSlurper} with default error handler.
	 * @return the {@link XmlSlurper} with default error handler
	 */
	public static XmlSlurper getXmlSlurperWithDefaultErrorHandler() {
		try {
			XmlSlurper xmlSlurper = new XmlSlurper();
			xmlSlurper.setErrorHandler(new DefaultHandler());
			return xmlSlurper;
		}
		catch (ParserConfigurationException | SAXException ex) {
			throw new IllegalStateException(ex);
		}
	}

	private static boolean isNotTemplate(String content) {
		return !new HandlebarsTemplateProcessor().containsTemplateEntry(content);
	}

	private static Closure<Object> toClosure(Function valueProvider) {
		return new Closure<Object>(null) {
			public @Nullable Object doCall(Object it) {
				return valueProvider.apply(it);
			}
		};
	}

}
