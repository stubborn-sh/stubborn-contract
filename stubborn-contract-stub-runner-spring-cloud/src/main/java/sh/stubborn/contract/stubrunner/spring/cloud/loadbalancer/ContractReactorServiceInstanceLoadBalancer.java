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

import org.jspecify.annotations.Nullable;
import reactor.core.publisher.Mono;
import sh.stubborn.contract.stubrunner.StubFinder;
import sh.stubborn.contract.stubrunner.spring.cloud.StubMapperProperties;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.CompletionContext;
import org.springframework.cloud.client.loadbalancer.DefaultResponse;
import org.springframework.cloud.client.loadbalancer.LoadBalancerLifecycle;
import org.springframework.cloud.client.loadbalancer.Request;
import org.springframework.cloud.client.loadbalancer.Response;
import org.springframework.cloud.loadbalancer.core.ReactorServiceInstanceLoadBalancer;

/**
 * Stub Runner variant of the {@link ReactorServiceInstanceLoadBalancer}.
 *
 * @author Marcin Grzejszczak
 * @since 1.0.0
 */
class ContractReactorServiceInstanceLoadBalancer implements ReactorServiceInstanceLoadBalancer, LoadBalancerLifecycle {

	private final BeanFactory beanFactory;

	private final String serviceId;

	private @Nullable StubFinder stubFinder;

	private @Nullable StubMapperProperties stubMapperProperties;

	ContractReactorServiceInstanceLoadBalancer(BeanFactory beanFactory, String serviceId) {
		this.beanFactory = beanFactory;
		this.serviceId = serviceId;
	}

	@Override
	public void onStart(Request request) {

	}

	@Override
	public void onStartRequest(Request request, Response lbResponse) {

	}

	@Override
	public void onComplete(CompletionContext completionContext) {

	}

	@Override
	public Mono<Response<ServiceInstance>> choose(Request request) {
		return Mono.just(
				new DefaultResponse(new StubbedServiceInstance(stubFinder(), stubMapperProperties(), this.serviceId)));
	}

	private StubFinder stubFinder() {
		if (this.stubFinder == null) {
			this.stubFinder = this.beanFactory.getBean(StubFinder.class);
		}
		return this.stubFinder;
	}

	private StubMapperProperties stubMapperProperties() {
		if (this.stubMapperProperties == null) {
			this.stubMapperProperties = this.beanFactory.getBean(StubMapperProperties.class);
		}
		return this.stubMapperProperties;
	}

}
