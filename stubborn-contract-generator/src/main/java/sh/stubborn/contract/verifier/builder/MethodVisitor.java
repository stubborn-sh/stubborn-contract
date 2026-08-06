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

package sh.stubborn.contract.verifier.builder;

import java.util.function.Function;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import sh.stubborn.contract.verifier.file.SingleContractMetadata;

interface MethodVisitor<T> extends MethodAcceptor, Function<SingleContractMetadata, MethodVisitor<T>> {

	/**
	 * Visits the given metadata, mutating the shared block builder. Implementations
	 * return themselves to allow fluent chaining, but callers frequently invoke this
	 * purely for the side effect, so the returned value may be ignored.
	 * @param singleContractMetadata metadata of the contract being visited
	 * @return this visitor
	 */
	@CanIgnoreReturnValue
	@Override
	MethodVisitor<T> apply(SingleContractMetadata singleContractMetadata);

}
