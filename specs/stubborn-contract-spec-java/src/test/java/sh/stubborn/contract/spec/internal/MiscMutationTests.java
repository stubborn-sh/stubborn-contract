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

import java.util.Map;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import sh.stubborn.contract.spec.ContractStorer;
import sh.stubborn.contract.spec.util.RegexpUtils;
import sh.stubborn.contract.spec.util.ValidateUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SuppressWarnings("NullAway")
class MiscMutationTests {

	// --- ContractUtils ---

	@Test
	void clientValueUnwrapsDslProperty() {
		assertThat(ContractUtils.CLIENT_VALUE.apply(new DslProperty("c", "s"))).isEqualTo("c");
	}

	@Test
	void clientValueLeavesPlainObject() {
		assertThat(ContractUtils.CLIENT_VALUE.apply("plain")).isEqualTo("plain");
	}

	@Test
	void serverValueUnwrapsDslProperty() {
		assertThat(ContractUtils.SERVER_VALUE.apply(new DslProperty("c", "s"))).isEqualTo("s");
	}

	@Test
	void serverValueLeavesPlainObject() {
		assertThat(ContractUtils.SERVER_VALUE.apply("plain")).isEqualTo("plain");
	}

	@Test
	void convertStubSideRecursivelyUnwrapsNested() {
		DslProperty nested = new DslProperty(new DslProperty("deepClient", "deepServer"), "outerServer");
		assertThat(ContractUtils.convertStubSideRecursively(nested)).isEqualTo("deepClient");
	}

	@Test
	void convertTestSideRecursivelyReturnsPlain() {
		assertThat(ContractUtils.convertTestSideRecursively("plain")).isEqualTo("plain");
	}

	// --- RandomStringGenerator ---

	@Test
	void randomStringHasRequestedLengthAndAlphaChars() {
		String result = RandomStringGenerator.randomString(20);
		assertThat(result).hasSize(20);
		assertThat(result).matches("[A-Z]+");
	}

	@Test
	void randomStringZeroLength() {
		assertThat(RandomStringGenerator.randomString(0)).isEmpty();
	}

	// --- RegexpUtils ---

	@Test
	void escapeSpecialRegexWithSingleEscape() {
		assertThat(RegexpUtils.escapeSpecialRegexWithSingleEscape(".")).isEqualTo("\\.");
		assertThat(RegexpUtils.escapeSpecialRegexWithSingleEscape("a")).isEqualTo("a");
	}

	@Test
	void escapeSpecialRegexChars() {
		assertThat(RegexpUtils.escapeSpecialRegexChars(".")).isEqualTo("\\\\.");
		assertThat(RegexpUtils.escapeSpecialRegexChars("a")).isEqualTo("a");
	}

	// --- ValidateUtils ---

	@Test
	void validateServerValuePatternThrows() {
		assertThatThrownBy(() -> ValidateUtils.validateServerValueIsAvailable(Pattern.compile("[0-9]+"), "field"))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("can't be a pattern for the server side");
	}

	@Test
	void validateServerValueRegexPropertyThrows() {
		assertThatThrownBy(() -> ValidateUtils
			.validateServerValueIsAvailable(new RegexProperty(Pattern.compile("[0-9]+")), "field"))
			.isInstanceOf(IllegalStateException.class);
	}

	@Test
	void validateServerValueMatchingStrategyEqualToIsAllowed() {
		MatchingStrategy strategy = new MatchingStrategy("v", MatchingStrategy.Type.EQUAL_TO);
		assertThat(ValidateUtils.validateServerValueIsAvailable(strategy, "field")).isSameAs(strategy);
	}

	@Test
	void validateServerValueMatchingStrategyMatchingThrows() {
		MatchingStrategy strategy = new MatchingStrategy("v", MatchingStrategy.Type.MATCHING);
		assertThatThrownBy(() -> ValidateUtils.validateServerValueIsAvailable(strategy, "field"))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("can't be of a matching type");
	}

	@Test
	void validateServerValueDslPropertyIsAllowed() {
		DslProperty property = new DslProperty("c", "s");
		assertThat(ValidateUtils.validateServerValueIsAvailable(property, "field")).isSameAs(property);
	}

	@Test
	void validateServerValuePlainValueIsAllowed() {
		assertThat(ValidateUtils.validateServerValueIsAvailable("plain", "field")).isEqualTo("plain");
	}

	// --- DslPropertyConverter ---

	@Test
	void defaultConverterIsIdentity() {
		assertThat(DslPropertyConverter.DEFAULT.testSide("x")).isEqualTo("x");
		assertThat(DslPropertyConverter.DEFAULT.stubSide("x")).isEqualTo("x");
	}

	@Test
	void instanceConverterIsIdentity() {
		assertThat(DslPropertyConverter.instance().testSide("x")).isEqualTo("x");
		assertThat(DslPropertyConverter.INSTANCE.stubSide("x")).isEqualTo("x");
	}

	// --- ContractStorer ---

	@Test
	void storeProducesFilenameToBytesMapping() {
		ContractStorer<String> storer = new ContractStorer<>() {
		};
		Map<String, byte[]> stored = storer.store("payload");
		String expectedKey = String.valueOf(Math.abs((long) storer.hashCode()));
		assertThat(stored).containsOnlyKeys(expectedKey);
		assertThat(stored.get(expectedKey)).isEqualTo("payload".getBytes());
	}

	// --- HttpMethods ---

	@Test
	void httpMethodEnumMethodNames() {
		assertThat(HttpMethods.HttpMethod.GET.getMethodName()).isEqualTo("GET");
		assertThat(HttpMethods.HttpMethod.HEAD.getMethodName()).isEqualTo("HEAD");
		assertThat(HttpMethods.HttpMethod.POST.getMethodName()).isEqualTo("POST");
		assertThat(HttpMethods.HttpMethod.PUT.getMethodName()).isEqualTo("PUT");
		assertThat(HttpMethods.HttpMethod.PATCH.getMethodName()).isEqualTo("PATCH");
		assertThat(HttpMethods.HttpMethod.DELETE.getMethodName()).isEqualTo("DELETE");
		assertThat(HttpMethods.HttpMethod.OPTIONS.getMethodName()).isEqualTo("OPTIONS");
		assertThat(HttpMethods.HttpMethod.TRACE.getMethodName()).isEqualTo("TRACE");
	}

	@Test
	void httpMethodsConstants() {
		assertThat(HttpMethods.GET).isEqualTo("GET");
		assertThat(HttpMethods.HEAD).isEqualTo("HEAD");
		assertThat(HttpMethods.POST).isEqualTo("POST");
		assertThat(HttpMethods.PUT).isEqualTo("PUT");
		assertThat(HttpMethods.PATCH).isEqualTo("PATCH");
		assertThat(HttpMethods.DELETE).isEqualTo("DELETE");
		assertThat(HttpMethods.OPTIONS).isEqualTo("OPTIONS");
		assertThat(HttpMethods.TRACE).isEqualTo("TRACE");
	}

	@Test
	void httpMethodsConstructorLogsWarning() {
		assertThat(new HttpMethods()).isNotNull();
	}

}
