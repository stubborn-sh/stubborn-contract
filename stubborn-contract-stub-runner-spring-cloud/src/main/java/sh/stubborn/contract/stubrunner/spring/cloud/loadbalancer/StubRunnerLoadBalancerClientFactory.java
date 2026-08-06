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

import java.io.Closeable;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClientsProperties;
import org.springframework.cloud.client.loadbalancer.reactive.ReactiveLoadBalancer;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.core.ResolvableType;

/**
 * Stub Runner variant of the {@link LoadBalancerClientFactory}.
 *
 * @author Marcin Grzejszczak
 * @since 2.2.3
 */
class StubRunnerLoadBalancerClientFactory extends LoadBalancerClientFactory implements Closeable {

	private final BeanFactory beanFactory;

	StubRunnerLoadBalancerClientFactory(BeanFactory beanFactory, LoadBalancerClientsProperties properties) {
		super(properties);
		this.beanFactory = beanFactory;
	}

	@Override
	public ReactiveLoadBalancer<ServiceInstance> getInstance(String serviceId) {
		return new ContractReactorServiceInstanceLoadBalancer(this.beanFactory, serviceId);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T getInstance(String name, Class<T> type) {
		return (T) getInstance(name);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T getInstance(String name, Class<?> clazz, Class<?>... generics) {
		return (T) getInstance(name);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T getInstance(String name, ResolvableType type) {
		return (T) getInstance(name);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> Map<String, T> getInstances(String name, Class<T> type) {
		Map<String, T> map = new HashMap<>();
		map.put(name, (T) getInstance(name));
		return map;
	}

	@Override
	public void close() {
		StubbedServiceInstance.CACHE.clear();
	}

}
