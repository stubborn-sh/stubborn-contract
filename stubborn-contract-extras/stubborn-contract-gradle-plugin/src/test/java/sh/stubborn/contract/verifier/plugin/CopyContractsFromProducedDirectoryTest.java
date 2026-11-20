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

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.BDDAssertions.then;

/**
 * Regression test for <a href=
 * "https://github.com/stubborn-sh/stubborn-contract/issues/272">#272</a>: when
 * {@code contractsDslDir} points to a directory created by another task of the build,
 * {@code copyContracts} must find the contracts on a clean build with the configuration
 * cache enabled, and must depend on the producing task without an explicit
 * {@code dependsOn}.
 */
class CopyContractsFromProducedDirectoryTest extends ContractVerifierIntegrationTest {

	@Test
	@DisplayName("should copy contracts from a directory produced by another task with the configuration cache on")
	void shouldCopyContractsFromProducedDirectoryWithConfigurationCache() throws IOException {
		File root = getTestProjectDir();
		write(root.toPath().resolve("settings.gradle"), "rootProject.name = 'produced-contracts'\n");
		write(root.toPath().resolve("gradle.properties"), "org.gradle.configuration-cache=true\n"
				+ "org.gradle.configuration-cache.problems=fail\n");
		write(root.toPath().resolve("contracts-source/contracts/shouldReturnFoo.yml"),
				"request:\n  method: GET\n  url: /foo\nresponse:\n  status: 200\n");
		write(root.toPath().resolve("build.gradle"), """
				plugins {
					id 'java'
					id 'sh.stubborn.contract'
				}

				def unpackContracts = tasks.register('unpackContracts', Sync) {
					from(layout.projectDirectory.dir('contracts-source'))
					into(layout.buildDirectory.dir('unpacked'))
				}

				contracts {
					// no explicit dependsOn on copyContracts - it must be inferred
					contractsDslDir = layout.dir(unpackContracts.map { it.destinationDir })
				}
				""");

		BuildResult result = run("copyContracts", "--stacktrace");

		validateTasksOutcome(result, TaskOutcome.SUCCESS, "unpackContracts", "copyContracts");
		then(findCopiedContract(root)).as("copied contract").isNotNull();
		then(result.getOutput()).contains("Configuration cache entry stored");

		// second run reuses the configuration cache entry
		BuildResult rerun = run("copyContracts", "--stacktrace");
		then(rerun.getOutput()).contains("Configuration cache entry reused");
	}

	private static File findCopiedContract(File root) throws IOException {
		Path build = root.toPath().resolve("build");
		try (var paths = Files.walk(build)) {
			return paths.filter(p -> p.getFileName().toString().equals("shouldReturnFoo.yml"))
				.filter(p -> !p.startsWith(build.resolve("unpacked")))
				.map(Path::toFile)
				.findFirst()
				.orElse(null);
		}
	}

	private static void write(Path path, String content) throws IOException {
		Files.createDirectories(path.getParent());
		Files.writeString(path, content, StandardCharsets.UTF_8);
	}

}
