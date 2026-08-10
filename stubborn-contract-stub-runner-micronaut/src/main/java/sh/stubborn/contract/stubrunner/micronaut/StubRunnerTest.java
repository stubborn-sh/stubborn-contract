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

import java.io.IOException;
import java.util.Map;

import io.micronaut.test.support.TestPropertyProvider;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.TestInstance;
import sh.stubborn.contract.stubrunner.StubFinder;
import sh.stubborn.contract.stubrunner.StubRunnerOptions;
import sh.stubborn.contract.stubrunner.StubRunnerOptionsBuilder;
import sh.stubborn.contract.stubrunner.StubsMode;

/**
 * Base class for Micronaut tests that need Stubborn Contract stubs running before the
 * application context is created.
 *
 * <p>
 * It implements {@link TestPropertyProvider}, the blessed Micronaut Test seam that
 * supplies properties <em>before</em> the {@code ApplicationContext} starts. The stubs
 * are downloaded and started inside {@link #getProperties()} and their coordinates are
 * exposed as configuration properties (see {@link StubRunnerSupport#toProperties()}), so
 * that {@code @Client} interfaces and {@code @Value} injected URLs already point at the
 * running stubs when the context comes up.
 *
 * <p>
 * A subclass supplies at minimum the stub coordinates by overriding {@link #stubIds()}
 * and may tune the resolution mode, repository root and port range. Because
 * {@link TestPropertyProvider} requires a per-class test instance, this base class is
 * annotated with {@link TestInstance.Lifecycle#PER_CLASS}; subclasses inherit it.
 *
 * <p>
 * Typical usage: a {@code @MicronautTest} subclass overrides {@link #stubIds()} to return
 * {@code "com.example:beer-api-producer"} and injects a {@code @Client} that resolves its
 * base URL from the {@code stubborn.contract.stubrunner.runningstubs.*.url} property. See
 * {@code docs/integrations/micronaut.md} for a full worked example.
 *
 * @author Marcin Grzejszczak
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class StubRunnerTest implements TestPropertyProvider {

	private @Nullable StubRunnerSupport support;

	@Override
	public Map<String, String> getProperties() {
		return stubRunnerSupport().toProperties();
	}

	/**
	 * Returns the (lazily started) helper backing this test.
	 * @return the stub runner support
	 */
	protected synchronized StubRunnerSupport stubRunnerSupport() {
		StubRunnerSupport current = this.support;
		if (current == null) {
			current = new StubRunnerSupport(buildOptions()).start();
			this.support = current;
		}
		return current;
	}

	/**
	 * Returns the {@link StubFinder} for the running stubs, so a test can resolve stub
	 * URLs directly.
	 * @return the stub finder
	 */
	protected StubFinder stubFinder() {
		return stubRunnerSupport().stubFinder();
	}

	private StubRunnerOptions buildOptions() {
		StubRunnerOptionsBuilder builder = new StubRunnerOptionsBuilder().withStubsMode(stubsMode())
			.withMinMaxPort(minPort(), maxPort());
		String repositoryRoot = repositoryRoot();
		if (repositoryRoot != null) {
			builder.withStubRepositoryRoot(repositoryRoot);
		}
		builder.withStubs(stubIds());
		return builder.build();
	}

	/**
	 * The stub coordinates to run, in
	 * {@code groupId:artifactId[:version[:classifier[:port]]]} notation.
	 * @return the stub ids, never {@code null}
	 */
	protected abstract String[] stubIds();

	/**
	 * The stub resolution mode. Defaults to {@link StubsMode#CLASSPATH}.
	 * @return the stubs mode
	 */
	protected StubsMode stubsMode() {
		return StubsMode.CLASSPATH;
	}

	/**
	 * The stub repository root (for {@link StubsMode#LOCAL} or {@link StubsMode#REMOTE}).
	 * Defaults to {@code null} which is appropriate for {@link StubsMode#CLASSPATH}.
	 * @return the repository root or {@code null}
	 */
	protected @Nullable String repositoryRoot() {
		return null;
	}

	/**
	 * The lower bound of the port range used to bind stubs. Defaults to {@code 10000}.
	 * @return the minimum port
	 */
	protected int minPort() {
		return 10000;
	}

	/**
	 * The upper bound of the port range used to bind stubs. Defaults to {@code 15000}.
	 * @return the maximum port
	 */
	protected int maxPort() {
		return 15000;
	}

	@AfterAll
	void closeStubRunner() throws IOException {
		if (this.support != null) {
			this.support.close();
			this.support = null;
		}
	}

}
