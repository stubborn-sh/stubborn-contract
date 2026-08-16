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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("NullAway")
class ContractTemplateMutationTests {

	private final HandlebarsContractTemplate handlebars = new HandlebarsContractTemplate();

	private final CompositeContractTemplate composite = new CompositeContractTemplate();

	@Test
	void openingAndClosingTemplates() {
		assertThat(this.handlebars.openingTemplate()).isEqualTo("{{");
		assertThat(this.handlebars.closingTemplate()).isEqualTo("}}");
		assertThat(this.handlebars.escapedOpeningTemplate()).isEqualTo("{{{");
		assertThat(this.handlebars.escapedClosingTemplate()).isEqualTo("}}}");
	}

	@Test
	void unescapedTemplates() {
		assertThat(this.handlebars.url()).isEqualTo("{{request.url}}");
		assertThat(this.handlebars.query("q")).isEqualTo("{{request.query.q.[0]}}");
		assertThat(this.handlebars.query("q", 5)).isEqualTo("{{request.query.q.[5]}}");
		assertThat(this.handlebars.path()).isEqualTo("{{request.path}}");
		assertThat(this.handlebars.path(2)).isEqualTo("{{request.path.[2]}}");
		assertThat(this.handlebars.header("H")).isEqualTo("{{request.headers.H.[0]}}");
		assertThat(this.handlebars.header("H", 3)).isEqualTo("{{request.headers.H.[3]}}");
		assertThat(this.handlebars.cookie("C")).isEqualTo("{{request.cookies.C}}");
		assertThat(this.handlebars.body()).isEqualTo("{{request.body}}");
		assertThat(this.handlebars.body("$.a")).isEqualTo("{{jsonPath request.body '$.a'}}");
	}

	@Test
	void escapedTemplates() {
		assertThat(this.handlebars.escapedUrl()).isEqualTo("{{{request.url}}}");
		assertThat(this.handlebars.escapedQuery("q")).isEqualTo("{{{request.query.q.[0]}}}");
		assertThat(this.handlebars.escapedQuery("q", 5)).isEqualTo("{{{request.query.q.[5]}}}");
		assertThat(this.handlebars.escapedPath()).isEqualTo("{{{request.path}}}");
		assertThat(this.handlebars.escapedPath(2)).isEqualTo("{{{request.path.[2]}}}");
		assertThat(this.handlebars.escapedHeader("H")).isEqualTo("{{{request.headers.H.[0]}}}");
		assertThat(this.handlebars.escapedHeader("H", 3)).isEqualTo("{{{request.headers.H.[3]}}}");
		assertThat(this.handlebars.escapedCookie("C")).isEqualTo("{{{request.cookies.C}}}");
		assertThat(this.handlebars.escapedBody()).isEqualTo("{{{escapejsonbody}}}");
		assertThat(this.handlebars.escapedBody("$.a")).isEqualTo("{{{jsonPath request.body '$.a'}}}");
	}

	@Test
	void startsWithTemplate() {
		assertThat(this.handlebars.startsWithTemplate("{{x}}")).isTrue();
		assertThat(this.handlebars.startsWithTemplate("no")).isFalse();
	}

	@Test
	void startsWithEscapedTemplate() {
		assertThat(this.handlebars.startsWithEscapedTemplate("{{{x}}}")).isTrue();
		assertThat(this.handlebars.startsWithEscapedTemplate("no")).isFalse();
	}

	@Test
	void compositeDelegatesOpeningClosing() {
		assertThat(this.composite.openingTemplate()).isEqualTo("{{");
		assertThat(this.composite.closingTemplate()).isEqualTo("}}");
		assertThat(this.composite.escapedOpeningTemplate()).isEqualTo("{{{");
		assertThat(this.composite.escapedClosingTemplate()).isEqualTo("}}}");
	}

	@Test
	void compositeDelegatesStartsWith() {
		assertThat(this.composite.startsWithTemplate("{{x}}")).isTrue();
		assertThat(this.composite.startsWithTemplate("no")).isFalse();
		assertThat(this.composite.startsWithEscapedTemplate("{{{x}}}")).isTrue();
		assertThat(this.composite.startsWithEscapedTemplate("no")).isFalse();
	}

	@Test
	void compositeDelegatesUnescaped() {
		assertThat(this.composite.url()).isEqualTo("{{request.url}}");
		assertThat(this.composite.query("q")).isEqualTo("{{request.query.q.[0]}}");
		assertThat(this.composite.query("q", 5)).isEqualTo("{{request.query.q.[5]}}");
		assertThat(this.composite.path()).isEqualTo("{{request.path}}");
		assertThat(this.composite.path(2)).isEqualTo("{{request.path.[2]}}");
		assertThat(this.composite.header("H")).isEqualTo("{{request.headers.H.[0]}}");
		assertThat(this.composite.header("H", 3)).isEqualTo("{{request.headers.H.[3]}}");
		assertThat(this.composite.cookie("C")).isEqualTo("{{request.cookies.C}}");
		assertThat(this.composite.body()).isEqualTo("{{request.body}}");
		assertThat(this.composite.body("$.a")).isEqualTo("{{jsonPath request.body '$.a'}}");
	}

