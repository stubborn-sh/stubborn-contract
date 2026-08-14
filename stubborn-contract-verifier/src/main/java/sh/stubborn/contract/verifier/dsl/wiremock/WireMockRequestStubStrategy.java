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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.http.RequestMethod;
import com.github.tomakehurst.wiremock.matching.ContentPattern;
import com.github.tomakehurst.wiremock.matching.RequestPattern;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import com.github.tomakehurst.wiremock.matching.StringValuePattern;
import com.github.tomakehurst.wiremock.matching.UrlPattern;
import org.apache.commons.text.StringEscapeUtils;
import org.jspecify.annotations.Nullable;
import sh.stubborn.contract.spec.Contract;
import sh.stubborn.contract.spec.internal.Body;
import sh.stubborn.contract.spec.internal.BodyMatcher;
import sh.stubborn.contract.spec.internal.BodyMatchers;
import sh.stubborn.contract.spec.internal.ClientDslProperty;
import sh.stubborn.contract.spec.internal.DslProperty;
import sh.stubborn.contract.spec.internal.DynamicString;
import sh.stubborn.contract.spec.internal.FromFileProperty;
import sh.stubborn.contract.spec.internal.MatchingStrategy;
import sh.stubborn.contract.spec.internal.MatchingType;
import sh.stubborn.contract.spec.internal.NamedProperty;
import sh.stubborn.contract.spec.internal.OptionalProperty;
import sh.stubborn.contract.spec.internal.PathBodyMatcher;
import sh.stubborn.contract.spec.internal.QueryParameters;
import sh.stubborn.contract.spec.internal.RegexPatterns;
import sh.stubborn.contract.spec.internal.RegexProperty;
import sh.stubborn.contract.spec.internal.Request;
import sh.stubborn.contract.spec.internal.Url;
import sh.stubborn.contract.verifier.converter.YamlContract;
import sh.stubborn.contract.verifier.converter.YamlContractConverter;
import sh.stubborn.contract.verifier.dsl.ContractVerifierMetadata;
import sh.stubborn.contract.verifier.file.SingleContractMetadata;
import sh.stubborn.contract.verifier.util.ContentType;
import sh.stubborn.contract.verifier.util.ContentUtils;
import sh.stubborn.contract.verifier.util.JsonPaths;
import sh.stubborn.contract.verifier.util.JsonToJsonPathsConverter;
import sh.stubborn.contract.verifier.util.MapConverter;
import sh.stubborn.contract.verifier.util.MethodBufferingJsonVerifiable;
import sh.stubborn.contract.verifier.util.RegexpBuilders;
import sh.stubborn.contract.verifier.util.xml.XmlToXPathsConverter;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Converts a {@link Request} into {@link RequestPattern}.
 *
 * @author Marcin Grzejszczak
 * @author Tim Ysewyn
 * @author Olga Maciaszek-Sharma
 * @since 1.0.0
 */
class WireMockRequestStubStrategy extends BaseWireMockStubStrategy {

	private final Request request;

	private final ContentType contentType;

	WireMockRequestStubStrategy(Contract contractDsl, SingleContractMetadata singleContractMetadata) {
		super(contractDsl);
		this.request = Objects.requireNonNull(contractDsl.getRequest());
		this.contentType = contentType(singleContractMetadata);
	}

	protected ContentType contentType(SingleContractMetadata singleContractMetadata) {
		return singleContractMetadata.getEvaluatedInputStubContentType();
	}

	@Nullable RequestPattern buildClientRequestContent() {
		if (this.request == null) {
			return null;
		}
		RequestPatternBuilder requestPatternBuilder = Objects.requireNonNull(appendMethodAndUrl());
		appendCookies(requestPatternBuilder);
		appendHeaders(requestPatternBuilder);
		appendQueryParameters(requestPatternBuilder);
		appendBody(requestPatternBuilder);
		appendMultipart(requestPatternBuilder);
		return requestPatternBuilder.build();
	}

