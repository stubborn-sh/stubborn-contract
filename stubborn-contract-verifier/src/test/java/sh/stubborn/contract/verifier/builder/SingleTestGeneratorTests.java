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
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.util.FileSystemUtils;
import sh.stubborn.contract.verifier.TestGenerator;
import sh.stubborn.contract.verifier.config.ContractVerifierConfigProperties;
import sh.stubborn.contract.verifier.config.TestFramework;
import sh.stubborn.contract.verifier.file.ContractMetadata;
import sh.stubborn.contract.verifier.util.SyntaxChecker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.util.StringUtils.countOccurrencesOf;
import static sh.stubborn.contract.verifier.config.TestFramework.JUNIT5;
import static sh.stubborn.contract.verifier.config.TestFramework.SPOCK;
import static sh.stubborn.contract.verifier.config.TestFramework.TESTNG;
import static sh.stubborn.contract.verifier.config.TestMode.EXPLICIT;
import static sh.stubborn.contract.verifier.config.TestMode.JAXRSCLIENT;
import static sh.stubborn.contract.verifier.config.TestMode.MOCKMVC;
import static sh.stubborn.contract.verifier.util.ContractVerifierDslConverter.convertAsCollection;

class SingleTestGeneratorTests {

	@TempDir
	File tmpFolder;

	File file;

	File tmp;

	private static final List<String> MOCK_MVC_JUNIT5_REST_ASSURED3_CLASS_STRINGS = Arrays.asList(
			"import com.jayway.jsonpath.DocumentContext;", "import com.jayway.jsonpath.JsonPath;",
			"import org.junit.jupiter.api.Disabled;", "import org.junit.jupiter.api.Test;",
			"import static com.toomuchcoding.jsonassert.JsonAssertion.assertThatJson;",
			"import static io.restassured.module.mockmvc.RestAssuredMockMvc.*;", "@Test", "@Disabled",
			"import io.restassured.module.mockmvc.specification.MockMvcRequestSpecification;",
			"import io.restassured.response.ResponseOptions;",
			"import static sh.stubborn.contract.verifier.assertion.SpringCloudContractAssertions.assertThat",
			"import org.junit.jupiter.api.TestMethodOrder", "import org.junit.jupiter.api.MethodOrderer",
			"@TestMethodOrder(MethodOrderer.MethodName.class)");

	private static final List<String> EXPLICIT_JUNIT5_REST_ASSURED3_CLASS_STRINGS = Arrays.asList(
			"import com.jayway.jsonpath.DocumentContext;", "import com.jayway.jsonpath.JsonPath;",
			"import org.junit.jupiter.api.Disabled;", "import org.junit.jupiter.api.Test;",
			"import static com.toomuchcoding.jsonassert.JsonAssertion.assertThatJson;",
			"import static io.restassured.RestAssured.*;", "@Test", "@Disabled",
			"import io.restassured.specification.RequestSpecification;", "import io.restassured.response.Response;",
			"import static sh.stubborn.contract.verifier.assertion.SpringCloudContractAssertions.assertThat",
			"org.junit.jupiter.api.TestMethodOrder", "org.junit.jupiter.api.MethodOrderer",
			"@TestMethodOrder(MethodOrderer.MethodName.class)");

	private static final List<String> MOCK_MVC_TESTNG_REST_ASSURED3_CLASS_STRINGS = Arrays.asList(
			"import com.jayway.jsonpath.DocumentContext;", "import com.jayway.jsonpath.JsonPath;",
			"import org.testng.annotations.Test;",
			"import static com.toomuchcoding.jsonassert.JsonAssertion.assertThatJson;",
			"import static io.restassured.module.mockmvc.RestAssuredMockMvc.*;", "@Test",
			"import io.restassured.module.mockmvc.specification.MockMvcRequestSpecification;",
			"import io.restassured.response.ResponseOptions;",
			"import static sh.stubborn.contract.verifier.assertion.SpringCloudContractAssertions.assertThat");

	private static final List<String> EXPLICIT_TESTNG_REST_ASSURED3_CLASS_STRINGS = Arrays.asList(
			"import com.jayway.jsonpath.DocumentContext;", "import com.jayway.jsonpath.JsonPath;",
			"import org.testng.annotations.Test;",
			"import static com.toomuchcoding.jsonassert.JsonAssertion.assertThatJson;",
			"import static io.restassured.RestAssured.*;", "@Test",
			"import io.restassured.specification.RequestSpecification;", "import io.restassured.response.Response;",
			"import static sh.stubborn.contract.verifier.assertion.SpringCloudContractAssertions.assertThat");

	private static final List<String> SPOCK_CLASS_REST_ASSURED3_STRINGS = Arrays.asList(
			"import com.jayway.jsonpath.DocumentContext", "import com.jayway.jsonpath.JsonPath",
			"import spock.lang.Ignore", "import spock.lang.Specification", "import spock.lang.Stepwise",
			"import static com.toomuchcoding.jsonassert.JsonAssertion.assertThatJson",
			"import static io.restassured.module.mockmvc.RestAssuredMockMvc.*", "@Stepwise", "@Ignore",
			"sh.stubborn.contract.verifier.assertion.SpringCloudContractAssertions.assertThat");

	private static final List<String> EXPLICIT_SPOCK_REST_ASSURED2_CLASS_STRINGS = Arrays.asList(
			"import com.jayway.jsonpath.DocumentContext", "import com.jayway.jsonpath.JsonPath",
			"import spock.lang.Ignore", "import spock.lang.Specification", "import spock.lang.Stepwise",
			"import static com.toomuchcoding.jsonassert.JsonAssertion.assertThatJson",
			"import static com.jayway.restassured.RestAssured.*", "@Stepwise", "@Ignore",
			"sh.stubborn.contract.verifier.assertion.SpringCloudContractAssertions.assertThat");

	private static final List<String> EXPLICIT_SPOCK_REST_ASSURED3_CLASS_STRINGS = Arrays.asList(
			"import com.jayway.jsonpath.DocumentContext", "import com.jayway.jsonpath.JsonPath",
			"import spock.lang.Ignore", "import spock.lang.Specification", "import spock.lang.Stepwise",
			"import static com.toomuchcoding.jsonassert.JsonAssertion.assertThatJson",
			"import static io.restassured.RestAssured.*", "@Stepwise", "@Ignore",
			"sh.stubborn.contract.verifier.assertion.SpringCloudContractAssertions.assertThat");

	static final Consumer<String> JAVA_ASSERTER = classToTest -> {
		int name = Math.abs(new java.util.Random().nextInt());
		String changedTest = classToTest.replace("public class Test", "public class Test" + name)
			.replace("public class ContractsTest", "public class Test" + name);
		String fqn = fqn(classToTest);
		SyntaxChecker.tryToCompileJavaWithoutImports(fqn + name, changedTest);
	};

	static String fqn(String classToTest) {
		return classToTest.contains("0_1_0_dev_1_uncommitted_d1174dd")
				? "sh.stubborn.contract.verifier.tests.com_uscm.dale_api44_spec._0_1_0_dev_1_uncommitted_d1174dd.Test"
				: "test.Test";
	}

	static final Consumer<String> JAVA_JAXRS_ASSERTER = classToTest -> {
		int name = Math.abs(new java.util.Random().nextInt());
		String changedTest = classToTest
			.replace("public class Test {",
					"public class Test" + name + " {\njavax.ws.rs.client.WebTarget webTarget;\n")
			.replace("public class ContractsTest {",
					"public class Test" + name + " {\njavax.ws.rs.client.WebTarget webTarget;\n");
		String fqn = fqn(classToTest);
		SyntaxChecker.tryToCompileJavaWithoutImports(fqn + name, changedTest);
	};

	static final Consumer<String> GROOVY_ASSERTER = classToTest -> SyntaxChecker
		.tryToCompileGroovyWithoutImports(classToTest);

