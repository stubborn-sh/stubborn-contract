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
 */
public abstract class WireMockSpring {

	public static WireMockConfiguration options() {
		return new WireMockConfiguration();
	}

}
