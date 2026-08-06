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

package sh.stubborn.contract.stubrunner.spring.cloud.loadbalancer;

import sh.stubborn.contract.stubrunner.spring.cloud.ConditionalOnStubbedDiscoveryEnabled;
import sh.stubborn.contract.stubrunner.spring.cloud.StubMapperProperties;
import sh.stubborn.contract.stubrunner.spring.cloud.StubRunnerSpringCloudAutoConfiguration;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClientsProperties;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClientConfiguration;
import org.springframework.cloud.loadbalancer.config.LoadBalancerAutoConfiguration;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Provides autoconfiguraion for the Spring Cloud Load Balancer module.
 *
 * @author Marcin Grzejszczak
 * @since 2.2.3
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({ LoadBalancerClient.class, LoadBalancerClientFactory.class })
@ConditionalOnProperty(value = "stubborn.contract.stubrunner.cloud.loadbalancer.enabled", matchIfMissing = true)
@ConditionalOnBean(StubMapperProperties.class)
@AutoConfigureBefore(LoadBalancerAutoConfiguration.class)
@AutoConfigureAfter({ LoadBalancerClientConfiguration.class, StubRunnerSpringCloudAutoConfiguration.class })
@ConditionalOnStubbedDiscoveryEnabled
@EnableConfigurationProperties(LoadBalancerClientsProperties.class)
public class SpringCloudLoadBalancerAutoConfiguration {

	@Bean
	@Primary
	LoadBalancerClientFactory stubRunnerLoadBalancerClientFactory(BeanFactory beanFactory,
			LoadBalancerClientsProperties properties) {
		return new StubRunnerLoadBalancerClientFactory(beanFactory, properties);
	}

}