	@BeforeEach
	void setup() throws IOException, URISyntaxException {
		file = File.createTempFile("contract", ".groovy", tmpFolder);
		writeContract(file);
		tmp = new File(tmpFolder, "tmp_" + System.nanoTime());
		tmp.mkdirs();
		File classpath = new File(SingleTestGeneratorTests.class.getResource("/classpath/").toURI());
		FileSystemUtils.copyRecursively(classpath, tmp);
		java.net.URI resource = SingleTestGeneratorTests.class.getResource("/request.json") != null
				? SingleTestGeneratorTests.class.getResource("/request.json").toURI() : null;
		if (resource != null) {
			new File(resource).delete();
		}
		resource = SingleTestGeneratorTests.class.getResource("/response.json") != null
				? SingleTestGeneratorTests.class.getResource("/response.json").toURI() : null;
		if (resource != null) {
			new File(resource).delete();
		}
	}

	private static void writeContract(File file) throws IOException {
		java.nio.file.Files.writeString(file.toPath(),
				"\n\t\t\t\t\tsh.stubborn.contract.spec.Contract.make {\n" + "\t\t\t\t\t\trequest {\n"
						+ "\t\t\t\t\t\t\tmethod 'PUT'\n" + "\t\t\t\t\t\t\turl 'url'\n" + "\t\t\t\t\t\t}\n"
						+ "\t\t\t\t\t\tresponse {\n" + "\t\t\t\t\t\t\tstatus OK()\n"
						+ "\t\t\t\t\t\t\tbody([\"foo\" : \"bar\"])\n" + "\t\t\t\t\t\t}\n" + "\t\t\t\t\t}\n");
	}

	static Stream<Arguments> shouldBuildTestClassForFrameworkAndMode() {
		return Stream.of(Arguments.of(JUNIT5, MOCKMVC, MOCK_MVC_JUNIT5_REST_ASSURED3_CLASS_STRINGS, JAVA_ASSERTER),
				Arguments.of(JUNIT5, EXPLICIT, EXPLICIT_JUNIT5_REST_ASSURED3_CLASS_STRINGS, JAVA_ASSERTER),
				Arguments.of(TESTNG, MOCKMVC, MOCK_MVC_TESTNG_REST_ASSURED3_CLASS_STRINGS, JAVA_ASSERTER),
				Arguments.of(TESTNG, EXPLICIT, EXPLICIT_TESTNG_REST_ASSURED3_CLASS_STRINGS, JAVA_ASSERTER),
				Arguments.of(SPOCK, MOCKMVC, SPOCK_CLASS_REST_ASSURED3_STRINGS, GROOVY_ASSERTER),
				Arguments.of(SPOCK, EXPLICIT, EXPLICIT_SPOCK_REST_ASSURED3_CLASS_STRINGS, GROOVY_ASSERTER));
	}

	@ParameterizedTest
	@MethodSource("shouldBuildTestClassForFrameworkAndMode")
	void should_build_test_class_for_framework_and_mode(TestFramework testFramework,
			sh.stubborn.contract.verifier.config.TestMode mode, List<String> classStrings, Consumer<String> asserter)
			throws IOException {
		ContractVerifierConfigProperties properties = new ContractVerifierConfigProperties();
		properties.setTestFramework(testFramework);
		properties.setTestMode(mode);
		ContractMetadata contract = new ContractMetadata(file.toPath(), true, 1, 2,
				convertAsCollection(new File("/"), file));
		JavaTestGenerator testGenerator = new JavaTestGenerator();

		String clazz = testGenerator.buildClass(properties, List.of(contract), "com/foo",
				new SingleTestGenerator.GeneratedClassData("test", "test", file.toPath()));

		for (String s : classStrings) {
			assertThat(clazz).as("Expected class to contain: " + s).contains(s);
		}
		asserter.accept(clazz);
	}

	static Stream<Arguments> shouldBuildTestClassWhenPathContainsBizarreSigns() {
		return Stream.of(Arguments.of(JUNIT5, MOCKMVC, JAVA_ASSERTER, "ContractsTest.java"),
				Arguments.of(JUNIT5, EXPLICIT, JAVA_ASSERTER, "ContractsTest.java"),
				Arguments.of(TESTNG, MOCKMVC, JAVA_ASSERTER, "ContractsTest.java"),
				Arguments.of(TESTNG, EXPLICIT, JAVA_ASSERTER, "ContractsTest.java"),
				Arguments.of(SPOCK, MOCKMVC, GROOVY_ASSERTER, "ContractsSpec.groovy"),
				Arguments.of(SPOCK, EXPLICIT, GROOVY_ASSERTER, "ContractsSpec.groovy"));
	}

	@ParameterizedTest
	@MethodSource("shouldBuildTestClassWhenPathContainsBizarreSigns")
	void should_build_test_class_when_path_contains_bizarre_signs(TestFramework testFramework,
			sh.stubborn.contract.verifier.config.TestMode mode, Consumer<String> asserter, String testName)
			throws IOException {
		ContractVerifierConfigProperties properties = new ContractVerifierConfigProperties();
		properties.setTestFramework(testFramework);
		properties.setBasePackageForTests("sh.stubborn.contract.verifier.tests");

		File newFolder = new File(tmpFolder, "META_INF");
		newFolder.mkdirs();
		File subfolders = new File(newFolder, "/com.uscm/dale_api44_spec/0.1.0_dev.1.uncommitted+d1174dd/contracts/");
		subfolders.mkdirs();
		File newFile = new File(subfolders, "contract.groovy");
		newFile.createNewFile();
		writeContract(newFile);
		properties.setContractsDslDir(newFolder);
		properties.setGeneratedTestSourcesDir(newFolder.getParentFile());
		properties.setGeneratedTestResourcesDir(newFolder.getParentFile());

		int size = new TestGenerator(properties).generate();

		assertThat(size).isGreaterThan(0);
		String testContent = Files.readString(new File(newFolder.getParent(),
				"/sh/stubborn/contract/verifier/tests/com_uscm/dale_api44_spec/_0_1_0_dev_1_uncommitted_d1174dd/"
						+ testName)
			.toPath());
		asserter.accept(testContent);
	}

	static Stream<Arguments> shouldBuildTestClassWithTwoFiles() {
		return Stream.of(
				Arguments.of(JUNIT5, MOCKMVC, JAVA_ASSERTER,
						(Consumer<String>) test -> assertThat(
								countOccurrencesOf(test, "\t\t\tMockMvcRequestSpecification"))
							.isEqualTo(2)),
				Arguments.of(JUNIT5, EXPLICIT, JAVA_ASSERTER,
						(Consumer<String>) test -> assertThat(
								countOccurrencesOf(test, "\t\t\tMockMvcRequestSpecification"))
							.isEqualTo(2)),
				Arguments.of(TESTNG, MOCKMVC, JAVA_ASSERTER,
						(Consumer<String>) test -> assertThat(
								countOccurrencesOf(test, "\t\t\tMockMvcRequestSpecification"))
							.isEqualTo(2)),
				Arguments.of(TESTNG, EXPLICIT, JAVA_ASSERTER,
						(Consumer<String>) test -> assertThat(
								countOccurrencesOf(test, "\t\t\tMockMvcRequestSpecification"))
							.isEqualTo(2)),
				Arguments.of(SPOCK, MOCKMVC, GROOVY_ASSERTER,
						(Consumer<String>) test -> assertThat(
								countOccurrencesOf(test, "\t\t\tMockMvcRequestSpecification"))
							.isEqualTo(2)),
				Arguments
					.of(SPOCK, EXPLICIT, GROOVY_ASSERTER,
							(Consumer<String>) test -> assertThat(
									countOccurrencesOf(test, "\t\t\tMockMvcRequestSpecification request"))
								.isEqualTo(2)));
	}

