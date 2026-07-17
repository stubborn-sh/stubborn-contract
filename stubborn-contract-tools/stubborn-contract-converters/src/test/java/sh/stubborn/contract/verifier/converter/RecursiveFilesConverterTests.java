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
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import sh.stubborn.contract.spec.Contract;
import sh.stubborn.contract.verifier.file.ContractMetadata;

import org.springframework.util.FileSystemUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RecursiveFilesConverterTests {

	private static final Set<Path> EXPECTED_TARGET_FILES = new HashSet<>(
			List.of(Paths.get("dslRoot.json"), Paths.get("dir1/1_scenario.json"), Paths.get("dir1/2_scenario.json"),
					Paths.get("dir1/1_scenario_multiple.json"), Paths.get("dir1/2_scenario_multiple.json"),
					Paths.get("dir1/dsl1.json"), Paths.get("dir1/dsl1b.json"), Paths.get("dir2/dsl2.json"),
					Paths.get("dir1/dsl1_list_0.json"), Paths.get("dir1/dsl1_list_1.json"),
					Paths.get("dir1/shouldHaveIndex1.json"), Paths.get("dir1/shouldHaveIndex2.json")));

	@TempDir
	Path tmpDir;

	@Test
	void should_recursively_convert_all_matching_files_with_stubs() throws Exception {
		File originalSourceRootDirectory = new File(getClass().getResource("/converter/dir3").toURI());
		File contractsDslDir = Files.createDirectories(tmpDir.resolve("source")).toFile();
		File stubsOutputDir = Files.createDirectories(tmpDir.resolve("target")).toFile();
		FileSystemUtils.copyRecursively(originalSourceRootDirectory, contractsDslDir);
		RecursiveFilesConverter converter = new RecursiveFilesConverter(stubsOutputDir, contractsDslDir,
				new ArrayList<>(), ".*", false);
		converter.processFiles();
		List<File> createdFiles = listFilesRecursively(stubsOutputDir);
		Set<Path> relativized = getRelativePathsForFilesInDirectory(createdFiles, stubsOutputDir);
		assertThat(relativized)
			.isEqualTo(new HashSet<>(List.of(Paths.get("Account creating.json"), Paths.get("Test route.json"))));
	}

	@Test
	void should_recursively_convert_all_matching_files() throws Exception {
		File originalSourceRootDirectory = new File(getClass().getResource("/converter/source").toURI());
		File contractsDslDir = Files.createDirectories(tmpDir.resolve("source")).toFile();
		File stubsOutputDir = Files.createDirectories(tmpDir.resolve("target")).toFile();
		FileSystemUtils.copyRecursively(originalSourceRootDirectory, contractsDslDir);
		RecursiveFilesConverter converter = new RecursiveFilesConverter(stubsOutputDir, contractsDslDir,
				new ArrayList<>(), ".*", false);
		converter.processFiles();
		List<File> createdFiles = listFilesRecursively(stubsOutputDir);
		Set<Path> relativized = getRelativePathsForFilesInDirectory(createdFiles, stubsOutputDir);
		assertThat(relativized).isEqualTo(EXPECTED_TARGET_FILES);
		for (File file : createdFiles) {
			assertThat(file).content().contains("uuid");
		}
	}

	@Test
	void should_recursively_convert_matching_files_with_exclusions() throws Exception {
		File originalSourceRootDirectory = new File(getClass().getResource("/converter/source").toURI());
		File contractsDslDir = Files.createDirectories(tmpDir.resolve("source")).toFile();
		File stubsOutputDir = Files.createDirectories(tmpDir.resolve("target")).toFile();
		List<String> excludedFiles = List.of("dir1/**");
		FileSystemUtils.copyRecursively(originalSourceRootDirectory, contractsDslDir);
		RecursiveFilesConverter converter = new RecursiveFilesConverter(stubsOutputDir, contractsDslDir, excludedFiles,
				".*", false);
		converter.processFiles();
		List<File> createdFiles = listFilesRecursively(stubsOutputDir);
		Set<Path> relativized = getRelativePathsForFilesInDirectory(createdFiles, stubsOutputDir);
		assertThat(relativized)
			.isEqualTo(new HashSet<>(List.of(Paths.get("dslRoot.json"), Paths.get("dir2/dsl2.json"))));
		for (File file : createdFiles) {
			assertThat(file).content().contains("uuid");
		}
	}

	@Test
	void on_failure_should_break_processing_and_throw_meaningful_exception() throws Exception {
		File sourceFile = tmpDir.resolve("test.groovy").toFile();
		Files.writeString(sourceFile.toPath(), """
				sh.stubborn.contract.spec.Contract.make {
					request {
						method GET()
						url '/foo'
					}
					response {
						status OK()
					}
				}""");
		StubGenerator<String> failingGenerator = new StubGenerator<>() {
			@Override
			public boolean canReadStubMapping(File mapping) {
				return true;
			}

			@Override
			public Map<Contract, String> convertContents(String rootName, ContractMetadata content) {
				throw new NullPointerException("Test conversion error");
			}

			@Override
			public String generateOutputFileNameForInput(String inputFileName) {
				return inputFileName + "2";
			}
		};
		File contractsDslDir = tmpDir.toFile();
		File stubsOutputDir = tmpDir.toFile();
		RecursiveFilesConverter converter = new RecursiveFilesConverter(stubsOutputDir, contractsDslDir,
				new ArrayList<>(), ".*", false, new StubGeneratorProvider(List.of(failingGenerator)));
		assertThatThrownBy(converter::processFiles).isInstanceOf(ConversionContractVerifierException.class)
			.hasMessageContaining(sourceFile.getName())
			.cause()
			.hasMessage("Test conversion error");
	}

	@Test
	void should_convert_contract_into_stub_using_all_possible_converters() throws Exception {
		File sourceFile = tmpDir.resolve("test.groovy").toFile();
		Files.writeString(sourceFile.toPath(), """
				sh.stubborn.contract.spec.Contract.make {
					request {
						url "/baz"
						method "GET"
					}
					response {
						status OK()
					}
				}""");
		StubGenerator<String> generator1 = stubGenerator("foo");
		StubGenerator<String> generator2 = stubGenerator("bar");
		File contractsDslDir = tmpDir.toFile();
		File stubsOutputDir = tmpDir.toFile();
		RecursiveFilesConverter converter = new RecursiveFilesConverter(stubsOutputDir, contractsDslDir,
				new ArrayList<>(), ".*", false, new StubGeneratorProvider(List.of(generator1, generator2)));
		converter.processFiles();
		assertThat(List.of(tmpDir.toFile().list())).contains("foo", "bar");
	}

	@Test
	void should_not_create_stub_file_when_generated_stub_is_empty() throws Exception {
		File sourceFile = tmpDir.resolve("test.groovy").toFile();
		Files.writeString(sourceFile.toPath(), """
				sh.stubborn.contract.spec.Contract.make {
					request {
						url "/baz"
						method "GET"
					}
					response {
						status OK()
					}
				}""");
		StubGenerator<String> generator = stubGenerator("");
		File contractsDslDir = tmpDir.toFile();
		File stubsOutputDir = Files.createDirectories(tmpDir.resolve("target")).toFile();
		RecursiveFilesConverter converter = new RecursiveFilesConverter(stubsOutputDir, contractsDslDir,
				new ArrayList<>(), ".*", false, new StubGeneratorProvider(List.of(generator)));
		converter.processFiles();
		assertThat(stubsOutputDir.list()).isEmpty();
	}

	private static Set<Path> getRelativePathsForFilesInDirectory(Collection<File> createdFiles,
			File targetRootDirectory) {
		Path rootSourcePath = Paths.get(targetRootDirectory.toURI());
		Set<Path> result = new HashSet<>();
		for (File file : createdFiles) {
			result.add(rootSourcePath.relativize(Paths.get(file.toURI())));
		}
		return result;
	}

	private static List<File> listFilesRecursively(File dir) throws IOException {
		List<File> result = new ArrayList<>();
		Files.walk(dir.toPath()).filter(Files::isRegularFile).forEach(p -> result.add(p.toFile()));
		return result;
	}

	private static StubGenerator<String> stubGenerator(String stub) {
		return new StubGenerator<>() {
			@Override
			public boolean canReadStubMapping(File mapping) {
				return true;
			}

			@Override
			public Map<Contract, String> convertContents(String rootName, ContractMetadata content) {
				return Map.of(content.getConvertedContract().iterator().next(), stub);
			}

			@Override
			public String generateOutputFileNameForInput(String inputFileName) {
				return stub;
			}
		};
	}

}
