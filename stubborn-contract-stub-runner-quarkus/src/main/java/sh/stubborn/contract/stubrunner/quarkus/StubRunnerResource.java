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

package sh.stubborn.contract.stubrunner.quarkus;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sh.stubborn.contract.stubrunner.BatchStubRunner;
import sh.stubborn.contract.stubrunner.BatchStubRunnerFactory;
import sh.stubborn.contract.stubrunner.RunningStubs;
import sh.stubborn.contract.stubrunner.StubConfiguration;
import sh.stubborn.contract.stubrunner.StubDownloader;
import sh.stubborn.contract.stubrunner.StubDownloaderBuilderProvider;
import sh.stubborn.contract.stubrunner.StubFinder;
import sh.stubborn.contract.stubrunner.StubRunnerOptions;
import sh.stubborn.contract.stubrunner.StubRunnerOptionsBuilder;
import sh.stubborn.contract.verifier.messaging.MessageVerifierSender;

/**
 * A Quarkus {@link QuarkusTestResourceLifecycleManager} that boots the Stubborn Contract
 * stub runner for the lifetime of a {@code @QuarkusTest}. It downloads the requested
 * consumer stubs, starts an in-process WireMock server for each of them and exposes their
 * addresses to the Quarkus test configuration.
 *
 * <p>
 * It is deliberately Spring-free: it reuses the framework-agnostic
 * {@link BatchStubRunner} core exactly like the Spring Boot integration does, only wired
 * through Quarkus' test-resource lifecycle instead of a Spring
 * {@code ApplicationContext}.
 *
 * <p>
 * Register it on a test class by annotating it with {@code QuarkusTestResource} pointing
 * at this class, passing the stub coordinates and mode as {@code ResourceArg} init args.
 * See {@code docs/integrations/quarkus.md} for a full, copy-pasteable example.
 *
 * <p>
 * Supported {@code initArgs}:
 * <ul>
 * <li>{@code ids} (required) — comma-separated stub coordinates in
 * {@code groupId:artifactId:version:classifier:port} Ivy notation (port optional).</li>
 * <li>{@code stubsMode} — {@code CLASSPATH} (default), {@code LOCAL} or
 * {@code REMOTE}.</li>
 * <li>{@code repositoryRoot} — stub repository root for {@code LOCAL}/{@code REMOTE}
 * modes (a Maven repo URL or a file/classpath location).</li>
 * <li>{@code stubsClassifier} — stubs classifier, defaults to {@code stubs}.</li>
 * <li>{@code minPort} / {@code maxPort} — port range for the stub servers.</li>
 * </ul>
 *
 * <p>
 * On {@link #start()} the manager publishes, for every running stub, the following config
 * properties (both an artifactId-only and a {@code groupId.artifactId} variant):
 * <ul>
 * <li>{@code stubborn.contract.stubrunner.runningstubs.<artifactId>.port} — the bound
 * port.</li>
 * <li>{@code stubborn.contract.stubrunner.runningstubs.<artifactId>.url} — the base URL
 * ({@code http://localhost:<port>}).</li>
 * </ul>
 * These can be injected into a test with Quarkus'
 * {@code @ConfigProperty(name = "stubborn.contract.stubrunner.runningstubs.producer.url")}.
 *
 * @author Stubborn Contract
 * @since 1.0.0
 * @see BatchStubRunner
 */
public class StubRunnerResource implements QuarkusTestResourceLifecycleManager {

	/**
	 * Prefix under which the running stub coordinates are published to the Quarkus
	 * config.
	 */
	public static final String STUBRUNNER_PREFIX = "stubborn.contract.stubrunner.runningstubs";

	/**
	 * Init arg holding the stub coordinates (Ivy notation, comma separated).
	 */
	public static final String IDS = "ids";

	/**
	 * Init arg holding the stubs mode ({@code CLASSPATH}, {@code LOCAL}, {@code REMOTE}).
	 */
	public static final String STUBS_MODE = "stubsMode";

	/**
	 * Init arg holding the stub repository root.
	 */
	public static final String REPOSITORY_ROOT = "repositoryRoot";

	/**
	 * Init arg holding the stubs classifier.
	 */
	public static final String STUBS_CLASSIFIER = "stubsClassifier";

	/**
	 * Init arg holding the minimum port of the stub server range.
	 */
	public static final String MIN_PORT = "minPort";

	/**
	 * Init arg holding the maximum port of the stub server range.
	 */
	public static final String MAX_PORT = "maxPort";

	private static final Logger log = LoggerFactory.getLogger(StubRunnerResource.class);

	private Map<String, String> initArgs = new LinkedHashMap<>();

	private @Nullable BatchStubRunner batchStubRunner;

	@Override
	public void init(Map<String, String> initArgs) {
		this.initArgs = new LinkedHashMap<>(initArgs);
	}

