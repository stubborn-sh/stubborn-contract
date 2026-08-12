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

package sh.stubborn.contract.stubrunner.spring.cloud.eureka;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.awaitility.Awaitility;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sh.stubborn.contract.stubrunner.StubFinder;
import sh.stubborn.contract.stubrunner.StubsMode;
import sh.stubborn.contract.stubrunner.spring.AutoConfigureStubRunner;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.netflix.eureka.http.EurekaClientHttpRequestFactorySupplier;
import org.springframework.cloud.netflix.eureka.http.RestClientDiscoveryClientOptionalArgs;
import org.springframework.cloud.netflix.eureka.http.RestClientTransportClientFactories;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.BDDAssertions.then;

/**
 * @author Marcin Grzejszczak
 */
// TODO: Speed up this test somehow (move it out of Spring Cloud Contract core to samples)
@SpringBootTest(classes = StubRunnerSpringCloudEurekaAutoConfigurationSpec.Config.class,
		webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		properties = { "stubborn.contract.stubrunner.cloud.eureka.enabled=true",
				"stubborn.contract.stubrunner.cloud.stubbed.discovery.enabled=false", "eureka.client.enabled=true",
				"eureka.client.restclient.enabled=false", "eureka.client.webclient.enabled=false",
				"eureka.client.jersey.enabled=false", "debug=true", "eureka.instance.leaseRenewalIntervalInSeconds=1",
				"eureka.client.registryFetchIntervalSeconds=1",
				"eureka.client.instanceInfoReplicationIntervalSeconds=1",
				"eureka.client.initialInstanceInfoReplicationIntervalSeconds=0" })
@AutoConfigureStubRunner(ids = { "sh.stubborn.contract.verifier.stubs:loanIssuance",
		"sh.stubborn.contract.verifier.stubs:fraudDetectionServer", "sh.stubborn.contract.verifier.stubs:bootService" },
		repositoryRoot = "classpath:m2repo/repository/", stubsMode = StubsMode.REMOTE)
class StubRunnerSpringCloudEurekaAutoConfigurationSpec {

	private static final Logger log = LoggerFactory.getLogger(StubRunnerSpringCloudEurekaAutoConfigurationSpec.class);

	@Autowired
	StubFinder stubFinder;

	@Autowired
	@LoadBalanced
	RestTemplate restTemplate;

	static @Nullable ConfigurableApplicationContext eurekaServer;

	@BeforeAll
	static void setupSpec() {
		System.clearProperty("stubborn.contract.stubrunner.stubs.repository.root");
		System.clearProperty("stubborn.contract.stubrunner.stubs.classifier");
		eurekaServer = SpringApplication.run(EurekaServer.class,
				"--stubborn.contract.stubrunner.cloud.eureka.enabled=true",
				"--stubborn.contract.stubrunner.cloud.stubbed.discovery.enabled=false", "--eureka.client.enabled=true",
				"--server.port=8761", "--spring.profiles.active=eureka",
				// Fast propagation so a registered instance is discoverable within ~1-2s
				// instead of the 30s+ defaults (response cache + registry fetch). We do
				// NOT
				// shorten the lease/eviction here: under CI load an aggressive lease can
				// evict a healthy instance between renewals, which reintroduces
				// flakiness.
				"--eureka.server.responseCacheUpdateIntervalMs=500", "--eureka.server.useReadOnlyResponseCache=false");
	}

	@AfterAll
	static void cleanupSpec() {
		System.clearProperty("stubborn.contract.stubrunner.stubs.repository.root");
		System.clearProperty("stubborn.contract.stubrunner.stubs.classifier");
	}

	@Test
	void shouldMakeServiceDiscoveryWork() throws IOException {
		// WireMocks are running
		then(read(this.stubFinder.findStubUrl("loanIssuance").toString() + "/name")).isEqualTo("loanIssuance");
		then(read(this.stubFinder.findStubUrl("fraudDetectionServer").toString() + "/name"))
			.isEqualTo("fraudDetectionServer");
		// Stubs can be reached via load-balanced service discovery. Poll until both
		// instances have registered and the client has fetched them. ignoreExceptions()
		// is essential: before the instances are discovered the load balancer throws
		// IllegalStateException("No instances available ..."), which is NOT an
		// AssertionError, so untilAsserted would otherwise fail on the first poll instead
		// of retrying. Asserting both names in one block avoids a one-resolves-first
		// race.
		log.info("Waiting for stubs to register in Eureka...");
		Awaitility.await()
			.pollDelay(0, TimeUnit.SECONDS)
			.pollInterval(500, TimeUnit.MILLISECONDS)
			.atMost(90, TimeUnit.SECONDS)
			.ignoreExceptions()
			.untilAsserted(() -> {
				then(this.restTemplate.getForObject("http://loanIssuance/name", String.class))
					.isEqualTo("loanIssuance");
				then(this.restTemplate.getForObject("http://someNameThatShouldMapFraudDetectionServer/name",
						String.class))
					.isEqualTo("fraudDetectionServer");
			});
	}

	private static String read(String url) throws IOException {
		try (InputStream inputStream = URI.create(url).toURL().openStream()) {
			return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
		}
	}

	@Configuration
	@EnableAutoConfiguration
	static class Config {

		@Bean
		@LoadBalanced
		RestTemplate restTemplate() {
			RestTemplate template = new RestTemplate();
			template.setErrorHandler(new DefaultResponseErrorHandler() {
				@Override
				public void handleError(URI url, HttpMethod method, ClientHttpResponse response) throws IOException {
					try {
						super.handleError(url, method, response);
					}
					catch (Exception ex) {
						throw new AssertionError(ex);
					}
				}
			});
			return template;
		}

		// because eureka server has JerseyClient, need these beans for eureka client in
		// same jvm to work
		@Bean
		RestClientDiscoveryClientOptionalArgs restClientDiscoveryClientOptionalArgs(
				EurekaClientHttpRequestFactorySupplier eurekaClientHttpRequestFactorySupplier,
				ObjectProvider<RestClient.Builder> restClientBuilderProvider) {
			return new RestClientDiscoveryClientOptionalArgs(eurekaClientHttpRequestFactorySupplier,
					() -> restClientBuilderProvider.getIfAvailable(RestClient::builder));
		}

		@Bean
		RestClientTransportClientFactories restClientTransportClientFactories(
				RestClientDiscoveryClientOptionalArgs optionalArgs) {
			return new RestClientTransportClientFactories(optionalArgs);
		}

	}

	@Configuration
	@EnableAutoConfiguration
	@EnableEurekaServer
	static class EurekaServer {

		@Bean
		Filter httpTraceFilter() {
			return new Filter() {
				@Override
				public void init(FilterConfig filterConfig) throws ServletException {

				}

				@Override
				public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse,
						FilterChain filterChain) throws IOException, ServletException {
					filterChain.doFilter(servletRequest, servletResponse);
				}

				@Override
				public void destroy() {

				}
			};
		}

	}

}
