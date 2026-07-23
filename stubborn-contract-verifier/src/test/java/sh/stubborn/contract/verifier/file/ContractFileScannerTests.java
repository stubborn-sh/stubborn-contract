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

package sh.stubborn.contract.verifier.file;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import sh.stubborn.contract.spec.Contract;
import sh.stubborn.contract.spec.ContractConverter;

import org.springframework.util.FileSystemUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ContractFileScannerTests {

	@TempDir
	Path tmp;

	File tmpFolder;

	@BeforeEach
	void setup() throws Exception {
		tmpFolder = new File(tmp.toFile(), "contracts");
		tmpFolder.mkdirs();
	}

	@Test
	void should_find_contract_files() throws Exception {
		FileSystemUtils.copyRecursively(new File(getClass().getResource("/directory/with/stubs").toURI()), tmpFolder);
		File baseDir = tmpFolder;
		Set<String> excluded = Set.of("package/**");
		Set<String> ignored = Set.of("other/different/**");
		ContractFileScanner scanner = new ContractFileScanner(baseDir, excluded, ignored, Set.of(), null);
		Map<Path, List<ContractMetadata>> result = scanner.findContractsRecursively();
		assertThat(result.keySet()).hasSize(3);
		assertThat(result.get(baseDir.toPath().resolve("different"))).hasSize(1);
		assertThat(result.get(baseDir.toPath().resolve("other"))).hasSize(2);
		Collection<ContractMetadata> ignoredSet = result.get(baseDir.toPath().resolve("other").resolve("different"));
		assertThat(ignoredSet).hasSize(1);
		assertThat(ignoredSet).allMatch(ContractMetadata::isIgnored);
	}

	@Test
	void should_find_contract_files_in_strange_directories() throws Exception {
		File baseDir = new File(getClass().getResource("/strange_[3.3.3]_directory").toURI());
		Set<String> excluded = Set.of("foo/**");
		Set<String> ignored = Set.of("bar/**");
		ContractFileScanner scanner = new ContractFileScanner(baseDir, excluded, ignored, Set.of(), null);
		Map<Path, List<ContractMetadata>> result = scanner.findContractsRecursively();
		assertThat(result.entrySet()).hasSize(2);
		Collection<ContractMetadata> ignoredSet = result.get(baseDir.toPath().resolve("bar"));
		assertThat(ignoredSet).hasSize(1);
		assertThat(ignoredSet).allMatch(ContractMetadata::isIgnored);
	}

	@Test
	void should_find_contracts_group_in_scenario() throws Exception {
		File baseDir = new File(getClass().getResource("/directory/with/scenario").toURI());
		ContractFileScanner scanner = new ContractFileScanner(baseDir, Set.of(), Set.of(), Set.of(), null);
		Map<Path, List<ContractMetadata>> contracts = scanner.findContractsRecursively();
		List<ContractMetadata> firstEntry = contracts.values().iterator().next();
		assertThat(firstEntry).hasSize(3);
		assertThat(firstEntry.stream()
			.filter((m) -> m.getPath().getFileName().toString().startsWith("01"))
			.findFirst()
			.orElseThrow()
			.getGroupSize()).isEqualTo(3);
		assertThat(firstEntry.stream()
			.filter((m) -> m.getPath().getFileName().toString().startsWith("01"))
			.findFirst()
			.orElseThrow()
			.getOrder()).isEqualTo(0);
		assertThat(firstEntry.stream()
			.filter((m) -> m.getPath().getFileName().toString().startsWith("02"))
			.findFirst()
			.orElseThrow()
			.getOrder()).isEqualTo(1);
		assertThat(firstEntry.stream()
			.filter((m) -> m.getPath().getFileName().toString().startsWith("03"))
			.findFirst()
			.orElseThrow()
			.getOrder()).isEqualTo(2);
	}

	@Test
	void should_find_contract_files_with_converters() throws Exception {
		File baseDir = new File(getClass().getResource("/directory/with/mixed").toURI());
		ContractFileScanner scanner = new ContractFileScanner(baseDir, null, null, null, null) {
			@Override
			protected List<ContractConverter> converters() {
				return new ArrayList<>(List.of(new ContractConverter<Object>() {
					@Override
					public boolean isAccepted(File file) {
						return file.getName().endsWith(".json");
					}

					@Override
					public Collection<Contract> convertFrom(File file) {
						throw new RuntimeException("boom");
					}

					@Override
					public Object convertTo(Collection<Contract> contract) {
						throw new RuntimeException("boom");
					}
				}));
			}
		};
		assertThatThrownBy(scanner::findContractsRecursively).isInstanceOf(IllegalStateException.class)
			.hasCauseInstanceOf(RuntimeException.class)
			.getCause()
			.hasMessage("boom");
	}

	@Test
	void should_prefer_custom_yaml_converter_over_standard_yaml_converter() throws Exception {
		File baseDir = new File(getClass().getResource("/directory/with/custom/yml").toURI());
		ContractFileScanner scanner = new ContractFileScanner(baseDir, null, null, null, null) {
			@Override
			protected List<ContractConverter> converters() {
				return new ArrayList<>(List.of(new ContractConverter<Object>() {
					@Override
					public boolean isAccepted(File file) {
						if (!file.getName().endsWith(".yml") && !file.getName().endsWith(".yaml")) {
							return false;
						}
						try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
							String line = reader.readLine();
							return line != null && line.startsWith("custom_format: 1.0");
						}
						catch (Exception ex) {
							return false;
						}
					}

					@Override
					public Collection<Contract> convertFrom(File file) {
						return Collections.singleton(new Contract());
					}

					@Override
					public Object convertTo(Collection<Contract> contract) {
						return new Object();
					}
				}));
			}
		};
		Map<Path, List<ContractMetadata>> result = scanner.findContractsRecursively();
		assertThat(result.keySet()).hasSize(1);
		assertThat(result.entrySet())
			.allMatch((e) -> e.getValue().stream().anyMatch((m) -> m.getConvertedContract() != null));
	}

	@Test
	void should_find_contracts_for_include_pattern() throws Exception {
		FileSystemUtils.copyRecursively(new File(getClass().getResource("/directory/with/common-messaging").toURI()),
				tmpFolder);
		File baseDir = tmpFolder;
		Set<String> included = Set.of("social-service/**", "**/coupon-collected/**/*V1*");
		ContractFileScanner scanner = new ContractFileScanner(baseDir, Set.of(), Set.of(), included, null);
		Map<Path, List<ContractMetadata>> result = scanner.findContractsRecursively();
		assertThat(result.keySet()).hasSize(3);
		List<ContractMetadata> allContracts = result.values().stream().flatMap(Collection::stream).toList();
		assertThat(allContracts.stream()
			.filter((m) -> "couponCollectedEventV1.groovy".equals(m.getPath().getFileName().toString()))
			.findFirst()
			.orElseThrow()
			.getGroupSize()).isEqualTo(2);
		assertThat(allContracts.stream()
			.filter((m) -> m.getConvertedContract() != null
					&& m.getConvertedContract().stream().anyMatch((c) -> "couponCollectedV1".equals(c.getLabel())))
			.findFirst()).isPresent();
		assertThat(allContracts.stream()
			.filter((m) -> "couponCollectedEventV2.groovy".equals(m.getPath().getFileName().toString()))
			.toList()).isEmpty();
		assertThat(allContracts.stream()
			.filter((m) -> "shouldUpdateUserInfo.groovy".equals(m.getPath().getFileName().toString()))
			.findFirst()
			.orElseThrow()
			.getGroupSize()).isEqualTo(1);
		assertThat(allContracts.stream()
			.filter((m) -> "shouldReturnEmptyFriendsWhenGetFriends.groovy".equals(m.getPath().getFileName().toString()))
			.findFirst()
			.orElseThrow()
			.getGroupSize()).isEqualTo(1);
		assertThat(result.get(baseDir.toPath().resolve("coupon-sent"))).isNull();
		assertThat(result.get(baseDir.toPath().resolve("reward-rules"))).isNull();
	}

}
