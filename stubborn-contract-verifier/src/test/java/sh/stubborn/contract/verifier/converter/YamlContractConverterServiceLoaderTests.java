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

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

import org.junit.jupiter.api.Test;
import sh.stubborn.contract.spec.ContractConverter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Every converter this project ships is reachable through the documented
 * {@link ContractConverter} SPI. The in-tree callers add {@link YamlContractConverter} by
 * hand, so a missing registration is invisible here and shows up only in a downstream
 * tool that trusts the SPI: it reads no YAML contract, and whatever gate it powers passes
 * while asserting nothing.
 */
class YamlContractConverterServiceLoaderTests {

	@Test
	void yaml_contract_converter_is_discoverable_through_the_spi() {
		assertThat(loadedConverters()).hasAtLeastOneElementOfType(YamlContractConverter.class);
	}

	private static List<ContractConverter> loadedConverters() {
		List<ContractConverter> converters = new ArrayList<>();
		ServiceLoader.load(ContractConverter.class).forEach(converters::add);
		return converters;
	}

}
