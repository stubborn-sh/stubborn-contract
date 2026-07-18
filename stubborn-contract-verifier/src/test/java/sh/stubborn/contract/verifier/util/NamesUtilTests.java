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

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.util.FileSystemUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * @author Marcin Grzejszczak
 */
class NamesUtilTests {

	@TempDir
	Path tmpDir;

	@Test
	void should_return_the_whole_string_before_the_last_one() {
		String string = "a.b.c.d.e";
		assertThat(NamesUtil.beforeLast(string, ".")).isEqualTo("a.b.c.d");
	}

	@Test
	void should_return_empty_string_when_no_token_was_found_for_before_last() {
		String string = "a.b.c.d.e";
		assertThat(NamesUtil.beforeLast(string, "/")).isEqualTo("");
	}

	@Test
	void should_return_first_token_after_the_last_one() {
		String string = "a.b.c.d.e";
		assertThat(NamesUtil.afterLast(string, ".")).isEqualTo("e");
	}

	@Test
	void should_return_the_input_string_when_no_token_was_found_for_after_last() {
		String string = "a.b.c.d.e";
		assertThat(NamesUtil.afterLast(string, "/")).isEqualTo(string);
	}

	@Test
	void should_return_first_token_after_the_last_dot() {
		String string = "a.b.c.d.e";
		assertThat(NamesUtil.afterLastDot(string)).isEqualTo("e");
	}

	@Test
	void should_return_the_input_string_when_no_token_was_found_for_after_last_dot() {
		String string = "abcde";
		assertThat(NamesUtil.afterLastDot(string)).isEqualTo(string);
	}

	@Test
	void should_return_camel_case_version_of_a_string() {
		String string = "BlaBlaBla";
		assertThat(NamesUtil.camelCase(string)).isEqualTo("blaBlaBla");
	}

	@Test
	void should_return_capitalized_version_of_a_string() {
		String string = "blaBlaBla";
		assertThat(NamesUtil.capitalize(string)).isEqualTo("BlaBlaBla");
	}

	@Test
	void should_return_all_text_to_last_dot() {
		String string = "a.b.c.d.e";
		assertThat(NamesUtil.toLastDot(string)).isEqualTo("a.b.c.d");
	}

	@Test
	void should_return_the_input_string_when_no_token_was_found_for_to_last_dot() {
		String string = "abcde";
		assertThat(NamesUtil.toLastDot(string)).isEqualTo(string);
	}

	@Test
	void should_convert_a_package_notation_to_directory() {
		String string = "a.b.c.d.e";
		assertThat(NamesUtil.packageToDirectory(string)).isEqualTo("a/b/c/d/e".replace("/", File.separator));
	}

	@Test
	void should_convert_a_directory_notation_to_package() {
		String string = "a/b/c/d/e".replace("/", File.separator);
		assertThat(NamesUtil.directoryToPackage(string)).isEqualTo("a.b.c.d.e");
	}

	@Test
	void should_convert_a_directory_notation_to_package_when_folder_is_a_digit() {
		String string = "a/b/c/1.0.0/e".replace("/", File.separator);
		assertThat(NamesUtil.directoryToPackage(string)).isEqualTo("a.b.c._1_0_0.e");
	}

	@Test
	void should_convert_a_directory_notation_to_package_when_folder_is_only_a_digit() {
		String string = "1.0.0";
		assertThat(NamesUtil.directoryToPackage(string)).isEqualTo("_1_0_0");
	}

	@Test
	void should_convert_all_illegal_package_chars_to_legal_ones() {
		String string = "a-b c.1.0.x+d1174dd";
		assertThat(NamesUtil.convertIllegalPackageChars(string)).isEqualTo("a_b_c_1_0_x_d1174dd");
	}

	@Test
	void should_convert_all_illegal_method_chars_to_legal_ones() {
		String string = "10a-b c.1.0.x+d1174$dd";
		assertThat(NamesUtil.convertIllegalMethodNameChars(string)).isEqualTo("10a_b_c_1_0_x_d1174$dd");
	}

	@Test
	void should_recursively_convert_the_names_of_folders_to_package_names() throws URISyntaxException, IOException {
		File tmp = tmpDir.toFile();
		File resource = new File(getClass().getResource("/prependFolderName").toURI());
		FileSystemUtils.copyRecursively(resource, tmp);

		NamesUtil.recrusiveDirectoryToPackage(tmp);

		assertThat(new File(tmp, "META-INF/1_0_0_SNAPSHOT")).doesNotExist();
		assertThat(new File(tmp, "META-INF/2_0_0_SNAPSHOT")).doesNotExist();
		assertThat(new File(tmp, "META-INF/1_0_0_SNAPSHOT/3_0_0_SNAPSHOT")).doesNotExist();
		assertThat(new File(tmp, "META-INF/_1_0_0_SNAPSHOT")).exists();
		assertThat(new File(tmp, "META-INF/_2_0_0_SNAPSHOT")).exists();
		assertThat(new File(tmp, "META-INF/_1_0_0_SNAPSHOT/_3_0_0_SNAPSHOT")).exists();
		assertThat(new File(tmp, "META-INF/_1_0_0_SNAPSHOT/normal")).exists();
		assertThat(new File(tmp, "META-INF/_1_0_0_SNAPSHOT/_3_0_0_SNAPSHOT/normal")).exists();
	}

	@Test
	void should_not_throw_exception_if_folder_does_not_exist() {
		assertThatNoException().isThrownBy(() -> NamesUtil.recrusiveDirectoryToPackage(new File("I/do/not/exist")));
	}

}
