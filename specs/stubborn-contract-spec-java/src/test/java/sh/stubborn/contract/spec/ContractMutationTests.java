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

package sh.stubborn.contract.spec;

import java.util.Map;

import org.junit.jupiter.api.Test;
import sh.stubborn.contract.spec.internal.Input;
import sh.stubborn.contract.spec.internal.OutputMessage;
import sh.stubborn.contract.spec.internal.Request;
import sh.stubborn.contract.spec.internal.Response;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SuppressWarnings("NullAway")
class ContractMutationTests {

	private static Contract validHttpContract() {
		return Contract.make((c) -> {
			c.request((r) -> {
				r.method("GET");
				r.url("/foo");
			});
			c.response((r) -> r.status(200));
		});
	}

	@Test
	void priorityMethodSetsPriority() {
		Contract contract = new Contract();
		contract.priority(1);
		assertThat(contract.getPriority()).isEqualTo(1);
	}

	@Test
	void nameLabelDescriptionMethods() {
		Contract contract = new Contract();
		contract.name("n");
		contract.label("l");
		contract.description("d");
		assertThat(contract.getName()).isEqualTo("n");
		assertThat(contract.getLabel()).isEqualTo("l");
		assertThat(contract.getDescription()).isEqualTo("d");
	}

	@Test
	void makeBuildsAndValidates() {
		Contract contract = validHttpContract();
		assertThat(contract.getRequest()).isNotNull();
		assertThat(contract.getResponse()).isNotNull();
	}

	@Test
	void assertContractThrowsWhenUrlMissing() {
		Contract contract = new Contract();
		contract.request((r) -> r.method("GET"));
		assertThatThrownBy(() -> Contract.assertContract(contract)).isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("URL is missing");
	}

	@Test
	void assertContractThrowsWhenMethodMissing() {
		Contract contract = new Contract();
		contract.request((r) -> r.url("/foo"));
		assertThatThrownBy(() -> Contract.assertContract(contract)).isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("Method is missing");
	}

	@Test
	void assertContractThrowsWhenStatusMissing() {
		Contract contract = new Contract();
		contract.request((r) -> {
			r.method("GET");
			r.url("/foo");
		});
		contract.response((r) -> {
		});
		assertThatThrownBy(() -> Contract.assertContract(contract)).isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("Status is missing");
	}

	@Test
	void assertContractPassesForValidContract() {
		Contract.assertContract(validHttpContract());
	}

	@Test
	void assertContractPassesWhenUrlPathPresent() {
		Contract contract = new Contract();
		contract.request((r) -> {
			r.method("GET");
			r.urlPath("/foo");
		});
		contract.response((r) -> r.status(200));
		Contract.assertContract(contract);
	}

	@Test
	void inputConsumerCreatesInput() {
		Contract contract = new Contract();
		contract.input((i) -> i.triggeredBy("trigger()"));
		assertThat(contract.getInput()).isNotNull();
		assertThat(contract.getInput().getTriggeredBy().getExecutionCommand()).isEqualTo("trigger()");
	}

	@Test
	void outputMessageConsumerCreatesOutputMessage() {
		Contract contract = new Contract();
		contract.outputMessage((o) -> o.sentTo("queue"));
		assertThat(contract.getOutputMessage()).isNotNull();
		assertThat(contract.getOutputMessage().getSentTo().getClientValue()).isEqualTo("queue");
	}

	@Test
	void metadataAppends() {
		Contract contract = new Contract();
		contract.metadata(Map.of("a", "1"));
		contract.metadata(Map.of("b", "2"));
		assertThat(contract.getMetadata()).containsOnlyKeys("a", "b");
	}

	@Test
	void ignoredFlag() {
		Contract contract = new Contract();
		assertThat(contract.isIgnored()).isFalse();
		assertThat(contract.getIgnored()).isFalse();
		contract.ignored();
		assertThat(contract.isIgnored()).isTrue();
		assertThat(contract.getIgnored()).isTrue();
	}

	@Test
	void inProgressFlag() {
		Contract contract = new Contract();
		assertThat(contract.isInProgress()).isFalse();
		assertThat(contract.getInProgress()).isFalse();
		contract.inProgress();
		assertThat(contract.isInProgress()).isTrue();
		assertThat(contract.getInProgress()).isTrue();
	}

	@Test
	void settersAndGetters() {
		Contract contract = new Contract();
		contract.setPriority(3);
		assertThat(contract.getPriority()).isEqualTo(3);
		Request request = new Request();
		contract.setRequest(request);
		assertThat(contract.getRequest()).isSameAs(request);
		Response response = new Response();
		contract.setResponse(response);
		assertThat(contract.getResponse()).isSameAs(response);
		contract.setLabel("l");
		assertThat(contract.getLabel()).isEqualTo("l");
		contract.setDescription("d");
		assertThat(contract.getDescription()).isEqualTo("d");
		contract.setName("n");
		assertThat(contract.getName()).isEqualTo("n");
		Input input = new Input();
		contract.setInput(input);
		assertThat(contract.getInput()).isSameAs(input);
		OutputMessage message = new OutputMessage();
		contract.setOutputMessage(message);
		assertThat(contract.getOutputMessage()).isSameAs(message);
		contract.setIgnored(true);
		assertThat(contract.isIgnored()).isTrue();
		contract.setInProgress(true);
		assertThat(contract.isInProgress()).isTrue();
		contract.setMetadata(Map.of("k", "v"));
		assertThat(contract.getMetadata()).containsEntry("k", "v");
	}

	@Test
	void equalsHashCode() {
		Contract a = new Contract();
		a.name("n");
		Contract b = new Contract();
		b.name("n");
		assertThat(a).isEqualTo(b);
		assertThat(a).hasSameHashCodeAs(b);
		assertThat(a.equals(a)).isTrue();
		assertThat(a.equals(null)).isFalse();
		assertThat(a.equals("x")).isFalse();
	}

	@Test
	void notEqualWhenNameDiffers() {
		Contract a = new Contract();
		a.name("n1");
		Contract b = new Contract();
		b.name("n2");
		assertThat(a).isNotEqualTo(b);
		assertThat(a.hashCode()).isNotEqualTo(b.hashCode());
	}

	@Test
	void notEqualWhenIgnoredDiffers() {
		Contract a = new Contract();
		a.name("n");
		Contract b = new Contract();
		b.name("n");
		b.ignored();
		assertThat(a).isNotEqualTo(b);
	}

	@Test
	void notEqualWhenPriorityDiffers() {
		Contract a = new Contract();
		a.priority(1);
		Contract b = new Contract();
		b.priority(2);
		assertThat(a).isNotEqualTo(b);
	}

	@Test
	void toStringContainsFields() {
		Contract contract = new Contract();
		contract.name("theName");
		contract.label("theLabel");
		assertThat(contract.toString()).contains("Contract").contains("theName").contains("theLabel");
	}

}
