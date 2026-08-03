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
import java.util.regex.Pattern;

/**
 * Escape hatch that captures the class-level field declarations the legacy generator
 * emits for a class, so the model path can render them alongside the JavaPoet scaffold.
 *
 * <p>
 * Rather than re-derive the fields from the {@link Field} visitors, this parses them
 * straight out of the source the legacy generator produced. That makes the capture
 * faithful to <em>whatever</em> the delegate emits: the production collaborators (the
 * messaging {@code contractVerifierMessaging}/{@code contractVerifierObjectMapper}, the
 * CUSTOM-mode {@code httpVerifier}) as well as any fields a caller adds through a
 * {@code JavaTestGenerator#classBodyBuilder} override — e.g. the {@code WebTarget} the
 * JAX-RS tests inject, which no production {@code Field} visitor declares.
 *
 * <p>
 * The legacy layout is: fields, a blank line, then methods. The field block (which may be
 * empty) sits immediately after the class' opening brace and a single blank line
 * separates it from the first method. The block runs until the first <em>method</em> —
 * detected as a test-method annotation ({@code @Test}/{@code @Disabled}/{@code @Ignore})
 * or a method signature (a line containing an open parenthesis and ending in an opening
 * brace). Using that boundary rather than a trailing {@code ;} means the same parse works
 * for the Groovy targets, whose field declarations carry no semicolon. Blank lines
 * <em>between</em> fields are possible, because
 * {@link ClassBodyBuilder#visit(java.util.List) visit} emits one between distinct
 * {@link Field} visitors (e.g. the messaging collaborators and an injected
 * {@code WebTarget} when a class mixes both); those interior blanks are kept verbatim so
 * the injected block reproduces the legacy layout byte for byte. Only the trailing blank
 * separating the fields from the first method is dropped.
 *
 * @author Marcin Grzejszczak
 */
final class LegacyClassFieldExtractor {

	private static final Pattern CLASS_DECLARATION = Pattern.compile("^(public\\s+)?(final\\s+)?class\\s+\\w+.*\\{$");

	/**
	 * Captures the legacy class-field block from a rendered legacy test class.
	 * @param legacySource the full source the legacy generator produced for the class
	 * @return the field block: each declaration stripped of indentation (e.g.
	 * {@code @Autowired ContractVerifierMessaging contractVerifierMessaging;} or
	 * {@code WebTarget webTarget;}), with any interior blank lines between visitor groups
	 * preserved as empty strings; empty when the class declares no fields
	 */
	List<String> fieldLines(String legacySource) {
		List<String> lines = legacySource.lines().toList();
		int classLine = -1;
		for (int i = 0; i < lines.size(); i++) {
			if (CLASS_DECLARATION.matcher(lines.get(i).strip()).matches()) {
				classLine = i;
				break;
			}
		}
		if (classLine < 0) {
			return List.of();
		}
		List<String> region = new ArrayList<>();
		for (int i = classLine + 1; i < lines.size(); i++) {
			String stripped = lines.get(i).strip();
			// The first method (its annotation or signature) closes the field block; a
			// stray
			// closing brace guards the degenerate field-less, method-less class.
			if (stripped.equals("}") || isMethodStart(stripped)) {
				break;
			}
			region.add(stripped);
		}
		// Drop the trailing blank line(s) separating the fields from the first method;
		// the
		// renderer re-adds exactly one when it splices the block in.
		int end = region.size();
		while (end > 0 && region.get(end - 1).isEmpty()) {
			end--;
		}
		return List.copyOf(region.subList(0, end));
	}

	/**
	 * Whether a stripped line begins the first method, closing the field block. That is a
	 * test-method annotation ({@code @Test}/{@code @Disabled}/{@code @Ignore}, including
	 * {@code @Test(...)}) or a method signature — a line that opens a brace after a
	 * parameter list. Field declarations never match either.
	 * @param stripped the indentation-stripped line
	 * @return {@code true} if the line starts the first method
	 */
	private static boolean isMethodStart(String stripped) {
		if (stripped.isEmpty()) {
			return false;
		}
		if (stripped.equals("@Ignore") || stripped.equals("@Disabled") || stripped.startsWith("@Test")) {
			return true;
		}
		return stripped.endsWith("{") && stripped.contains("(");
	}

}
