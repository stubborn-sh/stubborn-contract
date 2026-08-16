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
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import sh.stubborn.contract.verifier.config.TestFramework;

/**
 * Produces the class-level import block and field block for the model path directly from
 * the existing visitor producers, without the legacy class scaffold.
 *
 * <p>
 * It drives the same {@link Imports} and {@link Field} visitors — in the same order and
 * with the same per-framework line-ending/label setup — that the retired
 * {@code GeneratedTestClassBuilder}/{@code ClassBodyBuilder} used, then scrapes the
 * emitted lines. By construction the produced imports equal what the legacy generator's
 * import block emitted, and the produced field lines equal the legacy class-field block,
 * so the model path stays byte-identical to the legacy output.
 *
 * <p>
 * The {@code extraFieldLines} are a test hook (empty in production) that lets a caller
 * inject additional class-level fields — e.g. the JAX-RS {@code WebTarget} the tests
 * supply, which no production {@link Field} visitor declares.
 *
 * @author Marcin Grzejszczak
 * @since 1.0.0
 */
final class ClassScaffoldProducer {

	private final GeneratedClassMetaData metaData;

	private final List<String> extraFieldLines;

	ClassScaffoldProducer(GeneratedClassMetaData metaData, List<String> extraFieldLines) {
		this.metaData = metaData;
		this.extraFieldLines = extraFieldLines;
	}

	/**
	 * The class-level import declarations (both {@code import} and
	 * {@code import static}), in the legacy emission order.
	 * @return the import lines, each trimmed and starting with {@code import }
	 */
	List<String> importDeclarations() {
		BlockBuilder bb = newBlockBuilder();
		// Mirrors the ImportsBuilder chaining in the retired
		// GeneratedTestClassBuilder pipeline.
		List<Imports> imports = new ArrayList<>();
		imports.add(new DefaultImports(bb, this.metaData));
		imports.add(new UserImports(bb, this.metaData));
		imports.add(new CustomModeImports(bb, this.metaData));
		imports.add(new JsonPathImports(bb, this.metaData));
		imports.add(new JUnit5Imports(bb, this.metaData));
		imports.add(new JUnit5IgnoreImports(bb, this.metaData));
		imports.add(new JUnit5OrderImports(bb, this.metaData));
		imports.add(new TestNGImports(bb, this.metaData));
		imports.add(new SpockImports(bb, this.metaData));
		imports.add(new SpockIgnoreImports(bb, this.metaData));
		imports.add(new SpockOrderImports(bb, this.metaData));
		imports.add(new XmlImports(bb, this.metaData));
		imports.add(new MessagingImports(bb, this.metaData));
		imports.add(new MockMvcRestAssuredImports(bb, this.metaData));
		imports.add(new ExplicitRestAssuredImports(bb, this.metaData));
		imports.add(new WebTestClientRestAssuredImports(bb, this.metaData));
		imports.add(new JaxRsImports(bb, this.metaData));

		List<Imports> staticImports = new ArrayList<>();
		staticImports.add(new DefaultStaticImports(bb));
		staticImports.add(new UserStaticImports(bb, this.metaData));
		staticImports.add(new CustomModeStaticImports(bb, this.metaData));
		staticImports.add(new DefaultJsonStaticImports(bb, this.metaData));
		staticImports.add(new MessagingStaticImports(bb, this.metaData));
		staticImports.add(new MockMvcRestAssuredStaticImports(bb, this.metaData));
		staticImports.add(new ExplicitRestAssuredStaticImports(bb, this.metaData));
		staticImports.add(new WebTestClientRestAssured3StaticImports(bb, this.metaData));
		staticImports.add(new JaxRsStaticImports(bb, this.metaData));

		visitSeparated(bb, imports);
		visitSeparated(bb, staticImports);

		List<String> result = new ArrayList<>();
		for (String line : bb.toString().lines().toList()) {
			String trimmed = line.trim();
			if (trimmed.startsWith("import ")) {
				result.add(trimmed);
			}
		}
		return result;
	}

	/**
	 * The class-level field declarations (messaging collaborators, CUSTOM-mode
	 * {@code httpVerifier}, and any injected {@code extraFieldLines}), in the legacy
	 * emission order.
	 * @return the field lines, each stripped of indentation, with interior blank lines
	 * between visitor groups preserved and trailing blanks dropped; empty when the class
	 * declares no fields
	 */
	List<String> fieldLines() {
		BlockBuilder bb = newBlockBuilder();
		List<Field> fields = new ArrayList<>();
		fields.add(new MessagingFields(bb, this.metaData));
		fields.add(new CustomModeFields(bb, this.metaData));
		for (String line : this.extraFieldLines) {
			fields.add(new Field() {
				@Override
				public boolean accept() {
					return true;
				}

				@Override
				public Field call() {
					bb.addLine(line);
					return this;
				}
			});
		}
		visitFields(bb, fields);

		List<String> region = new ArrayList<>();
		for (String line : bb.toString().lines().toList()) {
			region.add(line.strip());
		}
		int end = region.size();
		while (end > 0 && region.get(end - 1).isEmpty()) {
			end--;
		}
		return List.copyOf(region.subList(0, end));
	}

	/**
	 * A fresh block builder configured for the target language. Mirrors
	 * {@code JavaClassMetaData}/{@code GroovyClassMetaData}: the Java targets use a
	 * {@code ;} line ending and {@code // } label prefix, while Groovy/Spock leaves both
	 * at their defaults.
	 * @return the configured block builder
	 */
	private BlockBuilder newBlockBuilder() {
		BlockBuilder bb = new BlockBuilder("\t");
		TestFramework framework = this.metaData.configProperties.getTestFramework();
		if (framework == TestFramework.JUNIT5 || framework == TestFramework.TESTNG) {
			bb.setupLineEnding(";").setupLabelPrefix("// ");
		}
		return bb;
	}

	// Mirrors GeneratedTestClassBuilder.visitSeparated -> visit(list, true, true).
	private void visitSeparated(BlockBuilder bb, List<? extends Visitor> list) {
		List<Visitor> elements = list.stream().filter(Acceptor::accept).collect(Collectors.toList());
		elements.forEach(OurCallable::call);
		bb.addEndingIfNotPresent();
		if (!elements.isEmpty()) {
			bb.addEmptyLine();
		}
	}

	// Mirrors ClassBodyBuilder.visit(fields) (without the enclosing inBraces, whose
	// indentation the field extraction strips anyway).
	private void visitFields(BlockBuilder bb, List<? extends Visitor> list) {
		List<? extends Visitor> visitors = list.stream().filter(Acceptor::accept).collect(Collectors.toList());
		Iterator<? extends Visitor> iterator = visitors.iterator();
		while (iterator.hasNext()) {
			iterator.next().call();
			bb.addEndingIfNotPresent();
			if (iterator.hasNext()) {
				bb.addEmptyLine();
			}
		}
	}

}
