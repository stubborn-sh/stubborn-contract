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

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link FluentStatement} — proves the legacy fluent-chain {@code ;} rule:
 * only the last line of the chain is terminated.
 *
 * @author Marcin Grzejszczak
 */
class FluentStatementTests {

	@Test
	void head_only_chain_terminates_the_head() {
		FluentStatement statement = new FluentStatement("Response response = given().spec(request)", List.of());

		assertThat(statement.render()).containsExactly("Response response = given().spec(request);");
	}

	@Test
	void multi_line_chain_terminates_only_the_last_line() {
		FluentStatement statement = new FluentStatement("MockMvcRequestSpecification request = given()",
				List.of(".header(\"X-Trace\", \"abc\")", ".header(\"X-More\", \"def\")"));

		assertThat(statement.render()).containsExactly("MockMvcRequestSpecification request = given()",
				".header(\"X-Trace\", \"abc\")", ".header(\"X-More\", \"def\");");
	}

	@Test
	void does_not_add_a_second_semicolon_when_last_line_already_ends_with_one() {
		FluentStatement statement = new FluentStatement("ResponseOptions response = given().spec(request)",
				List.of(".get(\"/items/1\");"));

		List<String> rendered = statement.render();

		assertThat(rendered).containsExactly("ResponseOptions response = given().spec(request)", ".get(\"/items/1\");");
		assertThat(rendered.get(rendered.size() - 1)).doesNotEndWith(";;");
	}

}
