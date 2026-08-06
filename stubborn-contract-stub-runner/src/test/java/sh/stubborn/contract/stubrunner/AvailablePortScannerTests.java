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

package sh.stubborn.contract.stubrunner;

import java.net.BindException;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class AvailablePortScannerTests {

	private static final int MIN_PORT = 8989;

	private static final int MAX_PORT = 8990;

	@Test
	void shouldExecuteClosureWithNextAvailablePortNumber() {
		AvailablePortScanner scanner = new AvailablePortScanner(MIN_PORT, MAX_PORT);
		int port = scanner.tryToExecuteWithFreePort((p) -> p);
		assertThat(port).isBetween(MIN_PORT, MAX_PORT);
	}

	@Test
	void shouldExecuteClosureWithPortFromSingleValueRange() {
		AvailablePortScanner scanner = new AvailablePortScanner(MIN_PORT, MIN_PORT);
		int port = scanner.tryToExecuteWithFreePort((p) -> p);
		assertThat(port).isEqualTo(MIN_PORT);
	}

	@Test
	void shouldThrowWhenMinPortGreaterThanMaxPort() {
		assertThatExceptionOfType(AvailablePortScanner.InvalidPortRange.class)
			.isThrownBy(() -> new AvailablePortScanner(MAX_PORT, MIN_PORT, 2))
			.withMessage(
					"Invalid bounds exceptions, min port [" + MAX_PORT + "] is greater to max port [" + MIN_PORT + "]");
	}

	@Test
	void shouldThrowWhenNoPortAvailable() {
		AvailablePortScanner scanner = new AvailablePortScanner(MIN_PORT, MAX_PORT, 2);
		assertThatExceptionOfType(AvailablePortScanner.NoPortAvailableException.class)
			.isThrownBy(() -> scanner.tryToExecuteWithFreePort((p) -> {
				throw new BindException("Bind exception from closure");
			}))
			.withMessage("Could not find available port in range " + MIN_PORT + ":" + MAX_PORT);
	}

}
