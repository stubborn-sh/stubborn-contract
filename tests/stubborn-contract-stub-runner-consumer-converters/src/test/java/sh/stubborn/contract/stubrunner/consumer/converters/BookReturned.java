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

package sh.stubborn.contract.stubrunner.consumer.converters;

import org.jspecify.annotations.Nullable;

/**
 * Typed payload a {@code @KafkaListener} binds to, populated by the out-of-the-box JSON
 * converter from the contract's JSON {@code outputMessage} body.
 *
 * @author Marcin Grzejszczak
 */
public class BookReturned {

	private @Nullable String bookName;

	public @Nullable String getBookName() {
		return this.bookName;
	}

	public void setBookName(String bookName) {
		this.bookName = bookName;
	}

}
