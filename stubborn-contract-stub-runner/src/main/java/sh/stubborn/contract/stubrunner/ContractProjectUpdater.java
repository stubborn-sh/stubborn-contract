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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Updates the project containing contracts.
 *
 * @author Marcin Grzejszczak
 * @since 1.0.0
 */
public class ContractProjectUpdater {

	private static final Log log = LogFactory.getLog(ContractProjectUpdater.class);

	private static final int DEFAULT_ATTEMPTS_NO = 10;

	private static final long DEFAULT_WAIT_BETWEEN_ATTEMPTS = 1000;

	// TODO: Add this to the documentation
	private static final String DEFAULT_COMMIT_MESSAGE = "Updating project [$project] with stubs";

	private static final String GIT_ATTEMPTS_NO_PROP = "git.no-of-attempts";

	private static final String GIT_WAIT_BETWEEN_ATTEMPTS = "git.wait-between-attempts";

	private static final String GIT_COMMIT_MESSAGE = "git.commit-message";

	private final StubRunnerOptions stubRunnerOptions;

	private final GitContractsRepo gitContractsRepo;

	public ContractProjectUpdater(StubRunnerOptions stubRunnerOptions) {
		this.stubRunnerOptions = stubRunnerOptions;
		this.gitContractsRepo = new GitContractsRepo(stubRunnerOptions);
	}

	/**
	 * Merges the folder with stubs with the project containing contracts.
	 * @param projectName name of the project
	 * @param rootStubsFolder root folder of the stubs
	 */
	public void updateContractProject(String projectName, Path rootStubsFolder) {
		StubResource stubRepositoryRoot = Objects.requireNonNull(this.stubRunnerOptions.stubRepositoryRoot);
		File clonedRepo = this.gitContractsRepo.clonedRepo(stubRepositoryRoot);
		GitStubDownloaderProperties properties = new GitStubDownloaderProperties(stubRepositoryRoot,
				this.stubRunnerOptions);
		copyStubs(projectName, rootStubsFolder, clonedRepo);
		GitRepo gitRepo = new GitRepo(clonedRepo, properties);
		String msg = StubRunnerPropertyUtils.getProperty(this.stubRunnerOptions.getProperties(), GIT_COMMIT_MESSAGE);
		GitRepo.CommitResult commit = gitRepo.commit(clonedRepo, commitMessage(projectName, msg));
		if (commit == GitRepo.CommitResult.EMPTY) {
			log.info("There were no changes to commit. Won't push the changes");
			return;
		}
		String attempts = StubRunnerPropertyUtils.getProperty(this.stubRunnerOptions.getProperties(),
				GIT_ATTEMPTS_NO_PROP);
		int intAttempts = (attempts != null && !attempts.isBlank()) ? Integer.parseInt(attempts) : DEFAULT_ATTEMPTS_NO;
		String wait = StubRunnerPropertyUtils.getProperty(this.stubRunnerOptions.getProperties(),
				GIT_WAIT_BETWEEN_ATTEMPTS);
		long longWait = (wait != null && !wait.isBlank()) ? Long.parseLong(wait) : DEFAULT_WAIT_BETWEEN_ATTEMPTS;
		tryToPushCurrentBranch(clonedRepo, gitRepo, intAttempts, longWait);
	}

	private void tryToPushCurrentBranch(File clonedRepo, GitRepo gitRepo, int intAttempts, long longWait) {
		int currentAttempt = 0;
		while (currentAttempt < intAttempts) {
			log.info("Trying to push changes, attempt " + (currentAttempt + 1) + "/" + intAttempts);
			gitRepo.pull(clonedRepo);
			log.info("Successfully pulled changes from remote for project with contract and stubs");
			try {
				gitRepo.pushCurrentBranch(clonedRepo);
				log.info("Successfully pushed changes with current stubs");
				break;
			}
			catch (IllegalStateException ex) {
				// empty
				log.error("Exception occurred while trying to push the changes", ex);
				currentAttempt++;
				if (currentAttempt == intAttempts) {
					throw new IllegalStateException(
							"Failed to push changes to the project with contracts and stubs. Exceeded number of retries ["
									+ intAttempts + "]");
				}
				try {
					Thread.sleep(longWait);
				}
				catch (InterruptedException e1) {
					throw new IllegalStateException(e1);
				}
			}
		}
	}

	private String commitMessage(String projectName, String msg) {
		return (msg != null && !msg.isBlank()) ? replaceProject(projectName, msg)
				: replaceProject(projectName, DEFAULT_COMMIT_MESSAGE);
	}

	private String replaceProject(String projectName, String msg) {
		return msg.replace("$project", projectName);
	}

	private void copyStubs(String projectName, Path rootStubsFolder, File clonedRepo) {
		try {
			if (log.isDebugEnabled()) {
				log.debug("Copying stubs from [" + rootStubsFolder.toString() + "] to the cloned repo ["
						+ clonedRepo.getAbsolutePath() + "] for project [" + projectName + "]");
			}
			Files.walkFileTree(rootStubsFolder, new DirectoryCopyingVisitor(rootStubsFolder, clonedRepo.toPath()));
			if (log.isDebugEnabled()) {
				log.debug("Successfully copied stubs to the cloned repo for project [" + projectName + "]");
			}
		}
		catch (IOException ex) {
			throw new IllegalStateException(ex);
		}
	}

}
