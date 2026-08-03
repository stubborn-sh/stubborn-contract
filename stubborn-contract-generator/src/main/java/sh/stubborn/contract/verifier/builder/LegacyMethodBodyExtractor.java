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
import java.util.stream.Collectors;

import sh.stubborn.contract.verifier.config.TestFramework;
import sh.stubborn.contract.verifier.file.SingleContractMetadata;

/**
 * Escape hatch that drives the legacy generation pipeline against a fresh
 * {@link BlockBuilder} to capture the body lines of a single method.
 *
 * <p>
 * During the test-generation migration the class scaffold (package, imports, annotations,
 * signatures) is produced from the {@link TestClassModel} by
 * {@link JavaPoetTestRenderer}, while each method's body is still generated verbatim by
 * the legacy string builders. This extractor bridges the two: it wires the legacy
 * {@link SingleMethodBuilder} for one contract and returns the emitted body lines so the
 * renderer can drop them into the JavaPoet-produced method.
 *
 * <p>
 * A <em>fresh</em> {@link BlockBuilder} per contract is required because
 * {@code TemplateUpdatingMethodPostProcessor} transforms the whole buffer; reusing a
 * shared buffer across contracts would let one method's post-processing bleed into
 * another.
 *
 * @author Marcin Grzejszczak
 */
final class LegacyMethodBodyExtractor {

	/**
	 * Captures the legacy body lines for a single contract.
	 * @param meta the class-level metadata (shared across all contracts of the class)
	 * @param contract the contract whose method body to render
	 * @return the body lines, verbatim (may contain {@code ;}, {@code // given:} labels
	 * and blank lines), without the method signature, annotations or braces
	 */
	List<String> bodyLines(GeneratedClassMetaData meta, SingleContractMetadata contract) {
		BlockBuilder bb = blockBuilder(meta);
		SingleMethodBuilder smb = new JavaTestGenerator().singleMethodBuilder(bb, meta);
		smb.buildSingleMethodBody(contract);
		return bb.toString().lines().collect(Collectors.toList());
	}

	/**
	 * A fresh {@link BlockBuilder} configured for the target language. The extractor
	 * bypasses {@code GeneratedTestClassBuilder.build()}, so the line-ending and label
	 * prefix the class-metadata visitors would set must be applied manually here: the
	 * Java targets ({@code JavaClassMetaData}) use a {@code ;} line ending and
	 * {@code // } label prefix, while Groovy/Spock ({@code GroovyClassMetaData}) leaves
	 * both empty — bare {@code given:}/{@code when:} labels and no semicolons. Without
	 * this the Spock body would be rendered with Java punctuation.
	 * @param meta the class-level metadata (carries the target framework)
	 * @return the configured block builder
	 */
	private BlockBuilder blockBuilder(GeneratedClassMetaData meta) {
		BlockBuilder bb = new BlockBuilder("\t");
		if (meta.configProperties.getTestFramework() == TestFramework.SPOCK) {
			// Emit the Groovy body at its absolute depth — the two enclosing levels
			// (class
			// body + method body). Indenting at the block level (not per physical line)
			// keeps multi-line string literals' embedded newlines flush-left, exactly as
			// the
			// legacy full pipeline nests them; a naive per-line indent would wrongly push
			// the
			// string continuations right.
			bb.indent();
		}
		else {
			bb.setupLineEnding(";").setupLabelPrefix("// ");
		}
		return bb;
	}

	/**
	 * Captures only the verbatim {@code // then:} block for a single contract, used when
	 * the request portion is emitted from the structured {@link RequestModel} and only
	 * the response assertions are still taken from the legacy pipeline.
	 * @param meta the class-level metadata (shared across all contracts of the class)
	 * @param contract the contract whose {@code then} block to render
	 * @return the {@code then} block lines, verbatim (may contain {@code ;},
	 * {@code // then:}/{@code // and:} labels and blank lines)
	 */
	List<String> responseBodyLines(GeneratedClassMetaData meta, SingleContractMetadata contract) {
		BlockBuilder bb = blockBuilder(meta);
		SingleMethodBuilder smb = new JavaTestGenerator().singleMethodBuilder(bb, meta);
		smb.buildThenOnly(contract);
		return bb.toString().lines().collect(Collectors.toList());
	}

}
