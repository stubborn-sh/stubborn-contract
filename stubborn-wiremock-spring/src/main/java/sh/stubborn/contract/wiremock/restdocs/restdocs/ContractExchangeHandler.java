/*
 * Copyright 2016-present the original author or authors.
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

package sh.stubborn.contract.wiremock.restdocs;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.http.Request;
import org.jspecify.annotations.Nullable;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.restdocs.webtestclient.WebTestClientRestDocumentationConfigurer;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.ReflectionUtils;

/**
 * WireMock verify helper that adapts a {@link EntityExchangeResult} produced by
 * {@link WebTestClient} into a WireMock stub definition.
 *
 * @author Dave Syer
 *
 */
public class ContractExchangeHandler extends WireMockVerifyHelper<EntityExchangeResult<?>, ContractExchangeHandler>
		implements Consumer<EntityExchangeResult<byte[]>> {

	@Override
	public void accept(EntityExchangeResult<byte[]> result) {
		configure(result);
	}

	@Override
	protected ResponseDefinitionBuilder getResponseDefinition(EntityExchangeResult<?> result) {
		ResponseDefinitionBuilder definition = ResponseDefinitionBuilder.responseDefinition()
			.withBody(result.getResponseBodyContent())
			.withStatus(result.getStatus().value());
		addResponseHeaders(definition, result.getResponseHeaders());
		return definition;
	}

	private void addResponseHeaders(ResponseDefinitionBuilder definition, HttpHeaders httpHeaders) {
		for (String name : httpHeaders.headerNames()) {
			List<String> values = httpHeaders.get(name);
			if (values != null) {
				definition.withHeader(name, values.toArray(new String[0]));
			}
		}
	}

	@Override
	protected Map<String, Object> getConfiguration(EntityExchangeResult<?> result) {
		Field field = ReflectionUtils.findField(WebTestClientRestDocumentationConfigurer.class, "configurations");
		if (field == null) {
			return new HashMap<>();
		}
		ReflectionUtils.makeAccessible(field);
		String index = result.getRequestHeaders().getFirst(WebTestClient.WEBTESTCLIENT_REQUEST_ID);
		@SuppressWarnings("unchecked")
		Map<String, Map<String, Object>> configurations = (Map<String, Map<String, Object>>) ReflectionUtils
			.getField(field, null);
		if (configurations == null) {
			return new HashMap<>();
		}
		Map<String, Object> map = configurations.get(index);
		if (map == null) {
			return new HashMap<>();
		}
		return map;
	}

	@Override
	protected Request getWireMockRequest(EntityExchangeResult<?> result) {
		return new WireMockHttpRequestAdapter(result);
	}

	@Override
	protected @Nullable MediaType getContentType(EntityExchangeResult<?> result) {
		return result.getRequestHeaders().getContentType();
	}

	@Override
	protected byte[] getRequestBodyContent(EntityExchangeResult<?> result) {
		byte[] content = result.getRequestBodyContent();
		return (content != null) ? content : new byte[0];
	}

}
