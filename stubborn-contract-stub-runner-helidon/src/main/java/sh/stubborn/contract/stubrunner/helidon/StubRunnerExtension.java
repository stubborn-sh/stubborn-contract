/*
 * Copyright 2026-present the original author or authors.
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

package sh.stubborn.contract.stubrunner.helidon;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.platform.commons.support.AnnotationSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sh.stubborn.contract.stubrunner.BatchStubRunner;
import sh.stubborn.contract.stubrunner.BatchStubRunnerFactory;
import sh.stubborn.contract.stubrunner.RunningStubs;
import sh.stubborn.contract.stubrunner.StubConfiguration;
import sh.stubborn.contract.stubrunner.StubFinder;
import sh.stubborn.contract.stubrunner.StubRunnerOptions;
import sh.stubborn.contract.stubrunner.StubRunnerOptionsBuilder;
import sh.stubborn.contract.stubrunner.StubRunning;

/**
 * JUnit 5 extension that boots a {@link BatchStubRunner} for a Helidon MicroProfile test
 * and publishes every running stub URL as MicroProfile Config.
 *
 * <p>
 * Helidon MP Config reads {@link System#getProperties() System properties} by default, so
 * for each running stub the extension sets:
 * <ul>
 * <li>{@code stubborn.contract.stubrunner.runningstubs.<artifactId>.port}</li>
 * <li>{@code stubborn.contract.stubrunner.runningstubs.<groupId>.<artifactId>.port}</li>
 * </ul>
 * These become resolvable through {@code @ConfigProperty} inside the application under
 * test. The properties are set from {@code beforeAll}, which is why {@link StubRunner}
 * must be ordered ahead of {@code @HelidonTest} (Helidon builds its {@code Config}
 * snapshot when its own extension starts the CDI container).
 *
 * <p>
 * The extension also resolves {@link StubFinder} / {@link StubRunning} test parameters so
 * tests can query the running stub URLs directly.
 *
 * @author Marcin Grzejszczak
 */
public class StubRunnerExtension implements BeforeAllCallback, AfterAllCallback, ParameterResolver {

	/**
	 * Prefix under which running stub ports are published, kept identical to the Spring
	 * Boot integration so contracts and property lookups are portable across runtimes.
	 */
	public static final String STUBRUNNER_PREFIX = "stubborn.contract.stubrunner.runningstubs";

	private static final Logger log = LoggerFactory.getLogger(StubRunnerExtension.class);

	private static final Namespace NAMESPACE = Namespace.create(StubRunnerExtension.class);

	@Override
	public void beforeAll(ExtensionContext context) {
		Class<?> testClass = context.getRequiredTestClass();
		StubRunner annotation = AnnotationSupport.findAnnotation(testClass, StubRunner.class)
			.orElseThrow(() -> new IllegalStateException(
					"@StubRunner annotation not found on test class [" + testClass.getName() + "]"));
		StubRunnerOptions options = buildOptions(annotation);
		BatchStubRunner runner = new BatchStubRunnerFactory(options).buildBatchStubRunner();
		RunningStubs running = runner.runStubs();
		List<String> publishedKeys = publishAsSystemProperties(running);
		context.getStore(NAMESPACE).put(testClass, new State(runner, publishedKeys));
		log.info("Started stub runner for {} stub(s); published keys {}", running.validNamesAndPorts().size(),
				publishedKeys);
	}

	@Override
	public void afterAll(ExtensionContext context) throws IOException {
		State state = context.getStore(NAMESPACE).remove(context.getRequiredTestClass(), State.class);
		if (state != null) {
			state.close();
		}
	}

	@Override
	public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
			throws ParameterResolutionException {
		Class<?> type = parameterContext.getParameter().getType();
		return type == StubFinder.class || type == StubRunning.class || type == BatchStubRunner.class;
	}

	@Override
	public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
			throws ParameterResolutionException {
		Class<?> testClass = extensionContext.getRequiredTestClass();
		State state = extensionContext.getStore(NAMESPACE).get(testClass, State.class);
		if (state == null) {
			throw new ParameterResolutionException(
					"Stub runner has not been started for [" + testClass.getName() + "]");
		}
		return state.runner();
	}

	private StubRunnerOptions buildOptions(StubRunner annotation) {
		StubRunnerOptionsBuilder builder = new StubRunnerOptionsBuilder().withStubsMode(annotation.stubsMode())
			.withMinMaxPort(annotation.minPort(), annotation.maxPort())
			.withStubs(annotation.ids());
		if (!annotation.repositoryRoot().isBlank()) {
			builder.withStubRepositoryRoot(annotation.repositoryRoot());
		}
		return builder.build();
	}

	private List<String> publishAsSystemProperties(RunningStubs running) {
		List<String> keys = new ArrayList<>();
		for (Map.Entry<StubConfiguration, Integer> entry : running.validNamesAndPorts().entrySet()) {
			StubConfiguration config = entry.getKey();
			String port = String.valueOf(entry.getValue());
			keys.add(setProperty(STUBRUNNER_PREFIX + "." + config.getArtifactId() + ".port", port));
			keys.add(setProperty(STUBRUNNER_PREFIX + "." + config.getGroupId() + "." + config.getArtifactId() + ".port",
					port));
		}
		return keys;
	}

	private String setProperty(String key, String value) {
		System.setProperty(key, value);
		return key;
	}

	/**
	 * Per-test-class state: the running batch stub runner and the System property keys it
	 * published, so both can be cleaned up in {@code afterAll}.
	 */
	private record State(BatchStubRunner runner, List<String> publishedKeys) {

		void close() throws IOException {
			try {
				this.runner.close();
			}
			finally {
				this.publishedKeys.forEach(System::clearProperty);
			}
		}

	}

}
