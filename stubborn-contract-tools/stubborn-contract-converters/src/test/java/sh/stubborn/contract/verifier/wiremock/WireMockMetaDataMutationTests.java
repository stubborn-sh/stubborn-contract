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

package sh.stubborn.contract.verifier.wiremock;

import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.BDDAssertions.then;

class WireMockMetaDataMutationTests {

	private final WireMockMetaData metaData = new WireMockMetaData();

	@Test
	void key_returns_metadata_key() {
		then(this.metaData.key()).isEqualTo("wiremock");
		then(this.metaData.key()).isEqualTo(WireMockMetaData.METADATA_KEY);
	}

	@Test
	void description_returns_full_description_including_applicable_class_names() {
		String description = this.metaData.description();
		then(description).contains("Metadata for extending WireMock stubs");
		then(description).contains("`String`");
		then(description).contains("`StubMapping`");
		then(description).contains("`Map`");
		then(description).contains("http://wiremock.org/docs/stubbing/");
	}

	@Test
	void additionalClassesToLookAt_returns_stub_mapping_class() {
		then(this.metaData.additionalClassesToLookAt()).containsExactly(StubMapping.class);
	}

}
