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
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Objects;

import org.eclipse.jgit.api.Git;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GitRepoMutationTests extends AbstractGitTests {

	private File project;

	private GitRepo gitRepo;

	@BeforeEach
	void setup() throws IOException, URISyntaxException {
		this.project = new File(GitRepoMutationTests.class.getResource("/git_samples/contract-git").toURI());
		TestUtils.prepareLocalRepo();
		this.gitRepo = new GitRepo(Objects.requireNonNull(this.tmpFolder));
	}

	@Test
	void commitReturnsSuccessfulWhenThereAreChanges() throws Exception {
		File cloned = this.gitRepo.cloneProject(this.project.toURI());
		createNewFile(cloned);

		GitRepo.CommitResult result = this.gitRepo.commit(cloned, "some message");

		assertThat(result).isEqualTo(GitRepo.CommitResult.SUCCESSFUL);
	}

	@Test
	void commitReturnsEmptyWhenThereAreNoChanges() throws Exception {
		File cloned = this.gitRepo.cloneProject(this.project.toURI());

		GitRepo.CommitResult result = this.gitRepo.commit(cloned, "nothing to commit");

		assertThat(result).isEqualTo(GitRepo.CommitResult.EMPTY);
	}

	@Test
	void shouldCheckoutRemoteBranchByCreatingTrackingBranch() throws Exception {
		// origin has an extra branch that only exists remotely from the working copy's
		// point of view; checking it out must go through the tracking-branch path
		File origin = clonedProject(newDir("origin"), this.project);
		try (Git git = openGitProject(origin)) {
			git.branchCreate().setName("feature").call();
		}
		File work = clonedProject(newDir("work"), origin);

		this.gitRepo.checkout(work, "feature");

		try (Git git = openGitProject(work)) {
			assertThat(git.getRepository().getBranch()).isEqualTo("feature");
		}
	}

	private File newDir(String prefix) throws IOException {
		return java.nio.file.Files.createTempDirectory(Objects.requireNonNull(this.tmpFolder).toPath(), prefix)
			.toFile();
	}

}
