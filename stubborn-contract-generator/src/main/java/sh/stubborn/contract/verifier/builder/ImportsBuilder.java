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

class ImportsBuilder {

	private final GeneratedTestClassBuilder parentBuilder;

	private final BlockBuilder builder;

	private final GeneratedClassMetaData metaData;

	ImportsBuilder(GeneratedTestClassBuilder generatedTestClassBuilder) {
		this.parentBuilder = generatedTestClassBuilder;
		this.builder = generatedTestClassBuilder.blockBuilder;
		this.metaData = generatedTestClassBuilder.generatedClassMetaData;
	}

	ImportsBuilder defaultImports() {
		this.parentBuilder.imports(new DefaultImports(this.builder, this.metaData));
		this.parentBuilder.staticImports(new DefaultStaticImports(this.builder));
		return this;
	}

	ImportsBuilder userImports() {
		this.parentBuilder.imports(new UserImports(this.builder, this.metaData));
		this.parentBuilder.staticImports(new UserStaticImports(this.builder, this.metaData));
		return this;
	}

	ImportsBuilder customMode() {
		this.parentBuilder.imports(new CustomModeImports(this.builder, this.metaData));
		this.parentBuilder.staticImports(new CustomModeStaticImports(this.builder, this.metaData));
		return this;
	}

	ImportsBuilder json() {
		this.parentBuilder.imports(new JsonPathImports(this.builder, this.metaData));
		this.parentBuilder.staticImports(new DefaultJsonStaticImports(this.builder, this.metaData));
		return this;
	}

	ImportsBuilder xml() {
		this.parentBuilder.imports(new XmlImports(this.builder, this.metaData));
		return this;
	}

	ImportsBuilder jUnit5() {
		this.parentBuilder.imports(new JUnit5Imports(this.builder, this.metaData),
				new JUnit5IgnoreImports(this.builder, this.metaData),
				new JUnit5OrderImports(this.builder, this.metaData));
		return this;
	}

	ImportsBuilder testNG() {
		this.parentBuilder.imports(new TestNGImports(this.builder, this.metaData));
		return this;
	}

	ImportsBuilder spock() {
		this.parentBuilder.imports(new SpockImports(this.builder, this.metaData),
				new SpockIgnoreImports(this.builder, this.metaData),
				new SpockOrderImports(this.builder, this.metaData));
		return this;
	}

	ImportsBuilder messaging() {
		this.parentBuilder.imports(new MessagingImports(this.builder, this.metaData));
		this.parentBuilder.staticImports(new MessagingStaticImports(this.builder, this.metaData));
		return this;
	}

	ImportsBuilder restAssured() {
		this.parentBuilder.imports(new MockMvcRestAssuredImports(this.builder, this.metaData),
				new ExplicitRestAssuredImports(this.builder, this.metaData),
				new WebTestClientRestAssuredImports(this.builder, this.metaData));
		this.parentBuilder.staticImports(new MockMvcRestAssuredStaticImports(this.builder, this.metaData),
				new ExplicitRestAssuredStaticImports(this.builder, this.metaData),
				new WebTestClientRestAssured3StaticImports(this.builder, this.metaData));
		return this;
	}

	ImportsBuilder jaxRs() {
		this.parentBuilder.imports(new JaxRsImports(this.builder, this.metaData));
		this.parentBuilder.staticImports(new JaxRsStaticImports(this.builder, this.metaData));
		return this;
	}

	GeneratedTestClassBuilder build() {
		return this.parentBuilder;
	}

}
