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
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import sh.stubborn.contract.spec.internal.ExecutionProperty;
import sh.stubborn.contract.spec.internal.MatchingStrategy;
import sh.stubborn.contract.spec.internal.QueryParameter;
import sh.stubborn.contract.spec.internal.Request;
import sh.stubborn.contract.spec.internal.Url;
import sh.stubborn.contract.verifier.file.SingleContractMetadata;
import sh.stubborn.contract.verifier.util.MapConverter;

class MockMvcQueryParamsWhen implements When, MockMvcAcceptor, QueryParamsResolver {

	private static final String QUERY_PARAM_METHOD = "queryParam";

	/**
	 * Stateless resolver used by the static {@link #queryParamLine} helper (the
	 * {@link QueryParamsResolver#resolveParamValue} default method carries no state).
	 */
	private static final QueryParamsResolver RESOLVER = new QueryParamsResolver() {
	};

	private final BlockBuilder blockBuilder;

	private final BodyParser bodyParser;

	MockMvcQueryParamsWhen(BlockBuilder blockBuilder, BodyParser bodyParser) {
		this.blockBuilder = blockBuilder;
		this.bodyParser = bodyParser;
	}

	@Override
	public MethodVisitor<When> apply(SingleContractMetadata metadata) {
		Request request = Objects.requireNonNull(metadata.getContract().getRequest());
		Url url = getUrl(request);
		addQueryParameters(url);
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

	private void addQueryParameters(Url buildUrl) {
		List<QueryParameter> queryParameters = Objects.requireNonNull(buildUrl.getQueryParameters())
			.getParameters()
			.stream()
			.filter(MockMvcQueryParamsWhen::allowedQueryParameter)
			.collect(Collectors.toList());
		Iterator<QueryParameter> iterator = queryParameters.iterator();
		while (iterator.hasNext()) {
			QueryParameter parameter = iterator.next();
			String text = addQueryParameter(parameter);
			if (iterator.hasNext()) {
				this.blockBuilder.addLine(text);
			}
			else {
				this.blockBuilder.addIndented(text);
			}
		}
	}

	/**
	 * Whether the query parameter is allowed (not an {@code ABSENT} matching strategy),
	 * matching the legacy filter. Reused by {@link RequestModelBuilder} so the structured
	 * request path stays byte-identical to the legacy output.
	 * @param o the query parameter (or its resolved server value)
	 * @return {@code true} if the parameter should be emitted
	 */
	static boolean allowedQueryParameter(Object o) {
		if (o instanceof QueryParameter) {
			return allowedQueryParameter(Objects.requireNonNull(((QueryParameter) o).getServerValue()));
		}
		else if (o instanceof MatchingStrategy) {
			return !MatchingStrategy.Type.ABSENT.equals(((MatchingStrategy) o).getType());
		}
		return true;
	}

	private String addQueryParameter(QueryParameter queryParam) {
		return queryParamLine(queryParam, this.bodyParser);
	}

	/**
	 * The {@code .queryParam(name, value)} continuation line for a single query
	 * parameter, in the exact form the legacy MockMvc/Explicit builders emit. Reused by
	 * {@link RequestModelBuilder} so the structured request path stays byte-identical to
	 * the legacy output.
	 * @param queryParam the query parameter
	 * @param bodyParser the body parser used to quote the name/value (the Java parser for
	 * MockMvc/Explicit)
	 * @return the {@code .queryParam(...)} line (no statement terminator)
	 */
	static String queryParamLine(QueryParameter queryParam, BodyParser bodyParser) {
		String queryParamValue = getQueryParamValue(queryParam, bodyParser);
		return "." + QUERY_PARAM_METHOD + "(" + bodyParser.quotedLongText(queryParam.getName()) + "," + queryParamValue
				+ ")";
	}

	private static String getQueryParamValue(QueryParameter queryParam, BodyParser bodyParser) {
		Object serverValue = queryParam.getServerValue();
		if (serverValue instanceof ExecutionProperty) {
			return ((ExecutionProperty) serverValue).getExecutionCommand();
		}
		return bodyParser
			.quotedLongText(RESOLVER.resolveParamValue(MapConverter.getTestSideValuesForNonBody(queryParam)));
	}

	@Override
	public boolean accept(SingleContractMetadata metadata) {
		Request request = Objects.requireNonNull(metadata.getContract().getRequest());
		Url url = getUrl(request);
		return url.getQueryParameters() != null;
	}

}
