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

import java.security.SecureRandom;
import java.security.cert.X509Certificate;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;

/**
 * Convenience factory class for a {@link WireMockConfiguration} that knows how to use
 * Spring Boot to create a stub server. Use, for example, in a JUnit rule:
 *
 * <pre>
 * &#64;ClassRule
 * public static WireMockClassRule wiremock = new WireMockClassRule(
 * 		WireMockSpring.options());
 * </pre>
 *
 * and then use {@link com.github.tomakehurst.wiremock.client.WireMock} as normal in your
 * test methods.
 *
 * @author Dave Syer
 *
 */
public abstract class WireMockSpring {

	private static boolean initialized = false;

	public static WireMockConfiguration options() {
		if (!initialized) {
			try {
				HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);
				TrustManager[] trustAll = new TrustManager[] { new X509TrustManager() {
					public X509Certificate[] getAcceptedIssuers() {
						return new X509Certificate[0];
					}

					public void checkClientTrusted(X509Certificate[] chain, String authType) {
					}

					public void checkServerTrusted(X509Certificate[] chain, String authType) {
					}
				} };
				SSLContext sslContext = SSLContext.getInstance("TLS");
				sslContext.init(null, trustAll, new SecureRandom());
				HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
			}
			catch (Exception e) {
				throw new AssertionError("Cannot install custom socket factory: [" + e.getMessage() + "]");
			}
			initialized = true;
		}
		return new WireMockConfiguration();
	}

}
