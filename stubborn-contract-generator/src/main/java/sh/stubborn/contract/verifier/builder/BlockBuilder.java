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

import org.apache.commons.lang3.StringUtils;

/**
 * Builds a block of code. Allows to start, end, indent etc. pieces of code.
 *
 * @author Jakub Kubrynski, codearte.io
 * @since 1.0.0
 */
public class BlockBuilder {

	private final StringBuilder builder;

	private final String spacer;

	private int indents;

	private String lineEnding = "";

	private String labelPrefix = "";

	/**
	 * @param spacer char used for spacing.
	 */
	public BlockBuilder(String spacer) {
		this.spacer = spacer;
		this.builder = new StringBuilder();
	}

	/**
	 * Setup line ending.
	 * @param lineEnding the line ending to use
	 * @return this block builder
	 */
	public BlockBuilder setupLineEnding(String lineEnding) {
		this.lineEnding = lineEnding;
		return this;
	}

	/**
	 * Setup label prefix.
	 * @param labelPrefix the label prefix to use
	 * @return this block builder
	 */
	public BlockBuilder setupLabelPrefix(String labelPrefix) {
		this.labelPrefix = labelPrefix;
		return this;
	}

	public String getLineEnding() {
		return this.lineEnding;
	}

	/**
	 * Adds indents to start a new block.
	 * @param label the label to append
	 * @return this block builder
	 */
	public BlockBuilder appendWithLabelPrefix(String label) {
		return append(this.labelPrefix).append(label);
	}

	/**
	 * Adds indents to start a new block.
	 * @return this block builder
	 */
	public BlockBuilder startBlock() {
		this.indents++;
		return this;
	}

	/**
	 * Ends block by removing indents.
	 * @return this block builder
	 */
	public BlockBuilder endBlock() {
		this.indents--;
		return this;
	}

	/**
	 * Creates a block and adds indents.
	 * @return this block builder
	 */
	public BlockBuilder indent() {
		startBlock().startBlock();
		return this;
	}

	/**
	 * Removes indents and closes the block.
	 * @return this block builder
	 */
	public BlockBuilder unindent() {
		endBlock().endBlock();
		return this;
	}

	public BlockBuilder addLine(String line) {
		return addIndented(line).append("\n");
	}

	public BlockBuilder addIndented(String line) {
		return addIndentation().append(line);
	}

	public BlockBuilder addIndented(Runnable runnable) {
		addIndentation();
		runnable.run();
		return this;
	}

	public BlockBuilder addLineWithEnding(String line) {
		addIndentation();
		append(line).addEndingIfNotPresent().addEmptyLine();
		return this;
	}

	public BlockBuilder addEndingIfNotPresent() {
		addAtTheEnd(this.lineEnding);
		return this;
	}

	public BlockBuilder addEmptyLine() {
		this.builder.append("\n");
		return this;
	}

	public BlockBuilder appendWithSpace(String text) {
		return addAtTheEnd(" ").append(text);
	}

	public BlockBuilder appendWithSpace(Runnable runnable) {
		addAtTheEnd(" ");
		runnable.run();
		return this;
	}

	public BlockBuilder append(Runnable runnable) {
		runnable.run();
		return this;
	}

	public BlockBuilder append(String string) {
		this.builder.append(string);
		return this;
	}

	public BlockBuilder addIndentation() {
		for (int i = 0; i < this.indents; i++) {
			this.builder.append(this.spacer);
		}
		return this;
	}

	BlockBuilder inBraces(Runnable runnable) {
		this.builder.append("{\n");
		startBlock();
		runnable.run();
		endBlock();
		addAtTheEnd("\n");
		addLine("}");
		return this;
	}

	public boolean endsWith(String text) {
		return this.builder.toString().endsWith(text);
	}

	public BlockBuilder addAtTheEndIfEndsWithAChar(String toAdd) {
		if (this.builder.length() == 0) {
			return this;
		}
		char lastChar = this.builder.charAt(this.builder.length() - 1);
		if (Character.isLetter(lastChar)) {
			this.builder.append(toAdd);
		}
		return this;
	}

	/**
	 * Adds the given text at the end of the line.
	 * @param toAdd the text to add
	 * @return updated BlockBuilder
	 */
	public BlockBuilder addAtTheEnd(String toAdd) {
		if (this.builder.length() == 0) {
			// Nothing has been emitted yet, so there is no line to terminate. The real
			// generation pipeline always primes the buffer (method signature/indentation)
			// before this runs; the model path's LegacyMethodBodyExtractor drives a fresh
			// buffer, where a leading addEndingIfNotPresent() would otherwise index -1.
			return this;
		}
		String lastChar = String.valueOf(this.builder.charAt(this.builder.length() - 1));
		String secondLastChar = (this.builder.length() >= 2)
				? String.valueOf(this.builder.charAt(this.builder.length() - 2)) : "";
		boolean isEndWithNewLine = endsWithNewLine(lastChar);
		boolean lastCharSpecial = aSpecialSign(lastChar, toAdd);
		boolean secondLastCharSpecial = aSpecialSign(secondLastChar, toAdd);
		boolean lineEndingToAdd = toAdd.equals(this.lineEnding);
		// lastChar = [;] , toAdd = [;]
		if (lastChar.equals(toAdd)) {
			return this;
		}
		// secondLastChar = [ ], lastChar = [{] , toAdd = [;]
		else if ((!isEndWithNewLine && lastCharSpecial) && lineEndingToAdd) {
			return this;
		}
		// secondLastChar = [{], lastChar = [\n] , toAdd = [;]
		else if (isEndWithNewLine && secondLastCharSpecial) {
			return this;
		}
		else if (isEndWithNewLine && !secondLastCharSpecial) {
			this.builder.replace(this.builder.length() - 1, this.builder.length(), toAdd);
			this.builder.append("\n");
		}
		else {
			this.builder.append(toAdd);
		}
		return this;
	}

	private boolean endsWithNewLine(String character) {
		return character.equals("\n");
	}

	private boolean aSpecialSign(String character, String toAdd) {
		if (StringUtils.isEmpty(character)) {
			return false;
		}
		return character.equals("{") || (character.equals(this.spacer) && toAdd.equals(this.spacer))
				|| (character.equals(this.spacer) && toAdd.equals(" ")) || character.equals(toAdd)
				|| (endsWithNewLine(character) && StringUtils.equalsAny(toAdd, "\n", " ", this.lineEnding));
	}

	/**
	 * Updates the current text with the provided one.
	 * @param contents text to replace the current content with
	 * @return updated Block Builder
	 */
	public BlockBuilder updateContents(String contents) {
		this.builder.replace(0, this.builder.length(), contents);
		return this;
	}

	@Override
	public String toString() {
		return this.builder.toString();
	}

}
