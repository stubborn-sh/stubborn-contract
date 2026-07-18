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

package sh.stubborn.contract.verifier.messaging.internal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import sh.stubborn.contract.verifier.messaging.noop.NoOpContractVerifierAutoConfiguration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.BDDAssertions.then;

/**
 * @author Marcin Grzejszczak
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = NoOpContractVerifierAutoConfiguration.class)
class ContractVerifierObjectMapperAvroTests {

	@Autowired
	ContractVerifierObjectMapper mapper;

	@Test
	void should_convert_an_Avro_generated_object_into_a_json_representation() throws Exception {
		FooAvro input = FooAvro.newBuilder().setFooAvro("barAvro").build();

		String result = this.mapper.writeValueAsString(input);

		then(result).isEqualTo("{\"fooAvro\":\"barAvro\"}");
	}

}