	private void appendBody(RequestPatternBuilder requestPatternBuilder) {
		if (contract.getMetadata().containsKey(ContractVerifierMetadata.METADATA_KEY)) {
			ContractVerifierMetadata metadata = ContractVerifierMetadata.fromMetadata(contract.getMetadata());
			appendSpringCloudContractMatcher(metadata, requestPatternBuilder);
			if (metadata.getTool() == null || metadata.getTool().isEmpty()) {
				doAppendBody(requestPatternBuilder);
			}
		}
		else {
			doAppendBody(requestPatternBuilder);
		}
	}

	private void appendSpringCloudContractMatcher(ContractVerifierMetadata metadata,
			RequestPatternBuilder requestPatternBuilder) {
		Parameters parameters = Parameters.one("tool", (metadata.getTool() != null) ? metadata.getTool() : "unknown");
		YamlContractConverter converter = new YamlContractConverter();
		List<YamlContract> contracts = converter.convertTo(Collections.singleton(contract));
		Map<String, byte[]> store = converter.store(contracts);
		parameters.put("contract", new String(store.entrySet().iterator().next().getValue()));
		requestPatternBuilder.andMatching(SpringCloudContractRequestMatcher.NAME, parameters);
	}

	private @Nullable RequestPatternBuilder appendMethodAndUrl() {
		if (this.request.getMethod() == null) {
			return null;
		}
		RequestMethod requestMethod = RequestMethod.fromString(
				Optional.ofNullable(this.request.getMethod().getClientValue()).map((c) -> c.toString()).orElse(null));
		UrlPattern urlPattern = urlPattern();
		return RequestPatternBuilder.newRequestPattern(requestMethod, urlPattern);
	}

	private void doAppendBody(RequestPatternBuilder requestPattern) {
		if (this.request.getBody() == null) {
			return;
		}
		boolean bodyHasMatchingStrategy = this.request.getBody().getClientValue() instanceof MatchingStrategy;
		MatchingStrategy matchingStrategy = getMatchingStrategyFromBody(this.request.getBody());
		if (this.contentType == ContentType.JSON) {
			Object clientSideBody = MapConverter.transformToClientValues(this.request.getBody());
			Object originalBody = Objects
				.requireNonNull(Optional.ofNullable(matchingStrategy).map(DslProperty::getClientValue).orElse(null));
			if (bodyHasMatchingStrategy) {
				requestPattern.withRequestBody(convertToValuePattern(matchingStrategy));
			}
			else if (clientSideBody instanceof Pattern || clientSideBody instanceof RegexProperty) {
				requestPattern.withRequestBody(
						convertToValuePattern(appendBodyRegexpMatchPattern(this.request.getBody(), this.contentType)));
			}
			else {
				BodyMatchers requestBodyMatchers = this.request.getBodyMatchers();
				Object body = JsonToJsonPathsConverter.removeMatchingJsonPaths(originalBody,
						(requestBodyMatchers != null) ? requestBodyMatchers : new BodyMatchers());
				JsonPaths values = JsonToJsonPathsConverter
					.transformToJsonPathWithStubsSideValuesAndNoArraySizeCheck(body);
				if ((values.isEmpty() && this.request.getBodyMatchers() != null
						&& !this.request.getBodyMatchers().hasMatchers()) || onlySizeAssertionsArePresent(values)) {
					try {
						requestPattern.withRequestBody(WireMock.equalToJson(new JsonMapper().writeValueAsString(
								getMatchingStrategy(Objects.requireNonNull(this.request.getBody().getClientValue()))
									.getClientValue()),
								false, false));
					}
					catch (JacksonException ex) {
						throw new IllegalArgumentException("The MatchingStrategy could not be serialized", ex);
					}
				}
				else {
					values.stream()
						.filter((v) -> !v.assertsSize())
						.forEach((it) -> requestPattern
							.withRequestBody(WireMock.matchingJsonPath(it.jsonPath().replace("\\\\", "\\"))));
				}
			}
			Optional.ofNullable(this.request.getBodyMatchers())
				.map(BodyMatchers::matchers)
				.ifPresent((bodyMatchers) -> bodyMatchers.forEach((bodyMatcher) -> {
					String newPath = JsonToJsonPathsConverter.convertJsonPathAndRegexToAJsonPath(bodyMatcher,
							originalBody);
					requestPattern.withRequestBody(WireMock.matchingJsonPath(newPath.replace("\\\\", "\\")));
				}));
		}
		else if (this.contentType == ContentType.XML) {
			Object originalBody = Objects
				.requireNonNull(Optional.ofNullable(matchingStrategy).map(DslProperty::getClientValue).orElse(null));
			if (bodyHasMatchingStrategy) {
				requestPattern.withRequestBody(convertToValuePattern(matchingStrategy));
			}
			else {
				Object body = XmlToXPathsConverter.removeMatchingXPaths(originalBody, this.request.getBodyMatchers());
				List<BodyMatcher> byEqualityMatchersFromXml = XmlToXPathsConverter.mapToMatchers(body);
				byEqualityMatchersFromXml.forEach(
						(bodyMatcher) -> addWireMockStubMatchingSection(bodyMatcher, requestPattern, originalBody));
			}
			Optional.ofNullable(this.request.getBodyMatchers())
				.map(BodyMatchers::matchers)
				.ifPresent((bodyMatchers) -> bodyMatchers.forEach(
						(bodyMatcher) -> addWireMockStubMatchingSection(bodyMatcher, requestPattern, originalBody)));
		}
		else if (containsPattern(this.request.getBody())) {
			requestPattern.withRequestBody(convertToValuePattern(appendBodyRegexpMatchPattern(this.request.getBody())));
		}
		else {
			requestBodyGuessedFromMatchingStrategy(requestPattern);
		}
	}

