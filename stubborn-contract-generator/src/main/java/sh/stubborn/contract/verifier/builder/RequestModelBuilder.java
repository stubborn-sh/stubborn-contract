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
import sh.stubborn.contract.spec.internal.Cookie;
import sh.stubborn.contract.spec.internal.Cookies;
import sh.stubborn.contract.spec.internal.Header;
import sh.stubborn.contract.spec.internal.Headers;
import sh.stubborn.contract.spec.internal.QueryParameter;
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
 * <li>{@link TestMode} is {@link TestMode#MOCKMVC}, {@link TestMode#EXPLICIT} or
 * {@link TestMode#WEBTESTCLIENT} (they share the RestAssured request producers, differing
 * only in the given/when head types)</li>
 * <li>{@link TestFramework} is not {@link TestFramework#SPOCK}</li>
 * <li>the request body is a plain body, an {@code ExecutionProperty} or a file-based
 * {@code FromFileProperty} body; cookies, query parameters and multipart parts are
 * emitted structurally, and an async/delayed response is emitted structurally in the
 * {@code // when:} chain</li>
 * <li>the request chain carries no template entry</li>
 * </ul>
 *
 * @author Marcin Grzejszczak
 * @since 1.0.0
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
		// The given chain follows the legacy RestAssuredGiven order: head, headers,
		// cookies, body, multipart. Headers and cookies are intermediate continuations;
		// the body and multipart lines (when present) are the trailing continuations, so
		// the LAST of them — never a header or cookie — receives the trailing `;` (placed
		// by FluentStatement.render, never by hand).
		List<String> givenLines = new ArrayList<>(headerLines(request));
		givenLines.addAll(cookieLines(request));
		if (request.getBody() != null) {
			givenLines.add(MockMvcBodyGiven.bodyLine(contract, meta, RestAssuredBodyParser.INSTANCE));
		}
		if (request.getMultipart() != null) {
			givenLines.addAll(JavaMultipartGiven.multipartLines(contract, meta, RestAssuredBodyParser.INSTANCE));
		}
		FluentStatement given = new FluentStatement(givenHead(mode), givenLines);
		// The when chain follows the legacy RestAssuredWhen order: queryParam, async,
		// url. Query params and the async line are intermediate continuations; the url
		// line is always last, so it carries the trailing `;`.
		List<String> whenLines = new ArrayList<>(queryParamLines(request));
		// The legacy async When is a MockMvcAcceptor, so the `.when().async()` line is
		// emitted only in MockMvc mode (Explicit mode omits it entirely).
		Response response = contract.getContract().getResponse();
		if (mode == TestMode.MOCKMVC && response != null && (response.getAsync() || response.getDelay() != null)) {
			whenLines.add(MockMvcAsyncWhen.asyncLine(response));
		}
		whenLines.add(MockMvcUrlWhen.urlLine(request, RestAssuredBodyParser.INSTANCE));
		FluentStatement whenBlock = new FluentStatement(responseHead(mode), whenLines);
		if (containsTemplateEntry(given) || containsTemplateEntry(whenBlock)) {
			return null;
		}
		return new RequestModel(given, whenBlock);
	}

	private boolean eligible(SingleContractMetadata contract, TestFramework framework, TestMode mode) {
		if (framework == TestFramework.SPOCK) {
			return false;
		}
		if (mode != TestMode.MOCKMVC && mode != TestMode.EXPLICIT && mode != TestMode.WEBTESTCLIENT) {
			return false;
		}
		if (!contract.isHttp()) {
			return false;
		}
		Request request = contract.getContract().getRequest();
		if (request == null) {
			return false;
		}
		if (noUrl(request)) {
			return false;
		}
		return true;
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
		return switch (mode) {
			case MOCKMVC -> "MockMvcRequestSpecification request = given()";
			case WEBTESTCLIENT -> "WebTestClientRequestSpecification request = given()";
			default -> "RequestSpecification request = given()";
		};
	}

	private String responseHead(TestMode mode) {
		return switch (mode) {
			case MOCKMVC -> "ResponseOptions response = given().spec(request)";
			case WEBTESTCLIENT -> "WebTestClientResponse response = given().spec(request)";
			default -> "Response response = given().spec(request)";
		};
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

	private List<String> cookieLines(Request request) {
		List<String> lines = new ArrayList<>();
		Cookies cookies = request.getCookies();
		if (cookies == null) {
			return lines;
		}
		for (Cookie cookie : cookies.getEntries()) {
			// The legacy MockMvcCookiesGiven stops at the first absent cookie (it returns
			// rather than continues), so break here to keep the emitted chain identical.
			if (MockMvcCookiesGiven.isAbsent(cookie)) {
				break;
			}
			lines.add(MockMvcCookiesGiven.cookieLine(cookie));
		}
		return lines;
	}

	private List<String> queryParamLines(Request request) {
		List<String> lines = new ArrayList<>();
		Url url = urlOrNull(request);
		if (url == null || url.getQueryParameters() == null) {
			return lines;
		}
		for (QueryParameter parameter : url.getQueryParameters().getParameters()) {
			if (!MockMvcQueryParamsWhen.allowedQueryParameter(parameter)) {
				continue;
			}
			lines.add(MockMvcQueryParamsWhen.queryParamLine(parameter, RestAssuredBodyParser.INSTANCE));
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