	@ParameterizedTest
	@MethodSource("shouldBuildTestClassWithTwoFiles")
	void should_build_test_class_for_framework_and_mode_with_two_files(TestFramework testFramework,
			sh.stubborn.contract.verifier.config.TestMode mode, Consumer<String> asserter,
			Consumer<String> textAssertion) throws IOException {
		File f1 = File.createTempFile("contract1", ".groovy", tmpFolder);
		f1.deleteOnExit();
		java.nio.file.Files.writeString(f1.toPath(), "\n\t\t\t\t\tsh.stubborn.contract.spec.Contract.make {\n"
				+ "\t\t\t\t\t\trequest {\n" + "\t\t\t\t\t\t\tmethod 'PUT'\n" + "\t\t\t\t\t\t\turl 'url1'\n"
				+ "\t\t\t\t\t\t\theaders { contentType(applicationJson()) }\n" + "\t\t\t\t\t\t}\n"
				+ "\t\t\t\t\t\tresponse {\n" + "\t\t\t\t\t\t\tstatus OK()\n"
				+ "\t\t\t\t\t\t\tbody(foo:\"foo\", bar:\"bar\")\n"
				+ "\t\t\t\t\t\t\theaders { contentType(applicationJson()) }\n" + "\t\t\t\t\t\t}\n" + "\t\t\t\t\t}\n");
		File f2 = File.createTempFile("contract2", ".groovy", tmpFolder);
		f2.deleteOnExit();
		java.nio.file.Files.writeString(f2.toPath(), "\n\t\t\t\t\tsh.stubborn.contract.spec.Contract.make {\n"
				+ "\t\t\t\t\t\trequest {\n" + "\t\t\t\t\t\t\tmethod 'PUT'\n" + "\t\t\t\t\t\t\turl 'url2'\n"
				+ "\t\t\t\t\t\t\theaders { contentType(applicationJson()) }\n" + "\t\t\t\t\t\t}\n"
				+ "\t\t\t\t\t\tresponse {\n" + "\t\t\t\t\t\t\tstatus OK()\n"
				+ "\t\t\t\t\t\t\tbody(foo:\"foo\", bar:\"bar\")\n"
				+ "\t\t\t\t\t\t\theaders { contentType(applicationJson()) }\n" + "\t\t\t\t\t\t}\n" + "\t\t\t\t\t}\n");
		ContractVerifierConfigProperties properties = new ContractVerifierConfigProperties();
		properties.setTestFramework(testFramework);
		ContractMetadata contract = new ContractMetadata(f1.toPath(), false, 1, null,
				convertAsCollection(new File("/"), f1));
		ContractMetadata contract2 = new ContractMetadata(f2.toPath(), false, 1, null,
				convertAsCollection(new File("/"), f2));
		JavaTestGenerator testGenerator = new JavaTestGenerator();

		String clazz = testGenerator.buildClass(properties, List.of(contract, contract2), "com/foo",
				new SingleTestGenerator.GeneratedClassData("test", "test", f1.toPath()));

		asserter.accept(clazz);
		textAssertion.accept(clazz);
	}

	static Stream<Arguments> shouldBuildJaxRsTestClass() {
		return Stream.of(
				Arguments.of(JUNIT5,
						List.of("import static javax.ws.rs.client.Entity.*", "import javax.ws.rs.core.Response"),
						JAVA_JAXRS_ASSERTER),
				Arguments.of(TESTNG,
						List.of("import static javax.ws.rs.client.Entity.*", "import javax.ws.rs.core.Response"),
						JAVA_JAXRS_ASSERTER),
				Arguments.of(SPOCK, List.of("import static javax.ws.rs.client.Entity.*"), GROOVY_ASSERTER));
	}

	@ParameterizedTest
	@MethodSource("shouldBuildJaxRsTestClass")
	void should_build_JaxRs_test_class(TestFramework testFramework, List<String> classStrings,
			Consumer<String> asserter) throws IOException {
		ContractVerifierConfigProperties properties = new ContractVerifierConfigProperties();
		properties.setTestMode(JAXRSCLIENT);
		properties.setTestFramework(testFramework);
		ContractMetadata contract = new ContractMetadata(file.toPath(), true, 1, null,
				convertAsCollection(new File("/"), file));
		SingleTestGenerator testGenerator = new JavaTestGenerator();

		String clazz = testGenerator.buildClass(properties, List.of(contract), "com/foo",
				new SingleTestGenerator.GeneratedClassData("test", "test", file.toPath()));

		for (String s : classStrings) {
			assertThat(clazz).as("Expected class to contain: " + s).contains(s);
		}
		asserter.accept(clazz);
	}

	static Stream<Arguments> shouldWorkIfThereIsMessagingAndRestInOneFolder() {
		return Stream.of(Arguments.of(JUNIT5, MOCK_MVC_JUNIT5_REST_ASSURED3_CLASS_STRINGS, JAVA_ASSERTER),
				Arguments.of(TESTNG, MOCK_MVC_TESTNG_REST_ASSURED3_CLASS_STRINGS, JAVA_ASSERTER),
				Arguments.of(SPOCK, SPOCK_CLASS_REST_ASSURED3_STRINGS, GROOVY_ASSERTER));
	}

	@ParameterizedTest
	@MethodSource("shouldWorkIfThereIsMessagingAndRestInOneFolder")
	void should_work_if_there_is_messaging_and_rest_in_one_folder(TestFramework testFramework,
			List<String> classStrings, Consumer<String> asserter) throws IOException {
		File secondFile = File.createTempFile("contract", ".groovy", tmpFolder);
		java.nio.file.Files.writeString(secondFile.toPath(),
				"\n\t\t\t\t\t\tsh.stubborn.contract.spec.Contract.make {\n" + "\t\t\t\t\t\t  ignored()\n"
						+ "\t\t\t\t\t\t  label 'some_label'\n" + "\t\t\t\t\t\t  input {\n"
						+ "\t\t\t\t\t\t\ttriggeredBy(\"hashCode()\")\n" + "\t\t\t\t\t\t\tassertThat('hashCode()')\n"
						+ "\t\t\t\t\t\t  }\n" + "\t\t\t\t\t\t}\n");
		ContractVerifierConfigProperties properties = new ContractVerifierConfigProperties();
		properties.setGeneratedTestResourcesDir(file.getParentFile());
		properties.setGeneratedTestSourcesDir(file.getParentFile());
		properties.setTestFramework(testFramework);
		ContractMetadata contract = new ContractMetadata(file.toPath(), true, 1, 2,
				convertAsCollection(new File("/"), file));
		ContractMetadata contract2 = new ContractMetadata(secondFile.toPath(), true, 1, 2,
				convertAsCollection(new File("/"), secondFile));
		JavaTestGenerator testGenerator = new JavaTestGenerator();

		String clazz = testGenerator.buildClass(properties, List.of(contract, contract2), "com/foo",
				new SingleTestGenerator.GeneratedClassData("test", "test", file.toPath()));

		for (String s : classStrings) {
			assertThat(clazz).as("Expected class to contain: " + s).contains(s);
		}
		assertThat(clazz).contains("@Autowired ContractVerifierMessaging");
		asserter.accept(clazz);
	}

	static Stream<Arguments> shouldIgnoreTestIfContractIsIgnoredInDsl() {
		return Stream.of(Arguments.of(JUNIT5, "@Disabled", JAVA_ASSERTER),
				Arguments.of(TESTNG, "@Test(enabled = false)", JAVA_ASSERTER),
				Arguments.of(SPOCK, "@Ignore", GROOVY_ASSERTER));
	}

