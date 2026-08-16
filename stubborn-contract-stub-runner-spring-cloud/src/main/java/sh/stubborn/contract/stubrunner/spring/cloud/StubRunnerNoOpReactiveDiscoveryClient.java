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

import reactor.core.publisher.Flux;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;

/**
 * No-op {@link ReactiveDiscoveryClient} used when no delegate is available.
 *
 * @author Marcin Grzejszczak
 * @since 1.0.0
 */
class StubRunnerNoOpReactiveDiscoveryClient implements ReactiveDiscoveryClient {

	@Override
	public String description() {
		return "Spring Cloud Stub Runner No-op ReactiveDiscoveryClient";
	}

	@Override
	public Flux<ServiceInstance> getInstances(String serviceId) {
		return Flux.empty();
	}

	@Override
	public Flux<String> getServices() {
		return Flux.empty();
	}

}
