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

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * Bridges deprecated {@code spring.cloud.contract.stubrunner.*} properties to their
 * replacement {@code stubborn.contract.stubrunner.*} equivalents.
 *
 * <p>
 * At application startup this processor iterates all resolved property sources, copies
 * any {@code spring.cloud.contract.stubrunner.*} key (that is not already overridden by a
 * {@code stubborn.contract.stubrunner.*} key) into a synthetic property source, and emits
 * a deprecation warning per key.
 *
 * <p>
 * The old prefix will be removed in the next major release.
 *
 * @author Marcin Grzejszczak
 * @since 0.1.0
 */
public class StubRunnerPropertiesMigrator implements EnvironmentPostProcessor {

	private static final Log log = LogFactory.getLog(StubRunnerPropertiesMigrator.class);

	private static final String OLD_PREFIX = "spring.cloud.contract.stubrunner.";

	private static final String NEW_PREFIX = "stubborn.contract.stubrunner.";

	@Override
	public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
		Map<String, Object> migrated = new HashMap<>();
		for (Map.Entry<String, Object> entry : environment.getSystemProperties().entrySet()) {
			// no-op: system properties already visible
		}
		for (org.springframework.core.env.PropertySource<?> source : environment.getPropertySources()) {
			if (!(source instanceof org.springframework.core.env.EnumerablePropertySource<?> enumerable)) {
				continue;
			}
			for (String name : enumerable.getPropertyNames()) {
				if (!name.startsWith(OLD_PREFIX)) {
					continue;
				}
				String suffix = name.substring(OLD_PREFIX.length());
				String newKey = NEW_PREFIX + suffix;
				if (environment.containsProperty(newKey)) {
					continue;
				}
				Object value = environment.getProperty(name, Object.class);
				if (value == null) {
					continue;
				}
				migrated.put(newKey, value);
				log.warn("Deprecated property '" + name + "' detected. " + "Please migrate to '" + newKey + "'. "
						+ "The 'spring.cloud.contract.stubrunner.*' prefix will be removed in the next major release.");
			}
		}
		if (!migrated.isEmpty()) {
			environment.getPropertySources()
				.addFirst(new MapPropertySource("stubborn-contract-legacy-properties", migrated));
		}
	}

}
