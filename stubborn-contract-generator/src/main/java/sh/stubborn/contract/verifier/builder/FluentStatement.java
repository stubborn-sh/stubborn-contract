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

import java.util.ArrayList;
import java.util.List;

/**
 * Formatting-free description of a single fluent statement — a {@code head} line followed
 * by zero or more {@code .xxx(...)} continuation lines that form one method chain.
 *
 * <p>
 * Mirrors the legacy {@code BlockBuilder.addEndingIfNotPresent} rule: the head and every
 * intermediate continuation carry <em>no</em> statement terminator; only the
 * <em>last</em> line of the chain is terminated with a single {@code ;}. When the chain
 * has only a head (no continuations), the head itself is terminated.
 *
 * @param head the first line of the chain (never terminated on its own unless it is the
 * only line)
 * @param continuations the {@code .xxx(...)} lines that follow the head, in order
 * @author Marcin Grzejszczak
 */
record FluentStatement(String head, List<String> continuations) {

	FluentStatement {
		continuations = List.copyOf(continuations);
	}

	/**
	 * Renders the chain as {@code [head, ...continuations]} with exactly one {@code ;}
	 * appended to the <em>last</em> element (and only if it does not already end in
	 * {@code ;}). Head and intermediate lines are left unterminated.
	 * @return the rendered lines, in order
	 */
	List<String> render() {
		List<String> lines = new ArrayList<>(this.continuations.size() + 1);
		lines.add(this.head);
		lines.addAll(this.continuations);
		int last = lines.size() - 1;
		String lastLine = lines.get(last);
		if (!lastLine.endsWith(";")) {
			lines.set(last, lastLine + ";");
		}
		return lines;
	}

}
