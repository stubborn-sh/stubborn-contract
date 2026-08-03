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

/**
 * Escape hatch that captures the class-level field declarations the legacy generator
 * would emit for a class, so the model path can render them alongside the JavaPoet
 * scaffold.
 *
 * <p>
 * The Java targets carry a small, fixed set of class fields for the non-MockMvc shapes:
 * the messaging collaborators ({@code contractVerifierMessaging},
 * {@code contractVerifierObjectMapper}) and the CUSTOM-mode {@code httpVerifier}. These
 * are produced by the legacy {@link Field} visitors ({@link MessagingFields},
 * {@link CustomModeFields}); this extractor drives them against a fresh
 * {@link BlockBuilder} and returns the emitted declaration lines, mirroring
 * {@code ClassBodyBuilder#visit} so the output is byte-identical to the legacy field
 * block.
 *
 * @author Marcin Grzejszczak
 */
final class LegacyClassFieldExtractor {

	/**
	 * Captures the legacy class-field declaration lines for a class.
	 * @param meta the class-level metadata
	 * @return the field declaration lines (e.g.
	 * {@code @Autowired ContractVerifierMessaging contractVerifierMessaging;}), without
	 * blank lines; empty when the class has no such fields
	 */
	List<String> fieldLines(GeneratedClassMetaData meta) {
		BlockBuilder bb = new BlockBuilder("\t");
		bb.setupLineEnding(";").setupLabelPrefix("// ");
		List<Field> fields = new ArrayList<>();
		fields.add(new MessagingFields(bb, meta));
		fields.add(new CustomModeFields(bb, meta));
		List<Field> accepted = fields.stream().filter(Field::accept).toList();
		Iterator<Field> iterator = accepted.iterator();
		while (iterator.hasNext()) {
			iterator.next().call();
			bb.addEndingIfNotPresent();
			if (iterator.hasNext()) {
				bb.addEmptyLine();
			}
		}
		return bb.toString().lines().map(String::strip).filter((line) -> !line.isEmpty()).toList();
	}

}