	private Object generateConcreteValue(Object originalBody) {
		if (originalBody instanceof Pattern || originalBody instanceof RegexProperty) {
			return new RegexProperty(originalBody).generate();
		}
		return originalBody;
	}

	private RequestPatternBuilder requestBodyGuessedFromMatchingStrategy(RequestPatternBuilder requestPattern) {
		Body body = Objects.requireNonNull(this.request.getBody());
		return requestPattern
			.withRequestBody(convertToValuePattern(getMatchingStrategy(Objects.requireNonNull(body.getClientValue()))));
	}

	private static void addWireMockStubMatchingSection(BodyMatcher matcher, RequestPatternBuilder requestPattern,
			Object body) {
		Set<MatchingType> matchingTypesUnsupportedForRequest = new HashSet<>(
				Arrays.asList(MatchingType.NULL, MatchingType.COMMAND, MatchingType.TYPE));
		if (!(matcher instanceof PathBodyMatcher)) {
			throw new IllegalArgumentException("Only jsonPath and XPath matchers can be processed.");
		}
		String retrievedValue = Optional.ofNullable(matcher.value()).map(Object::toString).orElseGet(() -> {
			if (matchingTypesUnsupportedForRequest.contains(matcher.matchingType())) {
				throw new IllegalArgumentException("Null, Command and Type matchers are not supported in requests.");
			}
			if (MatchingType.EQUALITY == matcher.matchingType()) {
				return XmlToXPathsConverter.retrieveValue(matcher, body);
			}
			else {
				return "";
			}
		});
		PathBodyMatcher pathMatcher = (PathBodyMatcher) matcher;
		requestPattern
			.withRequestBody(WireMock.matchingXPath(pathMatcher.path(), XPathBodyMatcherToWireMockValuePatternConverter
				.mapToPattern(Objects.requireNonNull(pathMatcher.matchingType()), String.valueOf(retrievedValue))));
	}

	private boolean onlySizeAssertionsArePresent(JsonPaths values) {
		return values != null && !values.isEmpty()
				&& (this.request.getBodyMatchers() == null || !this.request.getBodyMatchers().hasMatchers())
				&& this.every(values.iterator(), MethodBufferingJsonVerifiable::assertsSize);
	}

	private <T> boolean every(Iterator<T> self, Function<T, Boolean> function) {
		while (self.hasNext()) {
			if (!function.apply(self.next())) {
				return false;
			}
		}
		return true;
	}

