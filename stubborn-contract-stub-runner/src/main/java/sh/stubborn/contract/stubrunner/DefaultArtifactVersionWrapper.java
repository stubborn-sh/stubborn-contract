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
import java.util.Objects;

import org.apache.maven.artifact.versioning.DefaultArtifactVersion;

class DefaultArtifactVersionWrapper implements Comparable<DefaultArtifactVersionWrapper> {

	final DefaultArtifactVersion version;

	final File file;

	final ProjectVersion projectVersion;

	DefaultArtifactVersionWrapper(File file) {
		this.version = new DefaultArtifactVersion(file.getName());
		this.file = file;
		this.projectVersion = new ProjectVersion(this.version.toString());
	}

	boolean isSnapshot() {
		return this.projectVersion.isSnapshot();
	}

	boolean isNotSnapshot() {
		return !isSnapshot();
	}

	@Override
	public int compareTo(DefaultArtifactVersionWrapper o) {
		return this.projectVersion.isMoreMature(o.projectVersion);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof DefaultArtifactVersionWrapper that)) {
			return false;
		}
		return Objects.equals(this.file, that.file);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.file);
	}

}
