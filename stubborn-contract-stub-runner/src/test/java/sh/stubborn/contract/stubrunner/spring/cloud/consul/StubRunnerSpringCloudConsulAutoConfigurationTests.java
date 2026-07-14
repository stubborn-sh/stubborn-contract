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

package sh.stubborn.contract.stubrunner.spring.cloud.consul;

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.agent.model.NewService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.consul.discovery.ConsulDiscoveryProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import sh.stubborn.contract.stubrunner.StubFinder;
import sh.stubborn.contract.stubrunner.spring.AutoConfigureStubRunner;
import sh.stubborn.contract.stubrunner.spring.StubRunnerProperties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@SpringBootTest(classes = StubRunnerSpringCloudConsulAutoConfigurationTests.Config.class,
		webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		properties = { "eureka.client.enabled=false", "spring.cloud.zookeeper.enabled=false",
				"spring.cloud.contract.stubrunner.cloud.stubbed.discovery.enabled=false",
				"spring.cloud.contract.stubrunner.cloud.eureka.enabled=false",
				"spring.cloud.contract.stubrunner.cloud.consul.enabled=true" })
@AutoConfigureStubRunner(repositoryRoot = "classpath:m2repo/repository/",
		ids = "sh.stubborn.contract.verifier.stubs:fraudDetectionServer",
		stubsMode = StubRunnerProperties.StubsMode.REMOTE)
class StubRunnerSpringCloudConsulAutoConfigurationTests {

	@Autowired
	StubFinder stubFinder;

	@Autowired
	ConsulClient consulClient;

	@BeforeAll
	static void setup() {
		System.clearProperty("spring.cloud.contract.stubrunner.repository.root");
		System.clearProperty("spring.cloud.contract.stubrunner.classifier");
	}

	@AfterAll
	static void cleanup() {
		setup();
	}

	@Test
	void shouldRegisterStubsInConsul() {
		assertThat(this.stubFinder.findStubUrl("sh.stubborn.contract.verifier.stubs:fraudDetectionServer")).isNotNull();
		verify(this.consulClient).agentServiceRegister(
				argThat(new NewServiceMatcher("sh.stubborn.contract.verifier.stubs:fraudDetectionServer",
						"sh.stubborn.contract.verifier.stubs:fraudDetectionServer")));
	}

	static class NewServiceMatcher implements ArgumentMatcher<NewService> {

		private final String expectedId;

		private final String expectedName;

		NewServiceMatcher(String expectedId, String expectedName) {
			this.expectedId = expectedId;
			this.expectedName = expectedName;
		}

		@Override
		public boolean matches(NewService item) {
			return item.getId().equals(this.expectedId) && item.getName().equals(this.expectedName);
		}

	}

	@Configuration
	@EnableAutoConfiguration
	@EnableDiscoveryClient
	static class Config {

		@Bean
		ConsulClient mockedConsulClient() {
			return mock(ConsulClient.class);
		}

		@Bean
		ConsulDiscoveryProperties consulDiscoveryProperties() {
			return mock(ConsulDiscoveryProperties.class);
		}

	}

}