	@ParameterizedTest
	@MethodSource("shouldIgnoreTestIfContractIsIgnoredInDsl")
	void should_ignore_a_test_if_the_contract_is_ignored_in_the_dsl(TestFramework testFramework,
			String ignoreAnnotation, Consumer<String> asserter) throws IOException {
		File secondFile = File.createTempFile("contract", ".groovy", tmpFolder);
		java.nio.file.Files.writeString(secondFile.toPath(),
				"\n\t\t\t\t\t\tsh.stubborn.contract.spec.Contract.make {\n" + "\t\t\t\t\t\t\tignored()\n"
						+ "\t\t\t\t\t\t\trequest {\n" + "\t\t\t\t\t\t\t\tmethod 'PUT'\n" + "\t\t\t\t\t\t\t\turl 'url'\n"
						+ "\t\t\t\t\t\t\t}\n" + "\t\t\t\t\t\t\tresponse {\n" + "\t\t\t\t\t\t\t\tstatus OK()\n"
						+ "\t\t\t\t\t\t\t}\n" + "\t\t\t\t\t\t}\n");
		ContractVerifierConfigProperties properties = new ContractVerifierConfigProperties();
		properties.setTestFramework(testFramework);
		ContractMetadata contract2 = new ContractMetadata(secondFile.toPath(), true, 1, 2,
				convertAsCollection(new File("/"), file));
		JavaTestGenerator testGenerator = new JavaTestGenerator();

		String clazz = testGenerator.buildClass(properties, List.of(contract2), "com/foo",
				new SingleTestGenerator.GeneratedClassData("test", "test", file.toPath()));

		assertThat(clazz).contains(ignoreAnnotation);
		asserter.accept(clazz);
	}

	static Stream<Arguments> shouldOnlyIgnoreTestForIgnoredContractIfContractIsIgnoredByConfiguration() {
		return Stream.of(Arguments.of(JUNIT5, "@Disabled"), Arguments.of(TESTNG, "@Test(enabled = false)"),
				Arguments.of(SPOCK, "@Ignore"));
	}

	@ParameterizedTest
	@MethodSource("shouldOnlyIgnoreTestForIgnoredContractIfContractIsIgnoredByConfiguration")
	void should_only_ignore_test_for_ignored_contract_if_contract_is_ignored_by_configuration(
			TestFramework testFramework, String ignoreAnnotation) throws IOException {
		File fileToIgnore = new File(tmpFolder, "toIgnore.groovy");
		fileToIgnore.createNewFile();
		writeContract(fileToIgnore);
		ContractMetadata contractToIgnore = new ContractMetadata(fileToIgnore.toPath(), true, 2, 1,
				convertAsCollection(new File("/"), fileToIgnore));

		File fileToCheck = new File(tmpFolder, "toCheck.groovy");
		fileToCheck.createNewFile();
		writeContract(fileToCheck);
		ContractMetadata contractToCheck = new ContractMetadata(fileToCheck.toPath(), false, 2, 2,
				convertAsCollection(new File("/"), fileToCheck));

		ContractVerifierConfigProperties properties = new ContractVerifierConfigProperties();
		properties.setTestFramework(testFramework);
		properties.setIgnoredFiles(List.of(fileToIgnore.getName()));
		JavaTestGenerator testGenerator = new JavaTestGenerator();

		String clazz = testGenerator.buildClass(properties, List.of(contractToCheck, contractToIgnore), "com/foo",
				new SingleTestGenerator.GeneratedClassData("test", "test", file.toPath()));

		assertThat(countOccurrencesOf(clazz, ignoreAnnotation)).isEqualTo(1);
	}

	@Test
	void should_not_allow_the_usage_of_ignore_annotations_for_TestNG() {
		TestFramework testNG = TESTNG;

		assertThatThrownBy(testNG::getIgnoreClass).isInstanceOf(UnsupportedOperationException.class);
		assertThatThrownBy(testNG::getIgnoreAnnotation).isInstanceOf(UnsupportedOperationException.class);
	}

	static Stream<Arguments> shouldIgnore1TestIfOnlyOneOf2ContractsIsIgnoredInContractDsl() {
		return Stream.of(Arguments.of(JUNIT5, "@Disabled", JAVA_ASSERTER),
				Arguments.of(TESTNG, "@Test(enabled = false)", JAVA_ASSERTER),
				Arguments.of(SPOCK, "@Ignore", GROOVY_ASSERTER));
	}

	@ParameterizedTest
	@MethodSource("shouldIgnore1TestIfOnlyOneOf2ContractsIsIgnoredInContractDsl")
	void should_ignore_1_test_if_only_1_of_2_contracts_is_ignored_in_Contract_dsl(TestFramework testFramework,
			String ignoreAnnotation, Consumer<String> asserter) throws IOException {
		File firstFile = File.createTempFile("contract1", ".groovy", tmpFolder);
		java.nio.file.Files.writeString(firstFile.toPath(),
				"\n\t\t\t\t\t\tsh.stubborn.contract.spec.Contract.make {\n" + "\t\t\t\t\t\t\trequest {\n"
						+ "\t\t\t\t\t\t\t\tmethod 'GET'\n" + "\t\t\t\t\t\t\t\turl 'url'\n" + "\t\t\t\t\t\t\t}\n"
						+ "\t\t\t\t\t\t\tresponse {\n" + "\t\t\t\t\t\t\t\tstatus OK()\n" + "\t\t\t\t\t\t\t}\n"
						+ "\t\t\t\t\t\t}\n");

		File secondFile = File.createTempFile("contract2", ".groovy", tmpFolder);
		java.nio.file.Files.writeString(secondFile.toPath(),
				"\n\t\t\t\t\t\tsh.stubborn.contract.spec.Contract.make {\n" + "\t\t\t\t\t\t\tignored()\n"
						+ "\t\t\t\t\t\t\trequest {\n" + "\t\t\t\t\t\t\t\tmethod 'POST'\n"
						+ "\t\t\t\t\t\t\t\turl 'url'\n" + "\t\t\t\t\t\t\t}\n" + "\t\t\t\t\t\t\tresponse {\n"
						+ "\t\t\t\t\t\t\t\tstatus OK()\n" + "\t\t\t\t\t\t\t}\n" + "\t\t\t\t\t\t}\n");

		ContractVerifierConfigProperties properties = new ContractVerifierConfigProperties();
		properties.setTestFramework(testFramework);
		ContractMetadata firstContract = new ContractMetadata(firstFile.toPath(), false, 1, null,
				convertAsCollection(new File("/"), firstFile));
		ContractMetadata secondContract = new ContractMetadata(secondFile.toPath(), false, 1, null,
				convertAsCollection(new File("/"), secondFile));
		JavaTestGenerator testGenerator = new JavaTestGenerator();

		String clazz = testGenerator.buildClass(properties, List.of(secondContract, firstContract), "com/foo",
				new SingleTestGenerator.GeneratedClassData("test", "test", secondFile.toPath()));

		assertThat(countOccurrencesOf(clazz, ignoreAnnotation)).isEqualTo(1);
		asserter.accept(clazz);
	}

	static Stream<Arguments> shouldIgnore2TestsIf2ContractsAreIgnoredInContractDsl() {
		return Stream.of(Arguments.of(JUNIT5, "@Disabled", JAVA_ASSERTER),
				Arguments.of(TESTNG, "@Test(enabled = false)", JAVA_ASSERTER),
				Arguments.of(SPOCK, "@Ignore", GROOVY_ASSERTER));
	}

