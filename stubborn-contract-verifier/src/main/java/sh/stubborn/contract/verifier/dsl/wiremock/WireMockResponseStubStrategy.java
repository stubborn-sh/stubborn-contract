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

package sh.stubborn.contract.verifier.dsl.wiremock;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.extension.Extension;
import com.github.tomakehurst.wiremock.http.HttpHeader;
import com.github.tomakehurst.wiremock.http.HttpHeaders;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import org.jspecify.annotations.Nullable;
import sh.stubborn.contract.spec.Contract;
import sh.stubborn.contract.spec.internal.DynamicString;
import sh.stubborn.contract.spec.internal.FromFileProperty;
import sh.stubborn.contract.spec.internal.Request;
import sh.stubborn.contract.spec.internal.Response;
import sh.stubborn.contract.verifier.file.SingleContractMetadata;
import sh.stubborn.contract.verifier.util.ContentType;
import sh.stubborn.contract.verifier.util.MapConverter;

/**
 * Converts a {@link Request} into {@link ResponseDefinition}.
 *
 * @author Marcin Grzejszczak
 * @author Olga Maciaszek-Sharma
 * @since 1.0.0
 */
class WireMockResponseStubStrategy extends BaseWireMockStubStrategy {

	private final Response response;

	private final ContentType contentType;

	private final SingleContractMetadata contractMetadata;

	WireMockResponseStubStrategy(Contract groovyDsl, SingleContractMetadata singleContractMetadata) {
		super(groovyDsl);
		this.response = Objects.requireNonNull(groovyDsl.getResponse());
		this.contentType = contentType(singleContractMetadata);
		this.contractMetadata = singleContractMetadata;
	}

	protected ContentType contentType(SingleContractMetadata singleContractMetadata) {
		return singleContractMetadata.getEvaluatedOutputStubContentType();
	}

	@Nullable ResponseDefinition buildClientResponseContent() {
		if (this.response == null) {
			return null;
		}
		ResponseDefinitionBuilder builder = new ResponseDefinitionBuilder()
			.withStatus((Integer) MapConverter.getStubSideValues(Objects.requireNonNull(this.response.getStatus())));
		appendHeaders(builder);
		appendBody(builder);
		appendResponseDelayTime(builder);
		builder.withTransformers(responseTransformerNames());
		return builder.build();
	}

	private String[] responseTransformerNames() {
		List<WireMockExtensions> wireMockExtensions = new ArrayList<>();
		ServiceLoader.load(WireMockExtensions.class).forEach(wireMockExtensions::add);
		if (!wireMockExtensions.isEmpty()) {
			return wireMockExtensions.stream()
				.map(WireMockExtensions::extensions)
				.flatMap(Collection::stream)
				.map(Extension::getName)
				.toArray(String[]::new);
		}
		return new String[] { new DefaultResponseTransformer().getName(), SpringCloudContractRequestMatcher.NAME };
	}

	private void appendHeaders(ResponseDefinitionBuilder builder) {
		if (this.response.getHeaders() != null) {
			HttpHeaders headers = this.response.getHeaders()
				.getEntries()
				.stream()
				.map((it) -> new HttpHeader(it.getName(),
						MapConverter.getStubSideValues(Objects.requireNonNull(it.getClientValue())).toString()))
				.collect(Collectors.collectingAndThen(Collectors.toList(), HttpHeaders::new));
			builder.withHeaders(headers);
		}
	}

	private void appendBody(ResponseDefinitionBuilder builder) {
		if (this.response.getBody() != null) {
			Object body = MapConverter.getStubSideValues(this.response.getBody(), parsingClosureForContentType());
			if (body instanceof byte[]) {
				builder.withBody((byte[]) body);
			}
			else if (body instanceof FromFileProperty && ((FromFileProperty) body).isByte()) {
				builder.withBody(((FromFileProperty) body).asBytes());
			}
			else if (body instanceof Map) {
				builder.withBody(parseBody((Map<?, ?>) body, this.contentType));
			}
			else if (body instanceof List) {
				builder.withBody(parseBody((List<?>) body, this.contentType));
			}
			else if (body instanceof DynamicString) {
				builder.withBody(parseBody((DynamicString) body, this.contentType));
			}
			else {
				builder.withBody(parseBody(body, this.contentType));
			}
		}
	}

	Function<String, Object> parsingClosureForContentType() {
		return this.contractMetadata.getDefinedOutputStubContentType().contains("/stream") ? MapConverter.IDENTITY
				: MapConverter.JSON_PARSING_FUNCTION;
	}

	private void appendResponseDelayTime(ResponseDefinitionBuilder builder) {
		// TODO: Add a missing test for this
		if (this.response.getDelay() != null) {
			builder.withFixedDelay((Integer) this.response.getDelay().getClientValue());
		}
	}

}
