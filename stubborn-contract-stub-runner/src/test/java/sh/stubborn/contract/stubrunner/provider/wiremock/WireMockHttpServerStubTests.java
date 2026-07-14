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

package sh.stubborn.contract.stubrunner.provider.wiremock;

import java.io.File;
import java.net.URI;
import java.util.List;

import com.github.tomakehurst.wiremock.http.RequestMethod;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.test.TestSocketUtils;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;

import sh.stubborn.contract.stubrunner.HttpServerStubConfiguration;
import sh.stubborn.contract.stubrunner.HttpServerStubConfigurer;

import static org.assertj.core.api.Assertions.assertThat;

class WireMockHttpServerStubTests {

	private static final File MAPPING_DESCRIPTOR = new File("src/test/resources/transformers.json");

	private static final File ARBITRARY_JSON = new File("src/test/resources/sample_response.json");

	private static final File PDF = new File("src/test/resources/request.pdf");

	private static final File BROKEN_MAPPING = new File("src/test/resources/broken.json");

	@Test
	void shouldDescribeStubMapping() {
		WireMockHttpServerStub mappingDescriptor = (WireMockHttpServerStub) new WireMockHttpServerStub()
			.start(new HttpServerStubConfiguration(HttpServerStubConfigurer.NoOpHttpServerStubConfigurer.INSTANCE, null,
					null, TestSocketUtils.findAvailableTcpPort()));
		try {
			StubMapping mapping = mappingDescriptor.getMapping(MAPPING_DESCRIPTOR);
			assertThat(mapping.getRequest().getMethod()).isEqualTo(RequestMethod.GET);
			assertThat(mapping.getRequest().getUrl()).isEqualTo("/ping");
			assertThat(mapping.getResponse().getStatus()).isEqualTo(200);
			assertThat(mapping.getResponse().getBody()).isEqualTo("pong");
			assertThat(mapping.getResponse().getHeaders().getHeader("Content-Type").firstValue())
				.contains("text/plain");
			mappingDescriptor.registerMappings(List.of(MAPPING_DESCRIPTOR));
			URI uri = new URI("http://localhost:" + mappingDescriptor.port() + "/ping");
			String myHeader = new RestTemplate().exchange(uri, HttpMethod.GET, (HttpEntity<?>) null, String.class)
				.getHeaders()
				.getFirst("X-My-Header");
			assertThat(myHeader).isEqualTo("surprise!");
		}
		catch (Exception ex) {
			throw new RuntimeException(ex);
		}
		finally {
			mappingDescriptor.stop();
		}
	}

	@Disabled("There's sth wrong with SLF4J versions")
	@Test
	void shouldMakeWireMockPrintOutLogsOnInfo() {
		WireMockHttpServerStub mappingDescriptor = (WireMockHttpServerStub) new WireMockHttpServerStub()
			.start(new HttpServerStubConfiguration(HttpServerStubConfigurer.NoOpHttpServerStubConfigurer.INSTANCE, null,
					null, TestSocketUtils.findAvailableTcpPort()));
		try {
			mappingDescriptor.registerMappings(List
				.of(new File(WireMockHttpServerStubTests.class.getClassLoader().getResource("simple.json").getFile())));
			String response = new RestTemplate()
				.getForObject("http://localhost:" + mappingDescriptor.port() + "/foobar", String.class);
			assertThat(response).isEqualTo("foo");
		}
		finally {
			mappingDescriptor.stop();
		}
	}

	@Test
	void shouldAcceptAValidMapping() {
		WireMockHttpServerStub httpServerStub = new WireMockHttpServerStub();
		assertThat(httpServerStub.isAccepted(MAPPING_DESCRIPTOR)).isTrue();
	}

	@Test
	void shouldNotAcceptAnArbitraryJsonFile() {
		WireMockHttpServerStub httpServerStub = new WireMockHttpServerStub();
		assertThat(httpServerStub.isAccepted(ARBITRARY_JSON)).isFalse();
	}

	@Test
	void shouldNotAcceptABrokenMappingFile() {
		WireMockHttpServerStub httpServerStub = new WireMockHttpServerStub();
		assertThat(httpServerStub.isAccepted(BROKEN_MAPPING)).isFalse();
	}

	@Test
	void shouldNotAcceptANonJsonFile() {
		WireMockHttpServerStub httpServerStub = new WireMockHttpServerStub();
		assertThat(httpServerStub.isAccepted(PDF)).isFalse();
	}

}