	@ParameterizedTest
	@MethodSource("shouldIgnore2TestsIf2ContractsAreIgnoredInContractDsl")
	void should_ignore_2_tests_if_2_contracts_are_ignored_in_Contract_dsl(TestFramework testFramework,
			String ignoreAnnotation, Consumer<String> asserter) throws IOException {
		File firstFile = File.createTempFile("contract1", ".groovy", tmpFolder);
		java.nio.file.Files.writeString(firstFile.toPath(),
				"\n\t\t\t\t\t\tsh.stubborn.contract.spec.Contract.make {\n" + "\t\t\t\t\t\t\tignored()\n"
						+ "\t\t\t\t\t\t\trequest {\n" + "\t\t\t\t\t\t\t\tmethod 'GET'\n" + "\t\t\t\t\t\t\t\turl 'url'\n"
						+ "\t\t\t\t\t\t\t}\n" + "\t\t\t\t\t\t\tresponse {\n" + "\t\t\t\t\t\t\t\tstatus OK()\n"
						+ "\t\t\t\t\t\t\t}\n" + "\t\t\t\t\t\t}\n");

		File secondFile = File.createTempFile("contract2", ".groovy", tmpFolder);
		java.nio.file.Files.writeString(secondFile.toPath(),
				"\n\t\t\t\t\t\tsh.stubborn.contract.spec.Contract.make {\n" + "\t\t\t\t\t\t\tignored()\n"
						+ "\t\t\t\t\t\t\trequest {\n" + "\t\t\t\t\t\t\t\tmethod 'POST'\n"
						+ "\t\t\t\t\t\t\t\turl 'url'\n" + "\t\t\t\t\t\t\t}\n" + "\t\t\t\t\t\t\tresponse {\n"
						+ "\t\t\t\t\t\t\t\tstatus OK()\n" + "\t\t\t\t\t\t\t}\n" + "\t\t\t\t\t\t}\n");

		ContractVerifierConfigProperties properties = new ContractVerifierConfigProperties();
		properties.setTestFramework(testFramework);
		ContractMetadata firstContract = new ContractMetadata(firstFile.toPath(), false, 1, null,
				convertAsCollection(new File("/"), firstFile));
		ContractMetadata secondContract = new ContractMetadata(secondFile.toPath(), false, 1, null,
				convertAsCollection(new File("/"), secondFile));
		JavaTestGenerator testGenerator = new JavaTestGenerator();

		String clazz = testGenerator.buildClass(properties, List.of(secondContract, firstContract), "com/foo",
				new SingleTestGenerator.GeneratedClassData("test", "test", secondFile.toPath()));

		assertThat(countOccurrencesOf(clazz, ignoreAnnotation)).isEqualTo(2);
		asserter.accept(clazz);
	}

	static Stream<Arguments> shouldIgnore2TestsIf2ContractsAreIgnoredInContractDslAndInConfiguration() {
		return Stream.of(Arguments.of(JUNIT5, "@Disabled", JAVA_ASSERTER),
				Arguments.of(TESTNG, "@Test(enabled = false)", JAVA_ASSERTER),
				Arguments.of(SPOCK, "@Ignore", GROOVY_ASSERTER));
	}

	@ParameterizedTest
	@MethodSource("shouldIgnore2TestsIf2ContractsAreIgnoredInContractDslAndInConfiguration")
	void should_ignore_2_tests_if_2_contracts_are_ignored_in_Contract_dsl_and_in_Configuration(
			TestFramework testFramework, String ignoreAnnotation, Consumer<String> asserter) throws IOException {
		File firstFile = File.createTempFile("contract1", ".groovy", tmpFolder);
		java.nio.file.Files.writeString(firstFile.toPath(),
				"\n\t\t\t\t\t\tsh.stubborn.contract.spec.Contract.make {\n" + "\t\t\t\t\t\t\tignored()\n"
						+ "\t\t\t\t\t\t\trequest {\n" + "\t\t\t\t\t\t\t\tmethod 'GET'\n" + "\t\t\t\t\t\t\t\turl 'url'\n"
						+ "\t\t\t\t\t\t\t}\n" + "\t\t\t\t\t\t\tresponse {\n" + "\t\t\t\t\t\t\t\tstatus OK()\n"
						+ "\t\t\t\t\t\t\t}\n" + "\t\t\t\t\t\t}\n");

		File secondFile = File.createTempFile("contract2", ".groovy", tmpFolder);
		java.nio.file.Files.writeString(secondFile.toPath(),
				"\n\t\t\t\t\t\tsh.stubborn.contract.spec.Contract.make {\n" + "\t\t\t\t\t\t\tignored()\n"
						+ "\t\t\t\t\t\t\trequest {\n" + "\t\t\t\t\t\t\t\tmethod 'POST'\n"
						+ "\t\t\t\t\t\t\t\turl 'url'\n" + "\t\t\t\t\t\t\t}\n" + "\t\t\t\t\t\t\tresponse {\n"
						+ "\t\t\t\t\t\t\t\tstatus OK()\n" + "\t\t\t\t\t\t\t}\n" + "\t\t\t\t\t\t}\n");

		ContractVerifierConfigProperties properties = new ContractVerifierConfigProperties();
		properties.setTestFramework(testFramework);
		ContractMetadata firstContract = new ContractMetadata(firstFile.toPath(), true, 1, null,
				convertAsCollection(new File("/"), firstFile));
		ContractMetadata secondContract = new ContractMetadata(secondFile.toPath(), true, 1, null,
				convertAsCollection(new File("/"), secondFile));
		JavaTestGenerator testGenerator = new JavaTestGenerator();

		String clazz = testGenerator.buildClass(properties, List.of(secondContract, firstContract), "com/foo",
				new SingleTestGenerator.GeneratedClassData("test", "test", secondFile.toPath()));

		assertThat(countOccurrencesOf(clazz, ignoreAnnotation)).isEqualTo(2);
		asserter.accept(clazz);
	}

	static Stream<Arguments> shouldIgnore1TestsIf1Of2ContractIsIgnoredInContractDslAndInConfiguration() {
		return Stream.of(Arguments.of(JUNIT5, "@Disabled", JAVA_ASSERTER),
				Arguments.of(TESTNG, "@Test(enabled = false)", JAVA_ASSERTER),
				Arguments.of(SPOCK, "@Ignore", GROOVY_ASSERTER));
	}

	@ParameterizedTest
	@MethodSource("shouldIgnore1TestsIf1Of2ContractIsIgnoredInContractDslAndInConfiguration")
	void should_ignore_1_tests_if_1_of_2_contract_is_ignored_in_Contract_dsl_and_in_Configuration(
			TestFramework testFramework, String ignoreAnnotation, Consumer<String> asserter) throws IOException {
		File firstFile = File.createTempFile("contract1", ".groovy", tmpFolder);
		java.nio.file.Files.writeString(firstFile.toPath(),
				"\n\t\t\t\t\t\tsh.stubborn.contract.spec.Contract.make {\n" + "\t\t\t\t\t\t\tignored()\n"
						+ "\t\t\t\t\t\t\trequest {\n" + "\t\t\t\t\t\t\t\tmethod 'GET'\n" + "\t\t\t\t\t\t\t\turl 'url'\n"
						+ "\t\t\t\t\t\t\t}\n" + "\t\t\t\t\t\t\tresponse {\n" + "\t\t\t\t\t\t\t\tstatus OK()\n"
						+ "\t\t\t\t\t\t\t}\n" + "\t\t\t\t\t\t}\n");

		File secondFile = File.createTempFile("contract2", ".groovy", tmpFolder);
		java.nio.file.Files.writeString(secondFile.toPath(),
				"\n\t\t\t\t\t\tsh.stubborn.contract.spec.Contract.make {\n" + "\t\t\t\t\t\t\trequest {\n"
						+ "\t\t\t\t\t\t\t\tmethod 'POST'\n" + "\t\t\t\t\t\t\t\turl 'url'\n" + "\t\t\t\t\t\t\t}\n"
						+ "\t\t\t\t\t\t\tresponse {\n" + "\t\t\t\t\t\t\t\tstatus OK()\n" + "\t\t\t\t\t\t\t}\n"
						+ "\t\t\t\t\t\t}\n");

		ContractVerifierConfigProperties properties = new ContractVerifierConfigProperties();
		properties.setTestFramework(testFramework);
		ContractMetadata firstContract = new ContractMetadata(firstFile.toPath(), true, 1, null,
				convertAsCollection(new File("/"), firstFile));
		ContractMetadata secondContract = new ContractMetadata(secondFile.toPath(), false, 1, null,
				convertAsCollection(new File("/"), secondFile));
		JavaTestGenerator testGenerator = new JavaTestGenerator();

		String clazz = testGenerator.buildClass(properties, List.of(secondContract, firstContract), "com/foo",
				new SingleTestGenerator.GeneratedClassData("test", "test", secondFile.toPath()));

		assertThat(countOccurrencesOf(clazz, ignoreAnnotation)).isEqualTo(1);
		asserter.accept(clazz);
	}

