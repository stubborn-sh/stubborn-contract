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

package sh.stubborn.contract.verifier.converter;

import org.jspecify.annotations.Nullable;
import sh.stubborn.contract.spec.Contract;

/**
 * Test-only {@link StubPostProcessor} registered through the {@code ServiceLoader} SPI.
 * It is only applicable when a contract carries the {@link #MARKER} metadata key, so it
 * never interferes with real conversion tests, while still allowing the post-processing
 * pipeline (filter lambda + processing loop) to be exercised.
 */
public class MarkerStubPostProcessor implements StubPostProcessor<String> {

	/**
	 * Metadata key that activates this processor.
	 */
	public static final String MARKER = "mutationTestProcessor";

	/**
	 * Value returned by {@link #postProcess} to prove that processing happened.
	 */
	public static final String PROCESSED = "PROCESSED";

	@Override
	public @Nullable String postProcess(@Nullable String stubMapping, Contract contract) {
		return PROCESSED;
	}

	@Override
	public boolean isApplicable(Contract contract) {
		return contract.getMetadata().containsKey(MARKER);
	}

}
