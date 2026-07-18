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

package sh.stubborn.contract.stubrunner.spring;

import java.io.File;
import java.net.URL;

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.assertj.core.api.BDDAssertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.test.TestSocketUtils;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;

import sh.stubborn.contract.stubrunner.HttpServerStubConfiguration;
import sh.stubborn.contract.stubrunner.StubFinder;
import sh.stubborn.contract.stubrunner.StubNotFoundException;
import sh.stubborn.contract.stubrunner.provider.wiremock.WireMockHttpServerStubAccessor;
import sh.stubborn.contract.stubrunner.provider.wiremock.WireMockHttpServerStubConfigurer;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = StubRunnerConfigurationTests.Config.class, properties = { "stubrunner.cloud.enabled=false",
		"foo=${spring.cloud.contract.stubrunner.runningstubs.fraudDetectionServer.port}",
		"fooWithGroup=${spring.cloud.contract.stubrunner.runningstubs.sh.stubborn.contract.verifier.stubs.fraudDetectionServer.port}" })
@AutoConfigureStubRunner(mappingsOutputFolder = "target/outputmappings/",
		httpServerStubConfigurer = StubRunnerConfigurationTests.HttpsForFraudDetection.class)
@ActiveProfiles("test")
class StubRunnerConfigurationTests {

	@Autowired
	StubFinder stubFinder;

	@Autowired
	Environment environment;

	@StubRunnerPort("fraudDetectionServer")
	int fraudDetectionServerPort;

	@StubRunnerPort("sh.stubborn.contract.verifier.stubs:fraudDetectionServer")
	int fraudDetectionServerPortWithGroupId;

	@Value("${foo}")
	Integer foo;

	@BeforeAll
	static void setupSpec() {
		System.clearProperty("spring.cloud.contract.stubrunner.repository.root");
		System.clearProperty("spring.cloud.contract.stubrunner.classifier");
		WireMockHttpServerStubAccessor.clear();
	}

	@AfterAll
	static void cleanupSpec() {
		setupSpec();
	}

	@Test
	void shouldMarkAllPortsAsRandom() {
		assertThat(WireMockHttpServerStubAccessor.everyPortRandom()).isTrue();
	}

	@Test
	void shouldStartWireMockServers() throws Exception {
		assertThat(this.stubFinder.findStubUrl("sh.stubborn.contract.verifier.stubs", "loanIssuance")).isNotNull();
		assertThat(this.stubFinder.findStubUrl("loanIssuance")).isNotNull();
		assertThat(this.stubFinder.findStubUrl("loanIssuance"))
			.isEqualTo(this.stubFinder.findStubUrl("sh.stubborn.contract.verifier.stubs", "loanIssuance"));
		assertThat(this.stubFinder.findStubUrl("loanIssuance"))
			.isEqualTo(this.stubFinder.findStubUrl("sh.stubborn.contract.verifier.stubs:loanIssuance"));
		assertThat(this.stubFinder.findStubUrl("sh.stubborn.contract.verifier.stubs:loanIssuance:0.0.1-SNAPSHOT"))
			.isEqualTo(this.stubFinder
				.findStubUrl("sh.stubborn.contract.verifier.stubs:loanIssuance:0.0.1-SNAPSHOT:stubs"));
		assertThat(this.stubFinder.findStubUrl("sh.stubborn.contract.verifier.stubs:fraudDetectionServer")).isNotNull();
		assertThat(this.stubFinder.findAllRunningStubs().isPresent("loanIssuance")).isTrue();
		assertThat(this.stubFinder.findAllRunningStubs()
			.isPresent("sh.stubborn.contract.verifier.stubs", "fraudDetectionServer")).isTrue();
		assertThat(this.stubFinder.findAllRunningStubs()
			.isPresent("sh.stubborn.contract.verifier.stubs:fraudDetectionServer")).isTrue();
		URL loanUrl = this.stubFinder.findStubUrl("loanIssuance");
		assertThat(readUrl(loanUrl.toString() + "/name")).isEqualTo("loanIssuance");
		URL fraudUrl = this.stubFinder.findStubUrl("fraudDetectionServer");
		assertThat(readUrl(fraudUrl.toString() + "/name")).isEqualTo("fraudDetectionServer");
		assertThat(this.stubFinder.findStubUrl("fraudDetectionServer").toString()).startsWith("https");
	}

