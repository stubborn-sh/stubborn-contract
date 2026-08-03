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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import sh.stubborn.contract.verifier.config.ContractVerifierConfigProperties;
import sh.stubborn.contract.verifier.config.TestFramework;
import sh.stubborn.contract.verifier.config.TestMode;
import sh.stubborn.contract.verifier.file.ContractMetadata;
import sh.stubborn.contract.verifier.file.SingleContractMetadata;
import sh.stubborn.contract.verifier.util.ContractVerifierDslConverter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ModelBuilder} — proves the config + contracts → model mapping
 * populates the class scaffold and that the resulting model renders through
 * {@link JavaTestRenderer}.
 *
 * @author Marcin Grzejszczak
 */
class ModelBuilderTests {

	// @formatter:off
	private static final String CONTRACT = "sh.stubborn.contract.spec.Contract.make {\n"
			+ " request {\n"
			+ "  method 'GET'\n"
			+ "  url '/foo'\n"
			+ " }\n"
			+ " response {\n"
			+ "  status OK()\n"
			+ " }\n"
			+ "}";
	// @formatter:on

	// @formatter:off
	private static final String PLAIN_BODY_CONTRACT = "sh.stubborn.contract.spec.Contract.make {\n"
			+ " request {\n"
			+ "  method 'PUT'\n"
			+ "  url '/foo'\n"
			+ "  headers { contentType(applicationJson()) }\n"
			+ "  body([foo: 'bar'])\n"
			+ " }\n"
			+ " response {\n"
			+ "  status OK()\n"
			+ " }\n"
			+ "}";

	private static final String QUERY_COOKIE_CONTRACT = "sh.stubborn.contract.spec.Contract.make {\n"
			+ " request {\n"
			+ "  method 'GET'\n"
			+ "  urlPath('/items') {\n"
			+ "   queryParameters {\n"
			+ "    parameter('page', '2')\n"
			+ "   }\n"
			+ "  }\n"
			+ "  cookies {\n"
			+ "   cookie('session', 'abc123')\n"
			+ "  }\n"
			+ " }\n"
			+ " response {\n"
			+ "  status OK()\n"
			+ " }\n"
			+ "}";

	private static final String FILE_BODY_CONTRACT = "sh.stubborn.contract.spec.Contract.make {\n"
			+ " request {\n"
			+ "  method 'PUT'\n"
			+ "  url '/foo'\n"
			+ "  headers { contentType(applicationJson()) }\n"
			+ "  body(file('request.json'))\n"
			+ " }\n"
			+ " response {\n"
			+ "  status OK()\n"
			+ " }\n"
			+ "}";

	private static final String ASYNC_CONTRACT = "sh.stubborn.contract.spec.Contract.make {\n"
			+ " request {\n"
			+ "  method 'GET'\n"
			+ "  url '/async'\n"
			+ " }\n"
			+ " response {\n"
			+ "  async()\n"
			+ "  status OK()\n"
			+ " }\n"
			+ "}";

	private static final String RESPONSE_HEADER_CONTRACT = "sh.stubborn.contract.spec.Contract.make {\n"
			+ " request {\n"
			+ "  method 'GET'\n"
			+ "  url '/foo'\n"
			+ " }\n"
			+ " response {\n"
			+ "  status OK()\n"
			+ "  headers { header('X-Reply', 'def') }\n"
			+ "  body([id: 1])\n"
			+ " }\n"
			+ "}";

	private static final String RESPONSE_COOKIE_CONTRACT = "sh.stubborn.contract.spec.Contract.make {\n"
			+ " request {\n"
			+ "  method 'GET'\n"
			+ "  url '/foo'\n"
			+ " }\n"
			+ " response {\n"
			+ "  status OK()\n"
			+ "  cookies { cookie('session', 'abc123') }\n"
			+ "  body([id: 1])\n"
			+ " }\n"
			+ "}";

