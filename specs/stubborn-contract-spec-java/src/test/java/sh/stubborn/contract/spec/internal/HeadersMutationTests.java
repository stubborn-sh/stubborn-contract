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

package sh.stubborn.contract.spec.internal;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("NullAway")
class HeadersMutationTests {

	@Test
	void headerKeyValueAddsEntry() {
		Headers headers = new Headers();
		headers.header("Accept", "application/json");
		assertThat(headers.getEntries()).extracting(Header::getName).containsExactly("Accept");
	}

	@Test
	void headerFromMapAddsFirstEntry() {
		Headers headers = new Headers();
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("X-Key", "val");
		headers.header(map);
		assertThat(headers.getEntries()).extracting(Header::getName).containsExactly("X-Key");
	}

	@Test
	void headerFromEmptyMapAddsNothing() {
		Headers headers = new Headers();
		headers.header(new LinkedHashMap<>());
		assertThat(headers.getEntries()).isEmpty();
	}

	@Test
	void headersAddsAll() {
		Headers headers = new Headers();
		Set<Header> set = new LinkedHashSet<>();
		set.add(Header.build("A", "1"));
		set.add(Header.build("B", "2"));
		headers.headers(set);
		assertThat(headers.getEntries()).extracting(Header::getName).containsExactlyInAnyOrder("A", "B");
	}

	@Test
	void executeForEachHeaderVisitsAll() {
		Headers headers = new Headers();
		headers.header("A", "1");
		headers.header("B", "2");
		List<String> names = new ArrayList<>();
		headers.executeForEachHeader((header) -> names.add(header.getName()));
		assertThat(names).containsExactlyInAnyOrder("A", "B");
	}

	@Test
	void acceptAddsAcceptHeader() {
		Headers headers = new Headers();
		headers.accept("application/json");
		assertThat(headers.getEntries()).extracting(Header::getName).containsExactly(HttpHeaders.ACCEPT);
	}

	@Test
	void contentTypeAddsContentTypeHeader() {
		Headers headers = new Headers();
		headers.contentType("application/json");
		assertThat(headers.getEntries()).extracting(Header::getName).containsExactly(HttpHeaders.CONTENT_TYPE);
	}

	@Test
	void messagingContentTypeAddsHeader() {
		Headers headers = new Headers();
		headers.messagingContentType("application/json");
		assertThat(headers.getEntries()).extracting(Header::getName)
			.containsExactly(MessagingHeaders.MESSAGING_CONTENT_TYPE);
	}

	@Test
	void matchingReturnsDslPropertyWithValue() {
		DslProperty property = new Headers().matching("application/json");
		assertThat(property.getClientValue()).isEqualTo("application/json");
		assertThat(property.getServerValue()).isEqualTo("application/json");
	}

	@Test
	void asStubSideMapContainsClientValues() {
		Headers headers = new Headers();
		headers.header("A", "1");
		assertThat(headers.asStubSideMap()).containsExactly(Map.entry("A", "1"));
	}

	@Test
	void asTestSideMapContainsServerValues() {
		Headers headers = new Headers();
		headers.header("A", "1");
		assertThat(headers.asTestSideMap()).containsExactly(Map.entry("A", "1"));
	}

	@Test
	void asMapUsesProvidedFunction() {
		Headers headers = new Headers();
		headers.header("A", "1");
		Map<String, Object> map = headers.asMap((name, header) -> name + "!");
		assertThat(map).containsExactly(Map.entry("A", "A!"));
	}

	@Test
	void equalsAndHashCode() {
		Headers a = new Headers();
		a.header("A", "1");
		Headers b = new Headers();
		b.header("A", "1");
		assertThat(a).isEqualTo(b);
		assertThat(a).hasSameHashCodeAs(b);
		assertThat(a.equals(a)).isTrue();
		assertThat(a.equals(null)).isFalse();
		assertThat(a.equals("x")).isFalse();
	}

	@Test
	void notEqualWhenEntriesDiffer() {
		Headers a = new Headers();
		a.header("A", "1");
		Headers b = new Headers();
		b.header("A", "2");
		assertThat(a).isNotEqualTo(b);
		assertThat(a.hashCode()).isNotEqualTo(b.hashCode());
	}

	@Test
	void toStringContainsEntries() {
		Headers headers = new Headers();
		headers.header("A", "1");
		assertThat(headers.toString()).contains("Headers").contains("A");
	}

	@Test
	void setEntriesReplacesEntries() {
		Headers headers = new Headers();
		Set<Header> set = new LinkedHashSet<>();
		set.add(Header.build("Z", "9"));
		headers.setEntries(set);
		assertThat(headers.getEntries()).extracting(Header::getName).containsExactly("Z");
	}

