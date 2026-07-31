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
import java.io.InputStream;
import java.net.URI;
import java.net.URL;

import org.jspecify.annotations.Nullable;

/**
 * Primitive version of a Git {@link StubResource}.
 *
 * @author Marcin Grzejszczak
 */
class GitResource implements StubResource {

	private final String rawLocation;

	GitResource(String location) {
		this.rawLocation = location;
	}

	@Override
	public String getDescription() {
		return this.rawLocation;
	}

	@Override
	public @Nullable InputStream getInputStream() throws IOException {
		return null;
	}

	@Override
	public URI getURI() throws IOException {
		return URI.create(this.rawLocation);
	}

	@Override
	public URL getURL() throws IOException {
		return URI.create(this.rawLocation).toURL();
	}

	@Override
	public File getFile() throws IOException {
		return new File(URI.create(this.rawLocation));
	}

	@Override
	public String getFilename() {
		String path = this.rawLocation;
		int sep = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
		return (sep >= 0) ? path.substring(sep + 1) : path;
	}

}
