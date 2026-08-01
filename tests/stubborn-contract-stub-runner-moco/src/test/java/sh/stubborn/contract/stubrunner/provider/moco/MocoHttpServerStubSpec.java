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

package sh.stubborn.contract.stubrunner.provider.moco;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import sh.stubborn.contract.stubrunner.StubFinder;
import sh.stubborn.contract.stubrunner.StubsMode;
import sh.stubborn.contract.stubrunner.spring.AutoConfigureStubRunner;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;

// tag::[classpath_stub_runner]
@SpringBootTest(classes = MocoHttpServerStubSpec.MocoConfig.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
@AutoConfigureStubRunner(ids = "com.example:fraudDetectionServerMoco", stubsMode = StubsMode.CLASSPATH)
// end::[classpath_stub_runner]
@ActiveProfiles("test")
class MocoHttpServerStubSpec {

	@Autowired
	StubFinder stubFinder;

	@Test
	@DisplayName("should successfully receive a response from a stub")
	void shouldSuccessfullyReceiveAResponseFromAStub() throws IOException {
		String url = this.stubFinder.findStubUrl("fraudDetectionServerMoco").toString();

		then(read(url + "/name")).isEqualTo("fraudDetectionServerMoco");
		then(read(url + "/bye")).isEqualTo("bye");
		then(read(url + "/bye2")).isEqualTo("bye");
		thenThrownBy(() -> read(url + "/name2")).isInstanceOf(IOException.class);
	}

	private static String read(String url) throws IOException {
		try (InputStream inputStream = URI.create(url).toURL().openStream()) {
			return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
		}
	}

	@Configuration
	@EnableAutoConfiguration
	static class MocoConfig {

	}

}
