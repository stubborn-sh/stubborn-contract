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

package sh.stubborn.contract.verifier.config;

import org.jspecify.annotations.Nullable;

/**
 * How the generated tests should have their collaborator fields (the messaging
 * {@code ContractVerifierMessaging}/{@code ContractVerifierObjectMapper} and the
 * CUSTOM-mode {@code HttpVerifier}) injected.
 *
 * <p>
 * The verifier core is Spring-free. By default the annotation is chosen from the
 * {@link TestMode}: the Spring-based HTTP modes ({@code MOCKMVC}, {@code WEBTESTCLIENT})
 * are already tied to Spring, so their collaborators keep Spring's {@code @Autowired};
 * every other mode gets the portable {@code jakarta.inject.Inject} (which Spring also
 * understands). Consumers can still pin a specific style, or drop the annotation entirely
 * and wire the collaborators through the test's base class.
 *
 * @author Marcin Grzejszczak
 */
public enum FieldInjection {

	/**
	 * Choose the annotation from the {@link TestMode}: {@link #SPRING} for the
	 * Spring-based {@code MOCKMVC}/{@code WEBTESTCLIENT} modes, {@link #JAKARTA} for the
	 * rest. The default.
	 */
	AUTO(null, null),

	/**
	 * Annotate injected fields with Spring's
	 * {@code org.springframework.beans.factory.annotation.Autowired}.
	 */
	SPRING("Autowired", "org.springframework.beans.factory.annotation.Autowired"),

	/**
	 * Annotate injected fields with {@code jakarta.inject.Inject}. Spring also
	 * understands this annotation, so it is a portable choice for Micronaut, Jakarta
	 * EE/CDI and other containers.
	 */
	JAKARTA("Inject", "jakarta.inject.Inject"),

	/**
	 * Emit no injection annotation at all; the collaborators are expected to be provided
	 * by the test's base class. Fully framework-agnostic.
	 */
	NONE(null, null);

	private final @Nullable String annotationSimpleName;

	private final @Nullable String annotationImport;

	FieldInjection(@Nullable String annotationSimpleName, @Nullable String annotationImport) {
		this.annotationSimpleName = annotationSimpleName;
		this.annotationImport = annotationImport;
	}

	/**
	 * Resolves {@link #AUTO} against the test mode; a mode-independent value resolves to
	 * itself. The Spring-based HTTP modes ({@code MOCKMVC}, {@code WEBTESTCLIENT})
	 * resolve to {@link #SPRING}, every other mode to {@link #JAKARTA}.
	 * @param testMode the test mode the class is generated for
	 * @return the concrete injection style to emit ({@link #SPRING}, {@link #JAKARTA} or
	 * {@link #NONE})
	 */
	public FieldInjection resolve(TestMode testMode) {
		if (this != AUTO) {
			return this;
		}
		return (testMode == TestMode.MOCKMVC || testMode == TestMode.WEBTESTCLIENT) ? SPRING : JAKARTA;
	}

	/**
	 * The annotation prefix (including a trailing space) to place before a field
	 * declaration, e.g. {@code "@Autowired "}. Empty for {@link #NONE}.
	 * @return the annotation prefix, or an empty string when no annotation is emitted
	 */
	public String annotationPrefix() {
		return (this.annotationSimpleName != null) ? "@" + this.annotationSimpleName + " " : "";
	}

	/**
	 * The fully-qualified name of the injection annotation to import, or {@code null}
	 * when no annotation (and therefore no import) is emitted.
	 * @return the annotation's import, or {@code null} for {@link #NONE}
	 */
	public @Nullable String annotationImport() {
		return this.annotationImport;
	}

}
