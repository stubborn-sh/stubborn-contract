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

import java.net.URI;
import java.util.Objects;
import java.util.stream.Stream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * Test component that issues HTTP calls used to exercise the WireMock helpers.
 *
 * @author Dave Syer
 * @author Nikola Kološnjaji
 */
@Component
public class Service {

	private static final Log log = LogFactory.getLog(Service.class);

	@Value("${app.baseUrl:https://example.org}")
	@Nullable String base;

	private RestTemplate restTemplate;

	private RestTemplate apacheHttpClient;

	private RestTemplate apacheHttpClientWithInterceptor;

	Service(RestTemplate restTemplate, @Qualifier("apacheHttpClient") RestTemplate apacheHttpClient,
			@Qualifier("apacheHttpClientWithInterceptor") RestTemplate apacheHttpClientWithInterceptor) {
		this.restTemplate = restTemplate;
		this.apacheHttpClient = apacheHttpClient;
		this.apacheHttpClientWithInterceptor = apacheHttpClientWithInterceptor;
	}

	public String go() {
		String requestUrl = this.base + "/test";
		log.info("Will send a request to [" + requestUrl + "]");
		return Objects.requireNonNull(this.restTemplate.getForEntity(requestUrl, String.class).getBody());
	}

	public String goWithApacheClient() {
		String requestUrl = this.base + "/test";
		log.info("Will send a request to [" + requestUrl + "]");
		return Objects.requireNonNull(this.apacheHttpClient.getForEntity(requestUrl, String.class).getBody());
	}

	public String goWithApacheClientAndAdditonalInterceptor() {
		String requestUrl = this.base + "/test";
		log.info("Will send a request to [" + requestUrl + "]");
		return Objects
			.requireNonNull(this.apacheHttpClientWithInterceptor.getForEntity(requestUrl, String.class).getBody());
	}

	public String link() {
		String requestUrl = this.base + "/link";
		log.info("Will send a request to [" + requestUrl + "]");
		return Objects.requireNonNull(this.restTemplate.getForEntity(requestUrl, String.class).getBody());
	}

	public String pom() {
		String requestUrl = this.base + "/pom.xml";
		log.info("Will send a request to [" + requestUrl + "]");
		return Objects.requireNonNull(this.restTemplate
			.exchange(RequestEntity.get(URI.create(requestUrl)).accept(mediaTypes()).build(), String.class)
			.getBody());
	}

	private MediaType[] mediaTypes() {
		return Stream
			.of("text/plain", "text/plain", "application/json", "application/json", "application/*+json",
					"application/*+json", "*/*", "*/*")
			.map(MediaType::valueOf)
			.toArray(MediaType[]::new);
	}

	public String go2() {
		String requestUrl = this.base + "/test2";
		log.info("Will send a request to [" + requestUrl + "]");
		return Objects.requireNonNull(this.restTemplate.getForEntity(requestUrl, String.class).getBody());
	}

	public void setBase(String base) {
		this.base = base;
	}

}
