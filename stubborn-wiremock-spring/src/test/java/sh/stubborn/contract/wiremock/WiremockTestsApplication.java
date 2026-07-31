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

package sh.stubborn.contract.wiremock;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.support.BasicAuthenticationInterceptor;
import org.springframework.web.client.RestTemplate;

/**
 * Test application that wires up the {@link RestTemplate} beans used by the WireMock
 * helper tests.
 *
 * @author Dave Syer
 * @author Nikola Kološnjaji
 *
 */
@Configuration
@EnableAutoConfiguration
@Import(Service.class)
public class WiremockTestsApplication {

	public static void main(String[] args) {
		SpringApplication.run(WiremockTestsApplication.class, args);
	}

	@Bean
	@Primary
	public RestTemplate restTemplate() {
		return new RestTemplate();
	}

	@Bean
	public RestTemplate apacheHttpClient(RestTemplateBuilder builder) {
		return builder.requestFactory(() -> new HttpComponentsClientHttpRequestFactory()).build();
	}

	@Bean
	public RestTemplate apacheHttpClientWithInterceptor(RestTemplateBuilder builder) {
		return builder.requestFactory(() -> new HttpComponentsClientHttpRequestFactory())
			.additionalInterceptors(new BasicAuthenticationInterceptor("u", "p"))
			.build();
	}

}
