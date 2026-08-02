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
 * Unit tests for {@link StatementList} — proves the standalone-statement {@code ;} rule:
 * <em>every</em> statement is terminated (unlike {@link FluentStatement}, which
 * terminates only the last line of a chain).
 *
 * @author Marcin Grzejszczak
 */
class StatementListTests {

	@Test
	void terminates_every_statement() {
		StatementList statements = new StatementList(List.of("assertThat(response.statusCode()).isEqualTo(200)",
				"assertThat(response.header(\"X-Reply\")).isEqualTo(\"def\")"));

		assertThat(statements.render()).containsExactly("assertThat(response.statusCode()).isEqualTo(200);",
				"assertThat(response.header(\"X-Reply\")).isEqualTo(\"def\");");
	}

	@Test
	void does_not_add_a_second_semicolon_when_a_statement_already_ends_with_one() {
		StatementList statements = new StatementList(
				List.of("assertThat(response.statusCode()).isEqualTo(200);", "response.header(\"X-Exec\").length()"));

		List<String> rendered = statements.render();

		assertThat(rendered).containsExactly("assertThat(response.statusCode()).isEqualTo(200);",
				"response.header(\"X-Exec\").length();");
		assertThat(rendered.get(0)).doesNotEndWith(";;");
	}

	@Test
	void empty_list_renders_empty() {
		assertThat(new StatementList(List.of()).render()).isEmpty();
	}

}
