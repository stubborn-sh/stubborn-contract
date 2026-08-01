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

import java.util.List;

/**
 * Formatting-free description of a single test method within a {@link TestClassModel}.
 *
 * <p>
 * In Phase 1 {@code bodyLines} is a placeholder; the structured
 * {@code given}/{@code when} /{@code then} representation of request and response
 * verification lands in later phases. Each entry is a single complete statement
 * <em>without</em> a trailing {@code ;} — the renderer owns statement termination (and
 * the {@code ;}-vs-nothing Java/Groovy split).
 *
 * @param name the method name (e.g. {@code validate_shouldReturnOk})
 * @param annotations method-level annotations, in declaration order
 * @param bodyLines opaque method-body statements, in order (no trailing {@code ;})
 * @author Claude Code
 */
record TestMethodModel(String name, List<AnnotationModel> annotations, List<String> bodyLines) {

	TestMethodModel {
		annotations = List.copyOf(annotations);
		bodyLines = List.copyOf(bodyLines);
	}

}
