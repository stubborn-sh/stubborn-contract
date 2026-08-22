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
import java.nio.file.Files;
import java.nio.file.Path;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.eclipse.aether.RepositorySystem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * The module's {@code logback.xml} runs the {@code sh.stubborn} logger at TRACE so the
 * {@code isDebugEnabled()} / {@code isTraceEnabled()} guards are exercised on their
 * "true" side. That leaves the "false" side of those guards uncovered. This test drives
 * the git / Aether / filesystem classes once more with {@code sh.stubborn} logging
 * switched OFF, so the not-logging branch of each guard is covered too. No network access
 * is required.
 */
class StubRunnerLoggingGuardBranchTests {

	@Test
	void exercisesLoggingGuardsWithStubbornLoggingDisabled(@TempDir Path temp) throws Exception {
		Logger stubbornLogger = (Logger) LoggerFactory.getLogger("sh.stubborn");
		Level original = stubbornLogger.getLevel();
		stubbornLogger.setLevel(Level.OFF);
		try {
			exerciseAetherPaths();
			exerciseAetherFactories();
			exerciseGitRepoFailurePath(temp);
			exerciseDirectoryCopyingVisitor(temp);
		}
		finally {
			stubbornLogger.setLevel(original);
		}
	}

	private void exerciseAetherPaths() {
		StubRunnerOptions local = new StubRunnerOptionsBuilder().withStubsMode(StubsMode.LOCAL).build();
		AetherStubDownloader downloader = new AetherStubDownloader(local);

		// resolution miss -> the not-found guard/return branch, all with debug off
		assertThat(downloader
			.downloadAndUnpackStubJar(new StubConfiguration("no.such.group", "missing-artifact", "1.0-SNAPSHOT")))
			.isNull();
	}

	private void exerciseAetherFactories() {
		RepositorySystem system = AetherFactories.repositorySystemOr(null);
		assertThat(AetherFactories.repositorySystemOr(system)).isSameAs(system);
		AetherFactories.newSession(system, true);
		AetherFactories.newSession(system, false);
		assertThat(AetherFactories.settings()).isNotNull();
	}

	private void exerciseGitRepoFailurePath(Path temp) throws Exception {
		File basedir = Files.createDirectories(temp.resolve("git-clone-failure")).toFile();
		GitRepo gitRepo = new GitRepo(basedir, new ExceptionThrowingJGitFactory());

		assertThatIllegalStateException()
			.isThrownBy(() -> gitRepo.cloneProject(new File(temp.toFile(), "does-not-exist").toURI()));
	}

	private void exerciseDirectoryCopyingVisitor(Path temp) throws Exception {
		Path from = Files.createDirectories(temp.resolve("copy-from"));
		Path to = Files.createDirectories(temp.resolve("copy-to"));
		Files.writeString(Files.createDirectories(from.resolve("contracts")).resolve("c.json"), "{}");

		Files.walkFileTree(from, new DirectoryCopyingVisitor(from, to));

		assertThat(to.resolve("contracts/c.json")).exists();
	}

}
