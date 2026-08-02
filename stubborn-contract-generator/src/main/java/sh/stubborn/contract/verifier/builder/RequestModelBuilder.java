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
import java.util.List;

import org.jspecify.annotations.Nullable;
import sh.stubborn.contract.spec.internal.Header;
import sh.stubborn.contract.spec.internal.Headers;
import sh.stubborn.contract.spec.internal.Request;
import sh.stubborn.contract.spec.internal.Response;
import sh.stubborn.contract.spec.internal.Url;
import sh.stubborn.contract.verifier.config.TestFramework;
import sh.stubborn.contract.verifier.config.TestMode;
import sh.stubborn.contract.verifier.file.SingleContractMetadata;
import sh.stubborn.contract.verifier.template.HandlebarsTemplateProcessor;
import sh.stubborn.contract.verifier.template.TemplateProcessor;

/**
 * Builds the structured {@link RequestModel} (the {@code // given:} request-spec chain
 * and the {@code // when:} response chain) for the eligible subset of HTTP contracts,
 * reusing the exact legacy string helpers so the output stays byte-identical to the
 * legacy builders.
 *
 * <p>
 * A contract is eligible only when <em>all</em> of the following hold; otherwise
 * {@link #build} returns {@code null} and the caller falls back to capturing the whole
 * method body verbatim from the legacy generator:
 * <ul>
 * <li>HTTP contract</li>
 * <li>{@link TestMode} is {@link TestMode#MOCKMVC} or {@link TestMode#EXPLICIT}</li>
 * <li>{@link TestFramework} is not {@link TestFramework#SPOCK}</li>
 * <li>the request has no body, cookies, query parameters or multipart</li>
 * <li>the response is neither async nor delayed</li>
 * <li>the request chain carries no template entry</li>
 * </ul>
 *
 * @author Marcin Grzejszczak
 */
final class RequestModelBuilder {

	private final TemplateProcessor templateProcessor = new HandlebarsTemplateProcessor();

	/**
	 * Builds the structured request portion for a single contract, or {@code null} when
	 * the contract is not eligible for the structured path.
	 * @param contract the contract whose request to model
	 * @param framework the target test framework
	 * @param meta the class-level metadata (unused for now; kept for parity with the
	 * legacy builder wiring and future slices)
	 * @param mode the target test mode
	 * @return the structured request model, or {@code null} to use the verbatim fallback
	 */
	@Nullable RequestModel build(SingleContractMetadata contract, TestFramework framework, GeneratedClassMetaData meta,
			TestMode mode) {
		if (!eligible(contract, framework, mode)) {
			return null;
		}
		Request request = contract.getContract().getRequest();
		if (request == null) {
			return null;
		}
		FluentStatement given = new FluentStatement(givenHead(mode), headerLines(request));
		FluentStatement whenBlock = new FluentStatement(responseHead(mode),
				List.of(MockMvcUrlWhen.urlLine(request, RestAssuredBodyParser.INSTANCE)));
		if (containsTemplateEntry(given) || containsTemplateEntry(whenBlock)) {
			return null;
		}
		return new RequestModel(given, whenBlock);
	}

	private boolean eligible(SingleContractMetadata contract, TestFramework framework, TestMode mode) {
		if (framework == TestFramework.SPOCK) {
			return false;
		}
		if (mode != TestMode.MOCKMVC && mode != TestMode.EXPLICIT) {
			return false;
		}
		if (!contract.isHttp()) {
			return false;
		}
		Request request = contract.getContract().getRequest();
		if (request == null) {
			return false;
		}
		if (request.getBody() != null) {
			return false;
		}
		if (hasCookies(request)) {
			return false;
		}
		if (request.getMultipart() != null) {
			return false;
		}
		if (hasQueryParams(request)) {
			return false;
		}
		if (noUrl(request)) {
			return false;
		}
		Response response = contract.getContract().getResponse();
		if (response != null && (response.getAsync() || response.getDelay() != null)) {
			return false;
		}
		return true;
	}

	private boolean hasCookies(Request request) {
		return request.getCookies() != null && !request.getCookies().getEntries().isEmpty();
	}

	private boolean hasQueryParams(Request request) {
		Url url = urlOrNull(request);
		return url != null && url.getQueryParameters() != null;
	}

	private boolean noUrl(Request request) {
		return urlOrNull(request) == null;
	}

	private @Nullable Url urlOrNull(Request request) {
		if (request.getUrl() != null) {
			return request.getUrl();
		}
		return request.getUrlPath();
	}

	private String givenHead(TestMode mode) {
		return (mode == TestMode.MOCKMVC) ? "MockMvcRequestSpecification request = given()"
				: "RequestSpecification request = given()";
	}

	private String responseHead(TestMode mode) {
		return (mode == TestMode.MOCKMVC) ? "ResponseOptions response = given().spec(request)"
				: "Response response = given().spec(request)";
	}

	private List<String> headerLines(Request request) {
		List<String> lines = new ArrayList<>();
		Headers headers = request.getHeaders();
		if (headers == null) {
			return lines;
		}
		for (Header header : headers.getEntries()) {
			if (MockMvcHeadersGiven.isAbsent(header)) {
				continue;
			}
			lines.add(MockMvcHeadersGiven.headerLine(header));
		}
		return lines;
	}

	private boolean containsTemplateEntry(FluentStatement statement) {
		for (String line : statement.render()) {
			if (this.templateProcessor.containsTemplateEntry(line)) {
				return true;
			}
		}
		return false;
	}

}
