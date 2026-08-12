/*
 * Copyright 2012-present the original author or authors.
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

package sh.stubborn.contract.stubrunner.spring;

import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.boot.SpringApplication;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

import static org.assertj.core.api.BDDAssertions.then;

/**
 * Locks the backward-compatibility bridge from the deprecated
 * {@code spring.cloud.contract.stubrunner.*} prefix to the canonical
 * {@code stubborn.contract.stubrunner.*} one. This is the runtime counterpart to the
 * {@code MigrateStubRunnerProperties} OpenRewrite recipe: the legacy prefix must keep
 * resolving so existing Spring Cloud Contract setups run unchanged.
 *
 * @author Marcin Grzejszczak
 */
class StubRunnerPropertiesMigratorTests {

	private final StubRunnerPropertiesMigrator migrator = new StubRunnerPropertiesMigrator();

	@Test
	void should_bridge_the_legacy_prefix_to_the_canonical_one() {
		StandardEnvironment environment = environmentWith(
				Map.of("spring.cloud.contract.stubrunner.ids", "com.example:service:+:stubs"));

		this.migrator.postProcessEnvironment(environment, new SpringApplication());

		then(environment.getProperty("stubborn.contract.stubrunner.ids")).isEqualTo("com.example:service:+:stubs");
	}

	@Test
	void should_not_override_an_explicit_canonical_property() {
		StandardEnvironment environment = environmentWith(Map.of("spring.cloud.contract.stubrunner.ids", "legacy",
				"stubborn.contract.stubrunner.ids", "canonical"));

		this.migrator.postProcessEnvironment(environment, new SpringApplication());

		then(environment.getProperty("stubborn.contract.stubrunner.ids")).isEqualTo("canonical");
	}

	@Test
	void should_do_nothing_when_no_legacy_property_is_present() {
		StandardEnvironment environment = environmentWith(Map.of("some.other.property", "value"));

		this.migrator.postProcessEnvironment(environment, new SpringApplication());

		then(environment.containsProperty("stubborn.contract.stubrunner.ids")).isFalse();
	}

	private static StandardEnvironment environmentWith(Map<String, Object> properties) {
		StandardEnvironment environment = new StandardEnvironment();
		environment.getPropertySources().addFirst(new MapPropertySource("test", properties));
		return environment;
	}

}
