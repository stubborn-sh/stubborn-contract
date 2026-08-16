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

import java.util.Collections;
import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * Builds a {@link StubDownloader} to work with contracts and stubs from a SCM.
 *
 * @author Marcin Grzejszczak
 * @since 1.0.0
 */
public final class ScmStubDownloaderBuilder implements StubDownloaderBuilder {

	private static final List<String> ACCEPTABLE_PROTOCOLS = Collections.singletonList("git");

	/**
	 * Does any of the accepted protocols matches the URL of the repository.
	 * @param url - of the repository
	 * @return {@code true} if the protocol is accepted
	 */
	public static boolean isProtocolAccepted(String url) {
		return ACCEPTABLE_PROTOCOLS.stream().anyMatch(url::startsWith);
	}

	@Override
	public @Nullable StubDownloader build(StubRunnerOptions stubRunnerOptions) {
		if (stubRunnerOptions.getStubsMode() == StubsMode.CLASSPATH
				|| stubRunnerOptions.getStubRepositoryRoot() == null) {
			return null;
		}
		StubResource resource = stubRunnerOptions.getStubRepositoryRoot();
		if (!(resource instanceof GitResource)) {
			return null;
		}
		return new GitStubDownloader(stubRunnerOptions);
	}

	@Override
	public @Nullable StubResource resolve(String location) {
		if (location == null || location.isBlank() || !isProtocolAccepted(location)) {
			return null;
		}
		return new GitResource(location);
	}

}
