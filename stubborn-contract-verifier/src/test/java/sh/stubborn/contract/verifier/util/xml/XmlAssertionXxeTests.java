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

package sh.stubborn.contract.verifier.util.xml;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Parsing an XML string must not resolve external entities (XXE).
 */
class XmlAssertionXxeTests {

	@Test
	void should_reject_doctype_with_external_entity(@TempDir Path dir) throws IOException {
		Path secret = Files.writeString(dir.resolve("secret.txt"), "SECRET-XXE-MARKER");
		String xxe = "<?xml version=\"1.0\"?><!DOCTYPE foo [<!ENTITY xxe SYSTEM \"" + secret.toUri() + "\">]>"
				+ "<root><foo>&xxe;</foo></root>";

		assertThatThrownBy(() -> XmlAssertion.assertThat(xxe).node("root").node("foo").isEqualTo("SECRET-XXE-MARKER"))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("parse the XML");
	}

}
