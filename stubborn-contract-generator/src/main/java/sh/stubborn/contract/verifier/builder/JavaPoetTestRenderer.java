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

import javax.lang.model.element.Modifier;

import com.palantir.javapoet.AnnotationSpec;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.JavaFile;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.TypeSpec;

/**
 * Renders the Java targets (JUnit 5 / TestNG / JUnit 4) of a {@link TestClassModel} into
 * source using <a href="https://github.com/palantir/javapoet">JavaPoet</a>.
 *
 * <p>
 * JavaPoet models the Java language with typed {@code TypeSpec}/{@code MethodSpec}
 * builders, so braces, imports, indentation, and escaping are correct by construction —
 * the exact class of whitespace/brace bug the migration exists to remove. Groovy/Spock is
 * the one target JavaPoet cannot model and is rendered by a Handlebars renderer instead
 * (added in a later phase).
 *
 * <p>
 * Phase 1 scope: the class scaffold (package, class declaration, base class, class- and
 * method-level annotations, method signatures). Method bodies are the placeholder
 * {@link TestMethodModel#bodyLines()} statements; the structured request/response
 * verification is ported in Phases 2–4.
 *
 * @author Marcin Grzejszczak
 */
class JavaPoetTestRenderer {

	/**
	 * Renders the given model into Java source.
	 * @param model the class model; {@link TestClassModel#spock()} must be {@code false}
	 * @return the generated Java source
	 * @throws IllegalArgumentException if the model targets Spock, which JavaPoet cannot
	 * render
	 */
	String render(TestClassModel model) {
		if (model.spock()) {
			throw new IllegalArgumentException(
					"JavaPoetTestRenderer renders Java targets only; Spock is rendered by the Handlebars renderer");
		}
		TypeSpec.Builder type = TypeSpec.classBuilder(model.className()).addModifiers(Modifier.PUBLIC);
		String baseClass = model.baseClass();
		if (baseClass != null && !baseClass.isBlank()) {
			type.superclass(ClassName.bestGuess(baseClass.trim()));
		}
		for (AnnotationModel annotation : model.classAnnotations()) {
			type.addAnnotation(toAnnotationSpec(annotation));
		}
		for (TestMethodModel method : model.methods()) {
			type.addMethod(toMethodSpec(method));
		}
		return JavaFile.builder(model.packageName(), type.build())
			.skipJavaLangImports(true)
			.indent("\t")
			.build()
			.toString();
	}

	private MethodSpec toMethodSpec(TestMethodModel method) {
		MethodSpec.Builder builder = MethodSpec.methodBuilder(method.name())
			.addModifiers(Modifier.PUBLIC)
			.returns(void.class)
			.addException(Exception.class);
		for (AnnotationModel annotation : method.annotations()) {
			builder.addAnnotation(toAnnotationSpec(annotation));
		}
		for (String line : method.bodyLines()) {
			builder.addStatement("$L", line);
		}
		return builder.build();
	}

	private AnnotationSpec toAnnotationSpec(AnnotationModel annotation) {
		AnnotationSpec.Builder builder = AnnotationSpec.builder(ClassName.bestGuess(annotation.type()));
		String member = annotation.memberCode();
		if (member != null && !member.isBlank()) {
			builder.addMember("value", "$L", member);
		}
		return builder.build();
	}

}
