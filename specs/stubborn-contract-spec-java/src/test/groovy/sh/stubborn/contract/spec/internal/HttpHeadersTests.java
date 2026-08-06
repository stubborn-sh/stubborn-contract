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

import org.assertj.core.api.BDDAssertions;
import org.junit.jupiter.api.Test;

/**
 * @author Tim Ysewyn
 */
class HttpHeadersTests {

	@Test
	void ACCEPT() {
		BDDAssertions.then(HttpHeaders.ACCEPT).isEqualTo("Accept");
	}

	@Test
	void ACCEPT_CHARSET() {
		BDDAssertions.then(HttpHeaders.ACCEPT_CHARSET).isEqualTo("Accept-Charset");
	}

	@Test
	void ACCEPT_ENCODING() {
		BDDAssertions.then(HttpHeaders.ACCEPT_ENCODING).isEqualTo("Accept-Encoding");
	}

	@Test
	void ACCEPT_LANGUAGE() {
		BDDAssertions.then(HttpHeaders.ACCEPT_LANGUAGE).isEqualTo("Accept-Language");
	}

	@Test
	void ACCEPT_RANGES() {
		BDDAssertions.then(HttpHeaders.ACCEPT_RANGES).isEqualTo("Accept-Ranges");
	}

	@Test
	void ACCESS_CONTROL_ALLOW_CREDENTIALS() {
		BDDAssertions.then(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS).isEqualTo("Access-Control-Allow-Credentials");
	}

	@Test
	void ACCESS_CONTROL_ALLOW_HEADERS() {
		BDDAssertions.then(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS).isEqualTo("Access-Control-Allow-Headers");
	}

	@Test
	void ACCESS_CONTROL_ALLOW_METHODS() {
		BDDAssertions.then(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS).isEqualTo("Access-Control-Allow-Methods");
	}

	@Test
	void ACCESS_CONTROL_ALLOW_ORIGIN() {
		BDDAssertions.then(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN).isEqualTo("Access-Control-Allow-Origin");
	}

	@Test
	void ACCESS_CONTROL_EXPOSE_HEADERS() {
		BDDAssertions.then(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS).isEqualTo("Access-Control-Expose-Headers");
	}

	@Test
	void ACCESS_CONTROL_MAX_AGE() {
		BDDAssertions.then(HttpHeaders.ACCESS_CONTROL_MAX_AGE).isEqualTo("Access-Control-Max-Age");
	}

	@Test
	void ACCESS_CONTROL_REQUEST_HEADERS() {
		BDDAssertions.then(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS).isEqualTo("Access-Control-Request-Headers");
	}

	@Test
	void ACCESS_CONTROL_REQUEST_METHOD() {
		BDDAssertions.then(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD).isEqualTo("Access-Control-Request-Method");
	}

	@Test
	void AGE() {
		BDDAssertions.then(HttpHeaders.AGE).isEqualTo("Age");
	}

	@Test
	void ALLOW() {
		BDDAssertions.then(HttpHeaders.ALLOW).isEqualTo("Allow");
	}

	@Test
	void AUTHORIZATION() {
		BDDAssertions.then(HttpHeaders.AUTHORIZATION).isEqualTo("Authorization");
	}

	@Test
	void CACHE_CONTROL() {
		BDDAssertions.then(HttpHeaders.CACHE_CONTROL).isEqualTo("Cache-Control");
	}

	@Test
	void CONNECTION() {
		BDDAssertions.then(HttpHeaders.CONNECTION).isEqualTo("Connection");
	}

	@Test
	void CONTENT_ENCODING() {
		BDDAssertions.then(HttpHeaders.CONTENT_ENCODING).isEqualTo("Content-Encoding");
	}

	@Test
	void CONTENT_DISPOSITION() {
		BDDAssertions.then(HttpHeaders.CONTENT_DISPOSITION).isEqualTo("Content-Disposition");
	}

	@Test
	void CONTENT_LANGUAGE() {
		BDDAssertions.then(HttpHeaders.CONTENT_LANGUAGE).isEqualTo("Content-Language");
	}

	@Test
	void CONTENT_LENGTH() {
		BDDAssertions.then(HttpHeaders.CONTENT_LENGTH).isEqualTo("Content-Length");
	}

	@Test
	void CONTENT_LOCATION() {
		BDDAssertions.then(HttpHeaders.CONTENT_LOCATION).isEqualTo("Content-Location");
	}

	@Test
	void CONTENT_RANGE() {
		BDDAssertions.then(HttpHeaders.CONTENT_RANGE).isEqualTo("Content-Range");
	}

	@Test
	void CONTENT_TYPE() {
		BDDAssertions.then(HttpHeaders.CONTENT_TYPE).isEqualTo("Content-Type");
	}

	@Test
	void COOKIE() {
		BDDAssertions.then(HttpHeaders.COOKIE).isEqualTo("Cookie");
	}

	@Test
	void DATE() {
		BDDAssertions.then(HttpHeaders.DATE).isEqualTo("Date");
	}

