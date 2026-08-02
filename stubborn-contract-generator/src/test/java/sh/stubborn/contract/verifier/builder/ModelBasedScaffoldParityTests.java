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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import sh.stubborn.contract.verifier.config.ContractVerifierConfigProperties;
import sh.stubborn.contract.verifier.config.TestFramework;
import sh.stubborn.contract.verifier.config.TestMode;
import sh.stubborn.contract.verifier.file.ContractMetadata;
import sh.stubborn.contract.verifier.util.ContractVerifierDslConverter;
import sh.stubborn.contract.verifier.util.SyntaxChecker;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Parity oracle for the Phase 2 model-based generator. It proves that the model + typed
 * renderer path ({@link ModelBasedTestGenerator}) produces, for the Java targets, output
 * that matches the committed legacy golden files (see
 * {@link TestGenerationGoldenMasterTests}) under a <em>normalized</em> bar (whitespace-
 * and import-order-insensitive) and that the generated source compiles.
 *
 * <p>
 * The bar is deliberately not byte-exact: the scaffold is now produced by JavaPoet, whose
 * indentation and import ordering differ from the legacy string builders, while each
 * method body is captured verbatim from the legacy pipeline. Normalization drops blank
 * lines, trims each line, and compares the import block as a set; the remaining body is
 * compared as a trimmed line sequence.
 *
 * @author Marcin Grzejszczak
 */
class ModelBasedScaffoldParityTests {

	private static final Path GOLDEN_DIR = Path.of("src", "test", "resources", "testgen-golden");

	// Same corpus as TestGenerationGoldenMasterTests.
	private static final Map<String, String> CONTRACTS = Map.of("http_json_put", """
			sh.stubborn.contract.spec.Contract.make {
				request {
					method 'PUT'
					url '/foo'
					headers { contentType(applicationJson()) }
					body([foo: 'bar'])
				}
				response {
					status OK()
					headers { contentType(applicationJson()) }
					body([status: 'ok'])
				}
			}
			""", "http_get_headers", """
			sh.stubborn.contract.spec.Contract.make {
				request {
					method 'GET'
					url '/items/1'
					headers { header('X-Trace', 'abc') }
				}
				response {
					status OK()
					headers { header('X-Reply', 'def') }
					body([id: 1, name: 'widget'])
				}
			}
			""", "http_get_query_cookie", """
			sh.stubborn.contract.spec.Contract.make {
				request {
					method 'GET'
					urlPath('/items') {
						queryParameters {
							parameter('page', '2')
						}
					}
					cookies {
						cookie('session', 'abc123')
					}
				}
				response {
					status OK()
					body([id: 1])
				}
			}
			""", "http_async", """
			sh.stubborn.contract.spec.Contract.make {
				request {
					method 'GET'
					url '/async'
				}
				response {
					async()
					status OK()
					body([status: 'ok'])
				}
			}
			""", "http_multipart", """
			sh.stubborn.contract.spec.Contract.make {
				request {
					method 'PUT'
					url '/multipart'
					headers {
						contentType('multipart/form-data')
					}
					multipart(
						formParameter: 'formValue',
						file: named(
							name: value('filename.csv'),
							content: value('file content')
						)
					)
				}
				response {
					status OK()
				}
			}
			""", "http_resp_pattern_header", """
			sh.stubborn.contract.spec.Contract.make {
				request {
					method 'GET'
					url '/resp/pattern'
				}
				response {
					status OK()
					headers {
						header('X-Reply', value(producer(regex('[0-9]{7}')), consumer('1234567')))
					}
					body([id: 1])
				}
			}
			""", "http_resp_execproperty_header", """
			sh.stubborn.contract.spec.Contract.make {
				request {
					method 'GET'
					url '/resp/exec'
				}
				response {
					status OK()
					headers {
						header('X-Exec', value(consumer('abc'), producer(execute('$it.length()'))))
					}
				}
			}
			""", "http_resp_cookie", """
			sh.stubborn.contract.spec.Contract.make {
				request {
					method 'GET'
					url '/resp/cookie'
				}
				response {
					status OK()
					cookies {
						cookie('session', 'abc123')
					}
					body([id: 1])
				}
			}
			""", "http_file_body", """
			sh.stubborn.contract.spec.Contract.make {
				request {
					method 'PUT'
					url '/foo'
					headers { contentType(applicationJson()) }
					body(file('request.json'))
				}
				response {
					status OK()
				}
			}
			""");

