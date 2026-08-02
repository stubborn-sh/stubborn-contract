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

import sh.stubborn.contract.spec.internal.Cookie;
import sh.stubborn.contract.spec.internal.MatchingStrategy;
import sh.stubborn.contract.spec.internal.Request;
import sh.stubborn.contract.verifier.file.SingleContractMetadata;

class MockMvcCookiesGiven implements Given {

	private final BlockBuilder blockBuilder;

	MockMvcCookiesGiven(BlockBuilder blockBuilder) {
		this.blockBuilder = blockBuilder;
	}

	@Override
	public MethodVisitor<Given> apply(SingleContractMetadata metadata) {
		processInput(Objects.requireNonNull(metadata.getContract().getRequest()));
		return this;
	}

	private void processInput(Request request) {
		Iterator<Cookie> iterator = Objects.requireNonNull(request.getCookies()).getEntries().iterator();
		while (iterator.hasNext()) {
			Cookie cookie = iterator.next();
			if (ofAbsentType(cookie)) {
				return;
			}
			if (iterator.hasNext()) {
				this.blockBuilder.addLine(string(cookie));
			}
			else {
				this.blockBuilder.addIndented(string(cookie));
			}
		}
	}

	private String string(Cookie cookie) {
		return cookieLine(cookie);
	}

	private boolean ofAbsentType(Cookie cookie) {
		return isAbsent(cookie);
	}

	/**
	 * The {@code .cookie(name, value)} continuation line for a single request cookie, in
	 * the exact form the legacy MockMvc/Explicit builders emit. Reused by
	 * {@link RequestModelBuilder} so the structured request path stays byte-identical to
	 * the legacy output.
	 * @param cookie the request cookie
	 * @return the {@code .cookie(...)} line (no statement terminator)
	 */
	static String cookieLine(Cookie cookie) {
		return ".cookie(" + ContentHelper.getTestSideForNonBodyValue(cookie.getKey()) + ", "
				+ ContentHelper.getTestSideForNonBodyValue(Objects.requireNonNull(cookie.getServerValue())) + ")";
	}

	/**
	 * Whether the cookie is an {@code ABSENT} matching strategy. The legacy builder stops
	 * emitting cookies at the first absent one (it {@code return}s rather than
	 * {@code continue}s), so callers replicating the chain must break on this too.
	 * @param cookie the request cookie
	 * @return {@code true} if the cookie is absent
	 */
	static boolean isAbsent(Cookie cookie) {
		return cookie.getServerValue() instanceof MatchingStrategy
				&& MatchingStrategy.Type.ABSENT.equals(((MatchingStrategy) cookie.getServerValue()).getType());
	}

	@Override
	public boolean accept(SingleContractMetadata metadata) {
		Request request = metadata.getContract().getRequest();
		return request != null && request.getCookies() != null;
	}

}
