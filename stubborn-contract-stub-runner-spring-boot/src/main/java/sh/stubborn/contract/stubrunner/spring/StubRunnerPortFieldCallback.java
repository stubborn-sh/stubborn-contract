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

import java.lang.reflect.Field;

import org.springframework.core.env.Environment;
import org.springframework.util.ReflectionUtils;

/**
 * Reflection callback that injects the port of a running stub into a
 * {@link StubRunnerPort} annotated field.
 *
 * @author Marcin Grzejszczak
 * @since 1.0.0
 */
class StubRunnerPortFieldCallback implements ReflectionUtils.FieldCallback {

	private final Environment environment;

	private final Object bean;

	StubRunnerPortFieldCallback(Environment environment, Object bean) {
		this.environment = environment;
		this.bean = bean;
	}

	@Override
	public void doWith(Field field) throws IllegalArgumentException, IllegalAccessException {
		if (!field.isAnnotationPresent(StubRunnerPort.class)) {
			return;
		}
		ReflectionUtils.makeAccessible(field);
		String stub = field.getDeclaredAnnotation(StubRunnerPort.class).value();
		Integer port = this.environment.getProperty(
				StubRunnerConfiguration.STUBRUNNER_PREFIX + "." + stub.replace(":", ".") + ".port", Integer.class);
		if (port != null) {
			field.set(this.bean, port);
		}
	}

}
