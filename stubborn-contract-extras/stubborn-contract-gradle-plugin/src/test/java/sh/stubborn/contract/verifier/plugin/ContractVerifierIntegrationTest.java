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
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.BuildTask;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;

import static org.assertj.core.api.BDDAssertions.then;

abstract class ContractVerifierIntegrationTest {

	public static final String SPOCK = "testFramework = 'Spock'";

	public static final String JUNIT = "testFramework = 'JUnit'";

	public static final String MVC_SPEC = "'org.springframework.cloud.MvcSpec'";

	public static final String MVC_TEST = "'org.springframework.cloud.MvcTest'";

	protected static final boolean WORK_OFFLINE = Boolean.parseBoolean(System.getProperty("WORK_OFFLINE", "false"));

	@Nullable File testProjectDir;

	@BeforeEach
	void setup() {
		String dateString = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
		File testFolder = new File("build/generated-tests/" + getClass().getSimpleName() + "/" + dateString);
		testFolder.mkdirs();
		this.testProjectDir = testFolder;
	}

	protected File getTestProjectDir() {
		return Objects.requireNonNull(this.testProjectDir);
	}

	protected void setupForProject(String projectRoot) throws IOException {
		copyResourcesToRoot(projectRoot);
	}

	protected void switchToJunitTestFramework() throws IOException {
		switchToJunitTestFramework(MVC_SPEC, MVC_TEST);
	}

	protected void switchToJunitTestFramework(String from, String to) throws IOException {
		Path path = getBuildFile().toPath();
		String content = new String(Files.readAllBytes(path), StandardCharsets.UTF_8).replaceAll(SPOCK, JUNIT)
			.replaceAll(from, to);
		Files.write(path, content.getBytes(StandardCharsets.UTF_8));
	}

	protected void emptySourceSet() throws IOException {
		Path path = getBuildFile().toPath();
		String content = new String(Files.readAllBytes(path), StandardCharsets.UTF_8).replaceAll("sourceSet = \"java\"",
				"");
		Files.write(path, content.getBytes(StandardCharsets.UTF_8));
	}

	protected BuildResult runTasksSuccessfully(String... tasks) {
		BuildResult result = run(tasks);
		for (BuildTask task : result.getTasks()) {
			then(task.getOutcome()).isIn(TaskOutcome.SUCCESS, TaskOutcome.UP_TO_DATE, TaskOutcome.NO_SOURCE);
		}
		return result;
	}

	protected BuildResult validateTasksOutcome(BuildResult result, TaskOutcome expectedOutcome, String... tasks) {
		for (String taskName : tasks) {
			BuildTask task = Objects.requireNonNull(result.task(":" + taskName));
			then(task.getOutcome()).isEqualTo(expectedOutcome);
		}
		return result;
	}

	protected String[] checkAndPublishToMavenLocal() {
		List<String> args = new ArrayList<>(List.of("check", "publishToMavenLocal", "--info", "--stacktrace"));
		if (WORK_OFFLINE) {
			args.add("--offline");
		}
		return args.toArray(new String[0]);
	}

	protected BuildResult run(String... tasks) {
		return GradleRunner.create()
			.withProjectDir(getTestProjectDir())
			.withArguments(tasks)
			.withPluginClasspath()
			// .withDebug(true)
			.forwardOutput()
			.build();
	}

	protected void copyResourcesToRoot(String srcDir) throws IOException {
		copyResources(srcDir, getTestProjectDir());
	}

	protected void copyResources(String srcDir, File destinationFile) throws IOException {
		ClassLoader classLoader = getClass().getClassLoader();
		URL resource = classLoader.getResource(srcDir);
		if (resource == null) {
			throw new RuntimeException("Could not find classpath resource: " + srcDir);
		}
		File resourceFile;
		try {
			resourceFile = new File(resource.toURI());
		}
		catch (URISyntaxException ex) {
			throw new IOException(ex);
		}
		if (resourceFile.isFile()) {
			Files.copy(resourceFile.toPath(), destinationFile.toPath());
		}
		else {
			Files.walkFileTree(resourceFile.toPath(), new CopyFileVisitor(destinationFile.toPath()));
		}
	}

	protected File file(String path) {
		File file = new File(getTestProjectDir(), path);
		System.out.println("Resolved path is [" + file + "]");
		return file;
	}

	protected boolean fileExists(String path) {
		return file(path).exists();
	}

	protected File getBuildFile() {
		return new File(getTestProjectDir(), "build.gradle");
	}

	protected boolean jarContainsContractVerifierContracts(String path) throws IOException {
		then(fileExists(path)).isTrue();
		File rootFile = file(path);
		boolean containsGroovyFiles = false;
		try (Stream<Path> paths = Files.walk(rootFile.toPath())) {
			List<Path> files = paths.filter(Files::isRegularFile).toList();
			for (Path candidate : files) {
				File candidateFile = candidate.toFile();
				if (!candidateFile.getName().endsWith("jar")) {
					continue;
				}
				try (ZipFile zipFile = new ZipFile(candidateFile)) {
					Enumeration<? extends ZipEntry> entries = zipFile.entries();
					while (entries.hasMoreElements()) {
						if (entries.nextElement().getName().endsWith(".groovy")) {
							containsGroovyFiles = true;
						}
					}
				}
				catch (ZipException zipEx) {
					System.out.println("Unable to open file " + candidateFile.getName());
				}
			}
		}
		return containsGroovyFiles;
	}

	private static final class CopyFileVisitor extends SimpleFileVisitor<Path> {

		private final Path targetPath;

		private @Nullable Path sourcePath = null;

		CopyFileVisitor(Path targetPath) {
			this.targetPath = targetPath;
		}

		@Override
		public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
			if (this.sourcePath == null) {
				this.sourcePath = dir;
			}
			else {
				Files.createDirectories(this.targetPath.resolve(this.sourcePath.relativize(dir)));
			}
			return FileVisitResult.CONTINUE;
		}

		@Override
		public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
			Path target = this.targetPath.resolve(Objects.requireNonNull(this.sourcePath).relativize(file));
			if (!target.toFile().exists()) {
				Files.copy(file, target);
			}
			return FileVisitResult.CONTINUE;
		}

	}

}
