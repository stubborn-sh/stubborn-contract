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

package sh.stubborn.contract.maven.verifier;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.maven.api.plugin.testing.Basedir;
import org.apache.maven.api.plugin.testing.InjectMojo;
import org.apache.maven.api.plugin.testing.MojoExtension;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.supplier.RepositorySystemSupplier;
import org.eclipse.aether.util.repository.SimpleArtifactDescriptorPolicy;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.BDDAssertions.then;

/**
 * Contracts installed by Maven into a split local repository (
 * {@code -Daether.enhancedLocalRepository.split=true}) land under the {@code installed}
 * prefix. Resolution in {@code LOCAL} mode has to honor that layout.
 *
 * @see <a href=
 * "https://github.com/stubborn-sh/stubborn-contract/issues/266">stubborn-contract#266</a>
 */
class SplitLocalRepositoryIntegrationTests extends AbstractMojoIntegrationTests {

	private static final String SPLIT_CONFIG_PROPERTY = "aether.enhancedLocalRepository.split";

	private static final String LOCAL_REPOSITORY_PROPERTY = "maven.repo.local";

	private static final String ARTIFACT_PATH = "com/example/split-contracts/0.0.1-reproducer";

	private static final String ARTIFACT_FILE_NAME = "split-contracts-0.0.1-reproducer.jar";

	@Test
	@InjectMojo(goal = "generateTests", pom = "pom.xml")
	@Basedir("src/test/projects/local-split-contracts")
	void shouldGenerateTestsFromContractsInstalledInASplitLocalRepository(GenerateTestsMojo mojo) throws Exception {
		Path localRepository = freshLocalRepository();
		installContracts(localRepository.resolve("installed").resolve(ARTIFACT_PATH), "/split");

		String generatedTest = generateTests(mojo, localRepository);

		then(generatedTest).contains("/split");
	}

	@Test
	@InjectMojo(goal = "generateTests", pom = "pom.xml")
	@Basedir("src/test/projects/local-split-contracts")
	void shouldNotPreferAStaleArtifactFromTheUnsplitLayout(GenerateTestsMojo mojo) throws Exception {
		Path localRepository = freshLocalRepository();
		installContracts(localRepository.resolve("installed").resolve(ARTIFACT_PATH), "/split");
		installContracts(localRepository.resolve(ARTIFACT_PATH), "/stale");

		String generatedTest = generateTests(mojo, localRepository);

		then(generatedTest).contains("/split").doesNotContain("/stale");
	}

	private Path freshLocalRepository() throws Exception {
		Path localRepository = targetDir().resolve("split-local-repo");
		deleteDirectoryIfExists(localRepository);
		Files.createDirectories(localRepository);
		return localRepository;
	}

	private Path targetDir() {
		return Path.of(MojoExtension.getBasedir()).resolve("target");
	}

	/**
	 * Writes a contracts JAR holding a single HTTP contract for the given URL, the way
	 * {@code mvn install} would place it in the given directory.
	 * @param directory the artifact directory inside the local repository
	 * @param url the URL of the contract inside the JAR
	 */
	private void installContracts(Path directory, String url) throws Exception {
		Files.createDirectories(directory);
		String contract = """
				sh.stubborn.contract.spec.Contract.make {
					request {
						method 'GET'
						url '%s'
					}
					response {
						status 200
						body 'ok'
					}
				}
				""".formatted(url);
		try (OutputStream output = Files.newOutputStream(directory.resolve(ARTIFACT_FILE_NAME));
				ZipOutputStream jar = new ZipOutputStream(output)) {
			jar.putNextEntry(new ZipEntry("contracts/"));
			jar.closeEntry();
			jar.putNextEntry(new ZipEntry("contracts/shouldReturnOk.groovy"));
			jar.write(contract.getBytes(StandardCharsets.UTF_8));
			jar.closeEntry();
		}
	}

	private String generateTests(GenerateTestsMojo mojo, Path localRepository) throws Exception {
		Path targetDir = targetDir();
		Path generatedSources = targetDir.resolve("generated-test-sources/contracts");
		deleteDirectoryIfExists(generatedSources);
		Files.createDirectories(targetDir);
		setupBuildPaths(mojo, targetDir);
		MojoExtension.setVariableValueToObject(mojo, "generatedTestSourcesDir", generatedSources.toFile());
		MojoExtension.setVariableValueToObject(mojo, "generatedTestResourcesDir",
				targetDir.resolve("generated-test-resources/contracts").toFile());
		MojoExtension.setVariableValueToObject(mojo, "incrementalContractTests", false);
		configureSplitRepositorySystem(mojo, localRepository);
		String previousLocalRepository = System.getProperty(LOCAL_REPOSITORY_PROPERTY);
		System.setProperty(LOCAL_REPOSITORY_PROPERTY, localRepository.toString());
		try {
			mojo.execute();
		}
		finally {
			if (previousLocalRepository != null) {
				System.setProperty(LOCAL_REPOSITORY_PROPERTY, previousLocalRepository);
			}
			else {
				System.clearProperty(LOCAL_REPOSITORY_PROPERTY);
			}
		}
		List<Path> generatedJava;
		try (Stream<Path> paths = Files.walk(generatedSources)) {
			generatedJava = paths.filter((path) -> path.toString().endsWith(".java")).toList();
		}
		then(generatedJava).as("generated test sources in %s", generatedSources).hasSize(1);
		return Files.readString(generatedJava.get(0));
	}

	private void configureSplitRepositorySystem(GenerateTestsMojo mojo, Path localRepository) throws Exception {
		RepositorySystem repoSystem = new RepositorySystemSupplier().get();
		DefaultRepositorySystemSession repoSession = MavenRepositorySystemUtils.newSession();
		repoSession.setConfigProperty(SPLIT_CONFIG_PROPERTY, "true");
		repoSession.setLocalRepositoryManager(
				repoSystem.newLocalRepositoryManager(repoSession, new LocalRepository(localRepository.toFile())));
		repoSession.setArtifactDescriptorPolicy(new SimpleArtifactDescriptorPolicy(true, true));
		MojoExtension.setVariableValueToObject(mojo, "repositorySystem", repoSystem);
		MojoExtension.setVariableValueToObject(mojo, "repoSession", repoSession);
	}

}
