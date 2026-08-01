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

import java.io.IOException;
import java.net.URI;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

class GitStubDownloaderProperties {

	private static final Log log = LogFactory.getLog(GitStubDownloaderProperties.class);

	private static final String GIT_BRANCH_PROPERTY = "git.branch";

	private static final String GIT_USERNAME_PROPERTY = "git.username";

	private static final String GIT_PASSWORD_PROPERTY = "git.password";

	private static final String GIT_ENSURE_GIT_SUFFIX_PROPERTY = "git.ensure-git-suffix";

	final URI url;

	final @Nullable String username;

	final @Nullable String password;

	final String branch;

	final Boolean ensureGitSuffix;

	GitStubDownloaderProperties(StubResource repo, StubRunnerOptions options) {
		String repoUrl;
		Map<String, String> args = options.getProperties();
		try {
			repoUrl = schemeSpecificPart(repo.getURI());
		}
		catch (IOException ex) {
			throw new IllegalStateException(ex);
		}
		// if we had git://https://... we want the part starting from https
		// if we had git://git@... we want the full address again
		// if the URL starts with git@... and ends with .git, we want to remove it
		String modifiedRepo = repoUrl.startsWith("git@") ? modifyUrlForGitRepo(repoUrl) : repoUrl;
		this.url = URI.create(modifiedRepo);
		String username = StubRunnerPropertyUtils.getProperty(args, GIT_USERNAME_PROPERTY);
		this.username = (username != null && !username.isBlank()) ? username : options.getUsername();
		String password = StubRunnerPropertyUtils.getProperty(args, GIT_PASSWORD_PROPERTY);
		this.password = (password != null && !password.isBlank()) ? password : options.getPassword();
		String branch = StubRunnerPropertyUtils.getProperty(args, GIT_BRANCH_PROPERTY);
		this.branch = (branch != null && !branch.isBlank()) ? branch : "master";
		String ensureGitSuffix = StubRunnerPropertyUtils.getProperty(args, GIT_ENSURE_GIT_SUFFIX_PROPERTY);
		this.ensureGitSuffix = (ensureGitSuffix != null && !ensureGitSuffix.isBlank())
				? Boolean.parseBoolean(ensureGitSuffix) : true;

		if (log.isDebugEnabled()) {
			log.debug("Repo url is [" + repoUrl + "], modified url string " + "is [" + modifiedRepo + "] URL is ["
					+ this.url + "]  branch is [" + this.branch + "] and ensureGitSuffix is [" + this.ensureGitSuffix
					+ "]");
		}
	}

	private String schemeSpecificPart(URI uri) {
		String part = uri.getSchemeSpecificPart();
		if (part == null || part.isBlank()) {
			return part;
		}
		return part.startsWith("//") ? part.substring(2) : part;
	}

	private String modifyUrlForGitRepo(String gitRepo) {
		return "git:" + gitRepo;
	}

}
