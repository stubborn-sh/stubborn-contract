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

import sh.stubborn.contract.spec.internal.DslProperty;
import sh.stubborn.contract.spec.internal.ExecutionProperty;
import sh.stubborn.contract.spec.internal.MatchingStrategy;
import sh.stubborn.contract.spec.internal.QueryParameter;
import sh.stubborn.contract.spec.internal.QueryParameters;
import sh.stubborn.contract.spec.internal.Request;
import sh.stubborn.contract.verifier.file.SingleContractMetadata;
import sh.stubborn.contract.verifier.util.MapConverter;

class JaxRsUrlPathWhen implements When, JaxRsAcceptor, QueryParamsResolver {

	private final BlockBuilder blockBuilder;

	private final GeneratedClassMetaData generatedClassMetaData;

	private final BodyParser bodyParser;

	JaxRsUrlPathWhen(BlockBuilder blockBuilder, GeneratedClassMetaData metaData, BodyParser bodyParser) {
		this.blockBuilder = blockBuilder;
		this.generatedClassMetaData = metaData;
		this.bodyParser = bodyParser;
	}

	@Override
	public MethodVisitor<When> apply(SingleContractMetadata metadata) {
		appendUrlPathAndQueryParameters(metadata.getContract().getRequest());
		return this;
	}

	private void appendUrlPathAndQueryParameters(Request request) {
		if (request.getUrl() != null) {
			this.blockBuilder.addIndented(".path(" + concreteUrl(request.getUrl()) + ")");
			appendQueryParams(request.getUrl().getQueryParameters());
		}
		else if (request.getUrlPath() != null) {
			this.blockBuilder.addIndented(".path(" + concreteUrl(request.getUrlPath()) + ")");
			appendQueryParams(request.getUrlPath().getQueryParameters());
		}
	}

	private String concreteUrl(DslProperty url) {
		Object testSideUrl = MapConverter.getTestSideValues(url);
		if (!(testSideUrl instanceof ExecutionProperty)) {
			return '"' + testSideUrl.toString() + '"';
		}
		return testSideUrl.toString();
	}

	private void appendQueryParams(QueryParameters queryParameters) {
		if (queryParameters == null || queryParameters.getParameters().isEmpty()) {
			return;
		}
		this.blockBuilder.addEmptyLine();
		Iterator<QueryParameter> iterator = queryParameters.getParameters()
			.stream()
			.filter(this::allowedQueryParameter)
			.iterator();
		while (iterator.hasNext()) {
			QueryParameter param = iterator.next();
			String queryParamValue = getQueryParamValue(param);
			String text = ".queryParam(\"" + param.getName() + "\", " + queryParamValue + ")";
			if (iterator.hasNext()) {
				this.blockBuilder.addLine(text);
			}
			else {
				this.blockBuilder.addIndented(text);
			}
		}
	}

	private String getQueryParamValue(QueryParameter param) {
		Object serverValue = param.getServerValue();
		if (serverValue instanceof ExecutionProperty) {
			return ((ExecutionProperty) serverValue).getExecutionCommand();
		}
		return this.bodyParser.quotedShortText(resolveParamValue(param));
	}

	/**
	 * @return {@code true} if the query parameter is allowed
	 */
	private boolean allowedQueryParameter(QueryParameter param) {
		return allowedQueryParameter(param.getServerValue());
	}

	/**
	 * @return {@code true} if the query parameter is allowed
	 */
	private boolean allowedQueryParameter(MatchingStrategy matchingStrategy) {
		return matchingStrategy.getType() != MatchingStrategy.Type.ABSENT;
	}

	/**
	 * @return {@code true} if the query parameter is allowed
	 */
	private boolean allowedQueryParameter(Object o) {
		if (o instanceof QueryParameter) {
			return allowedQueryParameter((QueryParameter) o);
		}
		else if (o instanceof MatchingStrategy) {
			return allowedQueryParameter((MatchingStrategy) o);
		}
		else if (o instanceof DslProperty) {
			return allowedQueryParameter(((DslProperty) o).getServerValue());
		}
		return true;
	}

	@Override
	public boolean accept(SingleContractMetadata metadata) {
		return acceptType(this.generatedClassMetaData, metadata);
	}

}
