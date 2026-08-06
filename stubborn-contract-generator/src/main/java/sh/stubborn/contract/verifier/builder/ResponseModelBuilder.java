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
import sh.stubborn.contract.spec.internal.Response;
import sh.stubborn.contract.verifier.config.TestFramework;
import sh.stubborn.contract.verifier.config.TestMode;
import sh.stubborn.contract.verifier.file.SingleContractMetadata;
import sh.stubborn.contract.verifier.template.HandlebarsTemplateProcessor;
import sh.stubborn.contract.verifier.template.TemplateProcessor;

/**
 * Builds the structured {@link ResponseModel} (the response status-code assertion, the
 * response header assertions and the response cookie assertions of the {@code // then:}
 * block) for the eligible subset of HTTP contracts, reusing the exact legacy string
 * helpers so the output stays byte-identical to the legacy builders.
 *
 * <p>
 * A contract is eligible only when <em>all</em> of the following hold; otherwise
 * {@link #build} returns {@code null} and the caller falls back to capturing the whole
 * {@code // then:} block verbatim from the legacy generator:
 * <ul>
 * <li>the request portion is already eligible for the structured path (the
 * {@link RequestModelBuilder} returns non-{@code null})</li>
 * <li>the response is present</li>
 * <li>no structured then line (status, header or cookie) carries a template entry</li>
 * </ul>
 *
 * <p>
 * The response body assertions (the {@code // and:} block) are never modelled here; they
 * stay captured verbatim from the legacy pipeline.
 *
 * @author Marcin Grzejszczak
 */
final class ResponseModelBuilder {

	private final RequestModelBuilder requestModelBuilder = new RequestModelBuilder();

	private final TemplateProcessor templateProcessor = new HandlebarsTemplateProcessor();

	/**
	 * Builds the structured response portion for a single contract, or {@code null} when
	 * the contract is not eligible for the structured path.
	 * @param contract the contract whose response to model
	 * @param framework the target test framework
	 * @param meta the class-level metadata
	 * @param mode the target test mode
	 * @return the structured response model, or {@code null} to use the verbatim fallback
	 */
	@Nullable ResponseModel build(SingleContractMetadata contract, TestFramework framework, GeneratedClassMetaData meta,
			TestMode mode) {
		if (this.requestModelBuilder.build(contract, framework, meta, mode) == null) {
			return null;
		}
		Response response = contract.getContract().getResponse();
		if (response == null) {
			return null;
		}
		List<String> statements = new ArrayList<>();
		statements.add(RestAssuredStatusCodeThen.statusLine(contract, ComparisonBuilder.JAVA_HTTP_INSTANCE));
		if (response.getHeaders() != null) {
			statements.addAll(RestAssuredHeadersThen.headerLines(contract, ComparisonBuilder.JAVA_HTTP_INSTANCE));
		}
		if (response.getCookies() != null) {
			statements.addAll(RestAssuredCookiesThen.cookieLines(contract, ComparisonBuilder.JAVA_HTTP_INSTANCE));
		}
		StatementList thenBlock = new StatementList(statements);
		if (containsTemplateEntry(thenBlock)) {
			return null;
		}
		return new ResponseModel(thenBlock);
	}

	private boolean containsTemplateEntry(StatementList statementList) {
		for (String line : statementList.render()) {
			if (this.templateProcessor.containsTemplateEntry(line)) {
				return true;
			}
		}
		return false;
	}

}
