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

import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.consul.ConsulAutoConfiguration;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.cloud.netflix.eureka.EurekaClientAutoConfiguration;
import org.springframework.cloud.zookeeper.ZookeeperAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestTemplate;

import sh.stubborn.contract.stubrunner.StubFinder;
import sh.stubborn.contract.stubrunner.spring.AutoConfigureStubRunner;
import sh.stubborn.contract.stubrunner.StubsMode;
import sh.stubborn.contract.stubrunner.spring.StubRunnerProperties;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = StubRunnerSpringCloudReactiveAutoConfigurationTests.Config.class,
		properties = { "eureka.client.enabled=false", "spring.cloud.zookeeper.enabled=false",
				"spring.cloud.contract.stubrunner.cloud.stubbed.discovery.enabled=true",
				"spring.cloud.contract.stubrunner.cloud.eureka.enabled=false" })
@AutoConfigureStubRunner(repositoryRoot = "classpath:m2repo/repository/",
		ids = { "sh.stubborn.contract.verifier.stubs:fraudDetectionServer",
				"sh.stubborn.contract.verifier.stubs:loanIssuance" },
		stubsMode = StubsMode.REMOTE)
@ActiveProfiles("cloudtest")
class StubRunnerSpringCloudReactiveAutoConfigurationTests {

	@Autowired
	StubFinder stubFinder;

	@Autowired
	ReactiveDiscoveryClient reactiveDiscoveryClient;

	@Autowired
	LoadBalancerClientFactory loadBalancerClientFactory;

	@BeforeAll
	static void setup() {
		System.clearProperty("spring.cloud.contract.stubrunner.repository.root");
		System.clearProperty("spring.cloud.contract.stubrunner.classifier");
	}

	@AfterAll
	static void cleanup() {
		setup();
	}

	@BeforeEach
	void beforeEach() {
		System.clearProperty("spring.cloud.contract.stubrunner.repository.root");
		System.clearProperty("spring.cloud.contract.stubrunner.classifier");
	}

	@Test
	void shouldStartWireMockServers() {
		assertThat(this.stubFinder.findStubUrl("sh.stubborn.contract.verifier.stubs", "loanIssuance")).isNotNull();
		assertThat(this.stubFinder.findStubUrl("loanIssuance")).isNotNull();
	}

	@Test
	void shouldUseStubRunnerLoadBalancerFactory() {
		assertThat(this.loadBalancerClientFactory.getClass().getSimpleName()).contains("StubRunner");
	}

	@Test
	void shouldReturnStubInstancesViaReactiveDiscoveryClient() {
		List<ServiceInstance> instances = this.reactiveDiscoveryClient.getInstances("loanIssuance")
			.collectList()
			.block();
		assertThat(instances).isNotEmpty();
	}

	@Configuration
	@EnableAutoConfiguration(exclude = { EurekaClientAutoConfiguration.class, ConsulAutoConfiguration.class,
			ZookeeperAutoConfiguration.class })
	static class Config {

		@Bean
		@LoadBalanced
		RestTemplate restTemplate() {
			return new RestTemplate();
		}

	}

}
