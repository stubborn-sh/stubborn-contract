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

import java.util.Locale;
import java.util.Objects;

import sh.stubborn.contract.spec.internal.ExecutionProperty;
import sh.stubborn.contract.spec.internal.Request;
import sh.stubborn.contract.spec.internal.Url;
import sh.stubborn.contract.verifier.file.SingleContractMetadata;
import sh.stubborn.contract.verifier.util.MapConverter;

class MockMvcUrlWhen implements When, MockMvcAcceptor, QueryParamsResolver {

	private final BlockBuilder blockBuilder;

	private final BodyParser bodyParser;

	MockMvcUrlWhen(BlockBuilder blockBuilder, BodyParser bodyParser) {
		this.blockBuilder = blockBuilder;
		this.bodyParser = bodyParser;
	}

	@Override
	public MethodVisitor<When> apply(SingleContractMetadata metadata) {
		Request request = Objects.requireNonNull(metadata.getContract().getRequest());
		Url url = getUrl(request);
		addUrl(url, request);
		return this;
	}

	private Url getUrl(Request request) {
		if (request.getUrl() != null) {
			return request.getUrl();
		}
		if (request.getUrlPath() != null) {
			return request.getUrlPath();
		}
		throw new IllegalStateException("URL is not set!");
	}

	private void addUrl(Url buildUrl, Request request) {
		this.blockBuilder.addIndented(urlLine(request, this.bodyParser));
	}

	/**
	 * The {@code .<method>(url)} continuation line for the request, in the exact form the
	 * legacy MockMvc/Explicit builders emit. Reused by {@link RequestModelBuilder} so the
	 * structured request path stays byte-identical to the legacy output.
	 * @param request the request whose method and URL to render
	 * @param bodyParser the body parser used to quote the URL (the Java parser for
	 * MockMvc/Explicit)
	 * @return the {@code .<method>(url)} line (no statement terminator)
	 */
	static String urlLine(Request request, BodyParser bodyParser) {
		Url buildUrl = url(request);
		Object testSideUrl = MapConverter.getTestSideValues(buildUrl);
		String method = Objects.requireNonNull(Objects.requireNonNull(request.getMethod()).getServerValue())
			.toString()
			.toLowerCase(Locale.ROOT);
		String url = testSideUrl.toString();
		if (!(testSideUrl instanceof ExecutionProperty)) {
			url = bodyParser.quotedShortText(testSideUrl.toString());
		}
		return "." + method + "(" + url + ")";
	}

	private static Url url(Request request) {
		if (request.getUrl() != null) {
			return request.getUrl();
		}
		if (request.getUrlPath() != null) {
			return request.getUrlPath();
		}
		throw new IllegalStateException("URL is not set!");
	}

	@Override
	public boolean accept(SingleContractMetadata metadata) {
		return true;
	}

}
