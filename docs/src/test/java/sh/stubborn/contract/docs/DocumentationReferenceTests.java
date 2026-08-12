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

package sh.stubborn.contract.docs;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards the reference documentation against drift from the actual code. Every check here
 * is deliberately conservative (only high-confidence violations fail) but build-breaking:
 * a renamed module, a moved/deleted class, a dangling code include, or a slip back to the
 * deprecated {@code spring.cloud.contract.*} property prefix fails {@code mvn -pl docs
 * test}, so the documentation cannot silently rot as the code evolves.
 *
 * <p>
 * The checks work off the source tree (not a runtime classpath) so they stay stable
 * regardless of which modules the {@code docs} module happens to depend on.
 *
 * @author Marcin Grzejszczak
 */
class DocumentationReferenceTests {

	/** {@code @[code{tag:NAME} lang](relative/path)} include directives. */
	private static final Pattern INCLUDE = Pattern.compile("@\\[code\\{tag:([^}]+)}(?:\\s+\\w+)?]\\(([^)]+)\\)");

	/**
	 * {@code sh.stubborn:stubborn-contract*} Maven coordinates for real project modules.
	 * Deliberately anchored to the {@code stubborn-contract} artifact prefix so example
	 * coordinates for a user's own stubs (e.g. {@code sh.stubborn:order-service}) are not
	 * mistaken for module references.
	 */
	private static final Pattern COORDINATE = Pattern.compile("sh\\.stubborn:(stubborn-contract[a-z0-9-]*)");

	/** Fully-qualified {@code sh.stubborn.*} type references (package + Type). */
	private static final Pattern FQCN = Pattern.compile("sh\\.stubborn(?:\\.[a-z][a-z0-9]*)+\\.[A-Z][A-Za-z0-9_]*");

	/**
	 * The deprecated Boot / Maven-plugin property and env-var prefixes that docs must no
	 * longer teach. Both the {@code stubrunner} and {@code verifier} families were
	 * renamed to {@code stubborn.contract.*}; only the migration guide (and lines
	 * explicitly flagged legacy/deprecated) may still mention the old form.
	 */
	private static final Pattern LEGACY_PREFIX = Pattern
		.compile("spring\\.cloud\\.contract\\.(stubrunner|verifier)\\.|SPRING_CLOUD_CONTRACT_(STUBRUNNER|VERIFIER)_");

	private final Path repoRoot = repoRoot();

	private final Path docsRoot = this.repoRoot.resolve("docs");

	@Test
	void every_code_include_resolves_to_an_existing_tagged_region() {
		List<String> violations = new ArrayList<>();
		for (Path md : markdownFiles()) {
			String text = read(md);
			Path parent = md.getParent();
			if (parent == null) {
				continue;
			}
			Matcher m = INCLUDE.matcher(text);
			while (m.find()) {
				String tag = m.group(1);
				Path target = parent.resolve(m.group(2)).normalize();
				if (!Files.exists(target)) {
					violations.add(rel(md) + " -> missing include source '" + m.group(2) + "'");
					continue;
				}
				String src = read(target);
				if (!src.contains("tag::" + tag + "[]") || !src.contains("end::" + tag + "[]")) {
					violations.add(rel(md) + " -> tag '" + tag + "' not found in " + rel(target));
				}
			}
		}
		assertThat(violations).as("dangling documentation code includes").isEmpty();
	}

	@Test
	void every_module_coordinate_refers_to_a_real_module() {
		Set<String> artifactIds = declaredArtifactIds();
		List<String> violations = new ArrayList<>();
		for (Path md : markdownFiles()) {
			Matcher m = COORDINATE.matcher(read(md));
			while (m.find()) {
				String artifact = m.group(1);
				if (!artifactIds.contains(artifact)) {
					violations.add(rel(md) + " -> unknown module 'sh.stubborn:" + artifact + "'");
				}
			}
		}
		assertThat(violations).as("documentation references to non-existent modules").isEmpty();
	}

	@Test
	void every_fully_qualified_type_reference_exists_in_the_source_tree() {
		Set<String> declaredTypes = declaredTypes();
		List<String> violations = new ArrayList<>();
		for (Path md : markdownFiles()) {
			Matcher m = FQCN.matcher(read(md));
			while (m.find()) {
				String fqcn = m.group();
				if (!resolves(fqcn, declaredTypes)) {
					violations.add(rel(md) + " -> unknown type '" + fqcn + "'");
				}
			}
		}
		assertThat(violations).as("documentation references to non-existent sh.stubborn types").isEmpty();
	}

	@Test
	void no_page_teaches_the_deprecated_property_prefix() {
		List<String> violations = new ArrayList<>();
		for (Path md : markdownFiles()) {
			// The migration guide legitimately documents the old prefix.
			if (rel(md).contains("migration/")) {
				continue;
			}
			int lineNo = 0;
			for (String line : read(md).split("\n", -1)) {
				lineNo++;
				if (!LEGACY_PREFIX.matcher(line).find()) {
					continue;
				}
				// Allow lines that explicitly flag the prefix as legacy/deprecated.
				String lower = line.toLowerCase();
				if (lower.contains("legacy") || lower.contains("deprecated")) {
					continue;
				}
				violations.add(rel(md) + ":" + lineNo + " -> uses deprecated 'spring.cloud.contract.*' prefix");
			}
		}
		assertThat(violations).as("documentation still teaching the deprecated property prefix").isEmpty();
	}

