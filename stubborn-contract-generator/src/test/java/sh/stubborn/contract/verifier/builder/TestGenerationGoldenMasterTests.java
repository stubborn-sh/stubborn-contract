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
import java.util.List;
import java.util.Map;
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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Golden-master (snapshot) oracle for {@link SingleTestGenerator}. It captures the exact
 * output of the current generator for a corpus of contracts across the framework/mode
 * matrix, and fails if that output drifts.
 *
 * <p>
 * Purpose: this is the safety net for the model + typed-renderer migration (see
 * {@code proposals/test-generation-migration.md}). When the generator internals are
 * rewritten, these snapshots pin the observable output so any change is deliberate and
 * reviewed rather than accidental.
 *
 * <p>
 * The snapshots live under {@code src/test/resources/testgen-golden/}. To (re)generate
 * them after an intentional change, run with {@code -Dtestgen.golden.update=true}; the
 * test then writes the current output to the golden files and passes. Without the flag it
 * compares and fails on any difference. A missing golden always fails (unless updating)
 * so new corpus entries can't silently pass.
 *
 * @author Marcin Grzejszczak
 */
class TestGenerationGoldenMasterTests {

	private static final boolean UPDATE = Boolean.getBoolean("testgen.golden.update");

	/**
	 * Golden files live in the module source tree so an update run rewrites the committed
	 * snapshots (surefire runs with the module directory as the working directory).
	 */
	private static final Path GOLDEN_DIR = Path.of("src", "test", "resources", "testgen-golden");

	/**
	 * Representative corpus. Keep each contract small and focused on one shape; breadth
	 * comes from the framework/mode matrix. Expand as the migration ports more surface.
	 */
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
			""");

	@TempDir
	File tmp;

	static Stream<Arguments> corpus() {
		// One row per (contract, framework, mode). MOCKMVC covers the common HTTP path
		// for
		// each supported framework/language; EXPLICIT exercises the RestAssured
		// standalone
		// variant.
		return CONTRACTS.keySet()
			.stream()
			.sorted()
			.flatMap((name) -> Stream.of(Arguments.of(name, TestFramework.JUNIT5, TestMode.MOCKMVC),
					Arguments.of(name, TestFramework.JUNIT5, TestMode.EXPLICIT),
					Arguments.of(name, TestFramework.TESTNG, TestMode.MOCKMVC),
					Arguments.of(name, TestFramework.SPOCK, TestMode.MOCKMVC)));
	}

	@ParameterizedTest(name = "{0} [{1}/{2}]")
	@MethodSource("corpus")
	void generator_output_matches_golden(String contractName, TestFramework framework, TestMode mode)
			throws IOException {
		String generated = normalize(generate(CONTRACTS.get(contractName), framework, mode));

		Path golden = GOLDEN_DIR.resolve(contractName + "__" + framework + "__" + mode + ".txt");
		if (UPDATE) {
			Files.createDirectories(golden.getParent());
			Files.writeString(golden, generated, StandardCharsets.UTF_8);
			return;
		}
		assertThat(Files.exists(golden))
			.as("Missing golden %s — run with -Dtestgen.golden.update=true to create it", golden)
			.isTrue();
		String expected = Files.readString(golden, StandardCharsets.UTF_8);
		assertThat(generated).as("Generated output drifted from golden %s", golden).isEqualTo(expected);
	}

	private String generate(String contractDsl, TestFramework framework, TestMode mode) throws IOException {
		File contractFile = new File(this.tmp, "contract.groovy");
		Files.writeString(contractFile.toPath(), contractDsl, StandardCharsets.UTF_8);

		ContractVerifierConfigProperties properties = new ContractVerifierConfigProperties();
		properties.setTestFramework(framework);
		properties.setTestMode(mode);

		ContractMetadata contract = new ContractMetadata(contractFile.toPath(), true, 1, 2,
				ContractVerifierDslConverter.convertAsCollection(new File("/"), contractFile));
		return new JavaTestGenerator().buildClass(properties, List.of(contract), "com/example",
				new SingleTestGenerator.GeneratedClassData("ContractTest", "com.example", contractFile.toPath()));
	}

	/**
	 * Remove environment-specific noise so snapshots are stable across machines/runs: the
	 * absolute temp path of the contract file, and trailing whitespace / CRLF.
	 */
	private String normalize(String generated) {
		String out = generated.replace(this.tmp.getAbsolutePath(), "TMP").replace('\\', '/');
		StringBuilder sb = new StringBuilder(out.length());
		for (String line : out.split("\n", -1)) {
			sb.append(stripTrailing(line)).append('\n');
		}
		return sb.toString();
	}

	private static String stripTrailing(String line) {
		int end = line.length();
		while (end > 0 && Character.isWhitespace(line.charAt(end - 1))) {
			end--;
		}
		return line.substring(0, end);
	}

}
