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
 * {@code bodyLines} are the <em>verbatim</em> body lines captured from the legacy
 * generator by {@link LegacyMethodBodyExtractor}: each line is emitted exactly as-is by
 * the renderer, so a line may already carry a trailing {@code ;}, be a {@code // given:}
 * label comment, or be blank. The renderer does <em>not</em> add statement termination.
 *
 * @param name the method name (e.g. {@code validate_shouldReturnOk})
 * @param annotations method-level annotations, in declaration order
 * @param bodyLines verbatim method-body lines, in order (may contain {@code ;}, comments
 * and blank lines)
 * @author Marcin Grzejszczak
 */
record TestMethodModel(String name, List<AnnotationModel> annotations, List<String> bodyLines) {

	TestMethodModel {
		annotations = List.copyOf(annotations);
		bodyLines = List.copyOf(bodyLines);
	}

}
