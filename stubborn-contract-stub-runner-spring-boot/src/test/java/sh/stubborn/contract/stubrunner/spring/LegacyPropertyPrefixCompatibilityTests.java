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

package sh.stubborn.contract.stubrunner.spring;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import sh.stubborn.contract.stubrunner.StubsMode;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A migrating Spring Cloud Contract user keeps their
 * {@code spring.cloud.contract.stubrunner.*} configuration. This drives the whole
 * compatibility chain the stub runner actually consumes — legacy prefix →
 * {@link StubRunnerPropertiesMigrator} → bound {@link StubRunnerProperties} — proving
 * that a purely legacy configuration populates the properties bean that starts the stubs
 * (not merely that the raw keys are copied).
 */
class LegacyPropertyPrefixCompatibilityTests {

	@Test
	void legacyPrefixPopulatesTheBoundStubRunnerProperties() {
		Map<String, Object> legacy = new LinkedHashMap<>();
		legacy.put("spring.cloud.contract.stubrunner.ids",
				"sh.stubborn.contract.verifier.stubs:loanIssuance:0.0.1-SNAPSHOT:stubs");
		legacy.put("spring.cloud.contract.stubrunner.stubs-mode", "REMOTE");
		legacy.put("spring.cloud.contract.stubrunner.repository-root", "classpath:m2repo/repository/");
		legacy.put("spring.cloud.contract.stubrunner.consumer-name", "beer-api-consumer-a");
		legacy.put("spring.cloud.contract.stubrunner.stubs-per-consumer", "true");

		StubRunnerProperties properties = bindAfterMigration(legacy);

		assertThat(properties.getIds())
			.containsExactly("sh.stubborn.contract.verifier.stubs:loanIssuance:0.0.1-SNAPSHOT:stubs");
		assertThat(properties.getStubsMode()).isEqualTo(StubsMode.REMOTE);
		assertThat(properties.getConsumerName()).isEqualTo("beer-api-consumer-a");
		assertThat(properties.isStubsPerConsumer()).isTrue();
	}

	@Test
	void explicitCanonicalPropertyWinsOverTheLegacyOne() {
		Map<String, Object> mixed = new LinkedHashMap<>();
		mixed.put("spring.cloud.contract.stubrunner.ids", "com.example:legacy:+:stubs");
		mixed.put("stubborn.contract.stubrunner.ids", "com.example:canonical:+:stubs");

		StubRunnerProperties properties = bindAfterMigration(mixed);

		assertThat(properties.getIds()).containsExactly("com.example:canonical:+:stubs");
	}

	private static StubRunnerProperties bindAfterMigration(Map<String, Object> properties) {
		StandardEnvironment environment = new StandardEnvironment();
		environment.getPropertySources().addFirst(new MapPropertySource("test", properties));
		new StubRunnerPropertiesMigrator().postProcessEnvironment(environment, new SpringApplication());
		return Binder.get(environment).bind("stubborn.contract.stubrunner", StubRunnerProperties.class).get();
	}

}
