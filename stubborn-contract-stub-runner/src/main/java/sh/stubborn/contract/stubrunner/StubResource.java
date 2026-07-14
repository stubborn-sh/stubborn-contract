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

/**
 * Minimal resource abstraction used by Stub Runner, backed by {@link java.net.URL}.
 * Replaces the Spring {@code Resource} interface to remove the Spring Core dependency
 * from the stub-runner core module.
 *
 * @author Marcin Grzejszczak
 * @since 4.2.0
 */
public interface StubResource {

	URI getURI() throws IOException;

	URL getURL() throws IOException;

	File getFile() throws IOException;

	InputStream getInputStream() throws IOException;

	String getFilename();

	String getDescription();

	default boolean exists() {
		try {
			return getFile().exists();
		}
		catch (IOException ex) {
			return false;
		}
	}

}
