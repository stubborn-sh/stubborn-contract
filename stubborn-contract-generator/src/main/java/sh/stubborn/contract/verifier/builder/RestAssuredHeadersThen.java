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

import sh.stubborn.contract.spec.internal.ExecutionProperty;
import sh.stubborn.contract.spec.internal.Header;
import sh.stubborn.contract.spec.internal.Headers;
import sh.stubborn.contract.spec.internal.NotToEscapePattern;
import sh.stubborn.contract.spec.internal.Response;
import sh.stubborn.contract.verifier.file.SingleContractMetadata;
import sh.stubborn.contract.verifier.util.MapConverter;

class RestAssuredHeadersThen implements Then, RestAssuredAcceptor {

	private final BlockBuilder blockBuilder;

	private final ComparisonBuilder comparisonBuilder;

	RestAssuredHeadersThen(BlockBuilder blockBuilder, ComparisonBuilder comparisonBuilder) {
		this.blockBuilder = blockBuilder;
		this.comparisonBuilder = comparisonBuilder;
	}

	@Override
	public MethodVisitor<Then> apply(SingleContractMetadata metadata) {
		List<String> lines = headerLines(metadata, this.comparisonBuilder);
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
	 * The per-header response assertion lines for a single contract, without trailing
	 * statement terminators, e.g. {@code assertThat(response.header("X-Reply"))
	 * .isEqualTo("def")} or a {@code .matches(...)} / {@code ExecutionProperty} variant.
	 * Shared by the legacy {@link #apply} path and the structured
	 * {@code ResponseModelBuilder} so both emit byte-identical text.
	 * @param metadata the contract whose response headers to assert
	 * @param comparisonBuilder the comparison builder producing the assertion text
	 * @return one assertion per header, in declaration order, without trailing {@code ;}
	 */
	static List<String> headerLines(SingleContractMetadata metadata, ComparisonBuilder comparisonBuilder) {
		Response response = Objects.requireNonNull(metadata.getContract().getResponse());
		Headers headers = Objects.requireNonNull(response.getHeaders());
		List<String> lines = new ArrayList<>();
		for (Header header : headers.getEntries()) {
			lines.add(processHeaderElement(header.getName(),
					(header.getServerValue() instanceof NotToEscapePattern) ? header.getServerValue()
							: MapConverter.getTestSideValues(Objects.requireNonNull(header.getServerValue())),
					comparisonBuilder));
		}
		return lines;
	}

	private static String processHeaderElement(String property, Object value, ComparisonBuilder comparisonBuilder) {
		if (value instanceof NotToEscapePattern) {
			return comparisonBuilder.assertThat("response.header(\"" + property + "\")")
					+ matchesManuallyEscapedPattern((NotToEscapePattern) value, comparisonBuilder);
		}
		else if (value instanceof ExecutionProperty) {
			return ((ExecutionProperty) value).insertValue("response.header(\"" + property + "\")");

		}
		return comparisonBuilder.assertThat("response.header(\"" + property + "\")", value);
	}

	private static String matchesManuallyEscapedPattern(NotToEscapePattern value, ComparisonBuilder comparisonBuilder) {
		return comparisonBuilder
			.matchesEscaped(Objects.requireNonNull(value.getServerValue()).pattern().replace("\\", "\\\\"));
	}

	@Override
	public boolean accept(SingleContractMetadata metadata) {
		Response response = Objects.requireNonNull(metadata.getContract().getResponse());
		return response.getHeaders() != null;
	}

}
