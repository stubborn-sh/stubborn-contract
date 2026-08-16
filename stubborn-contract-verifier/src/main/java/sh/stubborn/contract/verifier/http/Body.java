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

package sh.stubborn.contract.verifier.http;

import java.nio.charset.Charset;

/**
 * Abstraction over an HTTP body.
 *
 * Warning! This API is experimental and can change in time.
 *
 * @author Marcin Grzejszczak
 * @since 1.0.0
 */
public class Body {

	final Object body;

	Body(Object body) {
		this.body = body;
	}

	/**
	 * Returns the body as a byte array.
	 * @return body as byte array
	 */
	public byte[] asByteArray() {
		if (this.body instanceof byte[]) {
			return (byte[]) this.body;
		}
		return this.body.toString().getBytes();
	}

	/**
	 * Returns the body as a string.
	 * @return body as string
	 */
	public String asString() {
		return asString(Charset.defaultCharset());
	}

	/**
	 * Returns the body as a string using the given charset.
	 * @param charset to encode the body
	 * @return body as string
	 */
	public String asString(Charset charset) {
		if (this.body instanceof String) {
			return (String) this.body;
		}
		else if (this.body instanceof byte[]) {
			return new String((byte[]) this.body, charset);
		}
		return this.body.toString();
	}

}
