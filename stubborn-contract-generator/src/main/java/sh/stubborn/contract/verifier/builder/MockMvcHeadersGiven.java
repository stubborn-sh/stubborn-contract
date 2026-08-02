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

import java.util.Iterator;
import java.util.Objects;

import sh.stubborn.contract.spec.internal.Header;
import sh.stubborn.contract.spec.internal.Headers;
import sh.stubborn.contract.spec.internal.MatchingStrategy;
import sh.stubborn.contract.spec.internal.Request;
import sh.stubborn.contract.verifier.file.SingleContractMetadata;
import sh.stubborn.contract.verifier.util.MapConverter;

class MockMvcHeadersGiven implements Given {

	private final BlockBuilder blockBuilder;

	MockMvcHeadersGiven(BlockBuilder blockBuilder) {
		this.blockBuilder = blockBuilder;
	}

	@Override
	public MethodVisitor<Given> apply(SingleContractMetadata metadata) {
		Request request = Objects.requireNonNull(metadata.getContract().getRequest());
		processInput(this.blockBuilder, Objects.requireNonNull(request.getHeaders()));
		return this;
	}

	private void processInput(BlockBuilder bb, Headers headers) {
		Iterator<Header> iterator = headers.getEntries().iterator();
		while (iterator.hasNext()) {
			Header header = iterator.next();
			if (ofAbsentType(header)) {
				continue;
			}
			if (iterator.hasNext()) {
				bb.addLine(string(header));
			}
			else {
				bb.addIndented(string(header));
			}
		}
	}

	private String string(Header header) {
		return headerLine(header);
	}

	private boolean ofAbsentType(Header header) {
		return isAbsent(header);
	}

	/**
	 * The {@code .header(name, value)} continuation line for a single request header, in
	 * the exact form the legacy MockMvc/Explicit builders emit. Reused by
	 * {@link RequestModelBuilder} so the structured request path stays byte-identical to
	 * the legacy output.
	 * @param header the request header
	 * @return the {@code .header(...)} line (no statement terminator)
	 */
	static String headerLine(Header header) {
		return ".header(" + ContentHelper.getTestSideForNonBodyValue(header.getName()) + ", "
				+ ContentHelper.getTestSideForNonBodyValue(
						MapConverter.getTestSideValuesForNonBody(Objects.requireNonNull(header.getServerValue())))
				+ ")";
	}

	/**
	 * Whether the header is an {@code ABSENT} matching strategy (skipped on the request
	 * side).
	 * @param header the request header
	 * @return {@code true} if the header should be skipped
	 */
	static boolean isAbsent(Header header) {
		return header.getServerValue() instanceof MatchingStrategy
				&& MatchingStrategy.Type.ABSENT.equals(((MatchingStrategy) header.getServerValue()).getType());
	}

	@Override
	public boolean accept(SingleContractMetadata metadata) {
		Request request = metadata.getContract().getRequest();
		return request != null && request.getHeaders() != null;
	}

}
