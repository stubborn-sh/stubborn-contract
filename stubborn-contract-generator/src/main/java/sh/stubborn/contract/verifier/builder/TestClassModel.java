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
 * Immutable, formatting-free description of a generated test class.
 *
 * <p>
 * This is the pure-data seam introduced by the test-generation migration (see
 * {@code proposals/test-generation-migration.md}). It carries the <em>decisions</em>
 * (package, class name, base class, class-level annotations, methods) with no
 * indentation, braces, or language tokens; a renderer ({@code JavaPoetTestRenderer} for
 * Java targets, a Handlebars renderer for Spock in a later phase) turns it into source.
 *
 * <p>
 * In Phase 1 the model is populated for the class scaffold only and is not yet on the
 * production output path — the legacy string builders still produce the emitted source.
 * Later phases move the request/response bodies into the model and flip the renderers on.
 *
 * @param packageName the package the test class is declared in
 * @param className the simple name of the generated test class
 * @param baseClass fully-qualified (or simple) name of the base class to extend, or
 * {@code null} when the test has no explicit base class
 * @param spock {@code true} when the target language is Groovy/Spock (rendered by the
 * Handlebars renderer), {@code false} for the Java targets (rendered by JavaPoet)
 * @param classAnnotations class-level annotations, in declaration order
 * @param methods the test methods, in declaration order
 * @author Marcin Grzejszczak
 */
record TestClassModel(String packageName, String className, @Nullable String baseClass, boolean spock,
		List<AnnotationModel> classAnnotations, List<TestMethodModel> methods) {

	TestClassModel {
		classAnnotations = List.copyOf(classAnnotations);
		methods = List.copyOf(methods);
	}

}
