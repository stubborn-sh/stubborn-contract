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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.io.ClassPathTemplateLoader;
import org.jspecify.annotations.Nullable;

/**
 * Renders the Java targets (JUnit 5 / TestNG / JUnit 4) of a {@link TestClassModel} into
 * source using <a href="https://github.com/jknack/handlebars.java">Handlebars</a>
 * templates ({@code templates/java/class.hbs} and its {@code method.hbs} partial).
 *
 * <p>
 * This is the sibling of {@link SpockTestRenderer}: both drive the same
 * {@link TestClassModel} through a fixed template that carries the invariant layout —
 * {@code package}/{@code import} lines, the class declaration, the field block and the
 * method framing — while the variable parts (imports, annotations, fields, method bodies)
 * are prepared here and spliced in. The Java template differs from the Spock one in three
 * ways: the base class is emitted conditionally ({@code extends} only when a base class
 * exists), field declarations are {@code ;}-terminated, and each method carries several
 * annotations ({@code @Test}, {@code @Disabled}, {@code @Test(enabled = false)}) rather
 * than Spock's single optional {@code @Ignore}.
 *
 * <p>
 * The method body is assembled here from the structured {@link RequestModel}/
 * {@link ResponseModel} (the {@code // given:}/{@code // when:}/{@code // then:} blocks)
 * at absolute indentation, followed by the verbatim {@code // and:} tail captured from
 * the legacy generator. Because the template controls the layout and annotations are
 * emitted by their simple name, the legacy post-processing passes (import merging, field
 * injection, trailing blank-line insertion and annotation unqualification) are no longer
 * needed.
 *
 * @author Marcin Grzejszczak
 */
class JavaPoetTestRenderer {

	private final Template template;

	JavaPoetTestRenderer() {
		Handlebars handlebars = new Handlebars(new ClassPathTemplateLoader("/templates/java", ".hbs"));
		try {
			this.template = handlebars.compile("class");
		}
		catch (IOException ex) {
			throw new UncheckedIOException("Failed to load the Java templates", ex);
		}
	}

	/**
	 * Renders the given model into Java source.
	 * @param model the class model; {@link TestClassModel#spock()} must be {@code false}
	 * @return the generated Java source
	 * @throws IllegalArgumentException if the model targets Spock, which this renderer
	 * cannot render
	 */
	String render(TestClassModel model) {
		if (model.spock()) {
			throw new IllegalArgumentException(
					"JavaPoetTestRenderer renders Java targets only; Spock is rendered by the SpockTestRenderer");
		}
		Map<String, Object> context = new LinkedHashMap<>();
		context.put("packageName", model.packageName());
		context.put("imports", imports(model.importDeclarations()));
		context.put("classAnnotations", classAnnotations(model.classAnnotations()));
		context.put("className", model.className());
		context.put("baseClass", baseClass(model.baseClass()));
		context.put("fields", fields(model.fields()));
		context.put("methods", methods(model.methods()));
		try {
			return this.template.apply(context);
		}
		catch (IOException ex) {
			throw new UncheckedIOException("Failed to render the Java test class", ex);
		}
	}

	/**
	 * Groups the captured legacy import lines into a regular block and a static block
	 * separated by one blank line, preserving order within each — exactly as the legacy
	 * generator lays them out. The model's import list is authoritative and already
	 * carries the Java {@code ;} terminators.
	 * @param importDeclarations the captured {@code import …;} lines, in order
	 * @return the formatted import section, without a trailing newline
	 */
	private String imports(List<String> importDeclarations) {
		List<String> regular = new ArrayList<>();
		List<String> statics = new ArrayList<>();
		for (String line : importDeclarations) {
			(line.startsWith("import static ") ? statics : regular).add(line);
		}
		StringBuilder sb = new StringBuilder();
		sb.append(String.join("\n", regular));
		if (!statics.isEmpty()) {
			if (!regular.isEmpty()) {
				sb.append("\n\n");
			}
			sb.append(String.join("\n", statics));
		}
		return sb.toString();
	}

	/**
	 * Renders the class-level annotations, one per line each terminated by a newline, so
	 * the {@code class} declaration follows immediately in the template.
	 * @param annotations the class annotations, in order
	 * @return the formatted annotation lines
	 */
	private String classAnnotations(List<AnnotationModel> annotations) {
		StringBuilder sb = new StringBuilder();
		for (AnnotationModel annotation : annotations) {
			sb.append(annotation(annotation)).append('\n');
		}
		return sb.toString();
	}

	/**
	 * Renders the class-level field block: each declaration indented one tab and
	 * {@code ;}-terminated, interior blank lines (between distinct legacy field groups)
	 * preserved. Empty when the class declares no fields. Mirrors the legacy per-line
	 * termination the JavaPoet renderer applied when injecting fields.
	 * @param fieldLines the captured field lines (empty strings mark interior blanks)
	 * @return the formatted field block, or an empty string
	 */
	private String fields(List<String> fieldLines) {
		StringBuilder sb = new StringBuilder();
		for (String field : fieldLines) {
			String trimmed = field.trim();
			if (!trimmed.isEmpty()) {
				sb.append('\t').append(trimmed.endsWith(";") ? trimmed : trimmed + ";");
			}
			sb.append('\n');
		}
		return sb.toString();
	}

