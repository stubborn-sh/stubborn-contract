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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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
 * Phase 2 scope: the full class scaffold (package, class declaration, base class, class-
 * and method-level annotations, method signatures) plus each method's body emitted
 * verbatim from {@link TestMethodModel#bodyLines()} (captured from the legacy generator).
 * After JavaPoet renders the scaffold, the legacy import set carried on the model is
 * merged into JavaPoet's own imports so body-referenced types resolve.
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
		String rendered = JavaFile.builder(model.packageName(), type.build())
			.skipJavaLangImports(true)
			.indent("\t")
			.build()
			.toString();
		return mergeImports(rendered, model.importDeclarations());
	}

	/**
	 * Merges JavaPoet's own emitted imports with the legacy import set carried on the
	 * model, de-duplicating by exact text. JavaPoet only imports the types it references
	 * in the scaffold (annotations, base class); the verbatim method bodies reference
	 * types JavaPoet never sees, so their imports must come from the legacy set.
	 * @param rendered the JavaPoet output
	 * @param extraImports the legacy {@code import ...;} lines to fold in
	 * @return the source with a single merged import block
	 */
	private String mergeImports(String rendered, List<String> extraImports) {
		List<String> lines = rendered.lines().toList();
		String packageLine = "";
		Set<String> imports = new LinkedHashSet<>();
		List<String> remainder = new ArrayList<>();
		boolean inRemainder = false;
		for (String line : lines) {
			String trimmed = line.trim();
			if (inRemainder) {
				remainder.add(line);
				continue;
			}
			if (trimmed.startsWith("package ")) {
				packageLine = line;
			}
			else if (trimmed.startsWith("import ")) {
				imports.add(trimmed);
			}
			else if (trimmed.isEmpty()) {
				// skip blank lines between package/imports
			}
			else {
				inRemainder = true;
				remainder.add(line);
			}
		}
		for (String extra : extraImports) {
			String trimmed = extra.trim();
			if (!trimmed.isEmpty()) {
				imports.add(trimmed);
			}
		}
		StringBuilder sb = new StringBuilder();
		sb.append(packageLine).append('\n').append('\n');
		for (String imp : imports) {
			sb.append(imp).append('\n');
		}
		sb.append('\n');
		sb.append(String.join("\n", remainder));
		if (!remainder.isEmpty()) {
			sb.append('\n');
		}
		return sb.toString();
	}

	private MethodSpec toMethodSpec(TestMethodModel method) {
		MethodSpec.Builder builder = MethodSpec.methodBuilder(method.name())
			.addModifiers(Modifier.PUBLIC)
			.returns(void.class)
			.addException(Exception.class);
		for (AnnotationModel annotation : method.annotations()) {
			builder.addAnnotation(toAnnotationSpec(annotation));
		}
		RequestModel request = method.request();
		if (request != null) {
			// Structured request path: emit the // given: and // when: chains from the
			// model, then the verbatim // then: block. Each line goes through addCode as
			// a
			// $L argument so $, { and } stay literal.
			emitLine(builder, "// given:");
			for (String line : request.given().render()) {
				emitLine(builder, line);
			}
			emitLine(builder, "");
			emitLine(builder, "// when:");
			for (String line : request.whenBlock().render()) {
				emitLine(builder, line);
			}
			emitLine(builder, "");
		}
		ResponseModel response = method.response();
		if (response != null) {
			// Structured response path: emit the // then: status/header assertions from
			// the model, then fall through to the verbatim // and: body block carried on
			// bodyLines (the existing !body.isBlank() guard handles an empty tail).
			emitLine(builder, "// then:");
			for (String line : response.thenBlock().render()) {
				emitLine(builder, line);
			}
			emitLine(builder, "");
		}
		// Emit the (remaining) body verbatim as a $L argument (never inline into the
		// format
		// string) so $, { and } in template/JSON bodies stay literal. addStatement is
		// avoided: it appends a stray ; and mangles multi-line bodies, comments and blank
		// lines.
		String body = String.join("\n", method.bodyLines());
		if (!body.isBlank()) {
			builder.addCode("$L\n", body);
		}
		return builder.build();
	}

	private void emitLine(MethodSpec.Builder builder, String line) {
		builder.addCode("$L\n", line);
	}

	private AnnotationSpec toAnnotationSpec(AnnotationModel annotation) {
		AnnotationSpec.Builder builder = AnnotationSpec.builder(ClassName.bestGuess(annotation.type()));
		String member = annotation.memberCode();
		if (member != null && !member.isBlank()) {
			if ("value".equals(annotation.memberName())) {
				builder.addMember("value", "$L", member);
			}
			else {
				// A named member (e.g. enabled = false) is emitted as a single value
				// member so JavaPoet keeps it inline (@Test(enabled = false)) rather than
				// spreading it over three lines.
				builder.addMember("value", "$L", annotation.memberName() + " = " + member);
			}
		}
		return builder.build();
	}

}
