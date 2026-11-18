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

package sh.stubborn.contract.stubrunner;

import java.io.File;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.WorkspaceReader;
import org.eclipse.aether.repository.WorkspaceRepository;
import org.eclipse.aether.util.repository.SimpleArtifactDescriptorPolicy;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.BDDAssertions.then;

/**
 * Maven installs artifacts under the {@code installed} prefix when the split local
 * repository is enabled ({@code -Daether.enhancedLocalRepository.split=true}). Stubs and
 * contracts have to be resolved through that layout, too.
 *
 * @see <a href=
 * "https://github.com/stubborn-sh/stubborn-contract/issues/266">stubborn-contract#266</a>
 */
class AetherSplitLocalRepositoryTests {

	private static final String SPLIT_PROPERTY = "aether.enhancedLocalRepository.split";

	private static final String LOCAL_REPOSITORY_PROPERTY = "maven.repo.local";

	private static final String GROUP_ID = "com.example";

	private static final String ARTIFACT_ID = "split-contracts";

	private static final String VERSION = "0.0.1-reproducer";

	@TempDir
	@Nullable Path tmpDir;

	private @Nullable String originalSplit;

	private @Nullable String originalLocalRepository;

	@BeforeEach
	void saveSystemProps() {
		this.originalSplit = System.getProperty(SPLIT_PROPERTY);
		this.originalLocalRepository = System.getProperty(LOCAL_REPOSITORY_PROPERTY);
	}

	@AfterEach
	void restoreState() {
		restoreProperty(SPLIT_PROPERTY, this.originalSplit);
		restoreProperty(LOCAL_REPOSITORY_PROPERTY, this.originalLocalRepository);
		AetherStubDownloader.setRepositorySystemSessionFromMaven(null);
	}

	private void restoreProperty(String key, @Nullable String value) {
		if (value != null) {
			System.setProperty(key, value);
		}
		else {
			System.clearProperty(key);
		}
	}

	@Test
	void shouldKeepTheLocalRepositoryManagerOfTheMavenInjectedSession() throws Exception {
		RepositorySystem system = AetherFactories.repositorySystemOr(null);
		RepositorySystemSession mavenSession = splitSession(system, localRepository());

		RepositorySystemSession session = AetherFactories.newSession(system, true, mavenSession);

		then(session.getLocalRepositoryManager()).isSameAs(mavenSession.getLocalRepositoryManager());
		then(session.getLocalRepositoryManager()
			.getPathForLocalArtifact(new DefaultArtifact(GROUP_ID, ARTIFACT_ID, "jar", VERSION)))
			.startsWith("installed/");
		then(session.isOffline()).isTrue();
	}

	@Test
	void shouldNotResolveStubsFromTheReactorOfTheMavenInjectedSession() throws Exception {
		RepositorySystem system = AetherFactories.repositorySystemOr(null);
		DefaultRepositorySystemSession mavenSession = splitSession(system, localRepository());
		mavenSession.setWorkspaceReader(new ThrowingWorkspaceReader());

		RepositorySystemSession session = AetherFactories.newSession(system, true, mavenSession);

		then(session.getWorkspaceReader()).isNull();
	}

	@Test
	void shouldHonorTheSplitPropertyWhenNoSessionWasInjected() throws Exception {
		Path localRepository = localRepository();
		System.setProperty(LOCAL_REPOSITORY_PROPERTY, localRepository.toString());
		System.setProperty(SPLIT_PROPERTY, "true");
		RepositorySystem system = AetherFactories.repositorySystemOr(null);

		RepositorySystemSession session = AetherFactories.newSession(system, true, null);

		then(session.getLocalRepository().getBasedir()).isEqualTo(localRepository.toFile());
		then(session.getLocalRepositoryManager()
			.getPathForLocalArtifact(new DefaultArtifact(GROUP_ID, ARTIFACT_ID, "jar", VERSION)))
			.startsWith("installed/");
	}