	@Test
	void compositeDelegatesEscaped() {
		assertThat(this.composite.escapedUrl()).isEqualTo("{{{request.url}}}");
		assertThat(this.composite.escapedQuery("q")).isEqualTo("{{{request.query.q.[0]}}}");
		assertThat(this.composite.escapedQuery("q", 5)).isEqualTo("{{{request.query.q.[5]}}}");
		assertThat(this.composite.escapedPath()).isEqualTo("{{{request.path}}}");
		assertThat(this.composite.escapedPath(2)).isEqualTo("{{{request.path.[2]}}}");
		assertThat(this.composite.escapedHeader("H")).isEqualTo("{{{request.headers.H.[0]}}}");
		assertThat(this.composite.escapedHeader("H", 3)).isEqualTo("{{{request.headers.H.[3]}}}");
		assertThat(this.composite.escapedCookie("C")).isEqualTo("{{{request.cookies.C}}}");
		assertThat(this.composite.escapedBody()).isEqualTo("{{{escapejsonbody}}}");
		assertThat(this.composite.escapedBody("$.a")).isEqualTo("{{{jsonPath request.body '$.a'}}}");
	}

	// --- FromRequest ---

	@Test
	void fromRequestEscapedAccessors() {
		FromRequest from = new FromRequest();
		assertThat(from.url().getClientValue()).isEqualTo("{{{request.url}}}");
		assertThat(from.query("q").getClientValue()).isEqualTo("{{{request.query.q.[0]}}}");
		assertThat(from.query("q", 5).getClientValue()).isEqualTo("{{{request.query.q.[5]}}}");
		assertThat(from.path().getClientValue()).isEqualTo("{{{request.path}}}");
		assertThat(from.path(2).getClientValue()).isEqualTo("{{{request.path.[2]}}}");
		assertThat(from.header("H").getClientValue()).isEqualTo("{{{request.headers.H.[0]}}}");
		assertThat(from.header("H", 3).getClientValue()).isEqualTo("{{{request.headers.H.[3]}}}");
		assertThat(from.cookie("C").getClientValue()).isEqualTo("{{{request.cookies.C}}}");
		assertThat(from.body().getClientValue()).isEqualTo("{{{escapejsonbody}}}");
		assertThat(from.body("$.a").getClientValue()).isEqualTo("{{{jsonPath request.body '$.a'}}}");
	}

	@Test
	void fromRequestRawAccessors() {
		FromRequest from = new FromRequest();
		assertThat(from.rawUrl().getClientValue()).isEqualTo("{{request.url}}");
		assertThat(from.rawQuery("q").getClientValue()).isEqualTo("{{request.query.q.[0]}}");
		assertThat(from.rawQuery("q", 5).getClientValue()).isEqualTo("{{request.query.q.[5]}}");
		assertThat(from.rawPath().getClientValue()).isEqualTo("{{request.path}}");
		assertThat(from.rawPath(2).getClientValue()).isEqualTo("{{request.path.[2]}}");
		assertThat(from.rawHeader("H").getClientValue()).isEqualTo("{{request.headers.H.[0]}}");
		assertThat(from.rawHeader("H", 3).getClientValue()).isEqualTo("{{request.headers.H.[3]}}");
		assertThat(from.rawCookie("C").getClientValue()).isEqualTo("{{request.cookies.C}}");
		assertThat(from.rawBody().getClientValue()).isEqualTo("{{request.body}}");
		assertThat(from.rawBody("$.a").getClientValue()).isEqualTo("{{jsonPath request.body '$.a'}}");
	}

	// --- ContractTemplate default methods via a minimal implementation ---

	@Test
	void interfaceDefaultsFallBackToUnescaped() {
		MinimalTemplate template = new MinimalTemplate();
		assertThat(template.escapedOpeningTemplate()).isEqualTo("[[");
		assertThat(template.escapedClosingTemplate()).isEqualTo("]]");
		assertThat(template.escapedUrl()).isEqualTo("URL");
		assertThat(template.escapedQuery("q")).isEqualTo("Q:q");
		assertThat(template.escapedQuery("q", 2)).isEqualTo("Q:q:2");
		assertThat(template.escapedPath()).isEqualTo("PATH");
		assertThat(template.escapedPath(2)).isEqualTo("PATH:2");
		assertThat(template.escapedHeader("h")).isEqualTo("H:h");
		assertThat(template.escapedHeader("h", 2)).isEqualTo("H:h:2");
		assertThat(template.escapedCookie("c")).isEqualTo("C:c");
		assertThat(template.escapedBody()).isEqualTo("BODY");
		assertThat(template.escapedBody("$.a")).isEqualTo("BODY:$.a");
		assertThat(template.startsWithTemplate("[[x")).isTrue();
		assertThat(template.startsWithEscapedTemplate("[[x")).isTrue();
	}

	static final class MinimalTemplate implements sh.stubborn.contract.spec.ContractTemplate {

		@Override
		public String openingTemplate() {
			return "[[";
		}

		@Override
		public String closingTemplate() {
			return "]]";
		}

		@Override
		public String url() {
			return "URL";
		}

		@Override
		public String query(String key) {
			return "Q:" + key;
		}

		@Override
		public String query(String key, int index) {
			return "Q:" + key + ":" + index;
		}

		@Override
		public String path() {
			return "PATH";
		}

		@Override
		public String path(int index) {
			return "PATH:" + index;
		}

		@Override
		public String header(String key) {
			return "H:" + key;
		}

		@Override
		public String header(String key, int index) {
			return "H:" + key + ":" + index;
		}

		@Override
		public String cookie(String key) {
			return "C:" + key;
		}

		@Override
		public String body() {
			return "BODY";
		}

		@Override
		public String body(String jsonPath) {
			return "BODY:" + jsonPath;
		}

	}

}
