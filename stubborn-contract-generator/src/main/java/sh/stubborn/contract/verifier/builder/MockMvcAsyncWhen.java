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

class MockMvcAsyncWhen implements When, MockMvcAcceptor {

	private final BlockBuilder blockBuilder;

	private final GeneratedClassMetaData generatedClassMetaData;

	MockMvcAsyncWhen(BlockBuilder blockBuilder, GeneratedClassMetaData generatedClassMetaData) {
		this.blockBuilder = blockBuilder;
		this.generatedClassMetaData = generatedClassMetaData;
	}

	@Override
	public MethodVisitor<When> apply(SingleContractMetadata metadata) {
		Response response = Objects.requireNonNull(metadata.getContract().getResponse());
		this.blockBuilder.addIndented(asyncLine(response));
		return this;
	}

	/**
	 * The {@code .when().async()} continuation line (with an appended
	 * {@code .timeout(<n>)} when the response declares a fixed delay), in the exact form
	 * the legacy MockMvc builder emits. Reused by {@link RequestModelBuilder} so the
	 * structured request path stays byte-identical to the legacy output.
	 * @param response the response whose async/delay to render
	 * @return the async continuation line (no statement terminator)
	 */
	static String asyncLine(Response response) {
		String async = ".when().async()";
		if (response.getDelay() != null) {
			return async + ".timeout(" + response.getDelay().getServerValue() + ")";
		}
		return async;
	}

	@Override
	public boolean accept(SingleContractMetadata metadata) {
		boolean accept = acceptType(this.generatedClassMetaData, metadata);
		if (!accept) {
			return false;
		}
		Response response = Objects.requireNonNull(metadata.getContract().getResponse());
		return response.getAsync() || response.getDelay() != null;
	}

}
