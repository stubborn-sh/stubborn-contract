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

package sh.stubborn.contract.verifier;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sh.stubborn.contract.spec.ContractVerifierException;
import sh.stubborn.contract.verifier.builder.JavaTestGenerator;
import sh.stubborn.contract.verifier.builder.ModelBasedTestGenerator;
import sh.stubborn.contract.verifier.builder.SingleTestGenerator;
import sh.stubborn.contract.verifier.config.ContractVerifierConfigProperties;
import sh.stubborn.contract.verifier.file.ContractFileScanner;
import sh.stubborn.contract.verifier.file.ContractMetadata;
import sh.stubborn.contract.verifier.util.NamesUtil;

/**
 * Generates contract verifier tests.
 *
 * @author Jakub Kubrynski, codearte.io
 */
public class TestGenerator {

	private static final Logger log = LoggerFactory.getLogger(TestGenerator.class);

	private static final String DEFAULT_CLASS_PREFIX = "ContractVerifier";

	private static final String DEFAULT_TEST_PACKAGE = "sh.stubborn.contract.verifier.tests";

	private final ContractVerifierConfigProperties configProperties;

	private final AtomicInteger counter = new AtomicInteger();

	private final SingleTestGenerator generator;

	private final FileSaver saver;

	private final ContractFileScanner contractFileScanner;

	public TestGenerator(ContractVerifierConfigProperties configProperties) {
		this(configProperties, singleTestGenerator(), new FileSaver(configProperties.getGeneratedTestSourcesDir(),
				configProperties.getTestFramework().getClassExtension()));
	}

	/**
	 * System property selecting the test generator. The model + typed-renderer generator
	 * ({@link ModelBasedTestGenerator}, JavaPoet-backed) is now the default; set this
	 * property to {@code false} to fall back to the legacy string-builder generator. The
	 * model generator itself keeps Spock and the not-yet-migrated shapes (WebTestClient,
	 * JAX-RS, messaging, URL-less contracts) on byte-identical legacy output, so the flip
	 * only changes the Java MockMvc/EXPLICIT HTTP path.
	 */
	private static final String MODEL_BASED_GENERATOR_PROPERTY = "stubborn.contract.verifier.model-based-generator";

	private static SingleTestGenerator singleTestGenerator() {
		if (!"false".equalsIgnoreCase(System.getProperty(MODEL_BASED_GENERATOR_PROPERTY, "true"))) {
			log.info("Using the model-based (JavaPoet) test generator [{} != false]", MODEL_BASED_GENERATOR_PROPERTY);
			return new ModelBasedTestGenerator();
		}
		log.info("Using the legacy test generator [{} = false]", MODEL_BASED_GENERATOR_PROPERTY);
		List<SingleTestGenerator> factories = new ArrayList<>();
		ServiceLoader.load(SingleTestGenerator.class).forEach(factories::add);
		if (factories.isEmpty()) {
			return new JavaTestGenerator();
		}
		return factories.get(0);
	}

	public TestGenerator(ContractVerifierConfigProperties configProperties, SingleTestGenerator generator,
			FileSaver saver) {
		this(configProperties, generator, saver,
				ContractFileScanner.builder()
					.baseDir(configProperties.getContractsDslDir())
					.excluded(toSet(configProperties.getExcludedFiles()))
					.ignored(toSet(configProperties.getIgnoredFiles()))
					.included(toSet(configProperties.getIncludedFiles()))
					.includeMatcher(configProperties.getIncludedContracts())
					.build());
	}

	private static Set<String> toSet(List<String> files) {
		return Optional.ofNullable(files).map(HashSet::new).orElseGet(HashSet::new);
	}

	protected TestGenerator(ContractVerifierConfigProperties configProperties, SingleTestGenerator generator,
			FileSaver saver, ContractFileScanner contractFileScanner) {
		this.configProperties = configProperties;
		if (configProperties.getContractsDslDir() == null) {
			throw new ContractVerifierException(
					"Stubs directory not found under " + configProperties.getContractsDslDir());
		}

		this.generator = generator;
		this.saver = saver;
		this.contractFileScanner = contractFileScanner;
	}

	public int generate() {
		generateTestClasses(basePackageName());
		NamesUtil.recrusiveDirectoryToPackage(this.configProperties.getGeneratedTestSourcesDir());
		NamesUtil.recrusiveDirectoryToPackage(this.configProperties.getGeneratedTestResourcesDir());
		return this.counter.get();
	}