	private List<Map<String, Object>> methods(List<TestMethodModel> methodModels) {
		List<Map<String, Object>> methods = new ArrayList<>();
		for (TestMethodModel method : methodModels) {
			Map<String, Object> map = new LinkedHashMap<>();
			List<String> annotations = new ArrayList<>();
			for (AnnotationModel annotation : method.annotations()) {
				annotations.add(annotation(annotation));
			}
			map.put("annotations", annotations);
			map.put("name", method.name());
			map.put("body", body(method));
			methods.add(map);
		}
		return methods;
	}

	/**
	 * Assembles a method body at absolute indentation, each line terminated by a newline
	 * so the template can place the method's closing brace on the following line. The
	 * structured {@code // given:}/{@code // when:} request chains and {@code // then:}
	 * response assertions carry baked-in indentation (label two tabs, chain head three,
	 * continuations five, then-statements three); the verbatim tail (the {@code // and:}
	 * response-body block, or the whole legacy body when the request is not structured)
	 * is captured at method-relative depth and shifted to its absolute position by the
	 * two-tab method base.
	 * @param method the method model
	 * @return the assembled body
	 */
	private String body(TestMethodModel method) {
		List<String> lines = new ArrayList<>();
		RequestModel request = method.request();
		if (request != null) {
			lines.add("\t\t// given:");
			appendFluentChain(lines, request.given().render());
			lines.add("");
			// Legacy (RestAssuredGiven#indentedBodyBlock) emits a second blank line after
			// the given block when it is empty (head only) or built from a
			// multipart/param
			// request; the remaining given shapes get a single blank.
			if (givenNeedsExtraBlank(request.given())) {
				lines.add("");
			}
			lines.add("\t\t// when:");
			appendFluentChain(lines, request.whenBlock().render());
			lines.add("");
		}
		ResponseModel response = method.response();
		if (response != null) {
			lines.add("\t\t// then:");
			for (String line : response.thenBlock().render()) {
				// Each assertion sits one level below the // then: label, as the legacy
				// GenericHttpThen block does.
				lines.add("\t\t\t" + line);
			}
		}
		// Emit the (remaining) body verbatim. It is captured at the method-relative depth
		// the legacy string builders produce (no method base), so the two enclosing
		// method
		// levels are added here — matching what the legacy JavaPoet renderer did when it
		// emitted the body inside the generated method.
		String verbatim = String.join("\n", method.bodyLines());
		if (!verbatim.isBlank()) {
			// Separate the verbatim // and: body block from the structured // then:
			// assertions that precede it.
			if (response != null) {
				lines.add("");
			}
			for (String line : method.bodyLines()) {
				lines.add(line.isEmpty() ? "" : "\t\t" + line);
			}
		}
		StringBuilder sb = new StringBuilder();
		for (String line : lines) {
			sb.append(line).append('\n');
		}
		return sb.toString();
	}

	/**
	 * Appends a fluent request chain ({@code // given:} / {@code // when:}) at the legacy
	 * indentation: the chain head sits one level below the label (three tabs) and each
	 * {@code .xxx(...)} continuation two further levels below the head (five tabs).
	 * Mirrors the legacy {@code RestAssuredGiven}/{@code RestAssuredWhen} layout.
	 * @param lines the body being assembled
	 * @param chain the rendered chain, head first
	 */
	private void appendFluentChain(List<String> lines, List<String> chain) {
		for (int i = 0; i < chain.size(); i++) {
			lines.add(((i == 0) ? "\t\t\t" : "\t\t\t\t\t") + chain.get(i));
		}
	}

	/**
	 * Whether the {@code // given:} block is followed by two blank lines rather than one.
	 * Mirrors the legacy {@code RestAssuredGiven}/{@code BodyMethodVisitor} layout: an
	 * empty given (only the {@code given()} head, no continuations) and a multipart/param
	 * given both emit an extra trailing blank line before the {@code // when:} block;
	 * every other given shape emits a single blank.
	 * @param given the rendered given chain
	 * @return {@code true} if a second blank line must follow the given block
	 */
	private boolean givenNeedsExtraBlank(FluentStatement given) {
		if (given.continuations().isEmpty()) {
			return true;
		}
		return given.continuations()
			.stream()
			.anyMatch((line) -> line.trim().startsWith(".multiPart(") || line.trim().startsWith(".param("));
	}

	private @Nullable String baseClass(@Nullable String baseClass) {
		if (baseClass == null || baseClass.isBlank()) {
			return null;
		}
		return simpleName(baseClass.trim());
	}

	private String annotation(AnnotationModel annotation) {
		String simple = simpleName(annotation.type());
		String member = annotation.memberCode();
		if (member == null || member.isBlank()) {
			return "@" + simple;
		}
		if ("value".equals(annotation.memberName())) {
			return "@" + simple + "(" + member + ")";
		}
		return "@" + simple + "(" + annotation.memberName() + " = " + member + ")";
	}

	private String simpleName(String type) {
		int lastDot = type.lastIndexOf('.');
		return (lastDot >= 0) ? type.substring(lastDot + 1) : type;
	}

}
