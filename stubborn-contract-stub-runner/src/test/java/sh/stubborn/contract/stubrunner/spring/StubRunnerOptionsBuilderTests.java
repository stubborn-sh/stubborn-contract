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

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = StubRunnerOptionsBuilderTests.Config.class,
		properties = { "some.property1=sh.stubborn.contract.verifier.stubs:loanIssuance" })
@AutoConfigureStubRunner
@ActiveProfiles("test-with-placeholders")
class StubRunnerOptionsBuilderTests {

	@StubRunnerPort("fraudDetectionServer")
	int fraudDetectionServerPort;

	@StubRunnerPort("loanIssuance")
	int loanIssuancePort;

	@Value("${stub.port}")
	int stubPort;

	@Test
	void shouldResolvePlaceholders() {
		assertThat(this.fraudDetectionServerPort).isGreaterThan(1000);
		assertThat(this.loanIssuancePort).isGreaterThan(1000);
		assertThat(this.stubPort).isEqualTo(this.fraudDetectionServerPort);
	}

	@Configuration
	@EnableAutoConfiguration
	static class Config {

	}

}