	static Stream<Arguments> shouldIgnore2TestsIf1stContractIsIgnoredInContractDslAnd2ndIsIgnoredInConfiguration() {
		return Stream.of(Arguments.of(JUNIT5, "@Disabled", JAVA_ASSERTER),
				Arguments.of(TESTNG, "@Test(enabled = false)", JAVA_ASSERTER),
				Arguments.of(SPOCK, "@Ignore", GROOVY_ASSERTER));
	}

	@ParameterizedTest
	@MethodSource("shouldIgnore2TestsIf1stContractIsIgnoredInContractDslAnd2ndIsIgnoredInConfiguration")
	void should_ignore_2_tests_if_1st_contracts_is_ignored_in_Contract_dsl_and_2nd_is_ignored_in_Configuration(
			TestFramework testFramework, String ignoreAnnotation, Consumer<String> asserter) throws IOException {
		File firstFile = File.createTempFile("contract1", ".groovy", tmpFolder);
		java.nio.file.Files.writeString(firstFile.toPath(),
				"\n\t\t\t\t\t\tsh.stubborn.contract.spec.Contract.make {\n" + "\t\t\t\t\t\t\tignored()\n"
						+ "\t\t\t\t\t\t\trequest {\n" + "\t\t\t\t\t\t\t\tmethod 'GET'\n" + "\t\t\t\t\t\t\t\turl 'url'\n"
						+ "\t\t\t\t\t\t\t}\n" + "\t\t\t\t\t\t\tresponse {\n" + "\t\t\t\t\t\t\t\tstatus OK()\n"
						+ "\t\t\t\t\t\t\t}\n" + "\t\t\t\t\t\t}\n");

		File secondFile = File.createTempFile("contract2", ".groovy", tmpFolder);
		java.nio.file.Files.writeString(secondFile.toPath(),
				"\n\t\t\t\t\t\tsh.stubborn.contract.spec.Contract.make {\n" + "\t\t\t\t\t\t\trequest {\n"
						+ "\t\t\t\t\t\t\t\tmethod 'POST'\n" + "\t\t\t\t\t\t\t\turl 'url'\n" + "\t\t\t\t\t\t\t}\n"
						+ "\t\t\t\t\t\t\tresponse {\n" + "\t\t\t\t\t\t\t\tstatus OK()\n" + "\t\t\t\t\t\t\t}\n"
						+ "\t\t\t\t\t\t}\n");

		ContractVerifierConfigProperties properties = new ContractVerifierConfigProperties();
		properties.setTestFramework(testFramework);
		ContractMetadata firstContract = new ContractMetadata(firstFile.toPath(), false, 1, null,
				convertAsCollection(new File("/"), firstFile));
		ContractMetadata secondContract = new ContractMetadata(secondFile.toPath(), true, 1, null,
				convertAsCollection(new File("/"), secondFile));
		JavaTestGenerator testGenerator = new JavaTestGenerator();

		String clazz = testGenerator.buildClass(properties, List.of(secondContract, firstContract), "com/foo",
				new SingleTestGenerator.GeneratedClassData("test", "test", secondFile.toPath()));

		assertThat(countOccurrencesOf(clazz, ignoreAnnotation)).isEqualTo(2);
		asserter.accept(clazz);
	}

	static Stream<Arguments> shouldIgnore0TestsIf0ContractsAreIgnoredInContractDslAndInConfiguration() {
		return Stream.of(Arguments.of(JUNIT5, MOCK_MVC_JUNIT5_REST_ASSURED3_CLASS_STRINGS, "@Disabled", JAVA_ASSERTER),
				Arguments.of(TESTNG, MOCK_MVC_TESTNG_REST_ASSURED3_CLASS_STRINGS, "@Test(enabled = false)",
						JAVA_ASSERTER),
				Arguments.of(SPOCK, SPOCK_CLASS_REST_ASSURED3_STRINGS, "@Ignore", GROOVY_ASSERTER));
	}

	@ParameterizedTest
	@MethodSource("shouldIgnore0TestsIf0ContractsAreIgnoredInContractDslAndInConfiguration")
	void should_ignore_0_tests_if_0_contracts_are_ignored_in_Contract_dsl_and_in_Configuration(
			TestFramework testFramework, List<String> classStrings, String ignoreAnnotation, Consumer<String> asserter)
			throws IOException {
		File firstFile = File.createTempFile("contract1", ".groovy", tmpFolder);
		java.nio.file.Files.writeString(firstFile.toPath(),
				"\n\t\t\t\t\t\tsh.stubborn.contract.spec.Contract.make {\n" + "\t\t\t\t\t\t\trequest {\n"
						+ "\t\t\t\t\t\t\t\tmethod 'GET'\n" + "\t\t\t\t\t\t\t\turl 'url'\n" + "\t\t\t\t\t\t\t}\n"
						+ "\t\t\t\t\t\t\tresponse {\n" + "\t\t\t\t\t\t\t\tstatus OK()\n" + "\t\t\t\t\t\t\t}\n"
						+ "\t\t\t\t\t\t}\n");

		File secondFile = File.createTempFile("contract2", ".groovy", tmpFolder);
		java.nio.file.Files.writeString(secondFile.toPath(),
				"\n\t\t\t\t\t\tsh.stubborn.contract.spec.Contract.make {\n" + "\t\t\t\t\t\t\trequest {\n"
						+ "\t\t\t\t\t\t\t\tmethod 'POST'\n" + "\t\t\t\t\t\t\t\turl 'url'\n" + "\t\t\t\t\t\t\t}\n"
						+ "\t\t\t\t\t\t\tresponse {\n" + "\t\t\t\t\t\t\t\tstatus OK()\n" + "\t\t\t\t\t\t\t}\n"
						+ "\t\t\t\t\t\t}\n");

		ContractVerifierConfigProperties properties = new ContractVerifierConfigProperties();
		properties.setTestFramework(testFramework);
		ContractMetadata firstContract = new ContractMetadata(firstFile.toPath(), false, 1, null,
				convertAsCollection(new File("/"), firstFile));
		ContractMetadata secondContract = new ContractMetadata(secondFile.toPath(), false, 1, null,
				convertAsCollection(new File("/"), secondFile));
		JavaTestGenerator testGenerator = new JavaTestGenerator();

		String clazz = testGenerator.buildClass(properties, List.of(secondContract, firstContract), "com/foo",
				new SingleTestGenerator.GeneratedClassData("test", "test", secondFile.toPath()));

		assertThat(countOccurrencesOf(clazz, ignoreAnnotation)).isEqualTo(0);
		asserter.accept(clazz);
	}

