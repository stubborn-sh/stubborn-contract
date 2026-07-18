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

package sh.stubborn.contract.stubrunner;

import org.junit.jupiter.api.Test;
import sh.stubborn.contract.stubrunner.spring.AutoConfigureStubRunner;
import sh.stubborn.contract.stubrunner.spring.StubRunnerProperties;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests that stub runner specific auto-configuration can be loaded up in combination with
 * other slice tests
 *
 * @author Biju Kunjummen
 */
@WebMvcTest
@AutoConfigureStubRunner(
		ids = { "sh.stubborn.contract.verifier.stubs:loanIssuance:+:stubs",
				"sh.stubborn.contract.verifier.stubs:fraudDetectionServer:+:stubs" },
		minPort = 10001, maxPort = 10020, mappingsOutputFolder = "target/outputmappings/",
		properties = { "hello=world", "foo=bar" })
@ActiveProfiles("test")
public class StubRunnerSliceTests {

	@Autowired
	private StubFinder stubFinder;

	@Autowired
	private StubRunnerProperties properties;

	@Value("${spring.cloud.contract.stubrunner.runningstubs.fraudDetectionServer.port}")
	private Integer fraudDetectionServerPort;

	@Value("${spring.cloud.contract.stubrunner.runningstubs.loanIssuance.port}")
	private Integer loanIssuancePort;

	@Test
	public void testThatListedStubsAreRunning() {

		assertThat(this.fraudDetectionServerPort).isBetween(10001, 10020);
		assertThat(this.loanIssuancePort).isBetween(10001, 10020);

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
		assertThat(this.properties.getProperties()).containsEntry("hello", "world").containsEntry("foo", "bar");
	}

	@SpringBootConfiguration
	static class Config {

	}

}
