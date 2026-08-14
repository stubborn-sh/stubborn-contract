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

package sh.stubborn.contract.verifier.util;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

import groovy.lang.GString;
import groovy.lang.GroovyShell;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sh.stubborn.contract.spec.Contract;
import sh.stubborn.contract.spec.ContractConverter;
import sh.stubborn.contract.spec.internal.DynamicStringImpl;

/**
 * Converts a String or a Groovy or Java file into a {@link Contract}.
 *
 * @author Marcin Grzejszczak
 * @author Olga Maciaszek-Sharma
 * @author Stessy Delcroix
 * @since 1.0.0
 */
public class ContractVerifierDslConverter implements ContractConverter<Collection<Contract>> {

	private static final Logger LOG = LoggerFactory.getLogger(ContractVerifierDslConverter.class);

	/**
	 * {@link ContractVerifierDslConverter} instance.
	 */
	public static final ContractVerifierDslConverter INSTANCE = new ContractVerifierDslConverter();

	private static final Pattern PACKAGE_PATTERN = Pattern.compile(".*package (.+?);.+?", Pattern.DOTALL);

	private static final Pattern CLASS_PATTERN = Pattern.compile(".+?class (.+?)( |\\{).+?", Pattern.DOTALL);

	private static final JavaCompiler COMPILER = ToolProvider.getSystemJavaCompiler();

	public static Collection<Contract> convertAsCollection(File rootFolder, String dsl) {
		ClassLoader classLoader = ContractVerifierDslConverter.class.getClassLoader();
		try {
			ClassLoader urlCl = updatedClassLoader(rootFolder, classLoader);
			Object object = groovyShell(urlCl, rootFolder).evaluate(dsl);
			return listOfContracts(object);
		}
		catch (DslParseException ex) {
			throw ex;
		}
		catch (Exception ex) {
			LOG.error("Exception occurred while trying to evaluate the contract", ex);
			throw new DslParseException(ex);
		}
		finally {
			Thread.currentThread().setContextClassLoader(classLoader);
		}
	}

	public static Collection<Contract> convertAsCollection(File dsl) {
		return convertAsCollection(dsl.getParentFile(), dsl);
	}

	public static Collection<Contract> convertAsCollection(File rootFolder, File dsl) {
		ClassLoader classLoader = ContractVerifierDslConverter.class.getClassLoader();
		try {
			ClassLoader urlCl = updatedClassLoader(rootFolder, classLoader);
			Object object = toObject(urlCl, rootFolder, dsl);
			return listOfContracts(dsl, object);
		}
		catch (DslParseException ex) {
			throw ex;
		}
		catch (Exception ex) {
			LOG.error("Exception occurred while trying to evaluate the contract at path [" + dsl.getPath() + "]", ex);
			throw new DslParseException(ex);
		}
		finally {
			Thread.currentThread().setContextClassLoader(classLoader);
		}
	}

	private static ClassLoader updatedClassLoader(File rootFolder, ClassLoader classLoader) {
		ClassLoader urlCl;
		try {
			urlCl = URLClassLoader
				.newInstance(Collections.singletonList(rootFolder.toURI().toURL()).toArray(new URL[0]), classLoader);
		}
		catch (MalformedURLException ex) {
			LOG.error("Exception occurred while trying to construct the URL from the root folder at path ["
					+ rootFolder.getPath() + "]", ex);
			throw new DslParseException(ex);
		}
		updateTheThreadClassLoader(urlCl);
		return urlCl;
	}

	private static void updateTheThreadClassLoader(ClassLoader urlCl) {
		Thread.currentThread().setContextClassLoader(urlCl);
	}

	private static @Nullable Object toObject(ClassLoader cl, File rootFolder, File dsl) throws IOException {
		if (isJava(dsl)) {
			try {
				return parseJavaFile(rootFolder, dsl);
			}
			catch (Exception ex) {
				if (LOG.isWarnEnabled()) {
					LOG.warn("Exception occurred while trying to parse the file [" + dsl
							+ "] as a contract. Will not parse it.", ex);
				}
				return null;
			}
		}
		return groovyShell(cl, rootFolder).evaluate(dsl);
	}