	@Test
	void headerConstantAccessors() {
		Headers h = new Headers();
		assertThat(h.messagingContentType()).isEqualTo(MessagingHeaders.MESSAGING_CONTENT_TYPE);
		assertThat(h.accept()).isEqualTo(HttpHeaders.ACCEPT);
		assertThat(h.acceptCharset()).isEqualTo(HttpHeaders.ACCEPT_CHARSET);
		assertThat(h.acceptEncoding()).isEqualTo(HttpHeaders.ACCEPT_ENCODING);
		assertThat(h.acceptLanguage()).isEqualTo(HttpHeaders.ACCEPT_LANGUAGE);
		assertThat(h.acceptRanges()).isEqualTo(HttpHeaders.ACCEPT_RANGES);
		assertThat(h.accessControlAllowCredentials()).isEqualTo(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS);
		assertThat(h.accessControlAllowHeaders()).isEqualTo(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS);
		assertThat(h.accessControlAllowMethods()).isEqualTo(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS);
		assertThat(h.accessControlAllowOrigin()).isEqualTo(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN);
		assertThat(h.accessControlExposeHeaders()).isEqualTo(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS);
		assertThat(h.accessControlMaxAge()).isEqualTo(HttpHeaders.ACCESS_CONTROL_MAX_AGE);
		assertThat(h.accessControlRequestHeaders()).isEqualTo(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS);
		assertThat(h.accessControlRequestMethod()).isEqualTo(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD);
		assertThat(h.age()).isEqualTo(HttpHeaders.AGE);
		assertThat(h.allow()).isEqualTo(HttpHeaders.ALLOW);
		assertThat(h.authorization()).isEqualTo(HttpHeaders.AUTHORIZATION);
		assertThat(h.cacheControl()).isEqualTo(HttpHeaders.CACHE_CONTROL);
		assertThat(h.connection()).isEqualTo(HttpHeaders.CONNECTION);
		assertThat(h.contentEncoding()).isEqualTo(HttpHeaders.CONTENT_ENCODING);
		assertThat(h.contentDisposition()).isEqualTo(HttpHeaders.CONTENT_DISPOSITION);
		assertThat(h.contentLanguage()).isEqualTo(HttpHeaders.CONTENT_LANGUAGE);
		assertThat(h.contentLength()).isEqualTo(HttpHeaders.CONTENT_LENGTH);
		assertThat(h.contentLocation()).isEqualTo(HttpHeaders.CONTENT_LOCATION);
		assertThat(h.contentRange()).isEqualTo(HttpHeaders.CONTENT_RANGE);
		assertThat(h.contentType()).isEqualTo(HttpHeaders.CONTENT_TYPE);
		assertThat(h.cookie()).isEqualTo(HttpHeaders.COOKIE);
		assertThat(h.date()).isEqualTo(HttpHeaders.DATE);
		assertThat(h.etag()).isEqualTo(HttpHeaders.ETAG);
		assertThat(h.expect()).isEqualTo(HttpHeaders.EXPECT);
		assertThat(h.expires()).isEqualTo(HttpHeaders.EXPIRES);
		assertThat(h.from()).isEqualTo(HttpHeaders.FROM);
		assertThat(h.host()).isEqualTo(HttpHeaders.HOST);
		assertThat(h.ifMatch()).isEqualTo(HttpHeaders.IF_MATCH);
		assertThat(h.ifModifiedSince()).isEqualTo(HttpHeaders.IF_MODIFIED_SINCE);
		assertThat(h.ifNoneMatch()).isEqualTo(HttpHeaders.IF_NONE_MATCH);
		assertThat(h.ifRange()).isEqualTo(HttpHeaders.IF_RANGE);
		assertThat(h.ifUnmodifiedSince()).isEqualTo(HttpHeaders.IF_UNMODIFIED_SINCE);
		assertThat(h.lastModified()).isEqualTo(HttpHeaders.LAST_MODIFIED);
		assertThat(h.link()).isEqualTo(HttpHeaders.LINK);
		assertThat(h.location()).isEqualTo(HttpHeaders.LOCATION);
		assertThat(h.max_forwards()).isEqualTo(HttpHeaders.MAX_FORWARDS);
		assertThat(h.origin()).isEqualTo(HttpHeaders.ORIGIN);
		assertThat(h.pragma()).isEqualTo(HttpHeaders.PRAGMA);
		assertThat(h.proxyAuthenticate()).isEqualTo(HttpHeaders.PROXY_AUTHENTICATE);
		assertThat(h.proxyAuthorization()).isEqualTo(HttpHeaders.PROXY_AUTHORIZATION);
		assertThat(h.range()).isEqualTo(HttpHeaders.RANGE);
		assertThat(h.referer()).isEqualTo(HttpHeaders.REFERER);
		assertThat(h.retryAfter()).isEqualTo(HttpHeaders.RETRY_AFTER);
		assertThat(h.server()).isEqualTo(HttpHeaders.SERVER);
		assertThat(h.setCookie()).isEqualTo(HttpHeaders.SET_COOKIE);
		assertThat(h.setCookie2()).isEqualTo(HttpHeaders.SET_COOKIE_2);
		assertThat(h.te()).isEqualTo(HttpHeaders.TE);
		assertThat(h.trailer()).isEqualTo(HttpHeaders.TRAILER);
		assertThat(h.transferEncoding()).isEqualTo(HttpHeaders.TRANSFER_ENCODING);
		assertThat(h.upgrade()).isEqualTo(HttpHeaders.UPGRADE);
		assertThat(h.user_agent()).isEqualTo(HttpHeaders.USER_AGENT);
		assertThat(h.vary()).isEqualTo(HttpHeaders.VARY);
		assertThat(h.via()).isEqualTo(HttpHeaders.VIA);
		assertThat(h.warning()).isEqualTo(HttpHeaders.WARNING);
		assertThat(h.wwwAuthenticate()).isEqualTo(HttpHeaders.WWW_AUTHENTICATE);
	}

