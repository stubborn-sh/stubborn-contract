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

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Mutation-focused coverage for {@link BlockBuilder}. Every assertion pins a concrete
 * rendered value and the returned builder reference so that NegateConditionals,
 * ConditionalsBoundary and NullReturn mutants are killed.
 */
class BlockBuilderMutationTests {

	private BlockBuilder builder() {
		return new BlockBuilder("\t").setupLineEnding(";");
	}

	// --- addAtTheEnd: every early "return this" must be observed as the same builder ---

	@Test
	void addAtTheEnd_on_empty_builder_returns_same_builder_and_no_change() {
		BlockBuilder bb = builder();
		BlockBuilder result = bb.addAtTheEnd(";");
		assertThat(result).isSameAs(bb);
		assertThat(bb.toString()).isEmpty();
	}

	@Test
	void addAtTheEnd_when_last_char_equals_toAdd_returns_same_and_no_change() {
		BlockBuilder bb = builder();
		bb.append("foo;");
		BlockBuilder result = bb.addAtTheEnd(";");
		assertThat(result).isSameAs(bb);
		assertThat(bb.toString()).isEqualTo("foo;");
	}

	@Test
	void addAtTheEnd_when_last_special_char_and_line_ending_returns_same_and_no_change() {
		BlockBuilder bb = builder();
		bb.append("foo{");
		BlockBuilder result = bb.addAtTheEnd(";");
		assertThat(result).isSameAs(bb);
		assertThat(bb.toString()).isEqualTo("foo{");
	}

	@Test
	void addAtTheEnd_when_ends_with_newline_and_second_last_special_returns_same_and_no_change() {
		BlockBuilder bb = builder();
		bb.append("foo{\n");
		BlockBuilder result = bb.addAtTheEnd(";");
		assertThat(result).isSameAs(bb);
		assertThat(bb.toString()).isEqualTo("foo{\n");
	}

	@Test
	void addAtTheEnd_when_ends_with_newline_and_second_last_not_special_replaces_newline() {
		BlockBuilder bb = builder();
		bb.append("foo\n");
		BlockBuilder result = bb.addAtTheEnd(";");
		assertThat(result).isSameAs(bb);
		assertThat(bb.toString()).isEqualTo("foo;\n");
	}

	@Test
	void addAtTheEnd_plain_append_returns_same_builder() {
		BlockBuilder bb = builder();
		bb.append("foo");
		BlockBuilder result = bb.addAtTheEnd(";");
		assertThat(result).isSameAs(bb);
		assertThat(bb.toString()).isEqualTo("foo;");
	}

	// --- L212 boundary: (length >= 2). A length-2 buffer ending in newline whose first
	// char is special must be preserved; the `>= 2 -> > 2` boundary mutant would drop the
	// second-last char, treat it as non-special, and rewrite the newline. ---

	@Test
	void addAtTheEnd_two_char_buffer_ending_in_newline_uses_second_last_char() {
		BlockBuilder bb = builder();
		bb.append("{\n");
		bb.addAtTheEnd(";");
		assertThat(bb.toString()).isEqualTo("{\n");
	}

	// --- L246 aSpecialSign empty-character branch: a length-1 "\n" buffer means
	// secondLastChar is "" -> aSpecialSign must return false, so the newline is
	// rewritten.
	// The BooleanTrueReturn mutant (return true) would instead preserve "\n". ---

	@Test
	void addAtTheEnd_single_newline_buffer_rewrites_newline_to_ending() {
		BlockBuilder bb = builder();
		bb.append("\n");
		bb.addAtTheEnd(";");
		assertThat(bb.toString()).isEqualTo(";\n");
	}

	// --- L248 aSpecialSign "{" branch via the non-newline path. ---

	@Test
	void addAtTheEnd_brace_is_special_so_line_ending_is_not_appended() {
		BlockBuilder bb = builder();
		bb.append("foo{");
		bb.addAtTheEnd(";");
		assertThat(bb.toString()).isEqualTo("foo{");
		// contrast: a non-special last char DOES get the ending appended
		BlockBuilder other = builder();
		other.append("fooa");
		other.addAtTheEnd(";");
		assertThat(other.toString()).isEqualTo("fooa;");
	}

	// --- L249 aSpecialSign (char == spacer && toAdd == " ") branch. Uses spacer == tab,
	// lineEnding == " " so that a trailing tab is "special" and the space ending is
	// suppressed. Negating either half of the && appends the space. ---

	@Test
	void addAtTheEnd_tab_before_space_ending_is_special_and_suppressed() {
		BlockBuilder bb = new BlockBuilder("\t").setupLineEnding(" ");
		bb.append("x\t");
		bb.addAtTheEnd(" ");
		assertThat(bb.toString()).isEqualTo("x\t");
	}

