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

package sh.stubborn.contract.verifier.messaging.camel;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Mutation-focused tests for {@link StandaloneMetadata}.
 */
class StandaloneMetadataMutationTests {

	@Test
	void getters_return_non_null_defaults() {
		StandaloneMetadata metadata = new StandaloneMetadata();
		assertThat(metadata.getSetup()).isNotNull();
		assertThat(metadata.getInput()).isNotNull();
		assertThat(metadata.getOutputMessage()).isNotNull();
	}

	@Test
	void setters_round_trip() {
		StandaloneMetadata metadata = new StandaloneMetadata();
		StandaloneMetadata.SetupMetadata setup = new StandaloneMetadata.SetupMetadata();
		setup.setOptions("setup-options");
		metadata.setSetup(setup);
		assertThat(metadata.getSetup()).isSameAs(setup);
		assertThat(metadata.getSetup().getOptions()).isEqualTo("setup-options");

		StandaloneMetadata.MessageMetadata input = new StandaloneMetadata.MessageMetadata();
		input.setAdditionalOptions("input-opts");
		metadata.setInput(input);
		assertThat(metadata.getInput()).isSameAs(input);
		assertThat(metadata.getInput().getAdditionalOptions()).isEqualTo("input-opts");

		StandaloneMetadata.MessageMetadata output = new StandaloneMetadata.MessageMetadata();
		output.setAdditionalOptions("output-opts");
		metadata.setOutputMessage(output);
		assertThat(metadata.getOutputMessage()).isSameAs(output);
		assertThat(metadata.getOutputMessage().getAdditionalOptions()).isEqualTo("output-opts");
	}

	@Test
	void key_and_description() {
		StandaloneMetadata metadata = new StandaloneMetadata();
		assertThat(metadata.key()).isEqualTo("standalone");
		assertThat(StandaloneMetadata.METADATA_KEY).isEqualTo("standalone");
		assertThat(metadata.description()).isEqualTo("Metadata for standalone communication - with running middleware");
	}

	@Test
	void fromMetadata_returns_stored_and_default() {
		StandaloneMetadata stored = new StandaloneMetadata();
		stored.getSetup().setOptions("stored");
		Map<String, Object> map = new HashMap<>();
		map.put(StandaloneMetadata.METADATA_KEY, stored);
		StandaloneMetadata result = StandaloneMetadata.fromMetadata(map);
		assertThat(result.getSetup().getOptions()).isEqualTo("stored");
		StandaloneMetadata def = StandaloneMetadata.fromMetadata(new HashMap<>());
		assertThat(def.getSetup().getOptions()).isNull();
	}

}
