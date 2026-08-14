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

package sh.stubborn.contract.spec.internal;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import groovy.lang.GroovyShell;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.jspecify.annotations.Nullable;
import sh.stubborn.contract.spec.Contract;
import sh.stubborn.contract.spec.ContractConverter;

/**
 * Converts a Groovy DSL contract ({@code .groovy} / {@code .gvy}) into a {@link Contract}
 * by evaluating it with a {@link GroovyShell}.
 *
 * <p>
 * This is the home of the Groovy contract parser: it lives in the Groovy spec module so
 * that the framework-agnostic verifier core carries no Groovy dependency. It is discovered
 * at runtime through the {@link ContractConverter} {@link java.util.ServiceLoader} SPI, so
 * placing {@code stubborn-contract-spec-groovy} on the classpath is all that is required to
 * read Groovy contracts — exactly as {@code KotlinContractConverter} does for {@code .kts}.
 *
 * @author Marcin Grzejszczak
 * @since 0.1.0
 */
public class GroovyContractConverter implements ContractConverter<Collection<Contract>> {

	/**
	 * A shared {@link GroovyContractConverter} instance.
	 */
	public static final GroovyContractConverter INSTANCE = new GroovyContractConverter();

	/**
	 * Evaluates a Groovy DSL contract passed as a String and returns the parsed contracts.
	 * @param rootFolder the folder used as the compilation classpath root
	 * @param dsl the Groovy DSL source
	 * @return the parsed contracts
	 */
	public static Collection<Contract> convertAsCollection(File rootFolder, String dsl) {
		ClassLoader classLoader = GroovyContractConverter.class.getClassLoader();
		try {
			ClassLoader urlCl = updatedClassLoader(rootFolder, classLoader);
			Object object = groovyShell(urlCl, rootFolder).evaluate(dsl);
			return listOfContracts(object);
		}
		catch (RuntimeException ex) {
			throw ex;
		}
		catch (Exception ex) {
			throw new IllegalStateException("Exception occurred while trying to evaluate the contract", ex);
		}
		finally {
			Thread.currentThread().setContextClassLoader(classLoader);
		}
	}

	/**
	 * Evaluates a Groovy DSL contract file (using its parent folder as the classpath root).
	 * @param dsl the Groovy DSL contract file
	 * @return the parsed contracts
	 */
	public static Collection<Contract> convertAsCollection(File dsl) {
		return convertAsCollection(dsl.getParentFile(), dsl);
	}

	/**
	 * Evaluates a Groovy DSL contract file and returns the parsed contracts.
	 * @param rootFolder the folder used as the compilation classpath root
	 * @param dsl the Groovy DSL contract file
	 * @return the parsed contracts
	 */
	public static Collection<Contract> convertAsCollection(File rootFolder, File dsl) {
		ClassLoader classLoader = GroovyContractConverter.class.getClassLoader();
		try {
			ClassLoader urlCl = updatedClassLoader(rootFolder, classLoader);
			Object object = groovyShell(urlCl, rootFolder).evaluate(dsl);
			return listOfContracts(dsl, object);
		}
		catch (RuntimeException ex) {
			throw ex;
		}
		catch (Exception ex) {
			throw new IllegalStateException(
					"Exception occurred while trying to evaluate the contract at path [" + dsl.getPath() + "]", ex);
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
			throw new IllegalStateException("Exception occurred while trying to construct the URL from the root folder at path ["
					+ rootFolder.getPath() + "]", ex);
		}
		Thread.currentThread().setContextClassLoader(urlCl);
		return urlCl;
	}

	private static GroovyShell groovyShell(ClassLoader cl, File rootFolder) {
		CompilerConfiguration compilerConfiguration = new CompilerConfiguration();
		compilerConfiguration.setSourceEncoding("UTF-8");
		compilerConfiguration.setClasspathList(Collections.singletonList(rootFolder.getAbsolutePath()));
		return new GroovyShell(cl, compilerConfiguration);
	}

	@SuppressWarnings("unchecked")
	private static Collection<Contract> listOfContracts(@Nullable Object object) {
		if (object instanceof Collection) {
			return (Collection<Contract>) object;
		}
		else if (!(object instanceof Contract)) {
			throw new IllegalStateException("Contract is not returning a Contract or list of Contracts");
		}
		return Collections.singletonList((Contract) object);
	}

	@SuppressWarnings("unchecked")
	private static Collection<Contract> listOfContracts(File file, @Nullable Object object) {
		if (object == null) {
			return Collections.emptyList();
		}
		else if (isACollectionOfContracts(object)) {
			return withName(file, (Collection<Contract>) object);
		}
		else if (!(object instanceof Contract)) {
			throw new IllegalStateException("Contract is not returning a Contract or list of Contracts");
		}
		return withName(file, Collections.singletonList((Contract) object));
	}

	private static boolean isACollectionOfContracts(Object object) {
		return object instanceof Collection && ((Collection<?>) object).stream().allMatch((it) -> it instanceof Contract);
	}

	private static Collection<Contract> withName(File file, Collection<Contract> contracts) {
		AtomicInteger counter = new AtomicInteger(0);
		return contracts.stream().peek((it) -> {
			if (contractNameEmpty(it)) {
				it.name(defaultContractName(file, contracts, counter.get()));
			}
			counter.getAndIncrement();
		}).collect(Collectors.toList());
	}

	private static boolean contractNameEmpty(@Nullable Contract contract) {
		return contract != null && (contract.getName() == null || contract.getName().isEmpty());
	}

	private static String defaultContractName(File file, Collection<?> contracts, int counter) {
		int lastIndexOfDot = file.getName().lastIndexOf(".");
		String tillExtension = file.getName().substring(0, lastIndexOfDot);
		return tillExtension + ((counter > 0 || contracts.size() > 1) ? "_" + counter : "");
	}

	@Override
	public boolean isAccepted(File file) {
		return file.getName().endsWith(".groovy") || file.getName().endsWith(".gvy");
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
