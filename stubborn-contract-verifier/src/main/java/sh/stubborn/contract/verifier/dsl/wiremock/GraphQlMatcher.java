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

import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.matching.MatchResult;
import net.javacrumbs.jsonunit.assertj.JsonAssertions;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.assertj.core.api.Assertions;
import sh.stubborn.contract.verifier.converter.YamlContract;
import tools.jackson.databind.json.JsonMapper;

class GraphQlMatcher implements RequestMatcher {

	static final String NAME = "graphql";

	private static final Log log = LogFactory.getLog(GraphQlMatcher.class);

	private final JsonMapper objectMapper = new JsonMapper();

	@Override
	public MatchResult match(List<YamlContract> contracts, Request request, Parameters parameters) {
		YamlContract contract = contracts.get(0);
		// TODO: What if the body is in files?
		Map body = (Map) Objects.requireNonNull(contract.request).body;
		try {
			Map jsonBodyFromContract = Objects.requireNonNull(body);
			Map jsonBodyFromRequest = this.objectMapper.readerForMapOf(Object.class).readValue(request.getBody());
			String query = (String) jsonBodyFromContract.get("query");
			String queryFromRequest = (String) jsonBodyFromRequest.get("query");
			Map variables = (Map) jsonBodyFromContract.get("variables");
			Map variablesFromRequest = (Map) jsonBodyFromRequest.get("variables");
			String operationName = (String) jsonBodyFromContract.get("operationName");
			String operationNameFromRequest = (String) jsonBodyFromRequest.get("operationName");
			boolean queryMatches = assertThat(
					() -> Assertions.assertThat(query).isEqualToIgnoringWhitespace(queryFromRequest));
			boolean variablesMatch = assertThat(
					() -> JsonAssertions.assertThatJson(variables).isEqualTo(variablesFromRequest));
			boolean operationMatches = StringUtils.equals(operationName, operationNameFromRequest);
			return MatchResult.of(queryMatches && variablesMatch && operationMatches);
		}
		catch (Exception ex) {
			if (log.isWarnEnabled()) {
				log.warn("An exception occurred while trying to parse the graphql entries", ex);
			}
			return MatchResult.noMatch();
		}
	}

	@Override
	public boolean isApplicable(String tool) {
		return NAME.equals(tool);
	}

}
