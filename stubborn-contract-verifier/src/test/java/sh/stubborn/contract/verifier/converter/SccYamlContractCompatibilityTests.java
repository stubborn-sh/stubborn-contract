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

package sh.stubborn.contract.verifier.converter;

import java.io.File;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Objects;

import org.junit.jupiter.api.Test;
import sh.stubborn.contract.spec.Contract;
import sh.stubborn.contract.spec.internal.MatchingType;
import sh.stubborn.contract.spec.internal.RegexProperty;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards that a contract authored in Spring Cloud Contract's YAML format parses,
 * unchanged, into the same {@link Contract} model — the YAML schema (request/response,
 * headers, body, {@code by_regex} matchers) is shared, so an SCC 5.x contract is consumed
 * as-is.
 */
class SccYamlContractCompatibilityTests {

	private final YamlContractConverter converter = new YamlContractConverter();

	@Test
	void parsesAnSccStyleYamlContractIntoTheSharedModel() {
		File yaml = resource("/yml/scc_compat_contract.yml");
		assertThat(this.converter.isAccepted(yaml)).isTrue();

		Collection<Contract> contracts = this.converter.convertFrom(yaml);

		assertThat(contracts).hasSize(1);
		Contract contract = contracts.iterator().next();
		assertThat(contract.getName()).isEqualTo("shouldGrantBeer");

		assertThat(Objects.requireNonNull(contract.getRequest()).getMethod().getClientValue()).isEqualTo("POST");
		assertThat(contract.getRequest().getUrl().getClientValue()).isEqualTo("/check");
		assertThat(contract.getRequest().getHeaders().getEntries())
			.anySatisfy((h) -> assertThat(h.getName()).isEqualTo("Content-Type"));

		// the SCC by_regex body matcher survives the conversion
		assertThat(contract.getRequest().getBodyMatchers().matchers()).anySatisfy((m) -> {
			assertThat(m.path()).isEqualTo("$.age");
			assertThat(m.matchingType()).isEqualTo(MatchingType.REGEX);
			assertThat(((RegexProperty) m.value()).pattern()).isEqualTo("[0-9]+");
		});

		assertThat(Objects.requireNonNull(contract.getResponse()).getStatus().getClientValue()).isEqualTo(200);
		assertThat(contract.getResponse().getBodyMatchers().matchers()).anySatisfy((m) -> {
			assertThat(m.path()).isEqualTo("$");
			assertThat(m.matchingType()).isEqualTo(MatchingType.REGEX);
		});
	}

	private static File resource(String path) {
		try {
			return new File(Objects.requireNonNull(SccYamlContractCompatibilityTests.class.getResource(path)).toURI());
		}
		catch (URISyntaxException ex) {
			throw new RuntimeException(ex);
		}
	}

}