	@Test
	void shouldDownloadStubsInstalledUnderTheSplitLocalRepositoryPrefix() throws Exception {
		Path localRepository = localRepository();
		installStubs(localRepository.resolve("installed"));
		RepositorySystem system = AetherFactories.repositorySystemOr(null);
		AetherStubDownloader.setRepositorySystemSessionFromMaven(splitSession(system, localRepository));
		StubRunnerOptions options = new StubRunnerOptionsBuilder().withStubsMode(StubsMode.LOCAL).build();

		Map.Entry<StubConfiguration, File> entry = new AetherStubDownloader(options)
			.downloadAndUnpackStubJar(new StubConfiguration(GROUP_ID, ARTIFACT_ID, VERSION));

		then(entry).isNotNull();
		then(new File(Objects.requireNonNull(entry).getValue(), "contracts/shouldReturnOk.groovy")).exists();
	}

	@Test
	void shouldNotFindStubsThatWereNotInstalledUnderTheSplitLocalRepositoryPrefix() throws Exception {
		Path localRepository = localRepository();
		installStubs(localRepository);
		RepositorySystem system = AetherFactories.repositorySystemOr(null);
		AetherStubDownloader.setRepositorySystemSessionFromMaven(splitSession(system, localRepository));
		StubRunnerOptions options = new StubRunnerOptionsBuilder().withStubsMode(StubsMode.LOCAL).build();

		Map.Entry<StubConfiguration, File> entry = new AetherStubDownloader(options)
			.downloadAndUnpackStubJar(new StubConfiguration(GROUP_ID, ARTIFACT_ID, VERSION));

		then(entry).isNull();
	}

	private Path localRepository() throws Exception {
		return Files.createDirectories(Path.of(String.valueOf(this.tmpDir), "split-local-repo"));
	}

	private DefaultRepositorySystemSession splitSession(RepositorySystem system, Path localRepository) {
		DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();
		session.setConfigProperty(SPLIT_PROPERTY, "true");
		session.setLocalRepositoryManager(
				system.newLocalRepositoryManager(session, new LocalRepository(localRepository.toFile())));
		session.setArtifactDescriptorPolicy(new SimpleArtifactDescriptorPolicy(true, true));
		return session;
	}

	/**
	 * Writes a stubs JAR the way {@code mvn install} would place it under the given
	 * prefix of the local repository.
	 * @param prefix the local repository, or its {@code installed} sub directory
	 */
	private void installStubs(Path prefix) throws Exception {
		Path directory = prefix.resolve(GROUP_ID.replace('.', '/') + "/" + ARTIFACT_ID + "/" + VERSION);
		Files.createDirectories(directory);
		String jarName = ARTIFACT_ID + "-" + VERSION + "-" + StubConfiguration.DEFAULT_CLASSIFIER + ".jar";
		try (OutputStream output = Files.newOutputStream(directory.resolve(jarName));
				ZipOutputStream jar = new ZipOutputStream(output)) {
			jar.putNextEntry(new ZipEntry("contracts/"));
			jar.closeEntry();
			jar.putNextEntry(new ZipEntry("contracts/shouldReturnOk.groovy"));
			jar.write("sh.stubborn.contract.spec.Contract.make { }".getBytes(StandardCharsets.UTF_8));
			jar.closeEntry();
		}
	}

	private static final class ThrowingWorkspaceReader implements WorkspaceReader {

		@Override
		public WorkspaceRepository getRepository() {
			return new WorkspaceRepository("test");
		}

		@Override
		public @Nullable File findArtifact(org.eclipse.aether.artifact.Artifact artifact) {
			throw new AssertionError("The workspace must not be consulted for stubs");
		}

		@Override
		public java.util.List<String> findVersions(org.eclipse.aether.artifact.Artifact artifact) {
			throw new AssertionError("The workspace must not be consulted for stubs");
		}

	}

}
