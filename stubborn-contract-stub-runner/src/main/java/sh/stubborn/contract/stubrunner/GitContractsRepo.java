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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

class GitContractsRepo {

	static final Map<StubResource, File> CACHED_LOCATIONS = new ConcurrentHashMap<>();

	private static final Log log = LogFactory.getLog(GitContractsRepo.class);

	private static final String TEMP_DIR_PREFIX = "git-contracts";

	private final StubRunnerOptions options;

	GitContractsRepo(StubRunnerOptions options) {
		this.options = options;
	}

	File clonedRepo(StubResource repo) {
		File file = CACHED_LOCATIONS.get(repo);
		GitStubDownloaderProperties properties = new GitStubDownloaderProperties(repo, this.options);
		if (file == null) {
			File tmpDirWhereStubsWillBeUnzipped = TemporaryFileStorage.createTempDir(TEMP_DIR_PREFIX);
			GitRepo gitRepo = new GitRepo(tmpDirWhereStubsWillBeUnzipped, properties);
			file = gitRepo.cloneProject(properties.url);
			gitRepo.checkout(file, properties.branch);
			CACHED_LOCATIONS.put(repo, file);
			if (log.isDebugEnabled()) {
				log.debug("The project hasn't already been cloned. Cloned it to [" + file + "]");
			}
		}
		else {
			if (log.isDebugEnabled()) {
				log.debug("The project has already been cloned to [" + file
						+ "]. Will reset any changes and pull the latest ones.");
			}
			GitRepo gitRepo = new GitRepo(file, properties);
			gitRepo.reset(file);
			gitRepo.pull(file);
		}
		return file;
	}

}
