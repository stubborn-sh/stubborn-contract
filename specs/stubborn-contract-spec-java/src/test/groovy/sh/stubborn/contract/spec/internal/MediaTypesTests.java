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

package sh.stubborn.contract.spec.internal;

import org.assertj.core.api.BDDAssertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link MediaTypes} constants.
 *
 * @author Tim Ysewyn
 */
class MediaTypesTests {

	@Test
	void ALL_VALUE() {
		BDDAssertions.then(MediaTypes.ALL_VALUE).isEqualTo("*/*");
	}

	@Test
	void APPLICATION_ATOM_XML() {
		BDDAssertions.then(MediaTypes.APPLICATION_ATOM_XML).isEqualTo("application/atom+xml");
	}

	@Test
	void APPLICATION_FORM_URLENCODED() {
		BDDAssertions.then(MediaTypes.APPLICATION_FORM_URLENCODED).isEqualTo("application/x-www-form-urlencoded");
	}

	@Test
	void APPLICATION_JSON() {
		BDDAssertions.then(MediaTypes.APPLICATION_JSON).isEqualTo("application/json");
	}

	@Test
	void APPLICATION_JSON_UTF8() {
		BDDAssertions.then(MediaTypes.APPLICATION_JSON_UTF8).isEqualTo("application/json;charset=UTF-8");
	}

	@Test
	void APPLICATION_OCTET_STREAM() {
		BDDAssertions.then(MediaTypes.APPLICATION_OCTET_STREAM).isEqualTo("application/octet-stream");
	}

	@Test
	void APPLICATION_PDF() {
		BDDAssertions.then(MediaTypes.APPLICATION_PDF).isEqualTo("application/pdf");
	}

	@Test
	void APPLICATION_XHTML_XML() {
		BDDAssertions.then(MediaTypes.APPLICATION_XHTML_XML).isEqualTo("application/xhtml+xml");
	}

	@Test
	void APPLICATION_XML() {
		BDDAssertions.then(MediaTypes.APPLICATION_XML).isEqualTo("application/xml");
	}

	@Test
	void IMAGE_GIF() {
		BDDAssertions.then(MediaTypes.IMAGE_GIF).isEqualTo("image/gif");
	}

	@Test
	void IMAGE_JPEG() {
		BDDAssertions.then(MediaTypes.IMAGE_JPEG).isEqualTo("image/jpeg");
	}

	@Test
	void IMAGE_PNG() {
		BDDAssertions.then(MediaTypes.IMAGE_PNG).isEqualTo("image/png");
	}

	@Test
	void MULTIPART_FORM_DATA() {
		BDDAssertions.then(MediaTypes.MULTIPART_FORM_DATA).isEqualTo("multipart/form-data");
	}

	@Test
	void TEXT_HTML() {
		BDDAssertions.then(MediaTypes.TEXT_HTML).isEqualTo("text/html");
	}

	@Test
	void TEXT_MARKDOWN() {
		BDDAssertions.then(MediaTypes.TEXT_MARKDOWN).isEqualTo("text/markdown");
	}

	@Test
	void TEXT_PLAIN() {
		BDDAssertions.then(MediaTypes.TEXT_PLAIN).isEqualTo("text/plain");
	}

	@Test
	void TEXT_XML() {
		BDDAssertions.then(MediaTypes.TEXT_XML).isEqualTo("text/xml");
	}

}