	private void appendMultipart(RequestPatternBuilder requestPattern) {
		if (this.request.getMultipart() == null) {
			return;
		}
		if (this.request.getMultipart().getClientValue() instanceof Map) {
			List<StringValuePattern> multipartPattern = ((Map<?, ?>) this.request.getMultipart().getClientValue())
				.entrySet()
				.stream()
				.map((it) -> {
					if (it.getValue() instanceof NamedProperty) {
						NamedProperty namedProperty = (NamedProperty) it.getValue();
						return WireMock.matching(RegexPatterns.multipartFile(it.getKey(),
								Objects
									.requireNonNull(Objects.requireNonNull(namedProperty.getName()).getClientValue()),
								Objects
									.requireNonNull(Objects.requireNonNull(namedProperty.getValue()).getClientValue()),
								Optional.ofNullable(namedProperty.getContentType())
									.map(DslProperty::getClientValue)
									.orElse(".*")));
					}
					return WireMock.matching(RegexPatterns.multipartParam(it.getKey(),
							MapConverter.getStubSideValuesForNonBody(it.getValue())));
				})
				.collect(Collectors.toList());
			multipartPattern.forEach(requestPattern::withRequestBody);

		}

	}

	private void appendHeaders(RequestPatternBuilder requestPattern) {
		if (this.request.getHeaders() != null) {
			this.request.getHeaders()
				.getEntries()
				.forEach((header) -> requestPattern.withHeader(header.getName(),
						(StringValuePattern) convertToValuePattern(header.getClientValue())));
		}
	}

	private void appendCookies(RequestPatternBuilder requestPattern) {
		if (this.request.getCookies() == null) {
			return;
		}
		this.request.getCookies()
			.getEntries()
			.forEach((cookie) -> requestPattern.withCookie(cookie.getKey(),
					(StringValuePattern) convertToValuePattern(cookie.getClientValue())));
	}

	private UrlPattern urlPattern() {
		Object urlPath = urlPathOrUrlIfQueryPresent();
		if (urlPath != null) {
			if (urlPath instanceof Pattern || urlPath instanceof RegexProperty) {
				return WireMock.urlPathMatching((String) getStubSideValue(new RegexProperty(urlPath).pattern()));
			}
			else {
				return WireMock.urlPathEqualTo((String) getStubSideValue(urlPath.toString()));
			}
		}
		if (this.request.getUrl() == null) {
			throw new IllegalStateException("URL is required!");
		}
		Object url = getUrlIfGstring(Objects.requireNonNull(this.request.getUrl().getClientValue()));
		if (url instanceof Pattern || url instanceof RegexProperty) {
			return WireMock.urlMatching(new RegexProperty(url).pattern());
		}
		return WireMock.urlEqualTo(url.toString());
	}

	private @Nullable Object urlPathOrUrlIfQueryPresent() {
		Object urlPath = Optional.ofNullable(this.request)
			.map(Request::getUrlPath)
			.map(DslProperty::getClientValue)
			.orElse(null);
		Object queryParamsFromUrl = Optional.ofNullable(this.request)
			.map(Request::getUrl)
			.map(Url::getQueryParameters)
			.map(QueryParameters::getParameters)
			.orElse(null);
		if (urlPath != null) {
			return urlPath;
		}
		if (queryParamsFromUrl != null) {
			return Optional.ofNullable(this.request).map(Request::getUrl).map(Url::getClientValue).orElse(null);
		}
		return null;
	}

	private Object getUrlIfGstring(Object clientSide) {
		if (clientSide instanceof DynamicString) {
			if (Arrays.stream(((DynamicString) clientSide).getValues()).anyMatch((it) -> {
				Object value = getStubSideValue(it);
				return value instanceof Pattern || value instanceof RegexProperty;
			})) {
				String string = getStubSideValue(clientSide).toString();
				return new RegexProperty(Pattern.compile(string));
			}
			else {
				return getStubSideValue(clientSide).toString();
			}
		}
		return clientSide;
	}

	private void appendQueryParameters(RequestPatternBuilder requestPattern) {
		QueryParameters queryParameters = Optional.ofNullable(this.request)
			.map(Request::getUrlPath)
			.map(Url::getQueryParameters)
			.orElseGet(() -> Optional.ofNullable(this.request)
				.map(Request::getUrl)
				.map(Url::getQueryParameters)
				.orElse(null));

		Optional.ofNullable(queryParameters)
			.map(QueryParameters::getParameters)
			.ifPresent(
					(parameters) -> parameters.forEach((parameter) -> requestPattern.withQueryParam(parameter.getName(),
							(StringValuePattern) convertToValuePattern(parameter.getClientValue()))));
	}