	private static @Nullable Object parseJavaFile(File rootFolder, File dsl) throws IllegalAccessException,
			InvocationTargetException, InstantiationException, IOException, NoSuchMethodException {
		Constructor<?> constructor = classConstructor(rootFolder, dsl);
		Object newInstance = constructor.newInstance();
		if (!(newInstance instanceof Supplier)) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("The class [" + dsl + "] is not instance of Supplier. Will not parse it as a contract");
			}
			return null;
		}
		Supplier<?> supplier = (Supplier<?>) newInstance;
		return supplier.get();
	}

	private static Constructor<?> classConstructor(File rootFolder, File dsl)
			throws IllegalAccessException, IOException, NoSuchMethodException {
		try (StandardJavaFileManager fileManager = COMPILER.getStandardFileManager(null, null, null)) {
			try (Stream<String> lines = Files.lines(Paths.get(dsl.getAbsolutePath()))) {
				String classText = lines.collect(Collectors.joining("\n"));
				String fqn = fqn(classText);
				Path directory = Files.createTempDirectory(fqn);
				fileManager.setLocation(StandardLocation.CLASS_OUTPUT, List.of(directory.toFile()));
				// set compiler's classpath to be same as the runtime's
				Set<File> classpathLocations = new HashSet<>();
				classpathLocations.add(rootFolder);
				appendUrlsFromAllClassLoaders(classpathLocations);
				String classPath = System.getProperty("java.class.path", "");
				if (classPath != null && !classPath.isBlank()) {
					classpathLocations.addAll(Arrays.stream(classPath.split(":")).map(File::new).toList());
				}
				fileManager.setLocation(StandardLocation.CLASS_PATH, classpathLocations);
				// Compile the file
				DiagnosticCollector<Object> diagnostics = new DiagnosticCollector<>();
				JavaCompiler.CompilationTask task = COMPILER.getTask(null, fileManager, diagnostics, null, null,
						fileManager.getJavaFileObjectsFromFiles(List.of(dsl)));
				boolean success = task.call();
				if (!success) {
					throw new IllegalStateException(
							"Exceptions occurred while trying to compile the file \n" + diagnostics.getDiagnostics()
								.stream()
								.map((d) -> "Error " + d.getMessage(Locale.getDefault()) + " on line "
										+ d.getLineNumber() + " in " + d.getSource())
								.collect(Collectors.joining("\n")));
				}
				try {
					// Add the folder with compiled classes to the class loader
					URLClassLoader urlClassLoader = new URLClassLoader("contract-classloader",
							new URL[] { new URL("file://" + directory.toAbsolutePath() + "/") },
							Thread.currentThread().getContextClassLoader());
					Class<?> clazz = Class.forName(fqn, true, urlClassLoader);
					Constructor<?> constructor = clazz.getDeclaredConstructor();
					constructor.setAccessible(true);
					return constructor;
				}
				catch (ClassNotFoundException ex) {
					throw new IllegalStateException("Class with name [" + fqn + "] not found");
				}
			}
		}
	}

	private static void appendUrlsFromAllClassLoaders(Set<File> files) {
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		if (classLoader != null) {
			appendUrlsFromClasspath(files, classLoader);
			while (classLoader.getParent() != null) {
				classLoader = classLoader.getParent();
				appendUrlsFromClasspath(files, classLoader);
			}
		}
	}

	private static void appendUrlsFromClasspath(Set<File> files, ClassLoader classLoader) {
		if (classLoader instanceof URLClassLoader urlClassLoader) {
			URL[] urLs = urlClassLoader.getURLs();
			if (urLs.length > 0) {
				Arrays.stream(urLs).forEach((url) -> files.add(new File(url.getFile())));
			}
		}
	}

	private static boolean isJava(File dsl) {
		return dsl.getName().endsWith(".java");
	}

	private static String fqn(String classText) throws IllegalAccessException {
		Matcher packageMatcher = PACKAGE_PATTERN.matcher(classText);
		String fqn = "";
		if (packageMatcher.matches()) {
			fqn = packageMatcher.group(1) + ".";
		}
		Matcher classMatcher = CLASS_PATTERN.matcher(classText);
		if (!classMatcher.matches()) {
			throw new IllegalAccessException("Can't parse the class name");
		}
		return fqn + classMatcher.group(1);
	}

	private static GroovyShell groovyShell(ClassLoader cl, File rootFolder) {
		CompilerConfiguration compilerConfiguration = new CompilerConfiguration();
		compilerConfiguration.setSourceEncoding("UTF-8");
		compilerConfiguration.setClasspathList(Collections.singletonList(rootFolder.getAbsolutePath()));
		return new GroovyShell(cl, compilerConfiguration);
	}

	private static Collection<Contract> listOfContracts(Object object) {
		Collection<Contract> contracts;
		if (object instanceof Collection) {
			contracts = (Collection<Contract>) object;
		}
		else if (!(object instanceof Contract)) {
			throw new DslParseException("Contract is not returning a Contract or list of Contracts");
		}
		else {
			contracts = Collections.singletonList((Contract) object);
		}
		contracts.forEach(ContractVerifierDslConverter::normalizeContract);
		return contracts;
	}

	private static Collection<Contract> listOfContracts(File file, @Nullable Object object) {
		Collection<Contract> contracts;
		if (object == null) {
			return Collections.emptyList();
		}
		else if (isACollectionOfContracts(object)) {
			contracts = withName(file, (Collection<Contract>) object);
		}
		else if (!(object instanceof Contract)) {
			throw new DslParseException("Contract is not returning a Contract or list of Contracts");
		}
		else {
			contracts = withName(file, Collections.singletonList((Contract) object));
		}
		contracts.forEach(ContractVerifierDslConverter::normalizeContract);
		return contracts;
	}

	/**
	 * Normalises a parsed {@link Contract} at the Groovy parse boundary: every
	 * {@code groovy.lang.GString} found anywhere in the contract's object graph is
	 * structurally replaced with a pure-Java
	 * {@link sh.stubborn.contract.spec.internal.DynamicStringImpl}, preserving the
	 * interpolated values as-is (no client/server resolution, no stringification). This
	 * lets the framework-agnostic core process Groovy-authored bodies without ever seeing
	 * a {@code GString}.
	 * @param contract the parsed contract to normalise in place
	 */
	private static void normalizeContract(@Nullable Contract contract) {
		if (contract == null) {
			return;
		}
		normalize(contract, new IdentityHashMap<>());
	}

	/**
	 * Recursively normalises a value from the contract graph, replacing every
	 * {@code GString} with a {@link DynamicStringImpl} while keeping the rest of the
	 * structure (and the interpolated values) intact. Containers (maps, lists, arrays)
	 * and model objects (package {@code sh.stubborn.contract.spec}) are walked in place;
	 * everything else is returned unchanged.
	 * @param value the value to normalise (may be {@code null})
	 * @param visited identity set guarding against cycles
	 * @return the normalised value (a new object only when a {@code GString} is replaced)
	 */
	@SuppressWarnings("NullAway")
	private static @Nullable Object normalize(@Nullable Object value, Map<Object, Object> visited) {
		if (value == null) {
			return null;
		}
		if (value instanceof GString) {
			GString gstring = (GString) value;
			return new DynamicStringImpl(normalizeArray(gstring.getValues(), visited), gstring.getStrings());
		}
		if (value instanceof CharSequence) {
			// String, DynamicStringImpl, StringBuilder, ... - already leaf values
			return value;
		}
		if (value instanceof Map) {
			if (visited.put(value, value) == null) {
				for (Map.Entry<Object, Object> entry : ((Map<Object, Object>) value).entrySet()) {
					Object original = entry.getValue();
					Object normalized = normalize(original, visited);
					if (normalized != original) {
						entry.setValue(normalized);
					}
				}
			}
			return value;
		}
		if (value instanceof List) {
			if (visited.put(value, value) == null) {
				List<Object> list = (List<Object>) value;
				for (int i = 0; i < list.size(); i++) {
					Object original = list.get(i);
					Object normalized = normalize(original, visited);
					if (normalized != original) {
						list.set(i, normalized);
					}
				}
			}
			return value;
		}
		if (value instanceof Collection) {
			// Any other collection (e.g. the LinkedHashSet backing Headers / Cookies /
			// QueryParameters) - cannot be indexed, so normalise each element and, only
			// if
			// an element reference actually changed (a GString was replaced), rebuild it
			// in place preserving iteration order.
			if (visited.put(value, value) == null) {
				Collection<Object> collection = (Collection<Object>) value;
				List<Object> normalized = new ArrayList<>(collection.size());
				boolean changed = false;
				for (Object original : collection) {
					Object element = normalize(original, visited);
					normalized.add(element);
					changed = changed || element != original;
				}
				if (changed) {
					collection.clear();
					collection.addAll(normalized);
				}
			}
			return value;
		}
		if (value.getClass().isArray()) {
			if (!value.getClass().getComponentType().isPrimitive() && visited.put(value, value) == null) {
				Object[] array = (Object[]) value;
				for (int i = 0; i < array.length; i++) {
					array[i] = normalize(array[i], visited);
				}
			}
			return value;
		}
		if (isModelObject(value)) {
			if (visited.put(value, value) == null) {
				walkModelObject(value, visited);
			}
			return value;
		}
		return value;
	}

	private static @Nullable Object[] normalizeArray(@Nullable Object[] values, Map<Object, Object> visited) {
		if (values == null) {
			return new Object[0];
		}
		@Nullable Object[] normalized = new @Nullable Object[values.length];
		for (int i = 0; i < values.length; i++) {
			normalized[i] = normalize(values[i], visited);
		}
		return normalized;
	}

	private static boolean isModelObject(Object value) {
		return value.getClass().getName().startsWith("sh.stubborn.contract.spec.");
	}

	private static void walkModelObject(Object value, Map<Object, Object> visited) {
		Class<?> clazz = value.getClass();
		while (clazz != null && clazz.getName().startsWith("sh.stubborn.contract.spec.")) {
			for (Field field : clazz.getDeclaredFields()) {
				if (Modifier.isStatic(field.getModifiers()) || field.getType().isPrimitive()) {
					continue;
				}
				try {
					field.setAccessible(true);
					Object fieldValue = field.get(value);
					if (fieldValue == null) {
						continue;
					}
					Object normalized = normalize(fieldValue, visited);
					if (normalized != fieldValue) {
						field.set(value, normalized);
					}
				}
				catch (Exception ex) {
					// Best effort - if a particular field cannot be read/written
					// reflectively we simply leave it untouched.
					if (LOG.isTraceEnabled()) {
						LOG.trace("Could not normalise field [" + field.getName() + "] of ["
								+ value.getClass().getName() + "]", ex);
					}
				}
			}
			clazz = clazz.getSuperclass();
		}
	}

	private static boolean isACollectionOfContracts(Object object) {
		return object instanceof Collection && ((Collection) object).stream().allMatch((it) -> it instanceof Contract);
	}

	private static Collection<Contract> withName(File file, Collection<Contract> contracts) {
		AtomicInteger counter = new AtomicInteger(0);
		return contracts.stream().peek((it) -> {
			if (contractNameEmpty(it)) {
				it.name(NamesUtil.defaultContractName(file, contracts, counter.get()));
			}
			counter.getAndIncrement();
		}).collect(Collectors.toList());
	}

	private static boolean contractNameEmpty(Contract it) {
		return it != null && (it.getName() == null || it.getName().isEmpty());
	}

	@Override
	public boolean isAccepted(File file) {
		return file.getName().endsWith(".groovy") || file.getName().endsWith(".gvy")
				|| file.getName().endsWith(".java");
	}

	@Override
	public Collection<Contract> convertFrom(File file) {
		return convertAsCollection(file);
	}

	@Override
	public Collection<Contract> convertTo(Collection<Contract> contract) {
		return contract;
	}

}
