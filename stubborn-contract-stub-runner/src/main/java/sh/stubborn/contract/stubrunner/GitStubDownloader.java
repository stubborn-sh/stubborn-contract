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
import java.util.AbstractMap;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

class GitStubDownloader implements StubDownloader {

	private static final Log log = LogFactory.getLog(GitStubDownloader.class);

	// Preloading class for the shutdown hook not to throw ClassNotFound
	private static final Class CLAZZ = TemporaryFileStorage.class;

	private final StubRunnerOptions stubRunnerOptions;

	private final boolean deleteStubsAfterTest;

	private final GitContractsRepo gitContractsRepo;

	GitStubDownloader(StubRunnerOptions stubRunnerOptions) {
		this.stubRunnerOptions = stubRunnerOptions;
		this.deleteStubsAfterTest = this.stubRunnerOptions.isDeleteStubsAfterTest();
		this.gitContractsRepo = new GitContractsRepo(stubRunnerOptions);
		registerShutdownHook();
	}

	@Override
	public Map.@Nullable Entry<StubConfiguration, File> downloadAndUnpackStubJar(StubConfiguration stubConfiguration) {
		try {
			if (log.isDebugEnabled()) {
				log.debug("Trying to find a contract for [" + stubConfiguration.toColonSeparatedDependencyNotation()
						+ "]");
			}
			StubResource repo = Objects.requireNonNull(this.stubRunnerOptions.getStubRepositoryRoot());
			File clonedRepo = this.gitContractsRepo.clonedRepo(repo);
			FileWalker walker = new FileWalker(stubConfiguration);
			Files.walkFileTree(clonedRepo.toPath(), walker);
			Path foundFile = walker.foundFile;
			if (foundFile != null) {
				return new AbstractMap.SimpleEntry<>(stubConfiguration, foundFile.toFile());
			}
		}
		catch (IOException ex) {
			throw new IllegalStateException(ex);
		}
		if (log.isWarnEnabled()) {
			log.warn("No matching contracts were found in the repo for ["
					+ stubConfiguration.toColonSeparatedDependencyNotation() + "]");
		}
		return null;
	}

	private void registerShutdownHook() {
		Runtime.getRuntime()
			.addShutdownHook(
					new Thread(() -> TemporaryFileStorage.cleanup(GitStubDownloader.this.deleteStubsAfterTest)));
	}

}
