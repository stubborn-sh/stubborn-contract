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
 * Renders the Groovy/Spock target of a {@link TestClassModel} into source using
 * <a href="https://github.com/jknack/handlebars.java">Handlebars</a> templates.
 *
 * <p>
 * The Java targets render through the sibling {@link JavaTestRenderer}; this renders the
 * Groovy/Spock target from its own template files ({@code templates/spock/spec.hbs} and
 * its {@code method.hbs} partial). The templates carry the fixed Groovy layout —
 * {@code package}/{@code import} lines without semicolons,
 * {@code class … extends Specification}, {@code def …()} feature methods, bare
 * {@code given:}/{@code when:} labels — while the variable parts (imports, class/method
 * annotations, fields, method bodies) are prepared here and spliced in. The method bodies
 * are captured verbatim from the legacy Groovy pipeline, as with the Java targets.
 *
 * @author Marcin Grzejszczak
 * @since 1.0.0
 */
class SpockTestRenderer {

	private static final String DEFAULT_BASE_CLASS = "Specification";

	private final Template template;

	SpockTestRenderer() {
		Handlebars handlebars = new Handlebars(new ClassPathTemplateLoader("/templates/spock", ".hbs"));
		try {
			this.template = handlebars.compile("spec");
		}
		catch (IOException ex) {
			throw new UncheckedIOException("Failed to load the Spock templates", ex);
		}
	}

	/**
	 * Renders the given model into Groovy/Spock source.
	 * @param model the class model; {@link TestClassModel#spock()} must be {@code true}
	 * @return the generated Groovy source
	 * @throws IllegalArgumentException if the model does not target Spock
	 */
	String render(TestClassModel model) {
		if (!model.spock()) {
			throw new IllegalArgumentException("SpockTestRenderer renders Groovy/Spock targets only");
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
			throw new UncheckedIOException("Failed to render the Spock test class", ex);
		}
	}

	/**
	 * Groups the captured legacy import lines (no semicolons, Groovy style) into a
	 * regular block and a static block separated by one blank line, preserving order
	 * within each — exactly as the legacy generator lays them out.
	 * @param importDeclarations the captured {@code import …} lines, in order
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
	 * terminated by a newline, interior blank lines (between distinct legacy field
	 * groups) preserved. Empty when the class declares no fields.
	 * @param fieldLines the captured field lines (empty strings mark interior blanks)
	 * @return the formatted field block, or an empty string
	 */
	private String fields(List<String> fieldLines) {
		StringBuilder sb = new StringBuilder();
		for (String field : fieldLines) {
			if (!field.isEmpty()) {
				sb.append('\t').append(field);
			}
			sb.append('\n');
		}
		return sb.toString();
	}

	private List<Map<String, Object>> methods(List<TestMethodModel> methodModels) {
		List<Map<String, Object>> methods = new ArrayList<>();
		for (TestMethodModel method : methodModels) {
			Map<String, Object> map = new LinkedHashMap<>();
			// Spock's only method annotation is @Ignore; the model carries it (and
			// nothing
			// else) for an ignored/in-progress contract.
			map.put("ignore", !method.annotations().isEmpty());
			map.put("name", method.name());
			map.put("body", body(method.bodyLines()));
			methods.add(map);
		}
		return methods;
	}

	/**
	 * Joins the verbatim body lines, each terminated by a newline, so the template can
	 * place the method's closing brace on the following line.
	 * @param bodyLines the verbatim body lines
	 * @return the joined body
	 */
	private String body(List<String> bodyLines) {
		StringBuilder sb = new StringBuilder();
		for (String line : bodyLines) {
			sb.append(line).append('\n');
		}
		return sb.toString();
	}

	private String baseClass(@Nullable String baseClass) {
		if (baseClass == null || baseClass.isBlank()) {
			return DEFAULT_BASE_CLASS;
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
