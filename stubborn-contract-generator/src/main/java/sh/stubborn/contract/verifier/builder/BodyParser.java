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

package sh.stubborn.contract.verifier.builder;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import groovy.json.JsonOutput;
import groovy.lang.GString;
import org.apache.commons.text.StringEscapeUtils;
import sh.stubborn.contract.spec.internal.DslProperty;
import sh.stubborn.contract.spec.internal.FromFileProperty;
import sh.stubborn.contract.verifier.file.SingleContractMetadata;
import sh.stubborn.contract.verifier.util.ContentType;
import sh.stubborn.contract.verifier.util.ContentUtils;
import sh.stubborn.contract.verifier.util.MapConverter;

interface BodyParser extends BodyThen {

	String byteArrayString();

	default String convertUnicodeEscapesIfRequired(String json) {
		String unescapedJson = StringEscapeUtils.unescapeEcmaScript(json);
		return StringEscapeUtils.escapeJava(unescapedJson);
	}

	default String convertToJsonString(Object bodyValue) {
		String json = JsonOutput.toJson(bodyValue);
		json = convertUnicodeEscapesIfRequired(json);
		return trimRepeatedQuotes(json);
	}

	default String trimRepeatedQuotes(String toTrim) {
		if (toTrim.startsWith("\"")) {
			return toTrim.replaceAll("\"", "");
			// #261
		}
		else if (toTrim.startsWith("\\\"") && toTrim.endsWith("\\\"")) {
			return toTrim.substring(2, toTrim.length() - 2);
		}
		return toTrim;
	}

	default Object convertResponseBody(SingleContractMetadata metadata) {
		ContentType contentType = metadata.getOutputTestContentType();
		DslProperty body = Objects.requireNonNull(responseBody(metadata));
		Object responseBody = extractServerValueFromBody(contentType, Objects.requireNonNull(body.getServerValue()));
		if (responseBody instanceof FromFileProperty) {
			responseBody = ((FromFileProperty) responseBody).asString();
		}
		else if (responseBody instanceof GString) {
			responseBody = ContentUtils.extractValue((GString) responseBody, contentType,
					(o) -> (o instanceof DslProperty) ? Objects.requireNonNull(((DslProperty) o).getServerValue()) : o);
		}
		else if (responseBody instanceof DslProperty) {
			responseBody = MapConverter.getTestSideValues(responseBody);
		}
		return responseBody;
	}

	String responseAsString();

	@SuppressWarnings("unchecked")
	default String requestBodyAsString(SingleContractMetadata metadata) {
		ContentType contentType = metadata.getInputTestContentType();
		DslProperty body = requestBody(metadata);
		Object bodyValue = extractServerValueFromBody(contentType, Objects.requireNonNull(body.getServerValue()));
		if (contentType == ContentType.FORM) {
			if (bodyValue instanceof Map) {
				// [a:3, b:4] == "a=3&b=4"
				return ((Map) bodyValue).entrySet().stream().map((o) -> {
					Map.Entry entry = (Map.Entry) o;
					return convertUnicodeEscapesIfRequired(
							entry.getKey().toString() + "=" + MapConverter.getTestSideValuesForText(entry.getValue()));
				}).collect(Collectors.joining("&")).toString();
			}
			else if (bodyValue instanceof List) {
				// ["a=3", "b=4"] == "a=3&b=4"
				return ((List) bodyValue).stream()
					.map((o) -> convertUnicodeEscapesIfRequired(MapConverter.getTestSideValuesForText(o).toString()))
					.collect(Collectors.joining("&"))
					.toString();
			}
			else if (bodyValue instanceof String) {
				return (String) bodyValue;
			}
		}
		else {
			return convertToJsonString(bodyValue);
		}
		return "";
	}

	/**
	 * Converts the passed body into ints server side representation. All
	 * {@link DslProperty} will return their server side values.
	 * @param contentType the content type of the body
	 * @param bodyValue the body value to convert
	 * @return the server side representation of the body
	 */
	default Object extractServerValueFromBody(ContentType contentType, Object bodyValue) {
		if (bodyValue instanceof GString) {
			return ContentUtils.extractValue((GString) bodyValue, contentType, ContentUtils.GET_TEST_SIDE_FUNCTION);
		}
		else if (bodyValue instanceof FromFileProperty) {
			return MapConverter.transformValues(bodyValue, ContentUtils.GET_TEST_SIDE_FUNCTION);
		}
		else if (ContentType.TEXT != contentType && ContentType.FORM != contentType
				&& ContentType.DEFINED != contentType) {
			boolean dontParseStrings = contentType == ContentType.JSON && bodyValue instanceof Map;
			Function<String, Object> parsingClosure = dontParseStrings ? MapConverter.IDENTITY
					: MapConverter.JSON_PARSING_FUNCTION;
			return MapConverter.getTestSideValues(bodyValue, parsingClosure);
		}
		return bodyValue;
	}

	default String escape(String text) {
		String escaped = StringEscapeUtils.escapeJava(text);
		return escaped.replace("\r", "\\r").replace("\n", "\\n");
	}

	default String escapeForSimpleTextAssertion(String text) {
		return escape(text);
	}

	default String postProcessJsonPath(String jsonPath) {
		return jsonPath;
	}

	default String quotedLongText(Object text) {
		return quotedEscapedLongText(escape(text.toString()));
	}

	default String quotedEscapedLongText(Object text) {
		return "\"" + text.toString() + "\"";
	}

	default String quotedShortText(Object text) {
		return quotedLongText(text);
	}

	default String quotedEscapedShortText(Object text) {
		return quotedEscapedLongText(text);
	}

}
