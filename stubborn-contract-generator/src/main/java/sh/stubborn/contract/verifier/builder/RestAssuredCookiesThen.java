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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

import sh.stubborn.contract.spec.internal.Cookie;
import sh.stubborn.contract.spec.internal.Cookies;
import sh.stubborn.contract.spec.internal.ExecutionProperty;
import sh.stubborn.contract.spec.internal.NotToEscapePattern;
import sh.stubborn.contract.spec.internal.Response;
import sh.stubborn.contract.verifier.file.SingleContractMetadata;
import sh.stubborn.contract.verifier.util.MapConverter;

class RestAssuredCookiesThen implements Then, RestAssuredAcceptor, CookieElementProcessor {

	private final BlockBuilder blockBuilder;

	private final ComparisonBuilder comparisonBuilder;

	RestAssuredCookiesThen(BlockBuilder blockBuilder, ComparisonBuilder comparisonBuilder) {
		this.blockBuilder = blockBuilder;
		this.comparisonBuilder = comparisonBuilder;
	}

	@Override
	public MethodVisitor<Then> apply(SingleContractMetadata metadata) {
		List<String> lines = cookieLines(metadata, this.comparisonBuilder);
		Iterator<String> iterator = lines.iterator();
		while (iterator.hasNext()) {
			String text = iterator.next();
			if (iterator.hasNext()) {
				this.blockBuilder.addLineWithEnding(text);
			}
			else {
				this.blockBuilder.addIndented(text);
			}
		}
		this.blockBuilder.addEndingIfNotPresent();
		return this;
	}

	/**
	 * The per-cookie response assertion lines for a single contract, without trailing
	 * statement terminators. Each cookie contributes two ordered standalone statements: a
	 * null-check (e.g. {@code assertThat(response.cookie("session")).isNotNull()})
	 * followed by the value assertion (e.g. {@code assertThat(response.cookie("session"))
	 * .isEqualTo("abc123")} or a {@code .matches(...)} / {@code ExecutionProperty}
	 * variant). Shared by the legacy {@link #apply} path and the structured
	 * {@code ResponseModelBuilder} so both emit byte-identical text.
	 * @param metadata the contract whose response cookies to assert
	 * @param comparisonBuilder the comparison builder producing the assertion text
	 * @return the null-check and value assertions, in declaration order, each without a
	 * trailing {@code ;}
	 */
	static List<String> cookieLines(SingleContractMetadata metadata, ComparisonBuilder comparisonBuilder) {
		Response response = Objects.requireNonNull(metadata.getContract().getResponse());
		Cookies cookies = Objects.requireNonNull(response.getCookies());
		List<String> lines = new ArrayList<>();
		for (Cookie cookie : cookies.getEntries()) {
			Object value = (cookie.getServerValue() instanceof NotToEscapePattern) ? cookie.getServerValue()
					: MapConverter.getTestSideValues(Objects.requireNonNull(cookie.getServerValue()));
			String key = cookie.getKey();
			lines.add(comparisonBuilder.assertThatIsNotNull(cookieKeyExpression(key)));
			lines.add(cookieValueAssertion(key, value, comparisonBuilder));
		}
		return lines;
	}

	private static String cookieKeyExpression(String key) {
		return "response.cookie(\"" + key + "\")";
	}

	private static String cookieValueAssertion(String property, Object value, ComparisonBuilder comparisonBuilder) {
		String cookieValue = cookieKeyExpression(property);
		if (value instanceof NotToEscapePattern) {
			return comparisonBuilder.assertThat(cookieValue)
					+ comparisonBuilder.matches(Objects.requireNonNull(((NotToEscapePattern) value).getServerValue())
						.pattern()
						.replace("\\", "\\\\"));
		}
		else if (value instanceof String || value instanceof Pattern) {
			return comparisonBuilder.assertThat(cookieValue, value);
		}
		else if (value instanceof Number) {
			return comparisonBuilder.assertThat(cookieValue, value);
		}
		else if (value instanceof ExecutionProperty) {
			return ((ExecutionProperty) value).insertValue(cookieValue);
		}
		else {
			// fallback
			return cookieValueAssertion(property, value.toString(), comparisonBuilder);
		}
	}

	@Override
	public ComparisonBuilder comparisonBuilder() {
		return this.comparisonBuilder;
	}

	@Override
	public BlockBuilder blockBuilder() {
		return this.blockBuilder;
	}

	@Override
	public String cookieKey(String key) {
		return cookieKeyExpression(key);
	}

	@Override
	public boolean accept(SingleContractMetadata metadata) {
		Response response = Objects.requireNonNull(metadata.getContract().getResponse());
		return response.getCookies() != null;
	}

}
