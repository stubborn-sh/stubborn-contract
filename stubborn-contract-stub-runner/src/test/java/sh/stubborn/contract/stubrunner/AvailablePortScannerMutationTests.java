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
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class AvailablePortScannerMutationTests {

	// Fixed, uncommon high port (matching AvailablePortScannerTests) that reliably
	// stays free on CI. The invocation count below only reaches maxRetryCount if the
	// port is available on every retry, since the callback runs only after
	// checkIfPortIsAvailable binds. TestSocketUtils.findAvailableTcpPort() flaked
	// here: it hands back a port from a churning pool that is quickly reused, so with
	// a single-port range a stolen port fails the bind on every retry, the callback
	// never runs, and the count comes out 0 instead of maxRetryCount.
	private static final int PORT = 8989;

	@Test
	void shouldRetryExactlyMaxRetryCountTimesBeforeGivingUp() {
		int maxRetryCount = 3;
		AvailablePortScanner scanner = new AvailablePortScanner(PORT, PORT, maxRetryCount);
		AtomicInteger invocations = new AtomicInteger();

		assertThatExceptionOfType(AvailablePortScanner.NoPortAvailableException.class)
			.isThrownBy(() -> scanner.tryToExecuteWithFreePort((p) -> {
				invocations.incrementAndGet();
				throw new BindException("always fails");
			}));

		assertThat(invocations).hasValue(maxRetryCount);
	}

}
