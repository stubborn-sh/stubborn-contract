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

import java.util.Objects;

import org.jspecify.annotations.Nullable;
import sh.stubborn.contract.spec.internal.BodyMatchers;
import sh.stubborn.contract.spec.internal.DslProperty;
import sh.stubborn.contract.spec.internal.Request;
import sh.stubborn.contract.spec.internal.Response;
import sh.stubborn.contract.verifier.file.SingleContractMetadata;

interface BodyThen {

	default DslProperty requestBody(SingleContractMetadata metadata) {
		Request request = Objects.requireNonNull(metadata.getContract().getRequest());
		return Objects.requireNonNull(request.getBody());
	}

	default @Nullable DslProperty responseBody(SingleContractMetadata metadata) {
		Response response = Objects.requireNonNull(metadata.getContract().getResponse());
		return response.getBody();
	}

	default @Nullable BodyMatchers responseBodyMatchers(SingleContractMetadata metadata) {
		Response response = Objects.requireNonNull(metadata.getContract().getResponse());
		return response.getBodyMatchers();
	}

}
