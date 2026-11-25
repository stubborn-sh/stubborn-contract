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

/**
 * Imports of the typed Avro assertions, added when at least one contract of the class
 * uses {@link GenericAvroBodyThen}: the assertion type and, as a static import, its entry
 * point.
 *
 * @author Jakub Havlik
 * @since 0.2.5
 */
class AvroImports implements Imports {

	private static final String TYPE = "sh.stubborn.contract.verifier.messaging.avro.GenericRecordAssert";

	private final BlockBuilder blockBuilder;

	private final GeneratedClassMetaData generatedClassMetaData;

	private final String line;

	private AvroImports(BlockBuilder blockBuilder, GeneratedClassMetaData generatedClassMetaData, String line) {
		this.blockBuilder = blockBuilder;
		this.generatedClassMetaData = generatedClassMetaData;
		this.line = line;
	}

	static AvroImports type(BlockBuilder blockBuilder, GeneratedClassMetaData generatedClassMetaData) {
		return new AvroImports(blockBuilder, generatedClassMetaData, "import " + TYPE);
	}

	static AvroImports entryPoint(BlockBuilder blockBuilder, GeneratedClassMetaData generatedClassMetaData) {
		return new AvroImports(blockBuilder, generatedClassMetaData, "import static " + TYPE + ".assertThatAvro");
	}

	@Override
	public Imports call() {
		this.blockBuilder.addLineWithEnding(this.line);
		return this;
	}

	@Override
	public boolean accept() {
		return usesAvro(this.generatedClassMetaData);
	}

	static boolean usesAvro(GeneratedClassMetaData generatedClassMetaData) {
		return generatedClassMetaData.listOfFiles.stream()
			.anyMatch((metadata) -> metadata.getConvertedContractWithMetadata()
				.stream()
				.anyMatch((contract) -> GenericAvroBodyThen.appliesTo(generatedClassMetaData.configProperties,
						contract)));
	}

}
