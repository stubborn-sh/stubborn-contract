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

import org.apache.commons.text.StringEscapeUtils;
import sh.stubborn.contract.verifier.template.TemplateProcessor;

interface GroovyBodyParser extends BodyParser {

	@Override
	default String convertUnicodeEscapesIfRequired(String json) {
		return StringEscapeUtils.unescapeEcmaScript(json);
	}

	@Override
	default String postProcessJsonPath(String jsonPath) {
		if (templateProcessor().containsTemplateEntry(jsonPath)) {
			return jsonPath;
		}
		return jsonPath.replace("$", "\\$");
	}

	TemplateProcessor templateProcessor();

	@Override
	default String escape(String text) {
		String escaped = text.replace("\r", "\\r").replace("\n", "\\n");
		return escaped.replaceAll("\\n", "\\\\n");
	}

	@Override
	default String escapeForSimpleTextAssertion(String text) {
		return escape(text);
	}

	@Override
	default String quotedShortText(Object text) {
		String string = text.toString();
		if (text instanceof Number) {
			return string;
		}
		else if (string.contains("'") || string.contains("\"")) {
			return quotedLongText(text);
		}
		return "'" + groovyEscapedString(text.toString()) + "'";
	}

	@Override
	default String quotedEscapedShortText(Object text) {
		String string = text.toString();
		if (text instanceof Number) {
			return string;
		}
		else if (string.contains("'") || string.contains("\"")) {
			return quotedEscapedLongText(text);
		}
		return "'" + text.toString() + "'";
	}

	@Override
	default String quotedEscapedLongText(Object text) {
		return "'''" + escapeInvalidGroovyStringEscapes(text.toString()) + "'''";
	}

	/**
	 * Doubles only those backslashes that do <em>not</em> start a valid Groovy string
	 * escape, so a literal backslash from a regex (e.g. {@code \d} inside an optional
	 * body) survives a triple-quoted string — otherwise the generated test fails to
	 * compile with {@code "Unexpected character: '\'"} — while genuine escapes such as
	 * {@code \n} that the body already carries are left untouched.
	 * @param text the text to embed in a Groovy triple-quoted string
	 * @return the text with invalid-escape backslashes doubled
	 */
	default String escapeInvalidGroovyStringEscapes(String text) {
		StringBuilder builder = new StringBuilder(text.length());
		for (int i = 0; i < text.length(); i++) {
			char current = text.charAt(i);
			if (current == '\\') {
				char next = (i + 1 < text.length()) ? text.charAt(i + 1) : '\0';
				// Groovy string escapes are b, t, n, f, r, the quotes, backslash, dollar
				// and
				// the unicode escape prefix. Anything else after a backslash (e.g. a
				// regex
				// d, dot, s or w) is a literal backslash and must be doubled.
				if ("btnfr\"'\\$u".indexOf(next) < 0) {
					builder.append('\\');
				}
			}
			builder.append(current);
		}
		return builder.toString();
	}

	@Override
	default String quotedLongText(Object text) {
		String escapedString = groovyEscapedString(text);
		if (escapedString.startsWith("'")) {
			escapedString = "\\'" + escapedString.substring(1);
		}
		if (escapedString.endsWith("'")) {
			escapedString = escapedString.substring(0, escapedString.length() - 1) + "\\'";
		}
		return "'''" + escapedString + "'''";
	}

	default String groovyEscapedString(Object text) {
		return escape(text.toString()).replaceAll("\\\\\"", "\"");
	}

}
