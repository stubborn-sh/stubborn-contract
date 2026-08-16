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

package sh.stubborn.contract.verifier.converter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import sh.stubborn.contract.spec.Contract;
import sh.stubborn.contract.verifier.file.ContractMetadata;

import static org.assertj.core.api.Assertions.assertThat;

class RecursiveFilesConverterMutationTests {

	private static final String SINGLE_CONTRACT = """
			sh.stubborn.contract.spec.Contract.make {
				request {
					method 'GET'
					url '/foo'
				}
				response {
					status OK()
				}
			}""";

	private static final String CONTRACT_LIST = """
			[
			sh.stubborn.contract.spec.Contract.make {
				request {
					method 'GET'
					url '/a'
				}
				response {
					status OK()
				}
			},
			sh.stubborn.contract.spec.Contract.make {
				request {
					method 'GET'
					url '/b'
				}
				response {
					status OK()
				}
			}
			]""";

	@TempDir
	@Nullable Path tmpDir;

	private Path tmpDir() {
		return Objects.requireNonNull(this.tmpDir);
	}

	@Test
	void excludes_contracts_in_build_folders_when_flag_enabled() throws Exception {
		File contractsDslDir = Files.createDirectories(tmpDir().resolve("src")).toFile();
		File stubsOutputDir = Files.createDirectories(tmpDir().resolve("out")).toFile();
		writeContract(contractsDslDir, "target", "insideTarget.groovy");
		writeContract(contractsDslDir, "build", "insideBuild.groovy");
		writeContract(contractsDslDir, "keep", "keep.groovy");

		RecursiveFilesConverter converter = new RecursiveFilesConverter(stubsOutputDir, contractsDslDir,
				new ArrayList<>(), ".*", true);
		converter.processFiles();

		Set<Path> relativized = relativePaths(stubsOutputDir);
		assertThat(relativized).containsExactly(Paths.get("keep/keep.json"));
	}

	@Test
	void keeps_contracts_in_build_folders_when_flag_disabled() throws Exception {
		File contractsDslDir = Files.createDirectories(tmpDir().resolve("src")).toFile();
		File stubsOutputDir = Files.createDirectories(tmpDir().resolve("out")).toFile();
		writeContract(contractsDslDir, "target", "insideTarget.groovy");
		writeContract(contractsDslDir, "keep", "keep.groovy");

		RecursiveFilesConverter converter = new RecursiveFilesConverter(stubsOutputDir, contractsDslDir,
				new ArrayList<>(), ".*", false);
		converter.processFiles();

		Set<Path> relativized = relativePaths(stubsOutputDir);
		assertThat(relativized).containsExactlyInAnyOrder(Paths.get("keep/keep.json"),
				Paths.get("target/insideTarget.json"));
	}

	@Test
	void multiple_unnamed_contracts_are_written_with_incrementing_index_prefix() throws Exception {
		File contractsDslDir = Files.createDirectories(tmpDir().resolve("src")).toFile();
		File stubsOutputDir = Files.createDirectories(tmpDir().resolve("out")).toFile();
		File sourceFile = new File(contractsDslDir, "list.groovy");
		Files.writeString(sourceFile.toPath(), CONTRACT_LIST);

		StubGenerator<String> generator = new StubGenerator<>() {
			@Override
			public boolean canReadStubMapping(File mapping) {
				return true;
			}

			@Override
			public Map<Contract, String> convertContents(String rootName, ContractMetadata content) {
				// Deliberately return unnamed contracts (distinguished by priority so
				// they are not equal) so that generateName falls through to the
				// index-prefixed branch instead of using the contract name.
				Map<Contract, String> result = new LinkedHashMap<>();
				for (int i = 0; i < content.getConvertedContract().size(); i++) {
					Contract unnamed = new Contract();
					unnamed.setPriority(i + 1);
					result.put(unnamed, "content-" + i);
				}
				return result;
			}

			@Override
			public String generateOutputFileNameForInput(String inputFileName) {
				return "out.json";
			}
		};
		RecursiveFilesConverter converter = new RecursiveFilesConverter(stubsOutputDir, contractsDslDir,
				new ArrayList<>(), ".*", false, new StubGeneratorProvider(List.of(generator)));
		converter.processFiles();

		Set<Path> relativized = relativePaths(stubsOutputDir);
		assertThat(relativized).containsExactlyInAnyOrder(Paths.get("0_out.json"), Paths.get("1_out.json"));
	}

	private static void writeContract(File baseDir, String subDir, String fileName) throws IOException {
		File dir = new File(baseDir, subDir);
		Files.createDirectories(dir.toPath());
		Files.writeString(new File(dir, fileName).toPath(), SINGLE_CONTRACT);
	}

	private static Set<Path> relativePaths(File outputDir) throws IOException {
		Path root = Paths.get(outputDir.toURI());
		Set<Path> result = new HashSet<>();
		for (File file : listFilesRecursively(outputDir)) {
			result.add(root.relativize(Paths.get(file.toURI())));
		}
		return result;
	}

	private static List<File> listFilesRecursively(File dir) throws IOException {
		List<File> result = new ArrayList<>();
		try (var stream = Files.walk(dir.toPath())) {
			stream.filter(Files::isRegularFile).forEach((p) -> result.add(p.toFile()));
		}
		return result;
	}

}
