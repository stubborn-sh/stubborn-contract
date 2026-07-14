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
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Replaces Spring's {@code PathMatchingResourcePatternResolver} for classpath and
 * file-system glob scanning without requiring Spring Core.
 *
 * <p>
 * Supports patterns of the form:
 * <ul>
 * <li>{@code classpath*:/some/path/**}/{@code *.json} — scans all classpath roots</li>
 * <li>{@code file:/absolute/path/**}/{@code *.json} — scans a file-system tree</li>
 * <li>Any other path treated as a plain file-system glob</li>
 * </ul>
 *
 * @author Marcin Grzejszczak
 * @since 4.2.0
 */
class ClasspathStubResourceScanner {

	private static final Log log = LogFactory.getLog(ClasspathStubResourceScanner.class);

	private static final String CLASSPATH_ALL_PREFIX = "classpath*:";

	private static final String CLASSPATH_PREFIX = "classpath:";

	List<StubResource> getResources(String pattern) throws IOException {
		if (pattern.startsWith(CLASSPATH_ALL_PREFIX)) {
			return scanClasspath(pattern.substring(CLASSPATH_ALL_PREFIX.length()));
		}
		if (pattern.startsWith(CLASSPATH_PREFIX)) {
			return scanClasspath(pattern.substring(CLASSPATH_PREFIX.length()));
		}
		return scanFileSystem(pattern);
	}

	void clearCache() {
	}

	private List<StubResource> scanClasspath(String subPattern) throws IOException {
		String globPattern = toGlob(subPattern);
		ClassLoader loader = Thread.currentThread().getContextClassLoader();
		if (loader == null) {
			loader = ClassLoader.getSystemClassLoader();
		}
		List<StubResource> result = new ArrayList<>();
		Enumeration<URL> roots = loader.getResources("");
		while (roots.hasMoreElements()) {
			URL root = roots.nextElement();
			String protocol = root.getProtocol();
			if ("file".equals(protocol)) {
				Path rootPath;
				try {
					rootPath = new File(root.toURI()).toPath();
				}
				catch (java.net.URISyntaxException ex) {
					throw new IOException("Cannot convert URL to URI: " + root, ex);
				}
				result.addAll(walkDirectory(rootPath, rootPath, globPattern));
			}
		}
		Enumeration<URL> jars = loader.getResources("META-INF");
		while (jars.hasMoreElements()) {
			URL url = jars.nextElement();
			String external = url.toExternalForm();
			if (external.startsWith("jar:")) {
				String jarPath = external.substring("jar:".length(), external.lastIndexOf("!/"));
				result.addAll(scanJar(jarPath, globPattern));
			}
		}
		return result;
	}

	private List<StubResource> walkDirectory(Path root, Path dir, String globPattern) throws IOException {
		PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + globPattern);
		List<StubResource> found = new ArrayList<>();
		if (!Files.isDirectory(dir)) {
			return found;
		}
		Files.walkFileTree(dir, new SimpleFileVisitor<>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
				Path relative = root.relativize(file);
				if (matcher.matches(relative) || matcher.matches(file)) {
					found.add(pathToResource(file));
				}
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFileFailed(Path file, IOException exc) {
				if (log.isTraceEnabled()) {
					log.trace("Skipping file [" + file + "]: " + exc.getMessage());
				}
				return FileVisitResult.CONTINUE;
			}
		});
		return found;
	}

	private List<StubResource> scanJar(String jarPath, String globPattern) {
		List<StubResource> result = new ArrayList<>();
		PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + globPattern);
		try (JarFile jar = new JarFile(jarPath.replace("file:", ""))) {
			Enumeration<JarEntry> entries = jar.entries();
			while (entries.hasMoreElements()) {
				JarEntry entry = entries.nextElement();
				if (!entry.isDirectory()) {
					Path entryPath = Path.of(entry.getName());
					if (matcher.matches(entryPath)) {
						URL url = new URL("jar:file:" + jar.getName() + "!/" + entry.getName());
						result.add(urlToResource(url, entry.getName()));
					}
				}
			}
		}
		catch (IOException e) {
			if (log.isTraceEnabled()) {
				log.trace("Could not scan jar [" + jarPath + "]: " + e.getMessage());
			}
		}
		return result;
	}

	private List<StubResource> scanFileSystem(String pattern) throws IOException {
		pattern = stripFileProtocol(pattern);
		int wildcardIndex = firstWildcard(pattern);
		if (wildcardIndex < 0) {
			File f = new File(pattern);
			List<StubResource> result = new ArrayList<>();
			if (f.exists()) {
				result.add(pathToResource(f.toPath()));
			}
			return result;
		}
		int lastSep = pattern.lastIndexOf('/', wildcardIndex - 1);
		String baseDir = lastSep > 0 ? pattern.substring(0, lastSep) : ".";
		String glob = lastSep > 0 ? pattern.substring(lastSep + 1) : pattern;
		Path root = Path.of(baseDir);
		return walkDirectory(root, root, glob);
	}

	private static String stripFileProtocol(String pattern) {
		if (pattern.startsWith("file:///")) {
			return pattern.substring("file://".length());
		}
		if (pattern.startsWith("file://")) {
			return pattern.substring("file://".length());
		}
		if (pattern.startsWith("file:")) {
			return pattern.substring("file:".length());
		}
		return pattern;
	}

	private static int firstWildcard(String pattern) {
		int star = pattern.indexOf('*');
		int question = pattern.indexOf('?');
		if (star < 0) {
			return question;
		}
		if (question < 0) {
			return star;
		}
		return Math.min(star, question);
	}

	private static String toGlob(String subPattern) {
		String p = subPattern.startsWith("/") ? subPattern.substring(1) : subPattern;
		return "**/" + p;
	}

	private static StubResource pathToResource(Path path) {
		return new StubResource() {
			@Override
			public URI getURI() {
				return path.toUri();
			}

			@Override
			public URL getURL() throws IOException {
				return path.toUri().toURL();
			}

			@Override
			public File getFile() {
				return path.toFile();
			}

			@Override
			public InputStream getInputStream() throws IOException {
				return Files.newInputStream(path);
			}

			@Override
			public String getFilename() {
				return path.getFileName() != null ? path.getFileName().toString() : null;
			}

			@Override
			public String getDescription() {
				return "file [" + path.toAbsolutePath() + "]";
			}

			@Override
			public boolean exists() {
				return Files.exists(path);
			}
		};
	}

	private static StubResource urlToResource(URL url, String name) {
		String filename = name.contains("/") ? name.substring(name.lastIndexOf('/') + 1) : name;
		return new StubResource() {
			@Override
			public URI getURI() throws IOException {
				try {
					return url.toURI();
				}
				catch (java.net.URISyntaxException ex) {
					throw new IOException("Cannot convert URL to URI: " + url, ex);
				}
			}

			@Override
			public URL getURL() {
				return url;
			}

			@Override
			public File getFile() throws IOException {
				throw new IOException("Classpath jar entry [" + name + "] cannot be resolved to a File");
			}

			@Override
			public InputStream getInputStream() throws IOException {
				return url.openStream();
			}

			@Override
			public String getFilename() {
				return filename;
			}

			@Override
			public String getDescription() {
				return "URL [" + url + "]";
			}

			@Override
			public boolean exists() {
				try (InputStream is = url.openStream()) {
					return true;
				}
				catch (IOException ex) {
					return false;
				}
			}
		};
	}

}
