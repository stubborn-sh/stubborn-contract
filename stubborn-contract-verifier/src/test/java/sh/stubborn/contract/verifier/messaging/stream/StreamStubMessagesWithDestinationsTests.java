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

package sh.stubborn.contract.verifier.messaging.stream;

import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.stream.binder.test.InputDestination;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.function.StreamFunctionProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.messaging.Message;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for stream stub messaging with destinations.
 *
 * @author Marcin Grzejszczak
 * @see <a href="https://github.com/spring-cloud/spring-cloud-contract/issues/694">issue
 * 694</a>
 */
class StreamStubMessagesWithDestinationsTests {

	@Test
	void should_resolve_input_channel_if_input_and_output_have_same_destination_and_receive_is_called() {
		ApplicationContext applicationContext = mock(ApplicationContext.class);
		StreamFunctionProperties functionProperties = mock(StreamFunctionProperties.class);
		OutputDestination output = mock(OutputDestination.class);
		when(applicationContext.getBean(OutputDestination.class)).thenReturn(output);
		when(applicationContext.getBean(StreamFunctionProperties.class)).thenReturn(functionProperties);
		when(functionProperties.getDefinition()).thenReturn("verifications");
		StreamOutputDestinationMessageReceiver messages = new StreamOutputDestinationMessageReceiver(
				applicationContext);

		messages.receive("verifications");

		verify(output, times(1)).receive(5000L, "verifications");
	}

	@Test
	void should_resolve_output_channel_if_input_and_output_have_same_destination_and_send_is_called() {
		ApplicationContext applicationContext = mock(ApplicationContext.class);
		StreamFunctionProperties functionProperties = mock(StreamFunctionProperties.class);
		InputDestination input = mock(InputDestination.class);
		when(applicationContext.getBean(InputDestination.class)).thenReturn(input);
		when(applicationContext.getBean(StreamFunctionProperties.class)).thenReturn(functionProperties);
		when(functionProperties.getDefinition()).thenReturn("verifications");
		StreamInputDestinationMessageSender messages = new StreamInputDestinationMessageSender(applicationContext);

		messages.send("foo", Collections.emptyMap(), "verifications");

		verify(input, times(1)).send(any(Message.class), eq("verifications"));
	}

	@Test
	void should_resolve_channel_via_destination_for_send() {
		ApplicationContext applicationContext = mock(ApplicationContext.class);
		StreamFunctionProperties functionProperties = mock(StreamFunctionProperties.class);
		InputDestination input = mock(InputDestination.class);
		OutputDestination output = mock(OutputDestination.class);
		when(applicationContext.getBean(InputDestination.class)).thenReturn(input);
		when(applicationContext.getBean(OutputDestination.class)).thenReturn(output);
		when(applicationContext.getBean(StreamFunctionProperties.class)).thenReturn(functionProperties);
		when(functionProperties.getDefinition()).thenReturn("verifications");
		StreamStubMessages messages = new StreamStubMessages(
				new StreamInputDestinationMessageSender(applicationContext),
				new StreamOutputDestinationMessageReceiver(applicationContext));

		messages.send("foo", Collections.emptyMap(), "verifications");

		verify(input, times(1)).send(any(Message.class), eq("verifications"));
	}

	@Test
	void should_resolve_channel_via_destination_for_receive() {
		ApplicationContext applicationContext = mock(ApplicationContext.class);
		StreamFunctionProperties functionProperties = mock(StreamFunctionProperties.class);
		InputDestination input = mock(InputDestination.class);
		OutputDestination output = mock(OutputDestination.class);
		when(applicationContext.getBean(InputDestination.class)).thenReturn(input);
		when(applicationContext.getBean(OutputDestination.class)).thenReturn(output);
		when(applicationContext.getBean(StreamFunctionProperties.class)).thenReturn(functionProperties);
		when(functionProperties.getDefinition()).thenReturn("verifications");
		StreamStubMessages messages = new StreamStubMessages(
				new StreamInputDestinationMessageSender(applicationContext),
				new StreamOutputDestinationMessageReceiver(applicationContext));

		messages.receive("verifications");

		verify(output, times(1)).receive(5000L, "verifications");
	}

}