	@Test
	void shouldThrowAnExceptionWhenStubIsNotFound() {
		BDDAssertions.thenThrownBy(() -> this.stubFinder.findStubUrl("nonExistingService"))
			.isInstanceOf(StubNotFoundException.class);
		BDDAssertions.thenThrownBy(() -> this.stubFinder.findStubUrl("nonExistingGroupId", "nonExistingArtifactId"))
			.isInstanceOf(StubNotFoundException.class);
	}

	@Test
	void shouldRegisterStartedServersAsEnvironmentVariables() {
		assertThat(this.environment.getProperty("spring.cloud.contract.stubrunner.runningstubs.loanIssuance.port"))
			.isNotNull();
		assertThat(this.stubFinder.findAllRunningStubs().getPort("loanIssuance")).isEqualTo(this.environment
			.getProperty("spring.cloud.contract.stubrunner.runningstubs.loanIssuance.port", Integer.class));
		assertThat(
				this.environment.getProperty("spring.cloud.contract.stubrunner.runningstubs.fraudDetectionServer.port"))
			.isNotNull();
		assertThat(this.stubFinder.findAllRunningStubs().getPort("fraudDetectionServer")).isEqualTo(this.environment
			.getProperty("spring.cloud.contract.stubrunner.runningstubs.fraudDetectionServer.port", Integer.class));
	}

	@Test
	void shouldBeAbleToInterpolateARunningStubInThePassedTestProperty() {
		int fraudPort = this.stubFinder.findAllRunningStubs().getPort("fraudDetectionServer");
		assertThat(fraudPort).isPositive();
		assertThat(this.environment.getProperty("foo", Integer.class)).isEqualTo(fraudPort);
		assertThat(this.environment.getProperty("fooWithGroup", Integer.class)).isEqualTo(fraudPort);
		assertThat(this.foo).isEqualTo(fraudPort);
	}

	@Test
	void shouldBeAbleToRetrieveThePortOfARunningStubViaAnAnnotation() {
		int fraudPort = this.stubFinder.findAllRunningStubs().getPort("fraudDetectionServer");
		assertThat(fraudPort).isPositive();
		assertThat(this.fraudDetectionServerPort).isEqualTo(fraudPort);
		assertThat(this.fraudDetectionServerPortWithGroupId).isEqualTo(fraudPort);
	}

	@Test
	void shouldDumpAllMappingsToAFile() {
		URL url = this.stubFinder.findStubUrl("fraudDetectionServer");
		assertThat(new File("target/outputmappings/", "fraudDetectionServer_" + url.getPort())).exists();
	}

	private static String readUrl(String url) throws Exception {
		return new String(new URL(url).openStream().readAllBytes()).trim();
	}

	@Configuration
	@EnableAutoConfiguration
	static class Config {

	}

	public static class HttpsForFraudDetection extends WireMockHttpServerStubConfigurer {

		private static final Log log = LogFactory.getLog(HttpsForFraudDetection.class);

		@Override
		public WireMockConfiguration configure(WireMockConfiguration httpStubConfiguration,
				HttpServerStubConfiguration httpServerStubConfiguration) {
			if ("fraudDetectionServer".equals(httpServerStubConfiguration.stubConfiguration.getArtifactId())) {
				int httpsPort = TestSocketUtils.findAvailableTcpPort();
				log.info("Will set HTTPs port [" + httpsPort + "] for fraud detection server");
				return httpStubConfiguration.httpsPort(httpsPort);
			}
			return httpStubConfiguration;
		}

	}

}
