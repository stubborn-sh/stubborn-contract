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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;
import sh.stubborn.contract.stubrunner.StubsMode;

/**
 * Class-level annotation that starts Stubborn Contract stub runner for a Helidon
 * MicroProfile test and publishes the running stub URLs as MicroProfile Config values
 * (via System properties, which Helidon MP Config reads by default).
 *
 * <p>
 * It registers {@link StubRunnerExtension}. When combined with {@code @HelidonTest}, this
 * annotation MUST be declared first so that {@link StubRunnerExtension} runs its
 * {@code beforeAll} callback (and therefore sets the port System properties) before
 * Helidon builds its {@code Config} and CDI container. See
 * {@code docs/integrations/helidon.md} for the ordering rationale.
 *
 * <pre>{@code
 * &#64;StubRunner(ids = "com.example:fraud-service:+:stubs", stubsMode = StubsMode.CLASSPATH)
 * &#64;HelidonTest
 * class OrderResourceTest {
 *     // @ConfigProperty(name = "stubborn.contract.stubrunner.runningstubs.fraud-service.port")
 * }
 * }</pre>
 *
 * @author Marcin Grzejszczak
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
@ExtendWith(StubRunnerExtension.class)
public @interface StubRunner {

	/**
	 * Ivy notations of the stubs to run, for example
	 * {@code group:artifact:version:classifier:port}. The port is optional; when omitted
	 * a free port from the {@link #minPort()}/{@link #maxPort()} range is chosen.
	 * @return the stub ids to run
	 */
	String[] ids() default {};

	/**
	 * How the stubs should be resolved.
	 * @return the stubs mode (defaults to {@link StubsMode#CLASSPATH})
	 */
	StubsMode stubsMode() default StubsMode.CLASSPATH;

	/**
	 * Location of the stubs repository. Ignored for {@link StubsMode#CLASSPATH} unless a
	 * concrete classpath root is required.
	 * @return the repository root (empty means "not set")
	 */
	String repositoryRoot() default "";

	/**
	 * Minimum value (inclusive) of the port range used when a stub does not declare an
	 * explicit port.
	 * @return the minimum port
	 */
	int minPort() default 10000;

	/**
	 * Maximum value (inclusive) of the port range used when a stub does not declare an
	 * explicit port.
	 * @return the maximum port
	 */
	int maxPort() default 15000;

}