	private String basePackageName() {
		if (StringUtils.isNotEmpty(this.configProperties.getBasePackageForTests())) {
			return this.configProperties.getBasePackageForTests();
		}
		else if (StringUtils.isNotEmpty(this.configProperties.getBaseClassForTests())) {
			return NamesUtil.toLastDot(this.configProperties.getBaseClassForTests());
		}
		else if (StringUtils.isNotEmpty(this.configProperties.getPackageWithBaseClasses())) {
			return this.configProperties.getPackageWithBaseClasses();
		}
		return DEFAULT_TEST_PACKAGE;
	}

	void generateTestClasses(final String basePackageName) {
		Map<Path, List<ContractMetadata>> contracts = this.contractFileScanner.findContractsRecursively();
		log.debug("Found the following contracts {}", contracts.keySet());

		Set<Map.Entry<Path, List<ContractMetadata>>> inProgress = inProgress(contracts);
		if (!inProgress.isEmpty() && this.configProperties.isFailOnInProgress()) {
			String inProgressContractsPaths = inProgress.stream()
				.map(Map.Entry::getKey)
				.map(Path::toString)
				.collect(Collectors.joining(","));
			throw new IllegalStateException("In progress contracts found in paths [" + inProgressContractsPaths
					+ "] and the switch [failOnInProgress] is set to [true]. Either unmark those contracts as in progress, or set the switch to [false].");
		}
		processAll(contracts, basePackageName);
	}

	private Set<Map.Entry<Path, List<ContractMetadata>>> inProgress(Map<Path, List<ContractMetadata>> contracts) {
		return contracts.entrySet()
			.stream()
			.filter((entry) -> entry.getValue().stream().anyMatch(ContractMetadata::anyInProgress))
			.collect(Collectors.toSet());
	}

	void processAll(Map<Path, List<ContractMetadata>> contracts, final String basePackageName) {
		contracts.entrySet()
			.stream()
			.forEach((entry) -> processIncludedDirectory(relativizeContractPath(entry), entry.getValue(),
					basePackageName));
	}

	private String relativizeContractPath(Map.Entry<Path, List<ContractMetadata>> entry) {
		Path relativePath = this.configProperties.getContractsDslDir().toPath().relativize(entry.getKey());
		return StringUtils.defaultIfEmpty(relativePath.toString(), DEFAULT_CLASS_PREFIX);
	}

	private void processIncludedDirectory(final String includedDirectoryRelativePath,
			final Collection<ContractMetadata> contracts, final String basePackageNameForClass) {
		log.debug("Collected contracts with metadata {} relative path is [{}]", contracts,
				includedDirectoryRelativePath);
		if (!contracts.isEmpty()) {
			String className = NamesUtil.afterLast(includedDirectoryRelativePath, File.separator) + resolveNameSuffix();
			String convertedClassName = ensureNameDoesNotStartWithNumber(
					NamesUtil.convertIllegalPackageChars(className));
			String packageName = buildPackage(basePackageNameForClass, includedDirectoryRelativePath);
			Path dir = this.saver.generateTestBaseDir(basePackageNameForClass,
					NamesUtil.convertIllegalPackageChars(includedDirectoryRelativePath));
			Path classPath = this.saver.pathToClass(dir, convertedClassName);
			byte[] classBytes = this.generator
				.buildClass(this.configProperties, contracts, includedDirectoryRelativePath,
						new SingleTestGenerator.GeneratedClassData(convertedClassName, packageName, classPath))
				.getBytes(StandardCharsets.UTF_8);
			this.saver.saveClassFile(classPath, classBytes);
			this.counter.incrementAndGet();
		}
	}

	private String ensureNameDoesNotStartWithNumber(String convertedClassName) {
		return convertedClassName.matches("[0-9]+.*") ? ("_" + convertedClassName) : convertedClassName;
	}

	private String resolveNameSuffix() {
		return StringUtils.defaultIfEmpty(this.configProperties.getNameSuffixForTests(),
				this.configProperties.getTestFramework().getClassNameSuffix());
	}

	protected static String buildPackage(final String packageNameForClass, final String includedDirectoryRelativePath) {
		String directory = NamesUtil.beforeLast(includedDirectoryRelativePath, File.separator);
		String convertedPackage = packageNameForClass + "."
				+ NamesUtil.directoryToPackage(NamesUtil.convertIllegalPackageChars(directory));
		return !directory.isEmpty() ? convertedPackage : packageNameForClass;
	}

}
