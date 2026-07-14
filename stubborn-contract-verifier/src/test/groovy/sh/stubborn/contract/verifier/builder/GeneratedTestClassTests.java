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
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Stream;

import org.assertj.core.api.BDDAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import sh.stubborn.contract.verifier.config.ContractVerifierConfigProperties;
import sh.stubborn.contract.verifier.config.TestFramework;
import sh.stubborn.contract.verifier.file.ContractMetadata;

import static sh.stubborn.contract.verifier.util.ContractVerifierDslConverter.convertAsCollection;

public class GeneratedTestClassTests {

	// @formatter:off
	String contract = "sh.stubborn.contract.spec.Contract.make {\n"
			+ " name \"foo\"\n"
			+ " request {\n"
			+ "  method 'PUT'\n"
			+ "  url 'url'\n"
			+ "  headers {\n"
			+ "    header('foo', 'bar')\n"
			+ "  }\n"
			+ "  body (\n"
			+ "    [\"foo1\":\"bar1\"]\n"
			+ "  )\n"
			+ " }\n"
			+ " response {\n"
			+ "  status OK()\n"
			+ "  headers {\n"
			+ "    header('foo2', 'bar2')\n"
			+ "  }\n"
			+ "  body (\n"
			+ "    [\"foo3\":\"bar3\"]\n"
			+ "  )\n"
			+ " }\n"
			+ "}";
	// @formatter:on

	// @formatter:off
	String expectedTest = "package test;\n"
+ "\n"
+ "import BazBar;\n"
+ "import com.jayway.jsonpath.DocumentContext;\n"
+ "import com.jayway.jsonpath.JsonPath;\n"
+ "import org.junit.jupiter.api.Test;\n"
+ "import org.junit.jupiter.api.extension.ExtendWith;\n"
+ "import org.junit.jupiter.api.Disabled;\n"
+ "import org.junit.jupiter.api.TestMethodOrder;\n"
+ "import org.junit.jupiter.api.MethodOrderer;\n"
+ "import io.restassured.module.mockmvc.specification.MockMvcRequestSpecification;\n"
+ "import io.restassured.response.ResponseOptions;\n"
+ "\n"
+ "import static sh.stubborn.contract.verifier.assertion.SpringCloudContractAssertions.assertThat;\n"
+ "import static sh.stubborn.contract.verifier.util.ContractVerifierUtil.*;\n"
+ "import static com.toomuchcoding.jsonassert.JsonAssertion.assertThatJson;\n"
+ "import static io.restassured.module.mockmvc.RestAssuredMockMvc.*;\n"
+ "\n"
+ "@SuppressWarnings(\"rawtypes\")\n"
+ "@TestMethodOrder(MethodOrderer.MethodName.class)"
+ "public class FooBarTest extends BazBar {\n"
+ "\n"
+ "\t@Test\n"
+ "\t@Disabled\n"
+ "\tpublic void validate_foo() throws Exception {\n"
+ "\t\t// given:\n"
+ "\t\t\tMockMvcRequestSpecification request = given()\n"
+ "\t\t\t\t\t.header(\"foo\", \"bar\")\n"
+ "\t\t\t\t\t.body(\"{\\\"foo1\\\":\\\"bar1\\\"}\");\n"
+ "\n"
+ "\t\t// when:\n"
+ "\t\t\tResponseOptions response = given().spec(request)\n"
+ "\t\t\t\t\t.put(\"url\");\n"
+ "\n"
+ "\t\t// then:\n"
+ "\t\t\tassertThat(response.statusCode()).isEqualTo(200);\n"
+ "\t\t\tassertThat(response.header(\"foo2\")).isEqualTo(\"bar2\");\n"
+ "\n"
+ "\t\t// and:\n"
+ "\t\t\tDocumentContext parsedJson = JsonPath.parse(response.getBody().asString());\n"
+ "\t\t\tassertThatJson(parsedJson).field(\"['foo3']\").isEqualTo(\"bar3\");\n"
+ "\t}\n"
+ "\n"
+ "}\n";
	// @formatter:on

	@TempDir
	Path tmpDir;

	File file;

	File tmp;

	@BeforeEach
	public void setup() throws IOException, URISyntaxException {
		this.file = Files.createTempFile(tmpDir, "tmp", ".groovy").toFile();
		Files.write(this.file.toPath(), this.contract.getBytes());
		this.tmp = Files.createTempDirectory(tmpDir, "tmp").toFile();
		File classpath = new File(GeneratedTestClassTests.class.getResource("/classpath/").toURI());
		copyRecursively(classpath.toPath(), this.tmp.toPath());
	}

	private static void copyRecursively(Path src, Path dst) throws IOException {
		try (Stream<Path> stream = Files.walk(src)) {
			stream.forEach(source -> {
				Path destination = dst.resolve(src.relativize(source));
				try {
					if (Files.isDirectory(source)) {
						Files.createDirectories(destination);
					}
					else {
						Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
					}
				}
				catch (IOException e) {
					throw new RuntimeException(e);
				}
			});
		}
	}

	@Test
	public void should_work_for_junit5_mockmvc_json_non_binary() {
		// given
		JavaTestGenerator generator = new JavaTestGenerator();
		ContractVerifierConfigProperties configProperties = new ContractVerifierConfigProperties();
		configProperties.setTestFramework(TestFramework.JUNIT5);
		Collection<ContractMetadata> contracts = Collections.singletonList(
				new ContractMetadata(this.file.toPath(), true, 1, 2, convertAsCollection(new File("/"), this.file)));
		String includedDirectoryRelativePath = "some/path";
		String convertedClassName = "fooBar";
		String packageName = "test";
		Path classPath = new File("/tmp").toPath();
		configProperties.setBaseClassForTests("BazBar");

		// when
		String builtClass = generator.buildClass(configProperties, contracts, includedDirectoryRelativePath,
				new SingleTestGenerator.GeneratedClassData(convertedClassName, packageName, classPath));

		// then
		BDDAssertions.then(builtClass).isEqualTo(this.expectedTest);
	}

}
