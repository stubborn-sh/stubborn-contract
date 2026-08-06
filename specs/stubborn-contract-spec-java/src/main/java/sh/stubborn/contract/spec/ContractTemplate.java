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

package sh.stubborn.contract.spec;

/**
 * Contract for defining templated responses.
 * <p>
 * If no implementation is provided then Handlebars will be picked as a default
 * implementation.
 *
 * @author Marcin Grzejszczak
 * @since 1.1.0
 */
public interface ContractTemplate {

	/**
	 * Asserts whether the given text starts with a proper opening template.
	 * @param text text to assert
	 * @return asserts whether the given text starts with proper opening template.
	 */
	default boolean startsWithTemplate(String text) {
		return text.startsWith(openingTemplate());
	}

	/**
	 * Asserts whether the given text starts with a proper escaped opening template.
	 * @param text text to assert
	 * @return asserts whether the given text starts with proper opening template for
	 * escaped value.
	 */
	default boolean startsWithEscapedTemplate(String text) {
		return text.startsWith(escapedOpeningTemplate());
	}

	/**
	 * Handlebars is using the Mustache template thus it looks like this {{ Mustache }}.
	 * In this case the opening template would return {{
	 * @return opening template for a variable
	 */
	String openingTemplate();

	/**
	 * Handlebars is using the Mustache template thus it looks like this {{ Mustache }}.
	 * In this case the closing template would return }}
	 * @return closing template for a variable
	 */
	String closingTemplate();

	/**
	 * Handlebars is using the Mustache template thus it looks like this {{{ Mustache }}}.
	 * In this case the opening template would return {{{
	 * @return escaped opening template for a variable
	 */
	default String escapedOpeningTemplate() {
		return openingTemplate();
	}

	/**
	 * Handlebars is using the Mustache template thus it looks like this {{{ Mustache }}}.
	 * In this case the closing template would return }}}
	 * @return escaped closing template for a variable
	 */
	default String escapedClosingTemplate() {
		return closingTemplate();
	}

	/**
	 * Returns the template for retrieving a URL path and query from the request.
	 * @return returns the template for retrieving a URL path and query from request
	 */
	String url();

	/**
	 * Returns the template for retrieving the first value of a query parameter.
	 * @param key query parameter key
	 * @return the template for retrieving first value of a query parameter e.g. {{
	 * request.query.search }}
	 */
	String query(String key);

	/**
	 * Returns the template for retrieving the nth value of a query parameter.
	 * @param key query parameter key
	 * @param index query parameter index
	 * @return the template for retrieving nth value of a query parameter (zero indexed)
	 * e.g. {{ request.query.search.[5] }}
	 */
	String query(String key, int index);

	/**
	 * Returns the template for retrieving a URL path.
	 * @return the template for retrieving a URL path
	 */
	String path();

	/**
	 * Returns the template for retrieving the nth value of a URL path.
	 * @param index request path index
	 * @return the template for retrieving nth value of a URL path (zero indexed) e.g. {{
	 * request.path.[2] }}
	 */
	String path(int index);

	/**
	 * Returns the template for retrieving the first value of a request header.
	 * @param key headers key
	 * @return the template for retrieving the first value of a request header e.g. {{
	 * request.headers.X-Request-Id }}
	 */
	String header(String key);

	/**
	 * Returns the template for retrieving the nth value of a request header.
	 * @param key headers key
	 * @param index headers index
	 * @return the template for retrieving the nth value of a request header (zero
	 * indexed) e.g. {{ request.headers.X-Request-Id.[5] }}
	 */
	String header(String key, int index);

	/**
	 * Returns the template for retrieving the first value of a cookie.
	 * @param key cookie key
	 * @return the template for retrieving the first value of a cookie with certain key
	 */
	String cookie(String key);

	/**
	 * Returns the template for retrieving the request body text.
	 * @return request body text (avoid for non-text bodies) e.g. {{ request.body }} . The
	 * body will not be escaped so you won't be able to directly embed it in a JSON for
	 * example.
	 */
	String body();

	/**
	 * Returns request body text for the given JsonPath.
	 * @param jsonPath json path value
	 * @return request body text for the given JsonPath. e.g. {{ jsonPath request.body
	 * '$.a.b.c' }}
	 */
	String body(String jsonPath);

	/**
	 * Returns the template for retrieving an escaped URL path and query from the request.
	 * @return the template for retrieving a escaped URL path and query from request
	 */
	default String escapedUrl() {
		return url();
	}

	/**
	 * Returns the template for retrieving the escaped first value of a query parameter.
	 * @param key query parameter key
	 * @return the template for retrieving escaped first value of a query parameter e.g.
	 * {{{ request.query.search }}}
	 */
	default String escapedQuery(String key) {
		return query(key);
	}

	/**
	 * Returns the template for retrieving the escaped nth value of a query parameter.
	 * @param key query parameter key
	 * @param index query parameter index
	 * @return the template for retrieving esacped nth value of a query parameter (zero
	 * indexed) e.g. {{{ request.query.search.[5] }}}
	 */
	default String escapedQuery(String key, int index) {
		return query(key, index);
	}

	/**
	 * Returns the template for retrieving an escaped URL path.
	 * @return the template for retrieving a escaped URL path
	 */
	default String escapedPath() {
		return path();
	}

	/**
	 * Returns the template for retrieving the escaped nth value of a URL path.
	 * @param index path index
	 * @return the template for retrieving escaped nth value of a URL path (zero indexed)
	 * e.g. {{{ request.path.[2] }}}
	 */
	default String escapedPath(int index) {
		return path(index);
	}

	/**
	 * Returns the template for retrieving the escaped first value of a request header.
	 * @param key headers key
	 * @return the template for retrieving the escaped first value of a request header
	 * e.g. {{{ request.headers.X-Request-Id }}}
	 */
	default String escapedHeader(String key) {
		return header(key);
	}

	/**
	 * Returns the template for retrieving the escaped nth value of a request header.
	 * @param key headers key
	 * @param index headers index
	 * @return the template for retrieving the esacaped nth value of a request header
	 * (zero indexed) e.g. {{{ request.headers.X-Request-Id.[5] }}}
	 */
	default String escapedHeader(String key, int index) {
		return header(key, index);
	}

	/**
	 * Returns the template for retrieving the escaped first value of a cookie.
	 * @param key cookie key
	 * @return the template for retrieving the escaped first value of a cookie with
	 * certain key
	 */
	default String escapedCookie(String key) {
		return cookie(key);
	}

	/**
	 * Returns the template for retrieving the escaped request body text.
	 * @return request body text (avoid for non-text bodies) e.g. {{{ request.body }}} .
	 * The body will not be escaped so you won't be able to directly embed it in a JSON
	 * for example.
	 */
	default String escapedBody() {
		return body();
	}

	/**
	 * Returns escaped request body text for the given JsonPath.
	 * @param jsonPath json path value
	 * @return request body text for the given JsonPath. e.g. {{{ jsonPath request.body
	 * '$.a.b.c' }}}
	 */
	default String escapedBody(String jsonPath) {
		return body(jsonPath);
	}

}
