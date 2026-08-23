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

package sh.stubborn.contract.stubrunner;

import java.lang.reflect.Constructor;

import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Covers {@link AetherFactories}'s Maven-injection short-circuits (return the injected
 * system/session rather than building a new one) and the utility-class constructor guard,
 * which the resolution tests in {@link AetherStubDownloaderTests} do not reach.
 */
class AetherFactoriesInjectionTests {

	@Test
	void repositorySystemOrReturnsTheInjectedSystem() {
		RepositorySystem injected = AetherFactories.repositorySystemOr(null);

		assertThat(AetherFactories.repositorySystemOr(injected)).isSameAs(injected);
	}

	@Test
	void sessionOrReturnsTheInjectedSession() {
		RepositorySystem system = AetherFactories.repositorySystemOr(null);
		RepositorySystemSession injected = AetherFactories.newSession(system, true);

		assertThat(AetherFactories.sessionOr(system, injected, false)).isSameAs(injected);
	}

	@Test
	void theUtilityConstructorCannotBeInstantiated() throws Exception {
		Constructor<AetherFactories> constructor = AetherFactories.class.getDeclaredConstructor();
		constructor.setAccessible(true);

		assertThatThrownBy(constructor::newInstance).hasCauseInstanceOf(IllegalStateException.class);
	}

}
