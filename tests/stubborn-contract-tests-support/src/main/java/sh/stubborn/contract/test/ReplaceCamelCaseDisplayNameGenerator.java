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

package sh.stubborn.contract.test;

import java.lang.reflect.Method;
import java.util.Locale;

import org.junit.jupiter.api.DisplayNameGenerator;

/**
 * {@link DisplayNameGenerator} that turns camelCase test method names into readable,
 * lowercase, space-separated phrases.
 * <p>
 * For example {@code shouldTriggerAMessageByLabel} becomes
 * {@code should trigger a message by label} and
 * {@code shouldTriggerALabelForTheExistingGroupIdAndArtifactId} becomes
 * {@code should trigger a label for the existing group id and artifact id}.
 * <p>
 * Wired as the JUnit Jupiter default via {@code junit-platform.properties} shipped on the
 * test classpath, so individual {@code @DisplayName} annotations are no longer required.
 *
 * @author Stubborn Contract
 */
public class ReplaceCamelCaseDisplayNameGenerator extends DisplayNameGenerator.Standard {

	@Override
	public String generateDisplayNameForMethod(Class<?> testClass, Method testMethod) {
		return replaceCamelCase(testMethod.getName());
	}

	private String replaceCamelCase(String name) {
		String result = name.replace('_', ' ');
		// end of an acronym run before a following word, e.g. "HTTPServer" -> "HTTP
		// Server"
		result = result.replaceAll("(?<=[A-Z])(?=[A-Z][a-z])", " ");
		// lower case or digit followed by an upper case, e.g. "groupId" -> "group Id"
		result = result.replaceAll("(?<=[a-z0-9])(?=[A-Z])", " ");
		// letter followed by a digit, e.g. "port8080" -> "port 8080"
		result = result.replaceAll("(?<=[a-zA-Z])(?=[0-9])", " ");
		// digit followed by a letter, e.g. "8080port" -> "8080 port"
		result = result.replaceAll("(?<=[0-9])(?=[a-zA-Z])", " ");
		result = result.replaceAll("\\s+", " ").trim();
		return result.toLowerCase(Locale.ROOT);
	}

}
