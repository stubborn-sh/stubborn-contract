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

package sh.stubborn.contract.stubrunner.server;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.assertj.core.api.BDDAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import sh.stubborn.contract.stubrunner.StubRunning;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = StubRunnerBoot.class, properties = "spring.cloud.zookeeper.enabled=false")
@ActiveProfiles("test")
class StubRunnerBootTests {

	private static final ObjectMapper MAPPER = new ObjectMapper();

	@Autowired
	StubRunning stubRunning;

	@BeforeEach
	void setup() {
		RestAssuredMockMvc.standaloneSetup(new HttpStubsController(this.stubRunning),
				new TriggerController(this.stubRunning));
	}

	@Test
	void shouldReturnListOfRunningStubs() throws Exception {
		String response = RestAssuredMockMvc.get("/stubs").body().asString();
		JsonNode root = MAPPER.readTree(response);
		assertThat(root.has("sh.stubborn.contract.verifier.stubs:loanIssuance:0.0.1-SNAPSHOT:stubs")).isTrue();
	}

	@Test
	void shouldReturnListOfTriggers() throws Exception {
		String response = RestAssuredMockMvc.get("/triggers").body().asString();
		JsonNode root = MAPPER.readTree(response);
		JsonNode triggers = root.get("sh.stubborn.contract.verifier.stubs:bootService:0.0.1-SNAPSHOT:stubs");
		assertThat(triggers).isNotNull();
		assertThat(triggers.toString()).contains("return_book_1");
	}

	@Test
	void shouldTriggerAMessagingLabel() {
		StubRunning stubRunning = Mockito.mock(StubRunning.class);
		RestAssuredMockMvc.standaloneSetup(new HttpStubsController(stubRunning), new TriggerController(stubRunning));
		int statusCode = RestAssuredMockMvc.post("/triggers/delete_book").statusCode();
		assertThat(statusCode).isEqualTo(200);
		Mockito.verify(stubRunning).trigger("delete_book");
	}

	@Test
	void shouldTriggerAMessagingLabelForAStubWithIvyNotation() {
		StubRunning stubRunning = Mockito.mock(StubRunning.class);
		RestAssuredMockMvc.standaloneSetup(new HttpStubsController(stubRunning), new TriggerController(stubRunning));
		List<String> stubIds = List.of("sh.stubborn.contract.verifier.stubs:bootService:stubs",
				"sh.stubborn.contract.verifier.stubs:bootService", "bootService");
		for (String stubId : stubIds) {
			int statusCode = RestAssuredMockMvc.post("/triggers/" + stubId + "/delete_book").statusCode();
			assertThat(statusCode).isEqualTo(200);
			Mockito.verify(stubRunning).trigger(stubId, "delete_book");
		}
	}

	@Test
	void shouldThrowExceptionWhenTriggerIsMissing() {
		BDDAssertions.thenThrownBy(() -> RestAssuredMockMvc.post("/triggers/missing_label"))
			.hasMessageContaining("Exception occurred while trying to return [missing_label] label.")
			.hasMessageContaining("Available labels are")
			.hasMessageContaining("sh.stubborn.contract.verifier.stubs:loanIssuance:0.0.1-SNAPSHOT:stubs=[]");
	}

}
