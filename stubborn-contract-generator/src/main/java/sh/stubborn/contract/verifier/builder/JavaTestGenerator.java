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

import java.util.Collection;

import sh.stubborn.contract.verifier.config.ContractVerifierConfigProperties;
import sh.stubborn.contract.verifier.file.ContractMetadata;

/**
 * Provides the method-body engine for the model-based generator. The class scaffold
 * (package, imports, annotations, fields, signatures) is produced by the model path
 * ({@link ModelBuilder} + {@link ClassScaffoldProducer} + {@link JavaTestRenderer}); this
 * class retains only {@link #singleMethodBuilder(BlockBuilder, GeneratedClassMetaData)
 * singleMethodBuilder}, which drives the Given/When/Then producers to emit a single
 * method's body.
 *
 * @author Marcin Grzejszczak
 * @since 1.0.0
 */
public class JavaTestGenerator implements SingleTestGenerator {

	/**
	 * @deprecated the legacy class scaffold has been removed; the model path
	 * ({@link ModelBasedTestGenerator}) builds the class. This method is retained only so
	 * the type keeps satisfying {@link SingleTestGenerator} while it serves as the
	 * method-body engine; it always throws.
	 */
	@Override
	@Deprecated
	public String buildClass(ContractVerifierConfigProperties properties, Collection<ContractMetadata> listOfFiles,
			String includedDirectoryRelativePath, GeneratedClassData generatedClassData) {
		throw new UnsupportedOperationException(
				"The legacy class scaffold has been removed; use ModelBasedTestGenerator to build a class. "
						+ "JavaTestGenerator now only provides the method-body engine via singleMethodBuilder(...).");
	}

	SingleMethodBuilder singleMethodBuilder(BlockBuilder builder, GeneratedClassMetaData metaData) {
		// @formatter:off
		return SingleMethodBuilder.builder(builder, metaData)
				.methodAnnotation()
					.jUnit5()
					.testNG()
					.spock()
					.build()
				.methodMetadata()
					.jUnit()
					.spock()
					.build()
				.restAssured()
				.customMode()
				.jaxRs()
				.messaging();
		// @formatter:on
	}

}
