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

import sh.stubborn.contract.stubrunner.StubFinder;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

/**
 * Wraps {@link DiscoveryClient} in a Stub Runner implementation that tries to find a
 * corresponding WireMock server for a searched dependency.
 *
 * @author Marcin Grzejszczak
 * @since 1.0.0
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(StubMapperProperties.class)
@ConditionalOnClass(DiscoveryClient.class)
@ConditionalOnProperty(value = "stubborn.contract.stubrunner.cloud.enabled", matchIfMissing = true)
public class StubRunnerSpringCloudAutoConfiguration {

	@Bean
	public StubRunnerDiscoveryClientWrapper stubRunnerDiscoveryClientWrapper(BeanFactory beanFactory) {
		return new StubRunnerDiscoveryClientWrapper(beanFactory);
	}

	@Bean
	@ConditionalOnStubbedDiscoveryEnabled
	@ConditionalOnProperty(value = "stubborn.contract.stubrunner.cloud.delegate.enabled", havingValue = "false",
			matchIfMissing = true)
	@Order(Ordered.HIGHEST_PRECEDENCE)
	public DiscoveryClient noOpStubRunnerDiscoveryClient(StubFinder stubFinder,
			StubMapperProperties stubMapperProperties) {
		return new StubRunnerDiscoveryClient(stubFinder, stubMapperProperties);
	}

	@Bean
	@ConditionalOnStubbedDiscoveryEnabled
	@ConditionalOnProperty(value = "stubborn.contract.stubrunner.cloud.delegate.enabled", havingValue = "false",
			matchIfMissing = true)
	@Order(Ordered.HIGHEST_PRECEDENCE)
	public ReactiveDiscoveryClient noOpStubRunnerReactiveDiscoveryClient(StubFinder stubFinder,
			StubMapperProperties stubMapperProperties) {
		return new StubRunnerReactiveDiscoveryClient(stubFinder, stubMapperProperties);
	}

}
