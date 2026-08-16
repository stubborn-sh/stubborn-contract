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

import org.springframework.cloud.test.TestSocketUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class AvailablePortScannerMutationTests {

	@Test
	void shouldRetryExactlyMaxRetryCountTimesBeforeGivingUp() {
		int port = TestSocketUtils.findAvailableTcpPort();
		int maxRetryCount = 3;
		AvailablePortScanner scanner = new AvailablePortScanner(port, port, maxRetryCount);
		AtomicInteger invocations = new AtomicInteger();

		assertThatExceptionOfType(AvailablePortScanner.NoPortAvailableException.class)
			.isThrownBy(() -> scanner.tryToExecuteWithFreePort((p) -> {
				invocations.incrementAndGet();
				throw new BindException("always fails");
			}));

		assertThat(invocations).hasValue(maxRetryCount);
	}

}
