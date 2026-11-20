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

import java.net.URI;
import java.util.Collection;

import javax.inject.Inject;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.ValueSource;
import org.gradle.api.provider.ValueSourceParameters;
import sh.stubborn.contract.stubrunner.ScmStubDownloaderBuilder;

/**
 * Resolves the commit id of the configured branch of a git contracts repository.
 * <p>
 * Implemented as a {@link ValueSource} so that, with the configuration cache on, Gradle
 * obtains the value again on every build when it checks whether the cache entry can be
 * reused. A plain provider would be evaluated once, when the entry is stored, and a new
 * commit on the contracts repository would never make {@code copyContracts} out of date.
 *
 * @author Marcin Grzejszczak
 * @since 0.2.4
 */
abstract class GitCommitIdValueSource implements ValueSource<String, GitCommitIdValueSource.Params> {

	private static final Logger log = Logging.getLogger(GitCommitIdValueSource.class);

	@Inject
	public GitCommitIdValueSource() {
	}

	interface Params extends ValueSourceParameters {

		Property<String> getRepositoryUrl();

		Property<String> getBranch();

		Property<String> getUsername();

		Property<String> getPassword();

	}

	@Override
	public String obtain() {
		Params params = getParameters();
		String repositoryUrl = params.getRepositoryUrl().getOrNull();
		if (repositoryUrl == null || !ScmStubDownloaderBuilder.isProtocolAccepted(repositoryUrl)) {
			return null;
		}
		String branch = params.getBranch().getOrNull();
		branch = hasText(branch) ? branch : "master";
		UsernamePasswordCredentialsProvider provider = null;
		if (hasText(params.getUsername().getOrNull())) {
			provider = new UsernamePasswordCredentialsProvider(params.getUsername().get(),
					params.getPassword().getOrElse(""));
		}
		try {
			Collection<Ref> refs = Git.lsRemoteRepository()
				.setRemote(stripScheme(repositoryUrl))
				.setCredentialsProvider(provider)
				.call();
			for (Ref ref : refs) {
				if (ref.getName().equals(branch) || ref.getName().equals("refs/heads/" + branch)
						|| ref.getName().equals("refs/tags/" + branch)) {
					return ref.getObjectId().name();
				}
			}
		}
		catch (GitAPIException e) {
			log.warn("Unable to determine git repository commit id", e);
		}
		return null;
	}

	private static String stripScheme(String repositoryUrl) {
		String part = URI.create(repositoryUrl).getSchemeSpecificPart();
		if (part == null || part.isEmpty()) {
			return part;
		}
		return part.startsWith("//") ? part.substring(2) : part;
	}

	private static boolean hasText(String s) {
		return s != null && !s.isBlank();
	}

}
