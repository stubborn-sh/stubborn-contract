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

package sh.stubborn.contract.verifier.plugin;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.revwalk.RevCommit;
import org.gradle.api.Project;
import org.gradle.api.provider.Provider;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.BDDAssertions.then;

class GitCommitIdValueSourceTest {

	@TempDir
	Path tmp;

	@Test
	void shouldResolveTheCurrentCommitOfTheBranch() throws Exception {
		File repoDir = tmp.resolve("contracts-repo").toFile();
		try (Git git = Git.init().setDirectory(repoDir).setInitialBranch("main").call()) {
			RevCommit first = commit(git, repoDir, "a.yml");
			Project project = ProjectBuilder.builder().withProjectDir(tmp.resolve("project").toFile()).build();
			then(commitId(project, repoDir).get()).isEqualTo(first.getName());

			RevCommit second = commit(git, repoDir, "b.yml");

			// a new build obtains the value source again - this is what makes the
			// configuration cache notice a new commit on the contracts repository
			then(commitId(project, repoDir).get()).isEqualTo(second.getName());
		}
	}

	@Test
	void shouldBeAbsentForANonGitRepository() {
		Project project = ProjectBuilder.builder().withProjectDir(tmp.toFile()).build();
		Provider<String> commitId = project.getProviders().of(GitCommitIdValueSource.class, spec -> {
			spec.getParameters().getRepositoryUrl().set("https://repo.example.com/maven");
		});

		then(commitId.isPresent()).isFalse();
	}

	private static Provider<String> commitId(Project project, File repoDir) {
		return project.getProviders().of(GitCommitIdValueSource.class, spec -> {
			spec.getParameters().getRepositoryUrl().set("git://" + repoDir.toURI());
			spec.getParameters().getBranch().set("main");
		});
	}

	private static RevCommit commit(Git git, File repoDir, String file) throws Exception {
		Files.writeString(repoDir.toPath().resolve(file), "request: {}\n");
		git.add().addFilepattern(file).call();
		return git.commit().setMessage("add " + file).setSign(false).call();
	}

}
