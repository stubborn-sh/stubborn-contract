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
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

/**
 * Concrete logic of picking stubs.
 *
 * @author Marcin Grzejszczak
 * @since 1.0.0
 */
class StubsStubDownloader implements StubDownloader {

	private static final Log log = LogFactory.getLog(StubsStubDownloader.class);

	private static final String STUBS_FIND_PRODUCER_PROPERTY = "stubs.find-producer";

	private static final String LATEST_VERSION = "+";

	private final StubRunnerOptions stubRunnerOptions;

	StubsStubDownloader(StubRunnerOptions stubRunnerOptions) {
		this.stubRunnerOptions = stubRunnerOptions;
	}

	// StubConfiguration is the concrete stub to be fetched
	@Override
	public Map.@Nullable Entry<StubConfiguration, File> downloadAndUnpackStubJar(StubConfiguration stubConfiguration) {
		boolean shouldFindProducer = shouldFindProducer();
		if (!shouldFindProducer) {
			String schemeSpecific = schemeSpecificPart();
			log.info("Stubs are present under [" + schemeSpecific + "]. Will copy them to a temporary directory.");
			return new ResourceResolvingStubDownloader(this.stubRunnerOptions, this::repoRootForSchemeSpecificPart,
					this::anyPattern)
				.downloadAndUnpackStubJar(stubConfiguration);
		}
		return new ResourceResolvingStubDownloader(this.stubRunnerOptions, this::repoRoot, this::gavPattern)
			.downloadAndUnpackStubJar(stubConfiguration);
	}

	private RepoRoots repoRootForSchemeSpecificPart(StubRunnerOptions stubRunnerOptions,
			StubConfiguration configuration) {
		String specificPart = schemeSpecificPart();
		specificPart = specificPart.endsWith("/") ? specificPart : (specificPart + "/");
		specificPart = specificPart + "**";
		return new RepoRoots(Collections.singleton(new RepoRoot(specificPart)));
	}

	private Pattern anyPattern(StubConfiguration config) {
		return Pattern.compile(resolvePath() + "(.*)");
	}

	private String resolvePath() {
		String schemeSpecificPart = schemeSpecificPart();
		StubResource resource = ResourceResolver.resource(schemeSpecificPart);
		if (resource != null) {
			try {
				return resource.getURL().getFile();
			}
			catch (IOException ex) {
				return schemeSpecificPart;
			}
		}
		return schemeSpecificPart;
	}

	// for group id a.b.c and artifact id d
	// a.b.c/d
	// a/b/c/d
	private RepoRoots repoRoot(StubRunnerOptions stubRunnerOptions, StubConfiguration configuration) {
		String pathWithGroupAndArtifactId = "/" + configuration.getGroupId() + "/" + configuration.getArtifactId();
		String pathWithGroupAndArtifactIdSlashSeparated = "/" + configuration.getGroupId().replace(".", File.separator)
				+ "/" + configuration.getArtifactId();
		String anyFileSuffix = "/**/*.*";
		RepoRoots roots = RepoRoots.asList(
				new RepoRoot(schemeSpecificPart() + pathWithGroupAndArtifactId, anyFileSuffix),
				new RepoRoot(schemeSpecificPart() + pathWithGroupAndArtifactIdSlashSeparated, anyFileSuffix),
				new RepoRoot(schemeSpecificPart() + anyFileSuffix));
		if (!latestVersionIsSet(configuration)) {
			String pathWithGAV = pathWithGroupAndArtifactId + "/" + configuration.getVersion();
			String pathWithSlashSeparatedGAV = pathWithGroupAndArtifactIdSlashSeparated + "/"
					+ configuration.getVersion();
			roots.addAll(RepoRoots.asList(new RepoRoot(schemeSpecificPart() + pathWithGAV, anyFileSuffix),
					new RepoRoot(schemeSpecificPart() + pathWithSlashSeparatedGAV, anyFileSuffix)));
		}
		return roots;
	}

	private Pattern gavPattern(StubConfiguration config) {
		String version = config.getVersion();
		String ga = config.getGroupId() + "." + config.getArtifactId();
		String gav = latestVersionIsSet(config) ? ga : (ga + "." + version);
		return Pattern.compile("^(.*)(" + gav + ")(.*)$");
	}

	private boolean latestVersionIsSet(StubConfiguration configuration) {
		return LATEST_VERSION.equals(configuration.getVersion());
	}

	private boolean shouldFindProducer() {
		Map<String, String> args = this.stubRunnerOptions.getProperties();
		String findProducer = StubRunnerPropertyUtils.getProperty(args, STUBS_FIND_PRODUCER_PROPERTY);
		return Boolean.parseBoolean(findProducer);
	}

	// stubs://foo -> foo
	private String schemeSpecificPart() {
		try {
			String part = Objects.requireNonNull(this.stubRunnerOptions.getStubRepositoryRoot())
				.getURI()
				.getSchemeSpecificPart();
			if (part == null || part.isBlank()) {
				return part;
			}
			return part.startsWith("//") ? part.substring(2) : part;
		}
		catch (IOException ex) {
			throw new IllegalStateException(ex);
		}
	}

}
