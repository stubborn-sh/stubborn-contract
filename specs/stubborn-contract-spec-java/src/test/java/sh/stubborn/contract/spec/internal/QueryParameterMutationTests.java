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

package sh.stubborn.contract.spec.internal;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("NullAway")
class QueryParameterMutationTests {

	@Test
	void buildWithValue() {
		QueryParameter parameter = QueryParameter.build("q", "search");
		assertThat(parameter.getName()).isEqualTo("q");
		assertThat(parameter.getClientValue()).isEqualTo("search");
		assertThat(parameter.getServerValue()).isEqualTo("search");
	}

	@Test
	void buildWithMatchingStrategy() {
		MatchingStrategy strategy = new MatchingStrategy("v", MatchingStrategy.Type.EQUAL_TO);
		QueryParameter parameter = QueryParameter.build("q", strategy);
		assertThat(parameter.getName()).isEqualTo("q");
		assertThat(parameter.getClientValue()).isEqualTo(strategy);
	}

	@Test
	void buildWithRegexPropertyProducesConcreteServerSide() {
		RegexProperty regex = new RegexProperty(Pattern.compile("[0-9]{3}"));
		QueryParameter parameter = QueryParameter.build("q", regex);
		assertThat(parameter.getName()).isEqualTo("q");
		assertThat(parameter.getClientValue()).isInstanceOf(Pattern.class);
	}

	@Test
	void constructorWithDslProperty() {
		QueryParameter parameter = new QueryParameter("q", new DslProperty("c", "s"));
		assertThat(parameter.getClientValue()).isEqualTo("c");
		assertThat(parameter.getServerValue()).isEqualTo("s");
	}

	@Test
	void setName() {
		QueryParameter parameter = QueryParameter.build("q", "v");
		parameter.setName("q2");
		assertThat(parameter.getName()).isEqualTo("q2");
	}

	@Test
	void equalsHashCode() {
		QueryParameter a = QueryParameter.build("q", "v");
		QueryParameter b = QueryParameter.build("q", "v");
		assertThat(a).isEqualTo(b);
		assertThat(a).hasSameHashCodeAs(b);
		assertThat(a.equals(a)).isTrue();
		assertThat(a.equals(null)).isFalse();
		assertThat(a.equals("x")).isFalse();
	}

	@Test
	void notEqualWhenNameDiffers() {
		assertThat(QueryParameter.build("q1", "v")).isNotEqualTo(QueryParameter.build("q2", "v"));
		assertThat(QueryParameter.build("q1", "v").hashCode()).isNotEqualTo(QueryParameter.build("q2", "v").hashCode());
	}

	@Test
	void notEqualWhenValueDiffers() {
		assertThat(QueryParameter.build("q", "v1")).isNotEqualTo(QueryParameter.build("q", "v2"));
	}

	@Test
	void toStringContainsNameAndValue() {
		QueryParameter parameter = new QueryParameter("theName", new DslProperty("cval", "sval"));
		assertThat(parameter.toString()).contains("QueryParameter").contains("theName").contains("cval");
	}

	@Test
	void queryParametersParameterKeyValue() {
		QueryParameters parameters = new QueryParameters();
		parameters.parameter("q", "search");
		assertThat(parameters.getParameters()).extracting(QueryParameter::getName).containsExactly("q");
	}

	@Test
	void queryParametersFromMap() {
		QueryParameters parameters = new QueryParameters();
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("q", "search");
		parameters.parameter(map);
		assertThat(parameters.getParameters()).extracting(QueryParameter::getName).containsExactly("q");
	}

	@Test
	void queryParametersFromEmptyMapAddsNothing() {
		QueryParameters parameters = new QueryParameters();
		parameters.parameter(new LinkedHashMap<>());
		assertThat(parameters.getParameters()).isEmpty();
	}

	@Test
	void queryParametersSetParameters() {
		QueryParameters parameters = new QueryParameters();
		List<QueryParameter> list = new LinkedList<>();
		list.add(QueryParameter.build("z", "9"));
		parameters.setParameters(list);
		assertThat(parameters.getParameters()).extracting(QueryParameter::getName).containsExactly("z");
	}

	@Test
	void queryParametersEqualsHashCode() {
		QueryParameters a = new QueryParameters();
		a.parameter("q", "v");
		QueryParameters b = new QueryParameters();
		b.parameter("q", "v");
		assertThat(a).isEqualTo(b);
		assertThat(a).hasSameHashCodeAs(b);
		assertThat(a.equals(a)).isTrue();
		assertThat(a.equals(null)).isFalse();
		assertThat(a.equals("x")).isFalse();
	}

	@Test
	void queryParametersNotEqualWhenDiffer() {
		QueryParameters a = new QueryParameters();
		a.parameter("q", "v1");
		QueryParameters b = new QueryParameters();
		b.parameter("q", "v2");
		assertThat(a).isNotEqualTo(b);
		assertThat(a.hashCode()).isNotEqualTo(b.hashCode());
	}

	@Test
	void queryParametersToString() {
		QueryParameters parameters = new QueryParameters();
		parameters.parameter("q", "v");
		assertThat(parameters.toString()).contains("QueryParameters").contains("q");
	}

}
