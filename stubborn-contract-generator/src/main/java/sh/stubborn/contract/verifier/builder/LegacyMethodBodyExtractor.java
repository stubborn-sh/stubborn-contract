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
		BlockBuilder bb = new BlockBuilder("\t");
		// Java targets: JavaClassMetaData sets these; the extractor bypasses
		// GeneratedTestClassBuilder.build() so they must be set manually here.
		bb.setupLineEnding(";").setupLabelPrefix("// ");
		SingleMethodBuilder smb = new JavaTestGenerator().singleMethodBuilder(bb, meta);
		smb.buildSingleMethodBody(contract);
		return bb.toString().lines().collect(Collectors.toList());
	}

}
