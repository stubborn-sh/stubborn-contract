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
import java.util.List;
import java.util.stream.Collectors;

import sh.stubborn.contract.verifier.file.SingleContractMetadata;

/**
 * Produces the verbatim {@code // and:} response-body assertion block for a single
 * contract by driving the retained {@link GenericHttpBodyThen} directly.
 *
 * <p>
 * This is the response-body tail of the model-based generator severed from the legacy
 * {@link SingleMethodBuilder}: instead of routing through
 * {@code JavaTestGenerator#singleMethodBuilder} and
 * {@code SingleMethodBuilder#buildResponseBodyOnly}, it constructs a
 * {@link GenericHttpBodyThen} for RestAssured Java targets and applies it against a fresh
 * {@link BlockBuilder}, capturing byte-identical output. It references neither
 * {@link SingleMethodBuilder} nor {@code JavaTestGenerator}.
 *
 * <p>
 * A <em>fresh</em> {@link BlockBuilder} per contract is required because
 * {@link TemplateUpdatingMethodPostProcessor} transforms the whole buffer; reusing a
 * shared buffer across contracts would let one method's post-processing bleed into
 * another.
 *
 * @author Marcin Grzejszczak
 */
final class ResponseBodyLineProducer {

	/**
	 * Captures only the verbatim {@code // and:} response-body assertion block for a
	 * single contract, used when the request portion and the {@code // then:}
	 * status/header assertions are emitted from the structured
	 * {@link RequestModel}/{@link ResponseModel} and only the response body assertions
	 * are still generated verbatim.
	 * @param meta the class-level metadata (shared across all contracts of the class)
	 * @param contract the contract whose {@code // and:} body block to render
	 * @return the {@code // and:} block lines, verbatim (may contain {@code ;} and the
	 * {@code // and:} label); an empty list when the response has no body
	 */
	List<String> andBlockLines(GeneratedClassMetaData meta, SingleContractMetadata contract) {
		BlockBuilder bb = new BlockBuilder("\t");
		// Java targets: JavaClassMetaData sets these; driving GenericHttpBodyThen
		// directly bypasses GeneratedTestClassBuilder.build() so they must be set here.
		bb.setupLineEnding(";").setupLabelPrefix("// ");
		// Mirror how restAssured()/buildResponseBodyOnly wire the body parser and
		// comparison builder for RestAssured Java targets.
		GenericHttpBodyThen bodyThen = new GenericHttpBodyThen(bb, meta, RestAssuredBodyParser.INSTANCE,
				ComparisonBuilder.JAVA_HTTP_INSTANCE);
		if (bodyThen.accept(contract)) {
			// GenericHttpBodyThen opens by closing the enclosing // then: block; prime a
			// balanced open block with a terminated placeholder statement so that opening
			// step has something to close. The placeholder (and the // then: transition
			// noise) is stripped below, keeping only the // and: block onward.
			bb.addIndentation().appendWithLabelPrefix("then:").addEmptyLine().startBlock();
			bb.addLineWithEnding("// placeholder");
			bodyThen.apply(contract);
			bb.addEndingIfNotPresent();
		}
		// Resolve templated response bodies, exactly as buildResponseBodyOnly does.
		MethodPostProcessor postProcessor = new TemplateUpdatingMethodPostProcessor(bb);
		if (postProcessor.accept(contract)) {
			postProcessor.apply(contract);
		}
		List<String> lines = bb.toString().lines().collect(Collectors.toList());
		// With no response body nothing is emitted, so no // and: label exists and the
		// tail is empty; otherwise keep only the // and: block onward.
		for (int i = 0; i < lines.size(); i++) {
			if (lines.get(i).trim().equals("// and:")) {
				return new ArrayList<>(lines.subList(i, lines.size()));
			}
		}
		return List.of();
	}

}
