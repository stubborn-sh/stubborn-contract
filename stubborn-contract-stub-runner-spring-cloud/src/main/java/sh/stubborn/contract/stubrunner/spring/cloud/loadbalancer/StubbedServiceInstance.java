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

import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jspecify.annotations.Nullable;
import sh.stubborn.contract.stubrunner.RunningStubs;
import sh.stubborn.contract.stubrunner.StubConfiguration;
import sh.stubborn.contract.stubrunner.StubFinder;
import sh.stubborn.contract.stubrunner.spring.cloud.StubMapperProperties;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.util.StringUtils;

/**
 * {@link ServiceInstance} backed by a running stub.
 *
 * @author Marcin Grzejszczak
 * @since 1.0.0
 */
class StubbedServiceInstance implements ServiceInstance {

	private final StubFinder stubFinder;

	private final StubMapperProperties stubMapperProperties;

	private final String serviceId;

	static final Map<String, Map.Entry<StubConfiguration, Integer>> CACHE = new ConcurrentHashMap<>();

	StubbedServiceInstance(StubFinder stubFinder, StubMapperProperties stubMapperProperties, String serviceId) {
		this.stubFinder = stubFinder;
		this.stubMapperProperties = stubMapperProperties;
		this.serviceId = serviceId;
	}

	private Map.@Nullable Entry<StubConfiguration, Integer> stubEntry() {
		Map.Entry<StubConfiguration, Integer> entry = CACHE.get(this.serviceId);
		if (entry != null) {
			return entry;
		}
		RunningStubs runningStubs = this.stubFinder.findAllRunningStubs();
		String ivyNotation = this.stubMapperProperties.fromServiceIdToIvyNotation(this.serviceId);
		String mappedServiceName = StringUtils.hasText(ivyNotation) ? ivyNotation : this.serviceId;
		entry = runningStubs.getEntry(mappedServiceName);
		CACHE.put(this.serviceId, entry);
		return entry;
	}

	@Override
	public String getServiceId() {
		return this.serviceId;
	}

	@Override
	public String getHost() {
		return "localhost";
	}

	@Override
	public int getPort() {
		Map.Entry<StubConfiguration, Integer> entry = stubEntry();
		if (entry == null) {
			return 0;
		}
		return entry.getValue();
	}

	@Override
	public boolean isSecure() {
		// TODO: What if the user marks a stub as secure?
		return false;
	}

	@Override
	public URI getUri() {
		return URI.create((isSecure() ? "https://" : "http://") + getHost() + ":" + getPort());
	}

	@Override
	public @Nullable Map<String, String> getMetadata() {
		return null;
	}

}
