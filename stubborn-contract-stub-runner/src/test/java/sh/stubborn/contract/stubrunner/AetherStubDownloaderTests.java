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
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.Authentication;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import sh.stubborn.contract.stubrunner.StubsMode;

import static org.assertj.core.api.Assertions.assertThat;

class AetherStubDownloaderTests {

	@TempDir
	Path tmpDir;

	private String originalMavenRepoLocal;

	private String originalMavenUserSettings;

	private String originalMavenUserConfigDir;

	@BeforeEach
	void saveSystemProps() {
		this.originalMavenRepoLocal = System.getProperty("maven.repo.local");
		this.originalMavenUserSettings = System.getProperty("org.apache.maven.user-settings");
		this.originalMavenUserConfigDir = System.getProperty("maven.user.config.dir");
	}

	@AfterEach
	void restoreSystemProps() {
		restoreProperty("maven.repo.local", this.originalMavenRepoLocal);
		restoreProperty("org.apache.maven.user-settings", this.originalMavenUserSettings);
		restoreProperty("maven.user.config.dir", this.originalMavenUserConfigDir);
	}

	private void restoreProperty(String key, String value) {
		if (value != null) {
			System.setProperty(key, value);
		}
		else {
			System.clearProperty(key);
		}
	}

	@Test
	void shouldThrowAnExceptionWhenArtifactNotFoundInLocalM2() {
		StubRunnerOptions stubRunnerOptions = new StubRunnerOptionsBuilder().withStubsMode(StubsMode.LOCAL).build();
		AetherStubDownloader aetherStubDownloader = new AetherStubDownloader(stubRunnerOptions);

		Map.Entry<StubConfiguration, File> entry = aetherStubDownloader.downloadAndUnpackStubJar(
				new StubConfiguration("non.existing.group", "missing-artifact-id", "1.0-SNAPSHOT"));

		assertThat(entry).isNull();
	}

	@Test
	void shouldThrowAnExceptionWhenLocalM2GetsReplacedWithATempDirAndAJarIsNotFoundInRemote() throws Exception {
		StubRunnerOptions stubRunnerOptions = new StubRunnerOptionsBuilder().withStubsMode(StubsMode.REMOTE)
			.withStubRepositoryRoot("file://" + Files.createTempDirectory(this.tmpDir, "repo").toAbsolutePath())
			.build();

		String m2repoFolder = "m2repo" + File.separator + "repository";
		URL resource = AetherStubDownloaderTests.class.getClassLoader().getResource(m2repoFolder);
		File localRepo = new File(resource.toURI());
		System.setProperty("maven.repo.local", localRepo.getAbsolutePath());
		assertThat(new File(localRepo, "sh/stubborn/contract/verifier/stubs/bootService".replace("/", File.separator))
			.list()).isNotEmpty();

		AetherStubDownloader aetherStubDownloader = new AetherStubDownloader(stubRunnerOptions);

		Map.Entry<StubConfiguration, File> entry = aetherStubDownloader.downloadAndUnpackStubJar(
				new StubConfiguration("sh.stubborn.contract.verifier.stubs", "bootService", "+", ""));

		assertThat(entry).isNull();
	}

	@Test
	void shouldUseLocalRepositoryFromSettingsXml() throws Exception {
		String m2repoFolder = "m2repo" + File.separator + "repository";
		URL resource = AetherStubDownloaderTests.class.getClassLoader().getResource(m2repoFolder);
		File localRepoFile = new File(resource.toURI());

		Path tempSettings = Files.createTempFile(this.tmpDir, "settings", ".xml");
		Files.writeString(tempSettings,
				"<settings><localRepository>" + localRepoFile.getAbsolutePath() + "</localRepository></settings>");
		System.setProperty("org.apache.maven.user-settings", tempSettings.toAbsolutePath().toString());

		RepositorySystemSession repositorySystemSession = AetherFactories
			.newSession(AetherFactories.repositorySystemOr(null), true);

		StubRunnerOptions stubRunnerOptions = new StubRunnerOptionsBuilder().withStubsMode(StubsMode.LOCAL).build();
		AetherStubDownloader aetherStubDownloader = new AetherStubDownloader(stubRunnerOptions);

		Map.Entry<StubConfiguration, File> jar = aetherStubDownloader.downloadAndUnpackStubJar(
				new StubConfiguration("sh.stubborn.contract.verifier.stubs", "bootService", "0.0.1-SNAPSHOT"));

		assertThat(jar).isNotNull();
		assertThat(repositorySystemSession.getLocalRepository().getBasedir().getAbsolutePath()).endsWith(m2repoFolder);
	}

	@Test
	void shouldReturnCredentialsFromSettingsXml() throws Exception {
		File settings = new File(AetherStubDownloaderTests.class.getResource("/.m2/settings.xml").toURI());
		System.setProperty("org.apache.maven.user-settings", settings.getAbsolutePath());

		File configDir = new File(AetherStubDownloaderTests.class.getResource("/.m2").toURI());
		System.setProperty("maven.user.config.dir", configDir.getAbsolutePath());

		StubRunnerOptions stubRunnerOptions = new StubRunnerOptionsBuilder().withStubsMode(StubsMode.REMOTE)
			.withStubRepositoryRoot(AetherStubDownloaderTests.class.getResource("/m2repo/repository").toString())
			.withServerId("my-server")
			.build();
		AetherStubDownloader aetherStubDownloader = new AetherStubDownloader(stubRunnerOptions) {
			@Override
			Authentication buildAuthentication(String stubServerPassword, String username) {
				assertThat(username).isEqualTo("admin");
				assertThat(stubServerPassword).isEqualTo("mypassword");
				return super.buildAuthentication(stubServerPassword, username);
			}
		};

		Map.Entry<StubConfiguration, File> jar = aetherStubDownloader.downloadAndUnpackStubJar(
				new StubConfiguration("sh.stubborn.contract.verifier.stubs", "bootService", "0.0.1-SNAPSHOT"));

		assertThat(jar).isNotNull();
	}

}
