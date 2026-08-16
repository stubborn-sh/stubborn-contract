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

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("NullAway")
class FromFilePropertyMutationTests {

	@TempDir
	Path tempDir;

	private File write(String name, String content) throws IOException {
		Path path = this.tempDir.resolve(name);
		Files.writeString(path, content);
		return path.toFile();
	}

	@Test
	void isStringForStringType() throws IOException {
		FromFileProperty property = new FromFileProperty(write("a.txt", "x"), String.class);
		assertThat(property.isString()).isTrue();
		assertThat(property.isByte()).isFalse();
	}

	@Test
	void isByteForByteArrayType() throws IOException {
		FromFileProperty property = new FromFileProperty(write("a.bin", "x"), byte[].class);
		assertThat(property.isByte()).isTrue();
		assertThat(property.isString()).isFalse();
	}

	@Test
	void isByteForBoxedByteArrayType() throws IOException {
		FromFileProperty property = new FromFileProperty(write("a.bin", "x"), Byte[].class);
		assertThat(property.isByte()).isTrue();
	}

	@Test
	void isJsonForJsonFile() throws IOException {
		FromFileProperty property = new FromFileProperty(write("a.json", "{}"), String.class);
		assertThat(property.isJson()).isTrue();
		assertThat(property.isXml()).isFalse();
	}

	@Test
	void isXmlForXmlFile() throws IOException {
		FromFileProperty property = new FromFileProperty(write("a.xml", "<a/>"), String.class);
		assertThat(property.isXml()).isTrue();
		assertThat(property.isJson()).isFalse();
	}

	@Test
	void asStringReturnsContent() throws IOException {
		FromFileProperty property = new FromFileProperty(write("a.txt", "hello"), String.class);
		assertThat(property.asString()).isEqualTo("hello");
	}

	@Test
	void asBytesReturnsContent() throws IOException {
		FromFileProperty property = new FromFileProperty(write("a.txt", "hello"), byte[].class);
		assertThat(property.asBytes()).isEqualTo("hello".getBytes(StandardCharsets.UTF_8));
	}

	@Test
	void fileNameReturnsName() throws IOException {
		FromFileProperty property = new FromFileProperty(write("myfile.txt", "x"), String.class);
		assertThat(property.fileName()).isEqualTo("myfile.txt");
	}

	@Test
	void toStringReturnsContent() throws IOException {
		FromFileProperty property = new FromFileProperty(write("a.txt", "content"), String.class);
		assertThat(property.toString()).isEqualTo("content");
	}

	@Test
	void gettersReturnValues() throws IOException {
		File file = write("a.txt", "x");
		FromFileProperty property = new FromFileProperty(file, String.class, StandardCharsets.UTF_8);
		assertThat(property.getFile()).isEqualTo(file);
		assertThat(property.getType()).isEqualTo(String.class);
		assertThat(property.getCharset()).isEqualTo(StandardCharsets.UTF_8.toString());
	}

}
