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
		return ensureBlankLineBeforeClassClose(
				injectFields(mergeImports(rendered, model.importDeclarations()), model.fields()));
	}

	/**
	 * Emits a blank line between the last member and the class' closing brace, matching
	 * the legacy generator's layout. JavaPoet closes the class immediately after the
	 * final method; the legacy string builders leave a trailing blank line, so
	 * replicating it keeps the model output byte-faithful to legacy.
	 * @param source the rendered source
	 * @return the source with a blank line before the class' closing brace
	 */
	private String ensureBlankLineBeforeClassClose(String source) {
		List<String> lines = new ArrayList<>(source.lines().toList());
		for (int i = lines.size() - 1; i >= 0; i--) {
			// The class' closing brace is the only unindented "}".
			if (lines.get(i).equals("}")) {
				if (i > 0 && !lines.get(i - 1).isBlank()) {
					lines.add(i, "");
				}
				break;
			}
		}
		String joined = String.join("\n", lines);
		return source.endsWith("\n") ? joined + "\n" : joined;
	}

	/**
	 * Inserts the captured class-level field declarations immediately after the class
	 * opening brace and guarantees the single blank line the legacy generator always
	 * places before the first method.
	 *
	 * <p>
	 * JavaPoet's typed {@code FieldSpec} model does not fit these annotated,
	 * framework-typed fields cleanly, so — as with the verbatim method bodies — they are
	 * captured from the legacy generator and spliced into the rendered source; their
	 * imports are already part of the merged import set. The legacy layout is {@code {
	 * <fields> <blank> <methods> }}, where the field block may be empty (the plain
	 * MockMvc/EXPLICIT/WebTestClient HTTP shapes) but the blank line before the first
	 * method is always present. JavaPoet emits the first method directly after the brace
	 * with no blank, so this restores it whether or not there are fields, keeping the
	 * output byte-faithful to legacy.
	 * @param rendered the rendered (import-merged) source
	 * @param fields the field declaration lines, or empty
	 * @return the source with the fields (and the leading blank line) inserted
	 */
	private String injectFields(String rendered, List<String> fields) {
		List<String> lines = new ArrayList<>(rendered.lines().toList());
		for (int i = 0; i < lines.size(); i++) {
			if (lines.get(i).stripTrailing().matches("^(public\\s+)?(final\\s+)?class\\s+\\w+.*\\{$")) {
				boolean bodyEmpty = i + 1 >= lines.size() || lines.get(i + 1).strip().equals("}");
				boolean nextAlreadyBlank = i + 1 < lines.size() && lines.get(i + 1).isBlank();
				List<String> insert = new ArrayList<>();
				for (String field : fields) {
					String trimmed = field.trim();
					insert.add("\t" + (trimmed.endsWith(";") ? trimmed : trimmed + ";"));
				}
				// Legacy always puts one blank line before the first method. Add it
				// unless
				// the body is empty, or there are no fields and JavaPoet already left a
				// blank there (which would otherwise double it).
				if (!bodyEmpty && !(fields.isEmpty() && nextAlreadyBlank)) {
					insert.add("");
				}
				lines.addAll(i + 1, insert);
				break;
			}
		}
		String joined = String.join("\n", lines);
		return rendered.endsWith("\n") ? joined + "\n" : joined;
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
		List<String> javaPoetImports = new ArrayList<>();
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
				javaPoetImports.add(trimmed);
			}
			else if (trimmed.isEmpty()) {
				// skip blank lines between package/imports
			}
			else {
				inRemainder = true;
				remainder.add(line);
			}
		}
		// The legacy import set is authoritative for ordering (it is a superset of the
		// scaffold imports JavaPoet emits); append any JavaPoet-only import last.
		Set<String> ordered = new LinkedHashSet<>();
		for (String extra : extraImports) {
			String trimmed = extra.trim();
			if (!trimmed.isEmpty()) {
				ordered.add(trimmed);
			}
		}
		ordered.addAll(javaPoetImports);
		// Group regular and static imports into separate blocks, as the legacy generator
		// does, preserving order within each block.
		List<String> regular = new ArrayList<>();
		List<String> statics = new ArrayList<>();
		for (String imp : ordered) {
			(imp.startsWith("import static ") ? statics : regular).add(imp);
		}
		StringBuilder sb = new StringBuilder();
		sb.append(packageLine).append('\n').append('\n');
		for (String imp : regular) {
			sb.append(imp).append('\n');
		}
		if (!statics.isEmpty()) {
			if (!regular.isEmpty()) {
				sb.append('\n');
			}
			for (String imp : statics) {
				sb.append(imp).append('\n');
			}
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