	private static final String MULTIPART_CONTRACT = "sh.stubborn.contract.spec.Contract.make {\n"
			+ " request {\n"
			+ "  method 'PUT'\n"
			+ "  url '/multipart'\n"
			+ "  headers { contentType('multipart/form-data') }\n"
			+ "  multipart(\n"
			+ "   formParameter: 'formValue',\n"
			+ "   file: named(name: value('filename.csv'), content: value('file content'))\n"
			+ "  )\n"
			+ " }\n"
			+ " response {\n"
			+ "  status OK()\n"
			+ " }\n"
			+ "}";
	// @formatter:on

	@TempDir
	Path tmpDir;

	private final ModelBuilder modelBuilder = new ModelBuilder();

	private final RequestModelBuilder requestModelBuilder = new RequestModelBuilder();

	private final ResponseModelBuilder responseModelBuilder = new ResponseModelBuilder();

	@Test
	void builds_java_class_scaffold_from_contract() throws IOException {
		Collection<ContractMetadata> contracts = contracts(true);
		ContractVerifierConfigProperties properties = new ContractVerifierConfigProperties();
		properties.setTestFramework(TestFramework.JUNIT5);
		properties.setBaseClassForTests("com.example.BazBar");
		SingleTestGenerator.GeneratedClassData data = new SingleTestGenerator.GeneratedClassData("FooTest", "test",
				new File("/tmp").toPath());

		TestClassModel model = this.modelBuilder.build(properties, contracts, "com/example", data, List.of());

		assertThat(model.packageName()).isEqualTo("test");
		assertThat(model.className()).isEqualTo("FooTest");
		assertThat(model.baseClass()).isEqualTo("com.example.BazBar");
		assertThat(model.spock()).isFalse();
		assertThat(model.classAnnotations()).extracting(AnnotationModel::type)
			.containsExactly("java.lang.SuppressWarnings");
		assertThat(model.methods()).hasSize(1);
		TestMethodModel method = model.methods().get(0);
		assertThat(method.name()).startsWith("validate_");
		assertThat(method.annotations()).extracting(AnnotationModel::type)
			.containsExactly("org.junit.jupiter.api.Test", "org.junit.jupiter.api.Disabled");
		// the request and the // then: status assertion are now emitted from the
		// structured request/response model; a bodyless response leaves no verbatim
		// // and: tail, so bodyLines is empty for this contract
		assertThat(method.request()).isNotNull();
		assertThat(method.response()).isNotNull();
		assertThat(method.response().thenBlock().render())
			.contains("assertThat(response.statusCode()).isEqualTo(200);");
		assertThat(model.importDeclarations()).isNotEmpty();

		// the model renders to a structurally-correct Java class
		String rendered = new JavaTestRenderer().render(model);
		assertThat(rendered).contains("public class FooTest extends BazBar {").contains("public void " + method.name());
	}

	@Test
	void marks_spock_target_and_omits_java_only_scaffold() throws IOException {
		Collection<ContractMetadata> contracts = contracts(false);
		ContractVerifierConfigProperties properties = new ContractVerifierConfigProperties();
		properties.setTestFramework(TestFramework.SPOCK);
		SingleTestGenerator.GeneratedClassData data = new SingleTestGenerator.GeneratedClassData("FooSpec", "test",
				new File("/tmp").toPath());

		TestClassModel model = this.modelBuilder.build(properties, contracts, "com/example", data, List.of());

		assertThat(model.spock()).isTrue();
		// Spock carries @SuppressWarnings (like every framework) but no JUnit/TestNG
		// scaffold; a non-ignored contract has no method annotation (Spock has no @Test).
		assertThat(model.classAnnotations()).extracting(AnnotationModel::type)
			.containsExactly("java.lang.SuppressWarnings");
		assertThat(model.methods()).hasSize(1);
		assertThat(model.methods().get(0).annotations()).isEmpty();
	}

