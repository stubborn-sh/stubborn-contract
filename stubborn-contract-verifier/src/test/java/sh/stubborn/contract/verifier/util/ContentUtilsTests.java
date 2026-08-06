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

package sh.stubborn.contract.verifier.util;

import org.junit.jupiter.api.Test;
import org.xml.sax.helpers.DefaultHandler;
import sh.stubborn.contract.spec.internal.DslProperty;

import static org.assertj.core.api.Assertions.assertThat;

class ContentUtilsTests {

	@Test
	void should_return_the_stub_side() {
		DslProperty<String> dslProperty = new DslProperty<>("stub", "test");
		assertThat(ContentUtils.GET_STUB_SIDE_FUNCTION.apply(dslProperty)).isEqualTo("stub");
	}

	@Test
	void should_return_the_test_side() {
		DslProperty<String> dslProperty = new DslProperty<>("stub", "test");
		assertThat(ContentUtils.GET_TEST_SIDE_FUNCTION.apply(dslProperty)).isEqualTo("test");
	}

	@Test
	void should_return_xml_slurper_with_default_error_handler() throws Exception {
		groovy.xml.XmlSlurper xmlSlurper = ContentUtils.getXmlSlurperWithDefaultErrorHandler();
		assertThat(xmlSlurper.getErrorHandler()).isInstanceOf(DefaultHandler.class);
	}

}
