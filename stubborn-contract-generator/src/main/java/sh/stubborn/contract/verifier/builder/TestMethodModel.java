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

import org.jspecify.annotations.Nullable;

/**
 * Formatting-free description of a single test method within a {@link TestClassModel}.
 *
 * <p>
 * When {@code request} is {@code null} the whole method body is emitted verbatim from
 * {@code bodyLines} — each line exactly as-is, so a line may already carry a trailing
 * {@code ;}, be a {@code // given:} label comment, or be blank; the renderer adds no
 * statement termination. This is the fallback path for anything the structured request
 * model does not yet cover.
 *
 * <p>
 * When {@code request} is non-{@code null} the HTTP request portion (the
 * {@code // given:} request-spec chain and the {@code // when:} response chain) is
 * emitted from that structured {@link RequestModel}, and {@code bodyLines} then carries
 * only the verbatim {@code // then:} block captured from the legacy generator by
 * {@link LegacyMethodBodyExtractor}.
 *
 * @param name the method name (e.g. {@code validate_shouldReturnOk})
 * @param annotations method-level annotations, in declaration order
 * @param bodyLines verbatim method-body lines, in order (may contain {@code ;}, comments
 * and blank lines); the full body when {@code request} is {@code null}, otherwise only
 * the {@code // then:} block
 * @param request the structured HTTP request portion, or {@code null} to emit the whole
 * body verbatim from {@code bodyLines}
 * @author Marcin Grzejszczak
 */
record TestMethodModel(String name, List<AnnotationModel> annotations, List<String> bodyLines,
		@Nullable RequestModel request) {

	TestMethodModel {
		annotations = List.copyOf(annotations);
		bodyLines = List.copyOf(bodyLines);
	}

	/**
	 * Convenience for the verbatim path: a method whose whole body is emitted from
	 * {@code bodyLines} with no structured request.
	 * @param name the method name
	 * @param annotations method-level annotations, in declaration order
	 * @param bodyLines verbatim method-body lines, in order
	 */
	TestMethodModel(String name, List<AnnotationModel> annotations, List<String> bodyLines) {
		this(name, annotations, bodyLines, null);
	}

}