	@TempDir
	File tmp;

	static Stream<Arguments> corpus() {
		// The Java rows of the golden matrix (Spock stays on the legacy generator).
		return CONTRACTS.keySet()
			.stream()
			.sorted()
			.flatMap((name) -> Stream.of(Arguments.of(name, TestFramework.JUNIT5, TestMode.MOCKMVC),
					Arguments.of(name, TestFramework.JUNIT5, TestMode.EXPLICIT),
					Arguments.of(name, TestFramework.TESTNG, TestMode.MOCKMVC)));
	}

	@ParameterizedTest(name = "{0} [{1}/{2}]")
	@MethodSource("corpus")
	void model_path_matches_golden_and_compiles(String contractName, TestFramework framework, TestMode mode)
			throws Exception {
		String generated = generate(CONTRACTS.get(contractName), framework, mode);

		Path golden = GOLDEN_DIR.resolve(contractName + "__" + framework + "__" + mode + ".txt");
		assertThat(Files.exists(golden)).as("Missing golden %s", golden).isTrue();
		String expected = Files.readString(golden, StandardCharsets.UTF_8);

		// Apply the SAME normalization the golden files were stored with (see
		// TestGenerationGoldenMasterTests#normalize): the environment-specific temp path
		// becomes TMP and every backslash becomes a forward slash. The raw source is kept
		// for compilation.
		String normalized = normalize(generated);

		// import-order-insensitive parity
		assertThat(imports(normalized)).as("Import set drifted from golden %s", golden).isEqualTo(imports(expected));
		// whitespace-insensitive, trimmed line-sequence parity of the remainder
		assertThat(body(normalized)).as("Body drifted from golden %s", golden).isEqualTo(body(expected));

		// compile-and-run bar: the model-generated source must compile
		SyntaxChecker.tryToCompileJavaWithoutImports("com.example.ContractTest", generated);
	}

	private String generate(String contractDsl, TestFramework framework, TestMode mode) throws IOException {
		File contractFile = new File(this.tmp, "contract.groovy");
		Files.writeString(contractFile.toPath(), contractDsl, StandardCharsets.UTF_8);

		ContractVerifierConfigProperties properties = new ContractVerifierConfigProperties();
		properties.setTestFramework(framework);
		properties.setTestMode(mode);
		// Needed by contracts whose body reads a file (body(file('request.json'))): the
		// generator writes the referenced fixture into these dirs as a side effect.
		properties.setGeneratedTestSourcesDir(this.tmp);
		properties.setGeneratedTestResourcesDir(this.tmp);

		ContractMetadata contract = new ContractMetadata(contractFile.toPath(), true, 1, 2,
				ContractVerifierDslConverter.convertAsCollection(new File("/"), contractFile));
		return new ModelBasedTestGenerator().buildClass(properties, List.of(contract), "com/example",
				new SingleTestGenerator.GeneratedClassData("ContractTest", "com.example", contractFile.toPath()));
	}

	// Mirrors TestGenerationGoldenMasterTests#normalize so the model output is compared
	// on
	// the same footing the golden files were stored with.
	private String normalize(String generated) {
		return generated.replace(this.tmp.getAbsolutePath(), "TMP").replace('\\', '/');
	}

	// Every `import ...;` line (regular and static), trimmed, as an order-insensitive
	// set.
	private Set<String> imports(String source) {
		Set<String> imports = new LinkedHashSet<>();
		for (String line : source.split("\n", -1)) {
			String trimmed = line.trim();
			if (trimmed.startsWith("import ")) {
				imports.add(trimmed);
			}
		}
		return imports;
	}

	// Non-blank, trimmed lines that are not imports — the package/annotations/class/body,
	// compared as an ordered sequence.
	private List<String> body(String source) {
		return source.lines()
			.map(String::trim)
			.filter((line) -> !line.isEmpty())
			.filter((line) -> !line.startsWith("import "))
			.toList();
	}

}
