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

package org.springframework.cloud.contract.spec;

import java.util.function.Consumer;

import groovy.lang.Closure;
import groovy.lang.DelegatesTo;

/**
 * Backward-compatibility alias for {@link sh.stubborn.contract.spec.Contract}.
 * <p>
 * Groovy contract files that use the old Spring Cloud Contract package continue to work
 * without modification:
 *
 * <pre>
 * import org.springframework.cloud.contract.spec.Contract
 *
 * Contract.make {
 *     request { ... }
 *     response { ... }
 * }
 * </pre>
 *
 * @author Marcin Grzejszczak
 * @deprecated Use {@link sh.stubborn.contract.spec.Contract} instead.
 */
@Deprecated
public class Contract extends sh.stubborn.contract.spec.Contract {

	/**
	 * Point of entry to build a contract.
	 * @param consumer function to manipulate the contract
	 * @return manipulated contract
	 */
	public static sh.stubborn.contract.spec.Contract make(Consumer<sh.stubborn.contract.spec.Contract> consumer) {
		return sh.stubborn.contract.spec.Contract.make(consumer);
	}

	/**
	 * Groovy point of entry to build a contract.
	 * @param closure closure to manipulate the contract
	 * @return manipulated contract
	 */
	public static sh.stubborn.contract.spec.Contract make(
			@DelegatesTo(sh.stubborn.contract.spec.Contract.class) Closure closure) {
		return sh.stubborn.contract.spec.Contract.make(closure);
	}

}
