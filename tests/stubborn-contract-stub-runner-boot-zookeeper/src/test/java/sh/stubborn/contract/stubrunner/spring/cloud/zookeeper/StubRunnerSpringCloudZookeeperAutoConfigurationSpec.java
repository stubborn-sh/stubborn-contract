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

package sh.stubborn.contract.stubrunner.spring.cloud.zookeeper;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import org.apache.curator.test.TestingServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sh.stubborn.contract.stubrunner.StubFinder;
import sh.stubborn.contract.stubrunner.StubsMode;
import sh.stubborn.contract.stubrunner.spring.AutoConfigureStubRunner;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.test.TestSocketUtils;
import org.springframework.cloud.zookeeper.ZookeeperProperties;
import org.springframework.cloud.zookeeper.discovery.ZookeeperDiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.BDDAssertions.then;

/**
 * @author Marcin Grzejszczak
 */
@SpringBootTest(classes = StubRunnerSpringCloudZookeeperAutoConfigurationSpec.Config.class,
		webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		properties = { "stubborn.contract.stubrunner.cloud.stubbed.discovery.enabled=false", "debug=true" })
@AutoConfigureStubRunner(ids = { "sh.stubborn.contract.verifier.stubs:loanIssuance",
		"sh.stubborn.contract.verifier.stubs:fraudDetectionServer", "sh.stubborn.contract.verifier.stubs:bootService" },
		repositoryRoot = "classpath:m2repo/repository/", stubsMode = StubsMode.REMOTE)
class StubRunnerSpringCloudZookeeperAutoConfigurationSpec {

	@Autowired
	StubFinder stubFinder;

	@Autowired
	@LoadBalanced
	RestTemplate restTemplate;

	@Autowired
	ZookeeperDiscoveryClient zookeeperServiceDiscovery;

	@BeforeAll
	static void setupSpec() {
		System.clearProperty("spring.cloud.contract.stubrunner.stubs.repository.root");
		System.clearProperty("spring.cloud.contract.stubrunner.stubs.classifier");
	}

	@AfterAll
	static void cleanupSpec() {
		setupSpec();
	}

	@Test
	@DisplayName("should make service discovery work")
	void shouldMakeServiceDiscoveryWork() throws IOException {
		// WireMocks are running
		then(read(this.stubFinder.findStubUrl("loanIssuance").toString() + "/name")).isEqualTo("loanIssuance");
		then(read(this.stubFinder.findStubUrl("fraudDetectionServer").toString() + "/name"))
			.isEqualTo("fraudDetectionServer");
		// Stubs can be reached via load service discovery
		then(this.restTemplate.getForObject("http://loanIssuance/name", String.class)).isEqualTo("loanIssuance");
		then(this.restTemplate.getForObject("http://someNameThatShouldMapFraudDetectionServer/name", String.class))
			.isEqualTo("fraudDetectionServer");
	}

	@Test
	@DisplayName("should have all apps registered in Service Discovery")
	void shouldHaveAllAppsRegisteredInServiceDiscovery() {
		then(this.zookeeperServiceDiscovery.getInstances("loanIssuance")).isNotEmpty();
		then(this.zookeeperServiceDiscovery.getInstances("someNameThatShouldMapFraudDetectionServer")).isNotEmpty();
	}

	private static String read(String url) throws IOException {
		try (InputStream inputStream = URI.create(url).toURL().openStream()) {
			return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
		}
	}

	@Configuration
	@EnableAutoConfiguration
	@EnableDiscoveryClient
	static class Config {

		@Bean
		TestingServer testingServer() throws Exception {
			return new TestingServer(TestSocketUtils.findAvailableTcpPort());
		}

		@Bean
		ZookeeperProperties zookeeperProperties() throws Exception {
			ZookeeperProperties properties = new ZookeeperProperties();
			properties.setConnectString(testingServer().getConnectString());
			return properties;
		}

		@Bean
		@LoadBalanced
		RestTemplate restTemplate() {
			return new RestTemplate();
		}

	}

}