	protected ContentPattern<?> convertToValuePattern(@Nullable Object object) {
		if (object instanceof ClientDslProperty) {
			object = ((ClientDslProperty) object).getClientValue();
		}

		if (object instanceof Pattern || object instanceof RegexProperty) {
			return WireMock.matching(new RegexProperty(object).pattern());
		}
		else if (object instanceof OptionalProperty) {
			return WireMock.matching(((OptionalProperty) object).optionalPattern());
		}
		else if (object instanceof MatchingStrategy) {
			MatchingStrategy value = (MatchingStrategy) object;
			switch (value.getType()) {
				case NOT_MATCHING:
					return WireMock.notMatching(Objects.requireNonNull(value.getClientValue()).toString());
				case ABSENT:
					return WireMock.absent();
				case EQUAL_TO:
					return WireMock.equalTo(
							clientBody(Objects.requireNonNull(value.getClientValue()), this.contentType).toString());
				case CONTAINS:
					return WireMock.containing(
							clientBody(Objects.requireNonNull(value.getClientValue()), this.contentType).toString());
				case MATCHING:
					return WireMock.matching(
							clientBody(Objects.requireNonNull(value.getClientValue()), this.contentType).toString());
				case EQUAL_TO_JSON:
					return WireMock.equalToJson(
							clientBody(Objects.requireNonNull(value.getClientValue()), this.contentType).toString());
				case EQUAL_TO_XML:
					return WireMock.equalToXml(
							clientBody(Objects.requireNonNull(value.getClientValue()), this.contentType).toString());
				case BINARY_EQUAL_TO:
					return WireMock.binaryEqualTo(
							(byte[]) clientBody(Objects.requireNonNull(value.getClientValue()), this.contentType));
				default:
					throw new UnsupportedOperationException("Unknown matching strategy " + value.getType());
			}
		}
		else {
			return WireMock.equalTo(clientBody(Objects.requireNonNull(object), this.contentType).toString());
		}
	}

	protected Object clientBody(Object bodyValue, ContentType contentType) {
		if (ContentType.FORM == contentType) {
			if (bodyValue instanceof Map) {
				// [a:3, b:4] == "a=3&b=4"
				return ((Map<?, ?>) bodyValue).entrySet()
					.stream()
					.map((e) -> StringEscapeUtils.unescapeEcmaScript(e.getKey().toString() + "=" + e.getValue()))
					.collect(Collectors.joining("&"));
			}
			else if (bodyValue instanceof List) {
				// ["a=3", "b=4"] == "a=3&b=4"
				return ((List<?>) bodyValue).stream()
					.map((it) -> StringEscapeUtils.unescapeEcmaScript(it.toString()))
					.collect(Collectors.joining("&"));
			}
		}
		else if (bodyValue instanceof FromFileProperty) {
			return ((FromFileProperty) bodyValue).isByte() ? ((FromFileProperty) bodyValue).asBytes()
					: ((FromFileProperty) bodyValue).asString();
		}
		else if (ContentType.JSON == contentType) {
			return parseBody(bodyValue, contentType);
		}
		return bodyValue;
	}

	private @Nullable MatchingStrategy getMatchingStrategyFromBody(Body body) {
		if (body == null) {
			return null;
		}
		return getMatchingStrategy(Objects.requireNonNull(body.getClientValue()));
	}

	private MatchingStrategy getMatchingStrategy(Object bodyValue) {
		if (bodyValue instanceof DynamicString) {
			return this.getMatchingStrategy((DynamicString) bodyValue);
		}
		else if (bodyValue instanceof MatchingStrategy) {
			return this.getMatchingStrategy((MatchingStrategy) bodyValue);
		}
		else if (bodyValue instanceof FromFileProperty) {
			return this.getMatchingStrategy((FromFileProperty) bodyValue);
		}
		else {
			return tryToFindMachingStrategy(bodyValue);
		}
	}

	private MatchingStrategy getMatchingStrategy(FromFileProperty bodyValue) {
		return new MatchingStrategy(bodyValue, MatchingStrategy.Type.BINARY_EQUAL_TO);
	}

