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

package sh.stubborn.contract.verifier.template;

import sh.stubborn.contract.spec.internal.Request;

/**
 * Contract for conversion of templated responses.
 *
 * If no implementation is provided then Handlebars will be picked as a default
 * implementation.
 *
 * @author Marcin Grzejszczak
 * @since 1.0.0
 */
public interface TemplateProcessor {

	/**
	 * For the given {@link Request} and the test contents should perform a transformation
	 * and return the converted test.
	 * @param request the request side of the contract
	 * @param testContents the contents of the generated test
	 * @return the transformed test contents
	 */
	String transform(Request request, String testContents);

	/**
	 * Checks whether the current line contains a template related entry.
	 * @param line the line to inspect
	 * @return {@code true} if the current line contains template related entry. E.g. for
	 * Handlebars if a line contains {{{...}}} then it's considered to contain template
	 * related entry
	 */
	boolean containsTemplateEntry(String line);

	/**
	 * Checks whether the current line contains a json path template related entry.
	 * @param line the line to inspect
	 * @return {@code true} if the current line contains template related entry for json
	 * path processing. E.g. for Handlebars if a line contains {{{jsonpath ...}}} or
	 * {{{jsonPath ...}}} then it's considered to contain template related entry for json
	 * path processing
	 */
	boolean containsJsonPathTemplateEntry(String line);

	/**
	 * Returns the json path entry from the current line.
	 * @param line the line to inspect
	 * @return the json path entry from the current line that contains template related
	 * entry for json path processing. E.g. for Handlebars if a line contains {{{jsonpath
	 * this '$.a.b.c'}}} or {{{jsonPath request.body ...}}} then the te method would
	 * return {@code $.a.b.c}. Returns empty string if there's no matching json path entry
	 */
	String jsonPathFromTemplateEntry(String line);

}