	@Test
	void testng_uses_testng_annotation() throws IOException {
		Collection<ContractMetadata> contracts = contracts(false);
		ContractVerifierConfigProperties properties = new ContractVerifierConfigProperties();
		properties.setTestFramework(TestFramework.TESTNG);
		SingleTestGenerator.GeneratedClassData data = new SingleTestGenerator.GeneratedClassData("FooTest", "test",
				new File("/tmp").toPath());

		TestClassModel model = this.modelBuilder.build(properties, contracts, "com/example", data, List.of());

		AnnotationModel testAnnotation = model.methods().get(0).annotations().get(0);
		assertThat(testAnnotation.type()).isEqualTo("org.testng.annotations.Test");
		assertThat(testAnnotation.memberCode()).isNull();
	}

	@Test
	void testng_ignored_uses_enabled_false_member() throws IOException {
		Collection<ContractMetadata> contracts = contracts(true);
		ContractVerifierConfigProperties properties = new ContractVerifierConfigProperties();
		properties.setTestFramework(TestFramework.TESTNG);
		SingleTestGenerator.GeneratedClassData data = new SingleTestGenerator.GeneratedClassData("FooTest", "test",
				new File("/tmp").toPath());

		TestClassModel model = this.modelBuilder.build(properties, contracts, "com/example", data, List.of());

		AnnotationModel testAnnotation = model.methods().get(0).annotations().get(0);
		assertThat(model.methods().get(0).annotations()).hasSize(1);
		assertThat(testAnnotation.type()).isEqualTo("org.testng.annotations.Test");
		assertThat(testAnnotation.memberName()).isEqualTo("enabled");
		assertThat(testAnnotation.memberCode()).isEqualTo("false");
	}

	@Test
	void request_model_is_built_for_a_plain_body_contract() throws IOException {
		RequestModel model = requestModelFor(PLAIN_BODY_CONTRACT);

		assertThat(model).as("plain-body contract must flow through the structured path").isNotNull();
		// the .body(...) line is the LAST continuation of the given chain (so it, not a
		// header, receives the trailing `;`)
		assertThat(model.given().continuations()).last(org.assertj.core.api.InstanceOfAssertFactories.STRING)
			.startsWith(".body(");
		assertThat(model.given().render()).last(org.assertj.core.api.InstanceOfAssertFactories.STRING).endsWith(");");
	}

	@Test
	void request_model_is_built_for_a_query_and_cookie_contract() throws IOException {
		RequestModel model = requestModelFor(QUERY_COOKIE_CONTRACT);

		assertThat(model).as("query+cookie contract must flow through the structured path").isNotNull();
		// given chain: the .cookie(...) line comes AFTER any .header(...) line (legacy
		// order head, headers, cookies)
		assertThat(model.given().continuations()).anySatisfy((line) -> assertThat(line).startsWith(".cookie("));
		// when chain: the .queryParam(...) line comes BEFORE the terminal url line
		List<String> whenLines = model.whenBlock().continuations();
		int queryIndex = indexOfPrefix(whenLines, ".queryParam(");
		int urlIndex = indexOfPrefix(whenLines, ".get(");
		assertThat(queryIndex).as(".queryParam( must be present").isNotNegative();
		assertThat(urlIndex).as(".get( url line must be present").isNotNegative();
		assertThat(queryIndex).as(".queryParam( must precede the url line").isLessThan(urlIndex);
		// the url line is the LAST continuation and gets the trailing `;`
		assertThat(model.whenBlock().render()).last(org.assertj.core.api.InstanceOfAssertFactories.STRING)
			.endsWith(");");
	}

	private static int indexOfPrefix(List<String> lines, String prefix) {
		for (int i = 0; i < lines.size(); i++) {
			if (lines.get(i).startsWith(prefix)) {
				return i;
			}
		}
		return -1;
	}

	@Test
	void request_model_is_built_for_an_async_contract() throws IOException {
		RequestModel model = requestModelFor(ASYNC_CONTRACT);

		assertThat(model).as("async contract must flow through the structured path").isNotNull();
		// when chain: the .when().async() line comes AFTER any query params and BEFORE
		// the
		// terminal url line (legacy order queryParam, async, url)
		List<String> whenLines = model.whenBlock().continuations();
		int asyncIndex = indexOfPrefix(whenLines, ".when().async()");
		int urlIndex = indexOfPrefix(whenLines, ".get(");
		assertThat(asyncIndex).as(".when().async() must be present").isNotNegative();
		assertThat(urlIndex).as(".get( url line must be present").isNotNegative();
		assertThat(asyncIndex).as(".when().async() must precede the url line").isLessThan(urlIndex);
		// the url line is the LAST continuation and gets the trailing `;`; the async line
		// is intermediate and stays unterminated
		assertThat(model.whenBlock().render()).last(org.assertj.core.api.InstanceOfAssertFactories.STRING)
			.endsWith(");");
		assertThat(whenLines).contains(".when().async()").doesNotContain(".when().async();");
	}

