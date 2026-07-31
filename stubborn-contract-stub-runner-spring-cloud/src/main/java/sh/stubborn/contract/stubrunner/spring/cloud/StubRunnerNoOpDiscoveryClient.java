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

import java.util.Collections;
import java.util.List;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;

/**
 * No-op {@link DiscoveryClient} used when no delegate is available.
 *
 * @author Marcin Grzejszczak
 * @since 1.0.0
 */
class StubRunnerNoOpDiscoveryClient implements DiscoveryClient {

	@Override
	public String description() {
		return "Spring Cloud Stub Runner No-op DiscoveryClient";
	}

	@Override
	public List<ServiceInstance> getInstances(String serviceId) {
		return Collections.emptyList();
	}

	@Override
	public List<String> getServices() {
		return Collections.emptyList();
	}

}