	@Override
	public Map<String, String> start() {
		StubRunnerOptions options = buildOptions();
		StubDownloaderBuilderProvider provider = new StubDownloaderBuilderProvider();
		StubDownloader stubDownloader = provider.get(options);
		MessageVerifierSender<?> sender = messageVerifierSender();
		// Without a sender the factory defaults to the Spring-free NoOpStubMessages, so
		// consumer-side HTTP stubbing needs no messaging backend. A subclass can supply a
		// real sender (see the messaging Quarkus module) so a triggered stub message
		// reaches a real broker.
		BatchStubRunnerFactory factory = (sender != null) ? new BatchStubRunnerFactory(options, stubDownloader, sender)
				: new BatchStubRunnerFactory(options, stubDownloader);
		this.batchStubRunner = factory.buildBatchStubRunner();
		RunningStubs runningStubs = this.batchStubRunner.runStubs();
		Map<String, String> config = toConfig(runningStubs);
		log.info("Started Stubborn Contract stub runner for Quarkus. Published config: {}", config);
		return config;
	}

	@Override
	public void stop() {
		if (this.batchStubRunner != null) {
			try {
				this.batchStubRunner.close();
			}
			catch (IOException ex) {
				log.warn("Failed to close the stub runner", ex);
			}
			finally {
				this.batchStubRunner = null;
			}
		}
	}

	/**
	 * Injects the running {@link StubFinder} into any assignable test field (for example
	 * a field declared as {@link StubFinder} or {@link BatchStubRunner}), so a test can
	 * look up stub URLs programmatically.
	 * @param testInjector the Quarkus test injector
	 */
	@Override
	public void inject(TestInjector testInjector) {
		BatchStubRunner runner = this.batchStubRunner;
		if (runner == null) {
			return;
		}
		Class<?> valueType = runner.getClass();
		testInjector.injectIntoFields(runner,
				(field) -> field.getType() != Object.class && field.getType().isAssignableFrom(valueType));
	}

	/**
	 * Supplies the messaging {@link MessageVerifierSender} used to send contract-defined
	 * messages when a stub is triggered. The default is {@code null}, which keeps the
	 * consumer-side HTTP behaviour (a Spring-free no-op messaging backend). Messaging
	 * integrations (for example the Quarkus messaging module) override this to return a
	 * real Kafka or RabbitMQ sender built from the {@link #initArg(String) init args}.
	 * @return the sender to use, or {@code null} for no-op messaging
	 */
	protected @Nullable MessageVerifierSender<?> messageVerifierSender() {
		return null;
	}

	/**
	 * Returns the value of the named init arg, or {@code null} if it was not supplied.
	 * This lets a subclass read the same configuration passed to the resource.
	 * @param key the init arg name
	 * @return the value, or {@code null} if absent
	 */
	protected @Nullable String initArg(String key) {
		return this.initArgs.get(key);
	}

	private StubRunnerOptions buildOptions() {
		String ids = this.initArgs.get(IDS);
		if (ids == null || ids.isBlank()) {
			throw new IllegalArgumentException(
					"The '" + IDS + "' init arg is required (e.g. 'com.example:producer:+:stubs').");
		}
		StubRunnerOptionsBuilder builder = new StubRunnerOptionsBuilder()
			.withStubsMode(this.initArgs.getOrDefault(STUBS_MODE, "CLASSPATH"))
			.withStubs(ids);
		String classifier = this.initArgs.get(STUBS_CLASSIFIER);
		if (classifier != null && !classifier.isBlank()) {
			builder.withStubsClassifier(classifier);
		}
		String repositoryRoot = this.initArgs.get(REPOSITORY_ROOT);
		if (repositoryRoot != null && !repositoryRoot.isBlank()) {
			builder.withStubRepositoryRoot(repositoryRoot);
		}
		String minPort = this.initArgs.get(MIN_PORT);
		String maxPort = this.initArgs.get(MAX_PORT);
		if (minPort != null && !minPort.isBlank()) {
			builder.withMinPort(Integer.parseInt(minPort.trim()));
		}
		if (maxPort != null && !maxPort.isBlank()) {
			builder.withMaxPort(Integer.parseInt(maxPort.trim()));
		}
		return builder.build();
	}

	private Map<String, String> toConfig(RunningStubs runningStubs) {
		Map<String, String> config = new LinkedHashMap<>();
		for (Map.Entry<StubConfiguration, Integer> entry : runningStubs.validNamesAndPorts().entrySet()) {
			StubConfiguration stub = entry.getKey();
			int port = entry.getValue();
			String url = "http://localhost:" + port;
			String artifactId = stub.getArtifactId();
			String groupAndArtifact = stub.getGroupId() + "." + stub.getArtifactId();
			config.put(STUBRUNNER_PREFIX + "." + artifactId + ".port", String.valueOf(port));
			config.put(STUBRUNNER_PREFIX + "." + artifactId + ".url", url);
			config.put(STUBRUNNER_PREFIX + "." + groupAndArtifact + ".port", String.valueOf(port));
			config.put(STUBRUNNER_PREFIX + "." + groupAndArtifact + ".url", url);
		}
		return config;
	}

}
