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

package sh.stubborn.contract.verifier.dsl.wiremock;

import java.util.Arrays;
import java.util.List;

import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.matching.MatchResult;
import com.github.tomakehurst.wiremock.matching.RequestMatcherExtension;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import sh.stubborn.contract.verifier.converter.YamlContract;
import sh.stubborn.contract.verifier.converter.YamlContractConverter;

/**
 * Provides custom matching for WireMock's stub requests.
 *
 * @author Marcin Grzejszczak
 * @since 1.0.0
 */
public class SpringCloudContractRequestMatcher extends RequestMatcherExtension {

	private static final List<String> SUPPORTED_TOOLS = Arrays.asList(GraphQlMatcher.NAME);

	/**
	 * Name of the transformer inside the stub.
	 */
	public static final String NAME = "stubborn-contract";

	private static final Log log = LogFactory.getLog(SpringCloudContractRequestMatcher.class);

	@Override
	public MatchResult match(Request request, Parameters parameters) {
		if (!parameters.containsKey("contract") || !parameters.containsKey("tool")) {
			return MatchResult.noMatch();
		}
		String tool = parameters.getString("tool");
		if (!SUPPORTED_TOOLS.contains(tool)) {
			if (log.isWarnEnabled()) {
				log.warn("The tool [" + tool + "] is not supported");
			}
			return MatchResult.noMatch();
		}
		String string = parameters.getString("contract");
		List<YamlContract> contracts;
		try {
			contracts = YamlContractConverter.INSTANCE.read(string.getBytes());
		}
		catch (Exception ex) {
			if (log.isWarnEnabled()) {
				log.warn("An exception occurred while trying to parse the contract", ex);
			}
			return MatchResult.noMatch();
		}
		return new RequestMatcherFactory(matchers()).pick(tool).match(contracts, request, parameters);
	}

	List<RequestMatcher> matchers() {
		return Arrays.asList(new GraphQlMatcher());
	}

	@Override
	public String getName() {
		return NAME;
	}

}
