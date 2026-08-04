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

package sh.stubborn.contract.verifier.plugin;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

abstract class ContractVerifierKotlinIntegrationTest extends ContractVerifierIntegrationTest {

	public static final String SPOCK = "testFramework.set(TestFramework.SPOCK)";

	public static final String JUNIT = "testFramework.set(TestFramework.JUNIT5)";

	@Override
	protected File getBuildFile() {
		return new File(getTestProjectDir(), "build.gradle.kts");
	}

	@Override
	protected void switchToJunitTestFramework(String from, String to) throws IOException {
		Path path = getBuildFile().toPath();
		String content = new String(Files.readAllBytes(path), StandardCharsets.UTF_8).replaceAll(SPOCK, JUNIT)
			.replaceAll(from, to);
		Files.write(path, content.getBytes(StandardCharsets.UTF_8));
	}

}
