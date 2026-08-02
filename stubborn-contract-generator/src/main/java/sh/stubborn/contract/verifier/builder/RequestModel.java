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

/**
 * Formatting-free description of the HTTP request portion of a test method: the
 * {@code // given:} request-specification chain (spec head plus {@code .header(...)},
 * {@code .cookie(...)} and an optional trailing {@code .body(...)} line) and the
 * {@code // when:} response chain (response head plus {@code .queryParam(...)} lines and
 * the {@code .<method>(url)} call).
 *
 * <p>
 * Built by {@link RequestModelBuilder} only for the eligible subset of HTTP contracts (a
 * plain or {@code ExecutionProperty} body, cookies, query parameters, multipart parts and
 * an async/delayed response are allowed, but no file-based body or request templates);
 * otherwise the whole method body is captured verbatim from the legacy generator and this
 * model is {@code null}.
 *
 * @param given the {@code // given:} request-specification chain
 * @param whenBlock the {@code // when:} response chain (named to avoid the {@code when}
 * keyword confusion)
 * @author Marcin Grzejszczak
 */
record RequestModel(FluentStatement given, FluentStatement whenBlock) {

}