	@Test
	void mediaTypeAccessors() {
		Headers h = new Headers();
		assertThat(h.allValue()).isEqualTo(MediaTypes.ALL_VALUE);
		assertThat(h.applicationAtomXml()).isEqualTo(MediaTypes.APPLICATION_ATOM_XML);
		assertThat(h.applicationFormUrlencoded()).isEqualTo(MediaTypes.APPLICATION_FORM_URLENCODED);
		assertThat(h.applicationJson()).isEqualTo(MediaTypes.APPLICATION_JSON);
		assertThat(h.applicationJsonUtf8()).isEqualTo(MediaTypes.APPLICATION_JSON_UTF8);
		assertThat(h.applicationOctetStream()).isEqualTo(MediaTypes.APPLICATION_OCTET_STREAM);
		assertThat(h.applicationPdf()).isEqualTo(MediaTypes.APPLICATION_PDF);
		assertThat(h.applicationXhtmlXml()).isEqualTo(MediaTypes.APPLICATION_XHTML_XML);
		assertThat(h.applicationXml()).isEqualTo(MediaTypes.APPLICATION_XML);
		assertThat(h.imageGif()).isEqualTo(MediaTypes.IMAGE_GIF);
		assertThat(h.imageJpeg()).isEqualTo(MediaTypes.IMAGE_JPEG);
		assertThat(h.imagePng()).isEqualTo(MediaTypes.IMAGE_PNG);
		assertThat(h.multipartFormData()).isEqualTo(MediaTypes.MULTIPART_FORM_DATA);
		assertThat(h.textHtml()).isEqualTo(MediaTypes.TEXT_HTML);
		assertThat(h.textMarkdown()).isEqualTo(MediaTypes.TEXT_MARKDOWN);
		assertThat(h.textPlain()).isEqualTo(MediaTypes.TEXT_PLAIN);
		assertThat(h.textXml()).isEqualTo(MediaTypes.TEXT_XML);
	}

	@Test
	void headerBuildWithMatchingStrategy() {
		MatchingStrategy strategy = new MatchingStrategy("v", MatchingStrategy.Type.EQUAL_TO);
		Header header = Header.build("K", strategy);
		assertThat(header.getName()).isEqualTo("K");
		assertThat(header.getClientValue()).isEqualTo(strategy);
	}

	@Test
	void headerGetSetName() {
		Header header = Header.build("K", "v");
		header.setName("K2");
		assertThat(header.getName()).isEqualTo("K2");
	}

	@Test
	void headerEqualsHashCode() {
		Header a = Header.build("K", "v");
		Header b = Header.build("K", "v");
		assertThat(a).isEqualTo(b);
		assertThat(a).hasSameHashCodeAs(b);
		assertThat(a.equals(a)).isTrue();
		assertThat(a.equals(null)).isFalse();
		assertThat(a.equals("x")).isFalse();
	}

	@Test
	void headerNotEqualWhenNameDiffers() {
		assertThat(Header.build("K1", "v")).isNotEqualTo(Header.build("K2", "v"));
		assertThat(Header.build("K1", "v").hashCode()).isNotEqualTo(Header.build("K2", "v").hashCode());
	}

	@Test
	void headerNotEqualWhenValueDiffers() {
		assertThat(Header.build("K", "v1")).isNotEqualTo(Header.build("K", "v2"));
	}

	@Test
	void headerConstructorWithDslProperty() {
		Header header = new Header("K", new DslProperty("c", "s"));
		assertThat(header.getClientValue()).isEqualTo("c");
		assertThat(header.getServerValue()).isEqualTo("s");
	}

	@Test
	void headerToStringContainsNameAndValue() {
		Header header = new Header("K", new DslProperty("cval", "sval"));
		assertThat(header.toString()).contains("Header").contains("K").contains("cval").contains("sval");
	}

}
