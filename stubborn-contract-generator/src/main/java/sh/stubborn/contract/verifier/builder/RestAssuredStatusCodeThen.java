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

import java.util.Objects;

import sh.stubborn.contract.spec.internal.Response;
import sh.stubborn.contract.verifier.file.SingleContractMetadata;

class RestAssuredStatusCodeThen implements Then {

	private final BlockBuilder blockBuilder;

	private final ComparisonBuilder comparisonBuilder;

	RestAssuredStatusCodeThen(BlockBuilder blockBuilder, ComparisonBuilder comparisonBuilder) {
		this.blockBuilder = blockBuilder;
		this.comparisonBuilder = comparisonBuilder;
	}

	@Override
	public MethodVisitor<Then> apply(SingleContractMetadata metadata) {
		this.blockBuilder.addIndented(statusLine(metadata, this.comparisonBuilder)).addEndingIfNotPresent();
		return this;
	}

	/**
	 * The status-code assertion line for a single contract, without a trailing statement
	 * terminator, e.g. {@code assertThat(response.statusCode()).isEqualTo(200)}. Shared
	 * by the legacy {@link #apply} path and the structured {@code ResponseModelBuilder}
	 * so both emit byte-identical text.
	 * @param metadata the contract whose response status to assert
	 * @param comparisonBuilder the comparison builder producing the assertion text
	 * @return the status-code assertion, without a trailing {@code ;}
	 */
	static String statusLine(SingleContractMetadata metadata, ComparisonBuilder comparisonBuilder) {
		Response response = Objects.requireNonNull(metadata.getContract().getResponse());
		return comparisonBuilder.assertThat("response.statusCode()",
				Objects.requireNonNull(Objects.requireNonNull(response.getStatus()).getServerValue()));
	}

	@Override
	public boolean accept(SingleContractMetadata metadata) {
		return true;
	}

}
