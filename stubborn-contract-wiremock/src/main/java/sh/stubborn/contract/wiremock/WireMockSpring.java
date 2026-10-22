/*
 * Copyright 2015-present the original author or authors.
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

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;

/**
 * Convenience factory class for a {@link WireMockConfiguration}. Use, for example, in a
 * JUnit rule:
 *
 * <pre>
 * &#64;ClassRule
 * public static WireMockClassRule wiremock = new WireMockClassRule(
 * 		WireMockSpring.options());
 * </pre>
 *
 * and then use {@link com.github.tomakehurst.wiremock.client.WireMock} as normal in your
 * test methods.
 * <p>
 * If the WireMock server is configured for HTTPS, callers are responsible for configuring
 * their HTTP client to trust the WireMock server's certificate. Do not disable
 * certificate validation globally; instead use WireMock's bundled keystore or a dedicated
 * test trust-store.
 *
 * @author Dave Syer
 * @since 1.0.0
 */
public abstract class WireMockSpring {

	public static WireMockConfiguration options() {
		// Plaintext HTTP/2 is off deliberately. The stub server now runs on WireMock's
		// shaded standalone build, which — unlike the thin wiremock-jetty12 artifact —
		// bundles jetty-http2-server, so h2c becomes available and a
		// JDK-HttpClient-backed
		// RestClient tries to upgrade. A POST that carries a body cannot be upgraded, and
		// the request dies as "Received RST_STREAM: Stream cancelled" or
		// "EOF reached while reading". A stub server has no use for HTTP/2, so turning
		// h2c
		// off keeps the behaviour consumers already had. Call http2PlainDisabled(false)
		// on
		// the returned configuration if you do want it.
		return new WireMockConfiguration().http2PlainDisabled(true);
	}

}
