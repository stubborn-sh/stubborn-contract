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

import java.util.Objects;

import org.jspecify.annotations.Nullable;
import sh.stubborn.contract.verifier.config.ContractVerifierConfigProperties;
import sh.stubborn.contract.verifier.file.SingleContractMetadata;
import sh.stubborn.contract.verifier.messaging.avro.AvroMetadata;
import sh.stubborn.contract.verifier.messaging.kafka.KafkaMetadata;

/**
 * Typed body check for a messaging contract that declares
 * {@code metadata.kafka.avro.schema}, active only when {@code assertAvroTypes} is on. It
 * reads the received record with the contract's schema, which fails on any change Schema
 * Registry would reject, then emits one value assertion per field the contract names. All
 * Avro access lives in {@link AvroBodyAssertions}, so this class loads without Avro on
 * the classpath.
 *
 * @author Jakub Havlik
 * @since 0.2.5
 */
class GenericAvroBodyThen implements Then {

	private static final boolean AVRO_PRESENT = isAvroPresent();

	private final BlockBuilder blockBuilder;

	private final GeneratedClassMetaData generatedClassMetaData;

	private final BodyParser bodyParser;

	GenericAvroBodyThen(BlockBuilder blockBuilder, GeneratedClassMetaData metaData, BodyParser bodyParser) {
		this.blockBuilder = blockBuilder;
		this.generatedClassMetaData = metaData;
		this.bodyParser = bodyParser;
	}

	@Override
	public MethodVisitor<Then> apply(SingleContractMetadata metadata) {
		if (!AVRO_PRESENT) {
			throw new IllegalStateException(
					"assertAvroTypes needs org.apache.avro:avro on the test generator classpath. Add it as a dependency of the contract plugin.");
		}
		AvroBodyAssertions
			.lines(this.bodyParser, Objects.requireNonNull(avroSchema(metadata)),
					metadata.getContractMetadata().getPath(), this.bodyParser.convertResponseBody(metadata),
					this.bodyParser.responseBodyMatchers(metadata))
			.forEach(this.blockBuilder::addLineWithEnding);
		return this;
	}

	@Override
	public boolean accept(SingleContractMetadata metadata) {
		return appliesTo(this.generatedClassMetaData.configProperties, metadata);
	}

	static boolean appliesTo(ContractVerifierConfigProperties properties, SingleContractMetadata metadata) {
		return Boolean.TRUE.equals(properties.getAssertAvroTypes()) && metadata.isMessaging()
				&& avroSchema(metadata) != null;
	}

	private static @Nullable String avroSchema(SingleContractMetadata metadata) {
		AvroMetadata avro = KafkaMetadata.fromMetadata(metadata.getContract().getMetadata()).getAvro();
		return (avro != null) ? avro.getSchema() : null;
	}

	private static boolean isAvroPresent() {
		try {
			Class.forName("org.apache.avro.Schema", false, GenericAvroBodyThen.class.getClassLoader());
			return true;
		}
		catch (ClassNotFoundException ex) {
			return false;
		}
	}

}
