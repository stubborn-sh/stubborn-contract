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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.Objects;

/**
 * A {@link StubResource} backed by a classpath location. Provides the path string for
 * classpath-based stub resolution.
 *
 * @author Marcin Grzejszczak
 */
class ClassPathStubResource implements StubResource {

	private final String path;

	ClassPathStubResource(String path) {
		this.path = path.startsWith("/") ? path.substring(1) : path;
	}

	String getPath() {
		return this.path;
	}

	@Override
	public URI getURI() throws IOException {
		URL url = getURL();
		try {
			return url.toURI();
		}
		catch (Exception ex) {
			throw new IOException("Cannot convert URL [" + url + "] to URI", ex);
		}
	}

	@Override
	public URL getURL() throws IOException {
		URL url = Thread.currentThread().getContextClassLoader().getResource(this.path);
		if (url == null) {
			url = ClassLoader.getSystemResource(this.path);
		}
		if (url == null) {
			throw new FileNotFoundException("Cannot locate classpath resource [" + this.path + "]");
		}
		return url;
	}

	@Override
	public File getFile() throws IOException {
		return new File(getURI());
	}

	@Override
	public InputStream getInputStream() throws IOException {
		InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(this.path);
		if (is == null) {
			throw new IOException("Cannot open classpath resource [" + this.path + "]");
		}
		return is;
	}

	@Override
	public String getFilename() {
		int sep = Math.max(this.path.lastIndexOf('/'), this.path.lastIndexOf('\\'));
		return (sep >= 0) ? this.path.substring(sep + 1) : this.path;
	}

	@Override
	public String getDescription() {
		return "classpath [" + this.path + "]";
	}

	@Override
	public boolean exists() {
		return Thread.currentThread().getContextClassLoader().getResource(this.path) != null;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof ClassPathStubResource that)) {
			return false;
		}
		return Objects.equals(this.path, that.path);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.path);
	}

}
