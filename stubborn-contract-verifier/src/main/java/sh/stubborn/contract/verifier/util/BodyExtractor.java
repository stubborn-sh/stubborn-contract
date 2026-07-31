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

import java.util.function.Function;
import java.util.regex.Pattern;

import groovy.json.JsonOutput;
import groovy.json.StringEscapeUtils;
import groovy.lang.GString;
import org.jspecify.annotations.Nullable;

import sh.stubborn.contract.spec.internal.CanBeDynamic;
import sh.stubborn.contract.spec.internal.DslProperty;
import sh.stubborn.contract.spec.internal.FromFileProperty;
import sh.stubborn.contract.spec.internal.RegexProperty;

import static sh.stubborn.contract.verifier.util.ContentUtils.extractValue;

/**
 * Class that constructs a String from a body. The body can be a GString or a map.
 *
 * @author Marcin Grzejszczak
 * @since 1.0.0
 */
public final class BodyExtractor {

	private BodyExtractor() {
	}

	/**
	 * @return the string representation of the body for the server side. That means that
	 * all the interpolations etc. will be resolved for the server side.
	 */
	public static String extractTestValueFrom(@Nullable Object body) {
		Object bodyValue = extractServerValueFromBody(body);
		String json = JsonOutput.toJson(bodyValue);
		json = StringEscapeUtils.unescapeJavaScript(json);
		return trimRepeatedQuotes(json);
	}

	/**
	 * @return the string representation of the body for the client side. That means that
	 * all the interpolations etc. will be resolved for the client side.
	 */
	public static String extractStubValueFrom(@Nullable Object body) {
		Object bodyValue = extractClientValueFromBody(body);
		String json = JsonOutput.toJson(bodyValue);
		json = StringEscapeUtils.unescapeJavaScript(json);
		return trimRepeatedQuotes(json);
	}

	private static String trimRepeatedQuotes(String toTrim) {
		return toTrim.startsWith("\"") ? toTrim.replaceAll("\"", "") : toTrim;
	}

	public static @Nullable Object extractServerValueFromBody(@Nullable Object bodyValue) {
		if (bodyValue instanceof GString) {
			Function<Object, @Nullable Object> serverSide = (v) -> ((DslProperty<?>) v).getServerValue();
			return extractValue((GString) bodyValue, serverSide);
		}
		if (bodyValue == null) {
			return null;
		}
		Function<Object, @Nullable Object> serverSide = (it) -> it instanceof DslProperty
				? ((DslProperty<?>) it).getServerValue() : it;
		return MapConverter.transformValues(bodyValue, serverSide);
	}

	public static @Nullable Object extractClientValueFromBody(@Nullable Object bodyValue) {
		if (bodyValue == null) {
			return null;
		}
		if (bodyValue instanceof GString) {
			Function<Object, @Nullable Object> clientSide = (v) -> ((DslProperty<?>) v).getClientValue();
			return extractValue((GString) bodyValue, clientSide);
		}
		else if (bodyValue instanceof DslProperty) {
			return extractClientValueFromBody(((DslProperty<?>) bodyValue).getClientValue());
		}
		else if (bodyValue instanceof FromFileProperty && ((FromFileProperty) bodyValue).isString()) {
			return MapConverter.transformValues(((FromFileProperty) bodyValue).asString(), Function.identity());
		}
		else {
			Function<Object, @Nullable Object> clientSide = (it) -> {
				Object prop = it instanceof DslProperty ? ((DslProperty<?>) it).getClientValue() : it;
				if (prop instanceof CanBeDynamic || prop instanceof Pattern) {
					return new RegexProperty(prop).generateConcreteValue();
				}
				return prop;
			};
			return MapConverter.transformValues(bodyValue, clientSide);
		}
	}

}