	@Test
	void ETAG() {
		BDDAssertions.then(HttpHeaders.ETAG).isEqualTo("ETag");
	}

	@Test
	void EXPECT() {
		BDDAssertions.then(HttpHeaders.EXPECT).isEqualTo("Expect");
	}

	@Test
	void EXPIRES() {
		BDDAssertions.then(HttpHeaders.EXPIRES).isEqualTo("Expires");
	}

	@Test
	void FROM() {
		BDDAssertions.then(HttpHeaders.FROM).isEqualTo("From");
	}

	@Test
	void HOST() {
		BDDAssertions.then(HttpHeaders.HOST).isEqualTo("Host");
	}

	@Test
	void IF_MATCH() {
		BDDAssertions.then(HttpHeaders.IF_MATCH).isEqualTo("If-Match");
	}

	@Test
	void IF_MODIFIED_SINCE() {
		BDDAssertions.then(HttpHeaders.IF_MODIFIED_SINCE).isEqualTo("If-Modified-Since");
	}

	@Test
	void IF_NONE_MATCH() {
		BDDAssertions.then(HttpHeaders.IF_NONE_MATCH).isEqualTo("If-None-Match");
	}

	@Test
	void IF_RANGE() {
		BDDAssertions.then(HttpHeaders.IF_RANGE).isEqualTo("If-Range");
	}

	@Test
	void IF_UNMODIFIED_SINCE() {
		BDDAssertions.then(HttpHeaders.IF_UNMODIFIED_SINCE).isEqualTo("If-Unmodified-Since");
	}

	@Test
	void LAST_MODIFIED() {
		BDDAssertions.then(HttpHeaders.LAST_MODIFIED).isEqualTo("Last-Modified");
	}

	@Test
	void LINK() {
		BDDAssertions.then(HttpHeaders.LINK).isEqualTo("Link");
	}

	@Test
	void LOCATION() {
		BDDAssertions.then(HttpHeaders.LOCATION).isEqualTo("Location");
	}

	@Test
	void MAX_FORWARDS() {
		BDDAssertions.then(HttpHeaders.MAX_FORWARDS).isEqualTo("Max-Forwards");
	}

	@Test
	void ORIGIN() {
		BDDAssertions.then(HttpHeaders.ORIGIN).isEqualTo("Origin");
	}

	@Test
	void PRAGMA() {
		BDDAssertions.then(HttpHeaders.PRAGMA).isEqualTo("Pragma");
	}

	@Test
	void PROXY_AUTHENTICATE() {
		BDDAssertions.then(HttpHeaders.PROXY_AUTHENTICATE).isEqualTo("Proxy-Authenticate");
	}

	@Test
	void PROXY_AUTHORIZATION() {
		BDDAssertions.then(HttpHeaders.PROXY_AUTHORIZATION).isEqualTo("Proxy-Authorization");
	}

	@Test
	void RANGE() {
		BDDAssertions.then(HttpHeaders.RANGE).isEqualTo("Range");
	}

	@Test
	void REFERER() {
		BDDAssertions.then(HttpHeaders.REFERER).isEqualTo("Referer");
	}

	@Test
	void RETRY_AFTER() {
		BDDAssertions.then(HttpHeaders.RETRY_AFTER).isEqualTo("Retry-After");
	}

	@Test
	void SERVER() {
		BDDAssertions.then(HttpHeaders.SERVER).isEqualTo("Server");
	}

	@Test
	void SET_COOKIE() {
		BDDAssertions.then(HttpHeaders.SET_COOKIE).isEqualTo("Set-Cookie");
	}

	@Test
	void SET_COOKIE_2() {
		BDDAssertions.then(HttpHeaders.SET_COOKIE_2).isEqualTo("Set-Cookie2");
	}

	@Test
	void TE() {
		BDDAssertions.then(HttpHeaders.TE).isEqualTo("TE");
	}

	@Test
	void TRAILER() {
		BDDAssertions.then(HttpHeaders.TRAILER).isEqualTo("Trailer");
	}

	@Test
	void TRANSFER_ENCODING() {
		BDDAssertions.then(HttpHeaders.TRANSFER_ENCODING).isEqualTo("Transfer-Encoding");
	}

	@Test
	void UPGRADE() {
		BDDAssertions.then(HttpHeaders.UPGRADE).isEqualTo("Upgrade");
	}

	@Test
	void USER_AGENT() {
		BDDAssertions.then(HttpHeaders.USER_AGENT).isEqualTo("User-Agent");
	}

	@Test
	void VARY() {
		BDDAssertions.then(HttpHeaders.VARY).isEqualTo("Vary");
	}

	@Test
	void VIA() {
		BDDAssertions.then(HttpHeaders.VIA).isEqualTo("Via");
	}

	@Test
	void WARNING() {
		BDDAssertions.then(HttpHeaders.WARNING).isEqualTo("Warning");
	}

	@Test
	void WWW_AUTHENTICATE() {
		BDDAssertions.then(HttpHeaders.WWW_AUTHENTICATE).isEqualTo("WWW-Authenticate");
	}

}