	@Test
	void should_generate_test_in_explicit_test_mode_using_JUnit() throws IOException {
		String baseClass = "// tag::context_path_baseclass[]\n" + "import io.restassured.RestAssured;\n"
				+ "import org.junit.Before;\n" + "import org.springframework.boot.test.context.SpringBootTest;\n"
				+ "import org.springframework.boot.test.web.server.LocalServerPort;\n" + "\n"
				+ "@SpringBootTest(classes = ContextPathTestingBaseClass.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)\n"
				+ "class ContextPathTestingBaseClass {\n" + "\t\t\t\t\n" + "\t\t\t\t@LocalServerPort int port;\n"
				+ "\t\t\t\t\n" + "\t\t\t\t@Before\n" + "\t\t\t\tpublic void setup() {\n"
				+ "\t\t\t\t\tRestAssured.baseURI = \"http://localhost\";\n"
				+ "\t\t\t\t\tRestAssured.port = this.port;\n" + "\t\t\t\t}\n" + "\t\t\t}\n"
				+ "// end::context_path_baseclass[]";
		SyntaxChecker.tryToCompileJavaWithoutImports("test.ContextPathTestingBaseClass", "package test;\n" + baseClass);

		File secondFile = File.createTempFile("contract", ".groovy", tmpFolder);
		java.nio.file.Files.writeString(secondFile.toPath(),
				"\n\t\t\t\t\t\t// tag::context_path_contract[]\n"
						+ "\t\t\t\t\t\tsh.stubborn.contract.spec.Contract.make {\n" + "\t\t\t\t\t\t\trequest {\n"
						+ "\t\t\t\t\t\t\t\tmethod 'GET'\n" + "\t\t\t\t\t\t\t\turl '/my-context-path/url'\n"
						+ "\t\t\t\t\t\t\t}\n" + "\t\t\t\t\t\t\tresponse {\n" + "\t\t\t\t\t\t\t\tstatus OK()\n"
						+ "\t\t\t\t\t\t\t}\n" + "\t\t\t\t\t\t}\n" + "\t\t\t\t\t\t// end::context_path_contract[]\n");
		ContractVerifierConfigProperties properties = new ContractVerifierConfigProperties();
		properties.setTestFramework(JUNIT5);
		properties.setTestMode(EXPLICIT);
		properties.setBaseClassForTests("test.ContextPathTestingBaseClass");
		ContractMetadata contract = new ContractMetadata(file.toPath(), false, 1, null,
				convertAsCollection(new File("/"), file));
		SingleTestGenerator testGenerator = new JavaTestGenerator();

		String clazz = testGenerator.buildClass(properties, List.of(contract), "com/foo",
				new SingleTestGenerator.GeneratedClassData("test", "test", file.toPath()));

		assertThat(clazz).contains("RequestSpecification request = given();");
		assertThat(clazz).contains("Response response = given().spec(request)");
	}

	static Stream<Arguments> shouldPickContractNameAsTestMethod() {
		return Stream.of(Arguments.of(JUNIT5), Arguments.of(TESTNG), Arguments.of(SPOCK));
	}

	@ParameterizedTest
	@MethodSource("shouldPickContractNameAsTestMethod")
	void should_pick_the_contract_name_as_the_test_method(TestFramework testFramework) throws IOException {
		File secondFile = File.createTempFile("contract", ".groovy", tmpFolder);
		java.nio.file.Files.writeString(secondFile.toPath(),
				"\n\t\t\t\t\t\tsh.stubborn.contract.spec.Contract.make {\n" + "\t\t\t\t\t\t\tname(\"MySuperMethod\")\n"
						+ "\t\t\t\t\t\t\trequest {\n" + "\t\t\t\t\t\t\t\tmethod 'PUT'\n" + "\t\t\t\t\t\t\t\turl 'url'\n"
						+ "\t\t\t\t\t\t\t}\n" + "\t\t\t\t\t\t\tresponse {\n" + "\t\t\t\t\t\t\t\tstatus OK()\n"
						+ "\t\t\t\t\t\t\t}\n" + "\t\t\t\t\t\t}\n");
		ContractVerifierConfigProperties properties = new ContractVerifierConfigProperties();
		properties.setTestFramework(testFramework);
		ContractMetadata contract = new ContractMetadata(secondFile.toPath(), false, 1, null,
				convertAsCollection(new File("/"), secondFile));
		JavaTestGenerator testGenerator = new JavaTestGenerator();

		String clazz = testGenerator.buildClass(properties, List.of(contract), "com/foo",
				new SingleTestGenerator.GeneratedClassData("test", "test", file.toPath()));

		assertThat(clazz).contains("validate_mySuperMethod()");
	}

	static Stream<Arguments> shouldPickContractNameAsTestMethodWhenThereAreMultipleContracts() {
		return Stream.of(Arguments.of(JUNIT5), Arguments.of(TESTNG), Arguments.of(SPOCK));
	}

	@ParameterizedTest
	@MethodSource("shouldPickContractNameAsTestMethodWhenThereAreMultipleContracts")
	void should_pick_the_contract_name_as_the_test_method_when_there_are_multiple_contracts(TestFramework testFramework)
			throws IOException {
		File secondFile = File.createTempFile("contract", ".groovy", tmpFolder);
		java.nio.file.Files.writeString(secondFile.toPath(),
				"(1..2).collect { int index ->\n" + "sh.stubborn.contract.spec.Contract.make {\n"
						+ "\tname(\"shouldHaveIndex${index}\")\n" + "\trequest {\n" + "\t\tmethod(PUT())\n"
						+ "\t\theaders {\n" + "\t\t\tcontentType(applicationJson())\n" + "\t\t}\n"
						+ "\t\turl \"/${index}\"\n" + "\t}\n" + "\tresponse {\n" + "\t\tstatus OK()\n" + "\t}\n" + "}\n"
						+ "}");
		ContractVerifierConfigProperties properties = new ContractVerifierConfigProperties();
		properties.setTestFramework(testFramework);
		ContractMetadata contract = new ContractMetadata(secondFile.toPath(), false, 1, null,
				convertAsCollection(new File("/"), secondFile));
		JavaTestGenerator testGenerator = new JavaTestGenerator();

		String clazz = testGenerator.buildClass(properties, List.of(contract), "com/foo",
				new SingleTestGenerator.GeneratedClassData("test", "test", file.toPath()));

		assertThat(clazz).contains("validate_shouldHaveIndex1()");
		assertThat(clazz).contains("validate_shouldHaveIndex2()");
	}

	static Stream<Arguments> shouldGenerateTestMethodWhenThereAreMultipleContractsWithoutNameField() {
		return Stream.of(Arguments.of(JUNIT5), Arguments.of(TESTNG), Arguments.of(SPOCK));
	}

	@ParameterizedTest
	@MethodSource("shouldGenerateTestMethodWhenThereAreMultipleContractsWithoutNameField")
	void should_generate_the_test_method_when_there_are_multiple_contracts_without_name_field(
			TestFramework testFramework) throws IOException {
		File secondFile = File.createTempFile("contract", ".groovy", tmpFolder);
		java.nio.file.Files.writeString(secondFile.toPath(),
				"(1..2).collect { int index ->\n" + "sh.stubborn.contract.spec.Contract.make {\n" + "\trequest {\n"
						+ "\t\tmethod(PUT())\n" + "\t\theaders {\n" + "\t\t\tcontentType(applicationJson())\n"
						+ "\t\t}\n" + "\t\turl \"/${index}\"\n" + "\t}\n" + "\tresponse {\n" + "\t\tstatus OK()\n"
						+ "\t}\n" + "}\n" + "}");
		ContractVerifierConfigProperties properties = new ContractVerifierConfigProperties();
		properties.setTestFramework(testFramework);
		ContractMetadata contract = new ContractMetadata(secondFile.toPath(), false, 1, null,
				convertAsCollection(new File("/"), secondFile));
		JavaTestGenerator testGenerator = new JavaTestGenerator();

		String clazz = testGenerator.buildClass(properties, List.of(contract), "com/foo",
				new SingleTestGenerator.GeneratedClassData("test", "test", file.toPath()));

		assertThat(clazz).contains("_0() throws Exception");
		assertThat(clazz).contains("_1() throws Exception");
	}

	static Stream<Arguments> shouldGenerateTestsFromContractReferencingFile() {
		return Stream.of(Arguments.of(JUNIT5), Arguments.of(TESTNG), Arguments.of(SPOCK));
	}

	@ParameterizedTest
	@MethodSource("shouldGenerateTestsFromContractReferencingFile")
	void should_generate_tests_from_a_contract_that_references_a_file(TestFramework testFramework)
			throws IOException, URISyntaxException {
		File output = new File(tmp, "readFromFile.groovy");
		File contractLocation = output;
		File temp = new File(tmpFolder, "temp_" + System.nanoTime());
		temp.mkdirs();

		ContractVerifierConfigProperties properties = new ContractVerifierConfigProperties();
		properties.setTestFramework(testFramework);
		properties.setContractsDslDir(contractLocation.getParentFile());
		properties.setBasePackageForTests("a.b");
		properties.setGeneratedTestSourcesDir(temp);
		properties.setGeneratedTestResourcesDir(new File(tmpFolder, "res_" + System.nanoTime()));
		properties.getGeneratedTestResourcesDir().mkdirs();

		TestGenerator testGenerator = new TestGenerator(properties);

		int count = testGenerator.generate();

		assertThat(count).isEqualTo(1);
		String test = java.nio.file.Files
			.readString(new File(temp, "a/b/ContractVerifier" + getTestName(testFramework)).toPath());
		assertThat(test).contains("readFromFile_request_request.json");
		assertThat(test).contains("RESPONSE");
	}