	private MatchingStrategy getMatchingStrategy(MatchingStrategy matchingStrategy) {
		return getMatchingStrategyIncludingContentType(matchingStrategy);
	}

	private MatchingStrategy getMatchingStrategy(DynamicString gString) {
		if (gString == null) {
			return new MatchingStrategy("", MatchingStrategy.Type.EQUAL_TO);
		}
		Object extractedValue = ContentUtils.extractValue(gString, (it) -> Objects.requireNonNull(
				(it instanceof DslProperty) ? ((DslProperty<?>) it).getClientValue() : getStringFromGString(it)));

		Object value = getStringFromGString(extractedValue);
		return getMatchingStrategy(value);
	}

	private Object getStringFromGString(Object object) {
		return (object instanceof DynamicString) ? object.toString() : object;
	}

	private MatchingStrategy tryToFindMachingStrategy(Object bodyValue) {
		return new MatchingStrategy(MapConverter.transformToClientValues(bodyValue),
				ContentUtils.getEqualsTypeFromContentType(this.contentType));
	}

	private MatchingStrategy getMatchingStrategyIncludingContentType(MatchingStrategy matchingStrategy) {
		MatchingStrategy.Type type = matchingStrategy.getType();
		Object value = matchingStrategy.getClientValue();
		ContentType contentType = ContentUtils.recognizeContentTypeFromMatchingStrategy(type);
		if (contentType == ContentType.UNKNOWN && type == MatchingStrategy.Type.EQUAL_TO) {
			contentType = ContentUtils.recognizeContentTypeFromContent(value);
			type = ContentUtils.getEqualsTypeFromContentType(contentType);
		}
		MatchingStrategy newMatchingStrategy;
		if (value instanceof Map) {
			newMatchingStrategy = new MatchingStrategy(parseBody((Map<?, ?>) value, contentType), type);
		}
		else if (value instanceof List) {
			newMatchingStrategy = new MatchingStrategy(parseBody((List<?>) value, contentType), type);
		}
		else if (value instanceof DynamicString) {
			newMatchingStrategy = new MatchingStrategy(parseBody((DynamicString) value, contentType), type);
		}
		else {
			newMatchingStrategy = new MatchingStrategy(parseBody(Objects.requireNonNull(value), contentType), type);
		}
		return newMatchingStrategy;
	}

	private MatchingStrategy appendBodyRegexpMatchPattern(Object value, ContentType contentType) {
		Object clientValue = MapConverter.transformToClientValues(value);
		switch (contentType) {
			case JSON:
				return new MatchingStrategy(RegexpBuilders.buildJSONRegexpMatch(clientValue),
						MatchingStrategy.Type.MATCHING);
			case UNKNOWN:
				return new MatchingStrategy(RegexpBuilders.buildGStringRegexpForStubSide(clientValue),
						MatchingStrategy.Type.MATCHING);
			default:
				throw new IllegalStateException(contentType.name() + " pattern matching is not implemented yet");
		}
	}

	private MatchingStrategy appendBodyRegexpMatchPattern(Object value) {
		return appendBodyRegexpMatchPattern(value, ContentType.UNKNOWN);
	}

	private boolean containsPattern(@Nullable Object o) {
		if (o instanceof DynamicString) {
			return containsPattern(((DynamicString) o).getValues());
		}
		else if (o instanceof Map) {
			return containsPattern(((Map<?, ?>) o).entrySet());
		}
		else if (o instanceof Collection) {
			List<Boolean> result = (List<Boolean>) ((Collection) o).stream()
				.map(this::containsPattern)
				.collect(Collectors.toList());
			return result.stream().reduce(false, (a, b) -> a || b);
		}
		else if (o instanceof Object[]) {
			return containsPattern(Arrays.asList((Object[]) o));
		}
		else if (o instanceof Map.Entry<?, ?>) {
			return containsPattern(((Map.Entry<?, ?>) o).getValue());
		}
		else if (o instanceof RegexProperty) {
			return true;
		}
		else if (o instanceof DslProperty<?>) {
			return containsPattern(((DslProperty<?>) o).getClientValue());
		}
		else {
			return o instanceof Pattern;
		}
	}

}
