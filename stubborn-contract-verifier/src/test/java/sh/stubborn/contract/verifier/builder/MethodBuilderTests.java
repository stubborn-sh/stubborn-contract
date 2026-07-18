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

import java.io.File;

import org.junit.jupiter.api.Test;
import sh.stubborn.contract.spec.Contract;
import sh.stubborn.contract.verifier.file.ContractMetadata;
import sh.stubborn.contract.verifier.file.SingleContractMetadata;
import sh.stubborn.contract.verifier.util.ContractVerifierDslConverter;

import static org.assertj.core.api.Assertions.assertThat;

class MethodBuilderTests {

	@Test
	void should_map_create_valid_method_name_from_file_name_containing_illegal_chars() {
		Contract contractDsl = ContractVerifierDslConverter
			.convertAsCollection(new File("/"),
					"sh.stubborn.contract.spec.Contract.make {\n" + "request { method 'GET'; urlPath '/foo' }\n"
							+ "response { status OK(); body(foo: 'foo'); headers { contentType(applicationJson()) } }\n"
							+ "}")
			.iterator()
			.next();
		File stubFile = new File("invalid-method;name.groovy");
		ContractMetadata metadata = new ContractMetadata(stubFile.toPath(), false, 0, null, contractDsl);
		SingleContractMetadata singleContractMetadata = new SingleContractMetadata(contractDsl, metadata);
		String methodName = new NameProvider().methodName(singleContractMetadata);
		assertThat(methodName).isEqualTo("validate_invalid_method_name");
	}

}