	// --- addAtTheEndIfEndsWithAChar ---

	@Test
	void addAtTheEndIfEndsWithAChar_on_empty_returns_same_and_no_change() {
		BlockBuilder bb = builder();
		BlockBuilder result = bb.addAtTheEndIfEndsWithAChar(" ");
		assertThat(result).isSameAs(bb);
		assertThat(bb.toString()).isEmpty();
	}

	@Test
	void addAtTheEndIfEndsWithAChar_appends_after_letter_and_returns_same() {
		BlockBuilder bb = builder();
		bb.append("foo");
		BlockBuilder result = bb.addAtTheEndIfEndsWithAChar(" ");
		assertThat(result).isSameAs(bb);
		assertThat(bb.toString()).isEqualTo("foo ");
	}

	@Test
	void addAtTheEndIfEndsWithAChar_does_nothing_after_non_letter_but_returns_same() {
		BlockBuilder bb = builder();
		bb.append("foo\n");
		BlockBuilder result = bb.addAtTheEndIfEndsWithAChar(" ");
		assertThat(result).isSameAs(bb);
		assertThat(bb.toString()).isEqualTo("foo\n");
	}

	// --- addIndented(Runnable): return this + runnable executed after indentation ---

	@Test
	void addIndented_runnable_indents_then_runs_and_returns_same() {
		BlockBuilder bb = builder();
		bb.startBlock();
		AtomicInteger runs = new AtomicInteger();
		BlockBuilder result = bb.addIndented(() -> {
			runs.incrementAndGet();
			bb.append("body");
		});
		assertThat(result).isSameAs(bb);
		assertThat(runs.get()).isEqualTo(1);
		assertThat(bb.toString()).isEqualTo("\tbody");
	}

	// --- addIndentation: exact number of spacers for the current indent depth ---

	@Test
	void addIndentation_writes_one_spacer_per_indent_level() {
		BlockBuilder bb = builder();
		bb.startBlock().startBlock().startBlock();
		bb.addIndentation();
		assertThat(bb.toString()).isEqualTo("\t\t\t");
	}

	@Test
	void addIndentation_writes_nothing_at_zero_indent() {
		BlockBuilder bb = builder();
		bb.addIndentation();
		assertThat(bb.toString()).isEmpty();
	}

	// --- fluent "return this" methods: capture the reference so NullReturn is killed ---

	@Test
	void setupLabelPrefix_returns_same_and_is_applied() {
		BlockBuilder bb = builder();
		BlockBuilder result = bb.setupLabelPrefix("L:");
		assertThat(result).isSameAs(bb);
		bb.appendWithLabelPrefix("x");
		assertThat(bb.toString()).isEqualTo("L:x");
	}

	@Test
	void addLine_returns_same_and_appends_newline() {
		BlockBuilder bb = builder();
		BlockBuilder result = bb.addLine("foo");
		assertThat(result).isSameAs(bb);
		assertThat(bb.toString()).isEqualTo("foo\n");
	}

	@Test
	void append_runnable_returns_same_and_runs() {
		BlockBuilder bb = builder();
		BlockBuilder result = bb.append(() -> bb.append("ran"));
		assertThat(result).isSameAs(bb);
		assertThat(bb.toString()).isEqualTo("ran");
	}

	@Test
	void appendWithSpace_string_returns_same_and_inserts_space() {
		BlockBuilder bb = builder();
		bb.append("a");
		BlockBuilder result = bb.appendWithSpace("b");
		assertThat(result).isSameAs(bb);
		assertThat(bb.toString()).isEqualTo("a b");
	}

	@Test
	void appendWithSpace_runnable_returns_same_and_inserts_space_then_runs() {
		BlockBuilder bb = builder();
		bb.append("a");
		BlockBuilder result = bb.appendWithSpace(() -> bb.append("b"));
		assertThat(result).isSameAs(bb);
		assertThat(bb.toString()).isEqualTo("a b");
	}

	@Test
	void endsWith_is_true_only_when_suffix_matches() {
		BlockBuilder bb = builder();
		bb.append("foobar");
		assertThat(bb.endsWith("bar")).isTrue();
		assertThat(bb.endsWith("baz")).isFalse();
	}

	@Test
	void inBraces_wraps_body_and_returns_same() {
		BlockBuilder bb = new BlockBuilder(" ");
		BlockBuilder result = bb.inBraces(() -> bb.append("body"));
		assertThat(result).isSameAs(bb);
		assertThat(bb.toString()).isEqualTo("{\nbody\n}\n");
	}

	@Test
	void updateContents_replaces_everything_and_returns_same() {
		BlockBuilder bb = builder();
		bb.append("old content");
		BlockBuilder result = bb.updateContents("new");
		assertThat(result).isSameAs(bb);
		assertThat(bb.toString()).isEqualTo("new");
	}

}
