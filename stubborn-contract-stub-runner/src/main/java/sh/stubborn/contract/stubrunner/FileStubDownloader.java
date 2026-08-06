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
 * Allows to read stubs and contracts from a given location. Contrary to
 * {@link sh.stubborn.contract.stubrunner.AetherStubDownloaderBuilder}, doesn't require
 * the location to be a maven repository.
 *
 * @author Marcin Grzejszczak
 */
public class FileStubDownloader implements StubDownloaderBuilder {

	private static final List<String> ACCEPTABLE_PROTOCOLS = Collections.singletonList("stubs");

	/**
	 * Does any of the accepted protocols matches the URL of the repository.
	 * @param url - of the repository
	 * @return {@code true} if protocol is accepted
	 */
	public static boolean isProtocolAccepted(String url) {
		return ACCEPTABLE_PROTOCOLS.stream().anyMatch(url::startsWith);
	}

	@Override
	public @Nullable StubDownloader build(StubRunnerOptions stubRunnerOptions) {
		// should work only in remote and local option
		if (stubRunnerOptions.getStubsMode() == StubsMode.CLASSPATH
				|| stubRunnerOptions.getStubRepositoryRoot() == null) {
			return null;
		}
		StubResource resource = stubRunnerOptions.getStubRepositoryRoot();
		// we verify whether the protocol starts with `stubs://`
		if (!(resource instanceof StubsResource)) {
			return null;
		}
		return new StubsStubDownloader(stubRunnerOptions);
	}

	@Override
	public @Nullable StubResource resolve(String location) {
		if (location == null || location.isBlank() || !isProtocolAccepted(location)) {
			return null;
		}
		// Can be resolving a resource for Classpath as fallback
		if (!location.startsWith("stubs://file://")) {
			return new StubsResource(location);
		}
		// Convert any windows file format path to a uri
		String correctlyFormattedLocation = convertLocationToUriFormat(location);
		return new StubsResource(correctlyFormattedLocation);
	}

	private String convertLocationToUriFormat(String location) {
		final String correctlyFormattedLocation = separatorsToUnix(location);
		final String rawPath = correctlyFormattedLocation.replace("stubs://file://", "");
		if (rawPath.charAt(0) != '/') {
			return "stubs://file:///" + rawPath;
		}
		return correctlyFormattedLocation;
	}

	private String separatorsToUnix(String location) {
		return (location != null && location.indexOf(92) != -1) ? location.replace('\\', '/') : location;
	}

}