	@ParameterizedTest
	@MethodSource("shouldGenerateTestsFromContractReferencingFile")
	void should_generate_tests_in_a_folder_taken_from_basePackageForTests_when_it_is_set(TestFramework testFramework)
			throws IOException, URISyntaxException {
		File output = new File(tmp, "readFromFile.groovy");
		File contractLocation = output;
		File temp = new File(tmpFolder, "temp_" + System.nanoTime());
		temp.mkdirs();

		ContractVerifierConfigProperties properties = new ContractVerifierConfigProperties();
		properties.setTestFramework(testFramework);
		properties.setContractsDslDir(contractLocation.getParentFile());
		properties.setBasePackageForTests("a.b");
		properties.setGeneratedTestSourcesDir(temp);
		properties.setGeneratedTestResourcesDir(new File(tmpFolder, "res_" + System.nanoTime()));
		properties.getGeneratedTestResourcesDir().mkdirs();

		TestGenerator testGenerator = new TestGenerator(properties);

		int count = testGenerator.generate();

		assertThat(count).isEqualTo(1);
		String test = java.nio.file.Files
			.readString(new File(temp, "a/b/ContractVerifier" + getTestName(testFramework)).toPath());
		assertThat(test).contains("readFromFile_request_request.json");
		assertThat(test).contains("RESPONSE");
	}

	static Stream<Arguments> shouldGenerateTestsWithBodyFromFileWithCustomCharset() {
		return Stream.of(Arguments.of(JUNIT5), Arguments.of(SPOCK));
	}

	@ParameterizedTest
	@MethodSource("shouldGenerateTestsWithBodyFromFileWithCustomCharset")
	void should_generate_tests_with_body_from_file_with_custom_charset(TestFramework testFramework)
			throws IOException, URISyntaxException {
		file = File.createTempFile("contract", ".groovy", tmpFolder);
		writeContract(file);
		tmp = new File(tmpFolder, "tmp2_" + System.nanoTime());
		tmp.mkdirs();
		File charset = new File(SingleTestGeneratorTests.class.getResource("/charset/").toURI());
		FileSystemUtils.copyRecursively(charset, tmp);

		File output = new File(tmp, "readFromFileWithCharset.groovy");
		File contractLocation = output;
		File temp = new File(tmpFolder, "temp_" + System.nanoTime());
		temp.mkdirs();

		ContractVerifierConfigProperties properties = new ContractVerifierConfigProperties();
		properties.setTestFramework(testFramework);
		properties.setContractsDslDir(contractLocation.getParentFile());
		properties.setBasePackageForTests("a.b");
		properties.setGeneratedTestSourcesDir(temp);
		properties.setGeneratedTestResourcesDir(new File(tmpFolder, "res_" + System.nanoTime()));
		properties.getGeneratedTestResourcesDir().mkdirs();

		TestGenerator testGenerator = new TestGenerator(properties);

		int count = testGenerator.generate();

		assertThat(count).isEqualTo(1);
		String test = java.nio.file.Files
			.readString(new File(temp, "a/b/ContractVerifier" + getTestName(testFramework)).toPath());
		assertThat(test).contains("readFromFileWithCharset_request_request.json");
		assertThat(test).contains("RESPONSE");
		assertThat(test).contains(
				"body(new String(fileToBytes(this, \"readFromFileWithCharset_request_request.json\"), \"US-ASCII\"))");
	}

	@ParameterizedTest
	@MethodSource("shouldGenerateTestsFromContractReferencingFile")
	void should_generate_tests_in_a_folder_taken_from_baseClassForTests_package_when_it_is_set(
			TestFramework testFramework) throws IOException, URISyntaxException {
		File output = new File(tmp, "readFromFile.groovy");
		File contractLocation = output;
		File temp = new File(tmpFolder, "temp_" + System.nanoTime());
		temp.mkdirs();

		ContractVerifierConfigProperties properties = new ContractVerifierConfigProperties();
		properties.setTestFramework(testFramework);
		properties.setContractsDslDir(contractLocation.getParentFile());
		properties.setBaseClassForTests("a.b.SomeClass");
		properties.setGeneratedTestSourcesDir(temp);
		properties.setGeneratedTestResourcesDir(new File(tmpFolder, "res_" + System.nanoTime()));
		properties.getGeneratedTestResourcesDir().mkdirs();

		TestGenerator testGenerator = new TestGenerator(properties);

		int count = testGenerator.generate();

		assertThat(count).isEqualTo(1);
		String test = java.nio.file.Files
			.readString(new File(temp, "a/b/ContractVerifier" + getTestName(testFramework)).toPath());
		assertThat(test).contains("readFromFile_request_request.json");
		assertThat(test).contains("RESPONSE");
	}

	@ParameterizedTest
	@MethodSource("shouldGenerateTestsFromContractReferencingFile")
	void should_generate_tests_in_a_folder_taken_from_packageWithBaseClasses_when_it_is_set(TestFramework testFramework)
			throws IOException, URISyntaxException {
		File output = new File(tmp, "readFromFile.groovy");
		File contractLocation = output;
		File temp = new File(tmpFolder, "temp_" + System.nanoTime());
		temp.mkdirs();

		ContractVerifierConfigProperties properties = new ContractVerifierConfigProperties();
		properties.setTestFramework(testFramework);
		properties.setContractsDslDir(contractLocation.getParentFile());
		properties.setPackageWithBaseClasses("a.b");
		properties.setGeneratedTestSourcesDir(temp);
		properties.setGeneratedTestResourcesDir(new File(tmpFolder, "res_" + System.nanoTime()));
		properties.getGeneratedTestResourcesDir().mkdirs();

		TestGenerator testGenerator = new TestGenerator(properties);

		int count = testGenerator.generate();

		assertThat(count).isEqualTo(1);
		String test = java.nio.file.Files
			.readString(new File(temp, "a/b/ContractVerifier" + getTestName(testFramework)).toPath());
		assertThat(test).contains("readFromFile_request_request.json");
		assertThat(test).contains("RESPONSE");
	}

	@ParameterizedTest
	@MethodSource("shouldGenerateTestsFromContractReferencingFile")
	void should_generate_tests_in_a_default_folder_when_no_property_was_passed(TestFramework testFramework)
			throws IOException, URISyntaxException {
		File output = new File(tmp, "readFromFile.groovy");
		File contractLocation = output;
		File temp = new File(tmpFolder, "temp_" + System.nanoTime());
		temp.mkdirs();

		ContractVerifierConfigProperties properties = new ContractVerifierConfigProperties();
		properties.setTestFramework(testFramework);
		properties.setContractsDslDir(contractLocation.getParentFile());
		properties.setGeneratedTestSourcesDir(temp);
		properties.setGeneratedTestResourcesDir(new File(tmpFolder, "res_" + System.nanoTime()));
		properties.getGeneratedTestResourcesDir().mkdirs();

		TestGenerator testGenerator = new TestGenerator(properties);

		int count = testGenerator.generate();

		assertThat(count).isEqualTo(1);
		String test = java.nio.file.Files.readString(
				new File(temp, "sh/stubborn/contract/verifier/tests/ContractVerifier" + getTestName(testFramework))
					.toPath());
		assertThat(test).contains("readFromFile_request_request.json");
		assertThat(test).contains("RESPONSE");
	}

	private static String getTestName(TestFramework testFramework) {
		return testFramework == SPOCK ? "Spec.groovy" : "Test.java";
	}

}
