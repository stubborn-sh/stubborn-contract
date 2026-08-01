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

import org.jspecify.annotations.Nullable;

/**
 * Formatting-free description of a single annotation on a class or method within a
 * {@link TestClassModel}.
 *
 * @param type fully-qualified (or simple) name of the annotation type, e.g.
 * {@code org.junit.jupiter.api.Test}
 * @param memberCode the raw source for the annotation's single {@code value} member (e.g.
 * {@code "rawtypes"} or {@code MethodOrderer.MethodName.class}), or {@code null} for a
 * marker annotation with no members
 * @author Claude Code
 */
record AnnotationModel(String type, @Nullable String memberCode) {

	/**
	 * Creates a marker annotation with no members, e.g. {@code @Test}.
	 * @param type fully-qualified (or simple) name of the annotation type
	 * @return a member-less annotation model
	 */
	static AnnotationModel marker(String type) {
		return new AnnotationModel(type, null);
	}

}
