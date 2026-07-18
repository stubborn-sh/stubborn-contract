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

package sh.stubborn.contract.spec

import sh.stubborn.contract.spec.internal.DslProperty
import sh.stubborn.contract.spec.internal.QueryParameters
import sh.stubborn.contract.spec.internal.Url
import sh.stubborn.contract.spec.internal.UrlPath
import java.util.stream.Collectors

/**
 * Class extension functions which make our lives easier.
 * @author Tim Ysewyn
 * @since 2.2.0
 */
infix fun Url.withQueryParameters(parameters: QueryParameters.() -> Unit) = apply {
    queryParameters = QueryParameters().apply(parameters)
}

infix fun UrlPath.withQueryParameters(parameters: QueryParameters.() -> Unit) = apply {
    queryParameters = QueryParameters().apply(parameters)
}

fun <T : Any> T.toDslProperty(): DslProperty<T> = DslProperty(this)

fun Map<String, Any>.toDslProperties(): Map<String, DslProperty<Any>> {
    return entries.stream().collect(Collectors.toMap(
            { entry -> entry.key },
            { entry -> entry.value.toDslProperty() },
            { t, _ -> throw IllegalStateException(String.format("Duplicate key %s", t)) },
            { LinkedHashMap<String, DslProperty<Any>>() }
    ))
}

fun List<Any>.toDslProperties(): List<DslProperty<Any>> {
    return stream().map(Any::toDslProperty).collect(Collectors.toList())
}