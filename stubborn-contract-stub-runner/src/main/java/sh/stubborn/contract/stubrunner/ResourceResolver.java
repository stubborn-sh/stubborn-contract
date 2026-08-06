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
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

/**
 * Resolves a URL string to a {@link StubResource} by iterating over
 * {@link StubDownloaderBuilder} instances loaded from {@link java.util.ServiceLoader} and
 * the built-in default builders.
 *
 * IMPORTANT! Internal tool. Do not use.
 *
 * @author Marcin Grzejszczak
 * @since 2.0.0
 */
public final class ResourceResolver {

	private static final Log log = LogFactory.getLog(ResourceResolver.class);

	private static final List<StubDownloaderBuilder> RESOLVERS = new ArrayList<>();

	static {
		ServiceLoader.load(StubDownloaderBuilder.class).forEach(RESOLVERS::add);
		RESOLVERS.addAll(new StubDownloaderBuilderProvider().defaultStubDownloaderBuilders());
	}

	private ResourceResolver() {
		throw new IllegalStateException("Can't instantiate a utility class");
	}

	/**
	 * Converts a URL string to a {@link StubResource}.
	 * @param url string URL
	 * @return corresponding {@link StubResource}, or a plain-URL resource if no builder
	 * handles the protocol
	 */
	public static @Nullable StubResource resource(String url) {
		if (url == null || url.isBlank()) {
			return null;
		}
		for (StubDownloaderBuilder builder : RESOLVERS) {
			try {
				StubResource resolved = builder.resolve(url);
				if (resolved != null) {
					return resolved;
				}
			}
			catch (Exception ex) {
				log.error("Exception while resolving [" + url + "] via [" + builder.getClass().getSimpleName() + "]",
						ex);
			}
		}
		return plainUrlResource(url);
	}

	static ClassPathStubResource classpathResource(String path) {
		return new ClassPathStubResource(path);
	}

	private static StubResource plainUrlResource(String url) {
		if (url.startsWith("classpath:") || url.startsWith("classpath*:")) {
			String path = url.replaceFirst("classpath\\*?:", "");
			return classpathResource(path);
		}
		// No URL scheme → treat as classpath (matches Spring DefaultResourceLoader
		// behavior)
		if (!url.contains(":")) {
			return classpathResource(url);
		}
		return new StubResource() {
			@Override
			public URI getURI() throws IOException {
				try {
					return new URL(url).toURI();
				}
				catch (URISyntaxException ex) {
					throw new IOException("Cannot convert URL to URI: " + url, ex);
				}
			}

			@Override
			public URL getURL() throws IOException {
				return new URL(url);
			}

			@Override
			public File getFile() throws IOException {
				try {
					return new File(new URL(url).toURI());
				}
				catch (URISyntaxException ex) {
					throw new IOException("Cannot convert URL to URI: " + url, ex);
				}
			}

			@Override
			public InputStream getInputStream() throws IOException {
				return new URL(url).openStream();
			}

			@Override
			public String getFilename() {
				String path = (url.contains("?")) ? url.substring(0, url.indexOf('?')) : url;
				int sep = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
				return (sep >= 0) ? path.substring(sep + 1) : path;
			}

			@Override
			public String getDescription() {
				return "URL [" + url + "]";
			}
		};
	}

}
