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
import java.util.Map;
import java.util.Objects;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ContractProjectUpdaterMutationTests extends AbstractGitTests {

	private File originalProject;

	private File project;

	@BeforeEach
	void setup() throws Exception {
		GitContractsRepo.CACHED_LOCATIONS.clear();
		this.originalProject = new File(GitRepoTests.class.getResource("/git_samples/contract-git").toURI());
		TestUtils.prepareLocalRepo();
		File origin = clonedProject(
				Files.createTempDirectory(Objects.requireNonNull(this.tmpFolder).toPath(), "origin").toFile(),
				this.originalProject);
		this.project = clonedProject(
				Files.createTempDirectory(Objects.requireNonNull(this.tmpFolder).toPath(), "project").toFile(),
				this.originalProject);
		new GitRepo(Objects.requireNonNull(this.tmpFolder)).checkout(this.project, "master");
		setOriginOnProjectToTmp(origin, this.project);
	}

	@Test
	void shouldUseCustomCommitMessageFromProperty() throws Exception {
		StubRunnerOptions options = new StubRunnerOptionsBuilder()
			.withStubRepositoryRoot("file://" + this.project.getAbsolutePath() + "/")
			.withStubsMode(StubsMode.REMOTE)
			.withProperties(Map.of("git.commit-message", "Custom message for $project"))
			.build();
		ContractProjectUpdater updater = new ContractProjectUpdater(options);
		File stubs = new File(GitRepoTests.class.getResource("/git_samples/sample_stubs").toURI());

		updater.updateContractProject("hello-world", stubs.toPath());

		try (Git git = openGitProject(this.project)) {
			RevCommit revCommit = git.log().call().iterator().next();
			assertThat(revCommit.getShortMessage()).isEqualTo("Custom message for hello-world");
			git.reset().setMode(ResetCommand.ResetType.HARD).call();
		}
	}

	@Test
	void shouldHonourConfiguredAttemptsAndWaitProperties() throws Exception {
		StubRunnerOptions options = new StubRunnerOptionsBuilder()
			.withStubRepositoryRoot("file://" + this.project.getAbsolutePath() + "/")
			.withStubsMode(StubsMode.REMOTE)
			.withProperties(Map.of("git.no-of-attempts", "3", "git.wait-between-attempts", "10"))
			.build();
		ContractProjectUpdater updater = new ContractProjectUpdater(options);
		File stubs = new File(GitRepoTests.class.getResource("/git_samples/sample_stubs").toURI());

		updater.updateContractProject("hello-world", stubs.toPath());

		try (Git git = openGitProject(this.project)) {
			RevCommit revCommit = git.log().call().iterator().next();
			assertThat(revCommit.getShortMessage()).isEqualTo("Updating project [hello-world] with stubs");
			git.reset().setMode(ResetCommand.ResetType.HARD).call();
		}
	}

}
