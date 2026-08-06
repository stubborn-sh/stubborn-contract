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
 * Formatting-free description of a sequence of standalone statements — unlike
 * {@link FluentStatement}, these are not one method chain but independent statements, so
 * <em>every</em> statement is terminated with a single {@code ;}.
 *
 * <p>
 * Mirrors the legacy {@code // then:} block shape, where each response assertion (the
 * status-code check and each header check) is a {@code ;}-terminated line of its own.
 *
 * @param statements the statements, in order, each without a trailing terminator
 * @author Marcin Grzejszczak
 */
record StatementList(List<String> statements) {

	StatementList {
		statements = List.copyOf(statements);
	}

	/**
	 * Renders the statements in order, appending exactly one {@code ;} to every statement
	 * that does not already end in {@code ;}.
	 * @return the rendered lines, in order
	 */
	List<String> render() {
		List<String> lines = new ArrayList<>(this.statements.size());
		for (String statement : this.statements) {
			lines.add(statement.endsWith(";") ? statement : statement + ";");
		}
		return lines;
	}

}