	// --- helpers ---------------------------------------------------------------

	/**
	 * Resolves an FQCN against the declared-type index, tolerating references to inner
	 * types and enum constants / static members (e.g. {@code StubsMode.LOCAL}) by
	 * trimming trailing capitalised or member segments back to a declared top-level type.
	 */
	private static boolean resolves(String fqcn, Set<String> declaredTypes) {
		String candidate = fqcn;
		while (candidate.contains(".")) {
			if (declaredTypes.contains(candidate)) {
				return true;
			}
			candidate = candidate.substring(0, candidate.lastIndexOf('.'));
			// Stop once we have trimmed past the type name into the package.
			int lastDot = candidate.lastIndexOf('.');
			if (lastDot < 0 || !Character.isUpperCase(candidate.charAt(lastDot + 1))) {
				return declaredTypes.contains(candidate);
			}
		}
		return false;
	}

	private Set<String> declaredArtifactIds() {
		Pattern artifactId = Pattern.compile("<artifactId>([a-z0-9-]+)</artifactId>");
		Set<String> ids = new TreeSet<>();
		for (Path pom : filesNamed("pom.xml")) {
			Matcher m = artifactId.matcher(read(pom));
			while (m.find()) {
				ids.add(m.group(1));
			}
		}
		return ids;
	}

	private Set<String> declaredTypes() {
		Pattern pkg = Pattern.compile("(?m)^\\s*package\\s+([a-zA-Z0-9_.]+)\\s*;?");
		Pattern type = Pattern.compile("(?m)^\\s*(?:public\\s+|final\\s+|abstract\\s+|sealed\\s+|non-sealed\\s+)*"
				+ "(?:class|interface|enum|record|@interface)\\s+([A-Z][A-Za-z0-9_]*)");
		Set<String> types = new TreeSet<>();
		for (Path src : sourceFiles()) {
			String text = read(src);
			Matcher pm = pkg.matcher(text);
			if (!pm.find()) {
				continue;
			}
			String packageName = pm.group(1);
			Matcher tm = type.matcher(text);
			while (tm.find()) {
				types.add(packageName + "." + tm.group(1));
			}
		}
		types.addAll(declaredRewriteRecipes());
		return types;
	}

	/**
	 * Declarative OpenRewrite recipe names (from {@code META-INF/rewrite/*.yml}) are
	 * package-qualified like FQCNs but backed by YAML rather than Java types, so they
	 * must be indexed as valid references too.
	 */
	private Set<String> declaredRewriteRecipes() {
		Pattern recipeName = Pattern.compile("(?m)^\\s*name:\\s*(sh\\.stubborn[A-Za-z0-9_.]+)");
		Set<String> names = new TreeSet<>();
		for (Path yml : walk(this.repoRoot, path -> {
			String s = path.toString();
			return s.contains("/META-INF/rewrite/") && (s.endsWith(".yml") || s.endsWith(".yaml"))
					&& !s.contains("/target/");
		})) {
			Matcher m = recipeName.matcher(read(yml));
			while (m.find()) {
				names.add(m.group(1));
			}
		}
		return names;
	}

	private List<Path> markdownFiles() {
		return walk(this.docsRoot, path -> {
			String s = path.toString();
			return s.endsWith(".md") && !s.contains("/.vitepress/") && !s.contains("/target/")
					&& !s.contains("/node_modules/");
		});
	}

	private List<Path> sourceFiles() {
		return walk(this.repoRoot, path -> {
			String s = path.toString();
			return (s.endsWith(".java") || s.endsWith(".groovy")) && s.contains("/src/main/") && !s.contains("/target/")
					&& !s.contains("/.claude/");
		});
	}

	private List<Path> filesNamed(String name) {
		return walk(this.repoRoot, path -> path.getFileName().toString().equals(name)
				&& !path.toString().contains("/target/") && !path.toString().contains("/.claude/"));
	}

	private static List<Path> walk(Path root, java.util.function.Predicate<Path> filter) {
		try (Stream<Path> stream = Files.walk(root)) {
			return stream.filter(Files::isRegularFile).filter(filter).sorted().toList();
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	private static String read(Path path) {
		try {
			return Files.readString(path);
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	private String rel(Path path) {
		return this.repoRoot.relativize(path).toString();
	}

	private static Path repoRoot() {
		Path dir = Path.of("").toAbsolutePath();
		while (dir != null) {
			if (Files.exists(dir.resolve("docs/reference/modules.md"))) {
				return dir;
			}
			dir = dir.getParent();
		}
		throw new IllegalStateException("Could not locate repository root from " + Path.of("").toAbsolutePath());
	}

}