	@Test
	void request_model_is_built_for_a_multipart_contract() throws IOException {
		RequestModel model = requestModelFor(MULTIPART_CONTRACT);

		assertThat(model).as("multipart contract must flow through the structured path").isNotNull();
		// given chain: the multipart lines come AFTER the header line (legacy order head,
		// headers, cookies, body, multipart) — a plain .param(...) and a named
		// .multiPart()
		List<String> givenLines = model.given().continuations();
		int headerIndex = indexOfPrefix(givenLines, ".header(");
		int paramIndex = indexOfPrefix(givenLines, ".param(");
		int multiPartIndex = indexOfPrefix(givenLines, ".multiPart(");
		assertThat(headerIndex).as(".header( must be present").isNotNegative();
		assertThat(paramIndex).as(".param( must be present").isNotNegative();
		assertThat(multiPartIndex).as(".multiPart( must be present").isNotNegative();
		assertThat(headerIndex).as("multipart lines must follow the header").isLessThan(paramIndex);
		assertThat(paramIndex).as(".param( must precede .multiPart(").isLessThan(multiPartIndex);
		// the last multipart line is the LAST continuation and gets the trailing `;`
		assertThat(model.given().render()).last(org.assertj.core.api.InstanceOfAssertFactories.STRING).endsWith(");");
	}

	@Test
	void request_model_is_built_for_a_file_body_contract() throws IOException {
		RequestModel model = requestModelFor(FILE_BODY_CONTRACT);

		assertThat(model).as("file-based request body must now flow through the structured path").isNotNull();
		// the .body(...) line is the LAST continuation of the given chain and reads the
		// referenced file (e.g. new String(fileToBytes(this,
		// "..._request_request.json")))
		assertThat(model.given().continuations()).last(org.assertj.core.api.InstanceOfAssertFactories.STRING)
			.startsWith(".body(")
			.contains("fileToBytes(this,");
		assertThat(model.given().render()).last(org.assertj.core.api.InstanceOfAssertFactories.STRING).endsWith(");");
	}

	@Test
	void response_model_is_built_for_a_status_and_header_contract() throws IOException {
		ResponseModel model = responseModelFor(RESPONSE_HEADER_CONTRACT);

		assertThat(model).as("a status+header response must flow through the structured path").isNotNull();
		List<String> lines = model.thenBlock().render();
		// the status-code assertion is the first structured then statement
		assertThat(lines).first(org.assertj.core.api.InstanceOfAssertFactories.STRING)
			.isEqualTo("assertThat(response.statusCode()).isEqualTo(200);");
		// the header assertion follows
		assertThat(lines).anySatisfy((line) -> assertThat(line).startsWith("assertThat(response.header(\"X-Reply\"))"));
		// every structured then statement is ;-terminated (standalone statements, not a
		// fluent chain)
		assertThat(lines).allSatisfy((line) -> assertThat(line).endsWith(";"));
	}

