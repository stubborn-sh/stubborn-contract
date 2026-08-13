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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Objects;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import sh.stubborn.contract.spec.Contract;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A YAML contract authored for Spring Cloud Contract 5.x must be consumed by Stubborn
 * unmodified — the YAML schema is the portable interchange format between the two. This
 * parses an SCC-shaped contract (request matchers by type, a JSON response body and body
 * matchers) through {@link YamlContractConverter} and asserts it maps onto a
 * {@link Contract} with the expected request and response.
 */
class SpringCloudContractYamlCompatibilityTests {

	// @formatter:off
	private static final String SCC_YAML = "description: should return a beer for an adult\n"
			+ "name: shouldGrantABeer\n"
			+ "request:\n"
			+ "  method: POST\n"
			+ "  url: /check\n"
			+ "  headers:\n"
			+ "    Content-Type: application/json\n"
			+ "  body:\n"
			+ "    age: 25\n"
			+ "  matchers:\n"
			+ "    body:\n"
			+ "      - path: $.age\n"
			+ "        type: by_regex\n"
			+ "        value: '[2-9][0-9]'\n"
			+ "response:\n"
			+ "  status: 200\n"
			+ "  headers:\n"
			+ "    Content-Type: application/json\n"
			+ "  body:\n"
			+ "    status: OK\n"
			+ "  matchers:\n"
			+ "    body:\n"
			+ "      - path: $.status\n"
			+ "        type: by_regex\n"
			+ "        value: OK\n";
	// @formatter:on

	private final YamlContractConverter converter = new YamlContractConverter();

	@Test
	void parsesAnSccYamlContractIntoAStubbornContract(@TempDir Path dir) throws IOException {
		Path yaml = dir.resolve("shouldGrantABeer.yml");
		Files.writeString(yaml, SCC_YAML);

		assertThat(this.converter.isAccepted(yaml.toFile())).isTrue();
		Collection<Contract> contracts = this.converter.convertFrom(yaml.toFile());

		assertThat(contracts).hasSize(1);
		Contract contract = contracts.iterator().next();
		assertThat(contract.getName()).isEqualTo("shouldGrantABeer");
		assertThat(Objects.requireNonNull(contract.getRequest()).getMethod().getClientValue()).isEqualTo("POST");
		assertThat(contract.getRequest().getUrl().getClientValue()).isEqualTo("/check");
		assertThat(Objects.requireNonNull(contract.getResponse()).getStatus().getClientValue()).isEqualTo(200);
	}

}
