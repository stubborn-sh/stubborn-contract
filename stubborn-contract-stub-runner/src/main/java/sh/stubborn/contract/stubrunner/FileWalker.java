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
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

class FileWalker extends SimpleFileVisitor<Path> {

	private static final Log log = LogFactory.getLog(FileWalker.class);

	private static final List<String> LATEST = Arrays.asList("latest", "+");

	private static final String RELEASE = "release";

	private final PathMatcher matcherWithDot;

	private final PathMatcher matcherWithoutDot;

	private final boolean latestSnapshotVersion;

	private final boolean latestReleaseVersion;

	@Nullable Path foundFile;

	FileWalker(StubConfiguration stubConfiguration) {
		this.latestSnapshotVersion = LATEST.stream()
			.anyMatch((s) -> s.equals(stubConfiguration.version.toLowerCase(Locale.ROOT)));
		this.latestReleaseVersion = RELEASE.equals(stubConfiguration.version.toLowerCase(Locale.ROOT));
		this.matcherWithDot = FileSystems.getDefault().getPathMatcher("glob:" + matcherGlob(stubConfiguration, "."));
		this.matcherWithoutDot = FileSystems.getDefault().getPathMatcher("glob:" + matcherGlob(stubConfiguration, "/"));
	}

	private String matcherGlob(StubConfiguration stubConfiguration, String groupArtifactSeparator) {
		return "**" + stubConfiguration.groupId + groupArtifactSeparator + stubConfiguration.artifactId + "/"
				+ ((this.latestSnapshotVersion || this.latestReleaseVersion) ? "**" : stubConfiguration.version);
	}

	@Override
	public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
		if (this.matcherWithDot.matches(dir.toAbsolutePath()) || this.matcherWithoutDot.matches(dir.toAbsolutePath())) {
			if (this.latestSnapshotVersion || this.latestReleaseVersion) {
				// folders with name latest, release
				File[] files = Objects
					.requireNonNull(Objects.requireNonNull(dir.getParent()).toFile().listFiles(File::isDirectory));
				File file = folderWithPredefinedName(files);
				if (file != null) {
					if (log.isDebugEnabled()) {
						log.debug("Found folder with name corresponding to a latest version [" + file + "] ");
					}
					this.foundFile = file.toPath();
					return FileVisitResult.TERMINATE;
				}
				return latestVersionFromFolders(dir, files);
			}
			else {
				this.foundFile = dir;
			}
			return FileVisitResult.TERMINATE;
		}
		return FileVisitResult.CONTINUE;
	}

	private FileVisitResult latestVersionFromFolders(Path dir, File[] files) {
		List<DefaultArtifactVersionWrapper> versions = pickLatestVersion(files);
		if (versions.isEmpty()) {
			if (log.isDebugEnabled()) {
				log.debug("Not a single version matching semver for path [" + dir.toAbsolutePath().toString()
						+ "] was found");
			}
			return FileVisitResult.CONTINUE;
		}
		// 2.0.0.RELEASE, 2.0.0.BUILD-SNAPSHOT
		// 2.0.0.RELEASE
		DefaultArtifactVersionWrapper latestFoundVersion = versions.get(versions.size() - 1);
		latestFoundVersion = replaceWithSnapshotIfSameVersions(versions, latestFoundVersion);
		this.foundFile = latestFoundVersion.file.toPath();
		return FileVisitResult.TERMINATE;
	}

	private DefaultArtifactVersionWrapper replaceWithSnapshotIfSameVersions(
			List<DefaultArtifactVersionWrapper> versions, final DefaultArtifactVersionWrapper latestFoundVersion) {
		if (versions.size() > 1 && this.latestSnapshotVersion) {
			// 2.0.1.BUILD-SNAPSHOT, 2.0.0.BUILD-SNAPSHOT
			// 2.0.0.BUILD-SNAPSHOT, 2.0.0.RELEASE
			DefaultArtifactVersionWrapper sameVersionButSnapshot = versions.stream()
				.filter((w) -> w.projectVersion.isSameWithoutSuffix(latestFoundVersion.projectVersion)
						&& w.isSnapshot())
				.findFirst()
				.orElse(latestFoundVersion);
			// 2.0.0 vs 2.0.0
			// replace the RELEASE one with SNAPSHOT
			if (sameVersionButSnapshot != latestFoundVersion) {
				return sameVersionButSnapshot;
			}
		}
		return latestFoundVersion;
	}

	private @Nullable File folderWithPredefinedName(File[] files) {
		if (this.latestSnapshotVersion) {
			return Arrays.stream(files)
				.filter((file) -> LATEST.stream().anyMatch((s) -> s.equals(file.getName().toLowerCase(Locale.ROOT))))
				.findFirst()
				.orElse(null);
		}
		return Arrays.stream(files)
			.filter((file) -> RELEASE.equals(file.getName().toLowerCase(Locale.ROOT)))
			.findFirst()
			.orElse(null);
	}

	private List<DefaultArtifactVersionWrapper> pickLatestVersion(File[] files) {
		return Arrays.stream(files)
			.map(DefaultArtifactVersionWrapper::new)
			.filter((wrapper) -> this.latestSnapshotVersion || wrapper.isNotSnapshot())
			.sorted()
			.collect(Collectors.toList());
	}

}
