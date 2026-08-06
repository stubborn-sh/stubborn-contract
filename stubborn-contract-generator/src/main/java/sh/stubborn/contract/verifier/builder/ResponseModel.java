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
 * Formatting-free description of the structured portion of a test method's
 * {@code // then:} block: the response status-code assertion followed by the response
 * header assertions and the response cookie assertions (per cookie, a null-check then a
 * value assertion), each a standalone {@code ;}-terminated statement.
 *
 * <p>
 * The response body assertions (the {@code // and:} block — JSON/XML/matcher checks) are
 * <em>not</em> part of this model; they remain captured verbatim from the legacy
 * generator. Built by {@link ResponseModelBuilder} only for the eligible subset of HTTP
 * contracts (already request-eligible and no template entry in the structured then
 * lines); otherwise the whole {@code // then:} block is captured verbatim and this model
 * is {@code null}.
 *
 * @param thenBlock the status-code, header and cookie assertion statements
 * @author Marcin Grzejszczak
 */
record ResponseModel(StatementList thenBlock) {

}
