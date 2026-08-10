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

package sh.stubborn.contract.stubrunner.micronaut;

import java.io.Closeable;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.jspecify.annotations.Nullable;
import sh.stubborn.contract.stubrunner.BatchStubRunner;
import sh.stubborn.contract.stubrunner.BatchStubRunnerFactory;
import sh.stubborn.contract.stubrunner.RunningStubs;
import sh.stubborn.contract.stubrunner.StubConfiguration;
import sh.stubborn.contract.stubrunner.StubDownloaderBuilderProvider;
import sh.stubborn.contract.stubrunner.StubFinder;
import sh.stubborn.contract.stubrunner.StubRunnerOptions;

/**
 * Framework-agnostic helper that starts a {@link BatchStubRunner} for the supplied
 * {@link StubRunnerOptions}, exposes the resulting {@link StubFinder} and translates the
 * running stubs into Micronaut configuration properties.
 *
 * <p>
 * This class does not depend on Micronaut at all. The Micronaut specific wiring lives in
 * {@link StubRunnerTest}, which delegates its
 * {@link io.micronaut.test.support.TestPropertyProvider#getProperties()} implementation
 * to {@link #toProperties()}. It is deliberately reusable so that it can back other
 * integrations (for example a JUnit extension) without change.
 *
 * @author Marcin Grzejszczak
 */
public class StubRunnerSupport implements Closeable {

	/**
	 * Prefix under which running stub coordinates are published, matching the property
	 * namespace used by the Spring Boot integration.
	 */
	public static final String STUBRUNNER_PREFIX = "stubborn.contract.stubrunner.runningstubs";

	private final StubRunnerOptions options;

	private final Object lock = new Object();

	private @Nullable BatchStubRunner batchStubRunner;

	private @Nullable RunningStubs runningStubs;

	public StubRunnerSupport(StubRunnerOptions options) {
		this.options = options;
	}

	/**
	 * Downloads and starts the configured stubs. Repeated invocations are no-ops so the
	 * helper can be started eagerly or lazily.
	 * @return this helper, started
	 */
	public StubRunnerSupport start() {
		synchronized (this.lock) {
			if (this.batchStubRunner == null) {
				BatchStubRunner runner = new BatchStubRunnerFactory(this.options,
						new StubDownloaderBuilderProvider().get(this.options))
					.buildBatchStubRunner();
				this.runningStubs = runner.runStubs();
				this.batchStubRunner = runner;
			}
		}
		return this;
	}

	/**
	 * Returns the {@link StubFinder} for the running stubs, starting them if necessary.
	 * @return the stub finder
	 */
	public StubFinder stubFinder() {
		start();
		return requireStarted();
	}

	/**
	 * Returns the running stubs (names and ports), starting them if necessary.
	 * @return the running stubs
	 */
	public RunningStubs runningStubs() {
		start();
		RunningStubs stubs = this.runningStubs;
		if (stubs == null) {
			throw new IllegalStateException("Stub runner has not been started");
		}
		return stubs;
	}

	/**
	 * Translates the running stubs into Micronaut configuration properties. For each
	 * running stub both a {@code ...<artifactId>.port} / {@code ...<artifactId>.url} pair
	 * and a group-qualified {@code ...<groupId>.<artifactId>.port} / {@code .url} pair
	 * are emitted. The {@code .url} keys are directly consumable by {@code @Value} or
	 * {@code @Client} to point at the running stub.
	 * @return the properties to feed into the Micronaut application context
	 */
	public Map<String, String> toProperties() {
		start();
		Map<String, String> properties = new LinkedHashMap<>();
		for (Map.Entry<StubConfiguration, Integer> entry : runningStubs().validNamesAndPorts().entrySet()) {
			StubConfiguration configuration = entry.getKey();
			int port = entry.getValue();
			String baseUrl = "http://localhost:" + port;
			addProperties(properties, configuration.getArtifactId(), port, baseUrl);
			addProperties(properties, configuration.getGroupId() + "." + configuration.getArtifactId(), port, baseUrl);
		}
		return properties;
	}

	private void addProperties(Map<String, String> properties, String key, int port, String baseUrl) {
		properties.put(STUBRUNNER_PREFIX + "." + key + ".port", String.valueOf(port));
		properties.put(STUBRUNNER_PREFIX + "." + key + ".url", baseUrl);
	}

	private BatchStubRunner requireStarted() {
		BatchStubRunner runner = this.batchStubRunner;
		if (runner == null) {
			throw new IllegalStateException("Stub runner has not been started");
		}
		return runner;
	}

	@Override
	public void close() throws IOException {
		synchronized (this.lock) {
			if (this.batchStubRunner != null) {
				this.batchStubRunner.close();
				this.batchStubRunner = null;
				this.runningStubs = null;
			}
		}
	}

}
