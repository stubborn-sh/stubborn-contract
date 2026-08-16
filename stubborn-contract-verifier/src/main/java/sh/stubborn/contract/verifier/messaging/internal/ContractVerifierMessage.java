/*
 * Copyright 2012-present the original author or authors.
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

package sh.stubborn.contract.verifier.messaging.internal;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

/**
 * Yet another message abstraction. Provides generated tests with a layer that is
 * independent of the message provider.
 *
 * @author Dave Syer
 * @since 1.0.0
 */
public class ContractVerifierMessage {

	private @Nullable Object payload;

	private Map<String, Object> headers = new LinkedHashMap<>();

	public ContractVerifierMessage() {
	}

	public ContractVerifierMessage(@Nullable Object payload, @Nullable Map<String, Object> headers) {
		this.payload = payload;
		if (headers != null) {
			this.headers.putAll(headers);
		}
	}

	public @Nullable Object getPayload() {
		return this.payload;
	}

	public void setPayload(Object payload) {
		this.payload = payload;
	}

	public byte[] getPayloadAsByteArray() {
		if (this.payload instanceof byte[]) {
			return (byte[]) this.payload;
		}
		throw new IllegalStateException(
				"Payload [" + Objects.requireNonNull(this.payload).getClass() + "]" + "is not instance of byte[]");
	}

	public Map<String, Object> getHeaders() {
		return this.headers;
	}

	public void setHeaders(Map<String, Object> headers) {
		this.headers = headers;
	}

	public @Nullable Object getHeader(String name) {
		return this.headers.get(name);
	}

}
