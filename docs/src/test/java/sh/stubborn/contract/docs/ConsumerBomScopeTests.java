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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards the scope of the published consumer BOM
 * ({@code stubborn-contract-dependencies}).
 *
 * <p>
 * A consumer imports that BOM into their own {@code dependencyManagement}, often ahead of
 * their platform BOM. Anything it manages therefore beats Spring Boot's choice for that
 * artifact, silently and in a file the application does not own. Pinning third-party
 * libraries there broke real applications once already: Jetty was held on 12.0.x for CVE
 * reasons, which downgraded Boot 4.1's 12.1.x and stopped
 * {@code spring-boot-starter-jetty} applications from starting at all
 * (stubborn-sh/stubborn#142).
 *
 * <p>
 * So the rule is: the BOM manages Stubborn's own artifacts, plus only those third-party
 * artifacts that are part of Stubborn's API surface AND that Spring Boot does not manage.
 * A CVE pin belongs in {@code stubborn-contract-build/pom.xml}, which governs what we
 * compile and test against and never reaches a consumer's build.
 *
 * <p>
 * This test fails if a third-party entry appears in the BOM without being added to
 * {@link #ALLOWED_THIRD_PARTY} below — deliberately making that a decision somebody has
 * to write down, next to this comment, rather than a line that slips in with a CVE fix.
 *
 * @author Marcin Grzejszczak
 */
class ConsumerBomScopeTests {

	/**
	 * Third-party artifacts the consumer BOM is allowed to manage, each with the reason
	 * it is not an override of Spring Boot.
	 *
	 * <ul>
	 * <li>{@code org.wiremock:*} — WireMock is part of Stubborn's API surface (stub
	 * mappings, the stub server) and Boot does not manage it.</li>
	 * <li>{@code io.rest-assured:rest-assured} — generated contract tests compile against
	 * it; Boot does not manage it.</li>
	 * <li>{@code org.eclipse.jetty.ee10:*} — the servlet layer wiremock-jetty12 is built
	 * against. Boot imports {@code jetty-bom} and {@code jetty-ee11-bom}, so it manages
	 * Jetty core and ee11 but NOT ee10; left unmanaged these resolve to the 12.0.x
	 * wiremock-jetty12 declares and skew fatally against Boot's 12.1.x core. This fills a
	 * gap rather than overriding a choice Boot has made.</li>
	 * </ul>
	 */
	private static final Set<String> ALLOWED_THIRD_PARTY = Set.of("org.wiremock", "io.rest-assured",
			"org.eclipse.jetty.ee10");

	private static final Pattern DEPENDENCY = Pattern
		.compile("<dependency>\\s*<groupId>([^<]+)</groupId>\\s*<artifactId>([^<]+)</artifactId>");

	private final Path repoRoot = repoRoot();

	@Test
	void bomManagesOnlyStubbornArtifactsAndTheAllowedThirdPartyOnes() {
		List<String> unexpected = new ArrayList<>();
		for (String[] coordinate : managedDependencies()) {
			String groupId = coordinate[0];
			if (!groupId.equals("sh.stubborn") && !ALLOWED_THIRD_PARTY.contains(groupId)) {
				unexpected.add(groupId + ":" + coordinate[1]);
			}
		}
		assertThat(unexpected).as("stubborn-contract-dependencies manages third-party artifacts that are not in the "
				+ "allowlist. A consumer importing this BOM has these versions forced on them ahead "
				+ "of their platform BOM (stubborn-sh/stubborn#142). If this is a CVE pin, move it to "
				+ "stubborn-contract-build/pom.xml instead; if Stubborn genuinely cannot work without "
				+ "it and Spring Boot does not manage it, add the groupId to ALLOWED_THIRD_PARTY with " + "the reason.")
			.isEmpty();
	}

	/**
	 * Core Jetty is the specific artifact family that caused #142, and the one most
	 * likely to be pinned again by a well-meaning CVE fix, so it gets its own check with
	 * its own message. Only {@code org.eclipse.jetty.ee10} may appear;
	 * {@code org.eclipse.jetty}, {@code org.eclipse.jetty.http2} and
	 * {@code org.eclipse.jetty.ee11} are Boot's.
	 */
	@Test
	void bomDoesNotManageCoreJetty() {
		List<String> coreJetty = new ArrayList<>();
		for (String[] coordinate : managedDependencies()) {
			if (coordinate[0].startsWith("org.eclipse.jetty") && !coordinate[0].equals("org.eclipse.jetty.ee10")) {
				coreJetty.add(coordinate[0] + ":" + coordinate[1]);
			}
		}
		assertThat(coreJetty)
			.as("stubborn-contract-dependencies manages Jetty artifacts that Spring Boot manages via "
					+ "jetty-bom / jetty-ee11-bom. That downgrades Jetty for every consumer importing this "
					+ "BOM and stops spring-boot-starter-jetty applications from starting "
					+ "(stubborn-sh/stubborn#142). Only the ee10 servlet layer, which Boot does not manage, "
					+ "belongs here.")
			.isEmpty();
	}

	/**
	 * The BOM's parent is {@code stubborn-contract-build}, so
	 * {@code dependencyManagement} declared there is inherited into the BOM we publish
	 * and reaches consumers too. The reactor parent ({@code stubborn-contract-parent}) is
	 * the place for entries that every module needs but no consumer should inherit.
	 */
	@Test
	void buildParentDeclaresNoDependencyManagementThatWouldLeakIntoTheBom() {
		String buildParent = read(this.repoRoot.resolve("stubborn-contract-build/pom.xml"));
		List<String> leaked = new ArrayList<>();
		Matcher m = DEPENDENCY.matcher(dependencyManagementOf(buildParent));
		while (m.find()) {
			leaked.add(m.group(1) + ":" + m.group(2));
		}
		assertThat(leaked)
			.as("stubborn-contract-build declares dependencyManagement, which stubborn-contract-dependencies "
					+ "inherits as its parent and publishes to consumers. Move these to the reactor parent "
					+ "(pom.xml) so modules still get them but the consumer BOM does not "
					+ "(stubborn-sh/stubborn#142).")
			.isEmpty();
	}

	private List<String[]> managedDependencies() {
		String bom = read(this.repoRoot.resolve("stubborn-contract-dependencies/pom.xml"));
		String managed = dependencyManagementOf(bom);
		assertThat(managed).as("no <dependencyManagement> found in stubborn-contract-dependencies/pom.xml")
			.isNotEmpty();
		Set<String> seen = new LinkedHashSet<>();
		List<String[]> coordinates = new ArrayList<>();
		Matcher m = DEPENDENCY.matcher(managed);
		while (m.find()) {
			if (seen.add(m.group(1) + ":" + m.group(2))) {
				coordinates.add(new String[] { m.group(1), m.group(2) });
			}
		}
		return coordinates;
	}

	/**
	 * Returns the {@code <dependencyManagement>} block of a POM, or an empty string when
	 * it has none. Comments are stripped first so the prose in them (which names
	 * artifacts these checks look for, by design) is not mistaken for a declaration.
	 */
	private static String dependencyManagementOf(String pom) {
		String withoutComments = pom.replaceAll("(?s)<!--.*?-->", "");
		int start = withoutComments.indexOf("<dependencyManagement>");
		if (start < 0) {
			return "";
		}
		int end = withoutComments.indexOf("</dependencyManagement>", start);
		return withoutComments.substring(start, end);
	}

	private static String read(Path path) {
		try {
			return Files.readString(path);
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
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
