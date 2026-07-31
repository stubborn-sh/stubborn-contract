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

package sh.stubborn.contract.stubrunner.spring.cloud;

import org.jspecify.annotations.Nullable;
import sh.stubborn.contract.stubrunner.StubFinder;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.core.env.Environment;

/**
 * Wraps a {@link DiscoveryClient} bean in a Stub Runner implementation.
 *
 * @author Marcin Grzejszczak
 * @since 1.0.0
 */
class StubRunnerDiscoveryClientWrapper implements BeanPostProcessor {

	private final BeanFactory beanFactory;

	@Nullable StubFinder stubFinder;

	@Nullable StubMapperProperties stubMapperProperties;

	@Nullable Boolean stubbedDiscoveryEnabled;

	@Nullable Boolean cloudDelegateEnabled;

	StubRunnerDiscoveryClientWrapper(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		return bean;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		if (bean instanceof DiscoveryClient && !(bean instanceof StubRunnerDiscoveryClient)) {
			if (!isStubbedDiscoveryEnabled()) {
				return bean;
			}
			if (isCloudDelegateEnabled()) {
				return new StubRunnerDiscoveryClient((DiscoveryClient) bean, stubFinder(), stubMapperProperties());
			}
			return new StubRunnerDiscoveryClient(stubFinder(), stubMapperProperties());
		}
		return bean;
	}

	StubFinder stubFinder() {
		if (this.stubFinder == null) {
			this.stubFinder = this.beanFactory.getBean(StubFinder.class);
		}
		return this.stubFinder;
	}

	StubMapperProperties stubMapperProperties() {
		if (this.stubMapperProperties == null) {
			this.stubMapperProperties = this.beanFactory.getBean(StubMapperProperties.class);
		}
		return this.stubMapperProperties;
	}

	boolean isStubbedDiscoveryEnabled() {
		if (this.stubbedDiscoveryEnabled == null) {
			this.stubbedDiscoveryEnabled = Boolean.valueOf(this.beanFactory.getBean(Environment.class)
				.getProperty("stubborn.contract.stubrunner.cloud.stubbed.discovery.enabled", "true"));
		}
		return this.stubbedDiscoveryEnabled;
	}

	boolean isCloudDelegateEnabled() {
		if (this.cloudDelegateEnabled == null) {
			this.cloudDelegateEnabled = Boolean.valueOf(this.beanFactory.getBean(Environment.class)
				.getProperty("stubborn.contract.stubrunner.cloud.delegate.enabled", "false"));
		}
		return this.cloudDelegateEnabled;
	}

}