	@Test
	void response_model_is_built_for_a_status_and_cookie_contract() throws IOException {
		ResponseModel model = responseModelFor(RESPONSE_COOKIE_CONTRACT);

		assertThat(model).as("a status+cookie response must now flow through the structured path").isNotNull();
		List<String> lines = model.thenBlock().render();
		// the status-code assertion is the first structured then statement
		assertThat(lines).first(org.assertj.core.api.InstanceOfAssertFactories.STRING)
			.isEqualTo("assertThat(response.statusCode()).isEqualTo(200);");
		// each cookie contributes a null-check followed by a value assertion, both
		// present
		assertThat(lines).contains("assertThat(response.cookie(\"session\")).isNotNull();")
			.contains("assertThat(response.cookie(\"session\")).isEqualTo(\"abc123\");");
		// the null-check precedes the value assertion for the cookie
		int nullCheckIndex = lines.indexOf("assertThat(response.cookie(\"session\")).isNotNull();");
		int valueIndex = lines.indexOf("assertThat(response.cookie(\"session\")).isEqualTo(\"abc123\");");
		assertThat(nullCheckIndex).as("cookie null-check must precede its value assertion").isLessThan(valueIndex);
		// every structured then statement is ;-terminated (standalone statements, not a
		// fluent chain)
		assertThat(lines).allSatisfy((line) -> assertThat(line).endsWith(";"));
	}

	private ResponseModel responseModelFor(String contractDsl) throws IOException {
		File file = Files.createTempFile(this.tmpDir, "contract", ".groovy").toFile();
		Files.write(file.toPath(), contractDsl.getBytes());
		Collection<ContractMetadata> contracts = Collections.singletonList(new ContractMetadata(file.toPath(), false, 1,
				null, ContractVerifierDslConverter.convertAsCollection(new File("/"), file)));
		ContractVerifierConfigProperties properties = new ContractVerifierConfigProperties();
		properties.setTestFramework(TestFramework.JUNIT5);
		properties.setTestMode(TestMode.MOCKMVC);
		properties.setGeneratedTestSourcesDir(this.tmpDir.toFile());
		properties.setGeneratedTestResourcesDir(this.tmpDir.toFile());
		SingleTestGenerator.GeneratedClassData data = new SingleTestGenerator.GeneratedClassData("FooTest", "test",
				file.toPath());
		GeneratedClassMetaData meta = new GeneratedClassMetaData(properties, contracts, "com/example", data);
		SingleContractMetadata scm = meta.toSingleContractMetadata().iterator().next();
		return this.responseModelBuilder.build(scm, TestFramework.JUNIT5, meta, TestMode.MOCKMVC);
	}

	private RequestModel requestModelFor(String contractDsl) throws IOException {
		File file = Files.createTempFile(this.tmpDir, "contract", ".groovy").toFile();
		Files.write(file.toPath(), contractDsl.getBytes());
		// The DSL's file('request.json') is resolved (and validated to exist) at parse
		// time
		// via the context classloader; a matching resource lives at the test classpath
		// root.
		Collection<ContractMetadata> contracts = Collections.singletonList(new ContractMetadata(file.toPath(), false, 1,
				null, ContractVerifierDslConverter.convertAsCollection(new File("/"), file)));
		ContractVerifierConfigProperties properties = new ContractVerifierConfigProperties();
		properties.setTestFramework(TestFramework.JUNIT5);
		properties.setTestMode(TestMode.MOCKMVC);
		// A file-based body materialises the referenced file next to the generated test,
		// which the BodyReader mirrors into the generated test sources/resources dirs.
		properties.setGeneratedTestSourcesDir(this.tmpDir.toFile());
		properties.setGeneratedTestResourcesDir(this.tmpDir.toFile());
		SingleTestGenerator.GeneratedClassData data = new SingleTestGenerator.GeneratedClassData("FooTest", "test",
				file.toPath());
		GeneratedClassMetaData meta = new GeneratedClassMetaData(properties, contracts, "com/example", data);
		SingleContractMetadata scm = meta.toSingleContractMetadata().iterator().next();
		return this.requestModelBuilder.build(scm, TestFramework.JUNIT5, meta, TestMode.MOCKMVC);
	}

	private Collection<ContractMetadata> contracts(boolean ignored) throws IOException {
		File file = Files.createTempFile(this.tmpDir, "contract", ".groovy").toFile();
		Files.write(file.toPath(), CONTRACT.getBytes());
		return Collections.singletonList(new ContractMetadata(file.toPath(), ignored, 1, null,
				ContractVerifierDslConverter.convertAsCollection(new File("/"), file)));
	}

}
