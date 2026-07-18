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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import sh.stubborn.contract.stubrunner.StubsMode;
import sh.stubborn.contract.stubrunner.util.StubsParser;

/**
 * A builder object for {@link StubRunnerOptions}.
 *
 * @author Marcin Grzejszczak
 * @author Eddú Meléndez
 */
public class StubRunnerOptionsBuilder {

	private static final String DELIMITER = ":";

	private LinkedList<String> stubs = new LinkedList<>();

	private Collection<StubConfiguration> stubConfigurations = new ArrayList<>();

	private Map<StubConfiguration, Integer> stubIdsToPortMapping = new LinkedHashMap<>();

	private Integer minPortValue = 10000;

	private Integer maxPortValue = 15000;

	private StubResource stubRepositoryRoot;

	private String stubsClassifier = "stubs";

	private String username;

	private String password;

	private StubRunnerOptions.StubRunnerProxyOptions stubRunnerProxyOptions;

	private boolean stubsPerConsumer = false;

	private String consumerName;

	private String mappingsOutputFolder;

	private StubsMode stubsMode;

	private boolean deleteStubsAfterTest = true;

	private boolean generateStubs;

	private boolean failOnNoStubs = true;

	private Map<String, String> properties = new HashMap<>();

	private Class httpServerStubConfigurer = HttpServerStubConfigurer.NoOpHttpServerStubConfigurer.class;

	private String serverId;

	public StubRunnerOptionsBuilder() {
	}

	public StubRunnerOptionsBuilder(StubRunnerOptions options) {
		withOptions(options);
	}

	private static List<String> stubsToList(String[] stubIdsToPortMapping) {
		List<String> list = new ArrayList<>();
		if (stubIdsToPortMapping.length == 1 && !containsRange(stubIdsToPortMapping[0])) {
			list.addAll(Arrays.stream(stubIdsToPortMapping[0].split(","))
				.map(String::trim)
				.collect(Collectors.toCollection(LinkedHashSet::new)));
			return list;
		}
		else if (stubIdsToPortMapping.length == 1 && containsRange(stubIdsToPortMapping[0])) {
			List<String> parts = new ArrayList<>();
			String[] split = stubIdsToPortMapping[0].split(",");
			for (String string : split) {
				if (containsClosingRange(string)) {
					String last = parts.remove(parts.size() - 1);
					parts.add(last + "," + string);
				}
				else {
					parts.add(string);
				}
			}
			list.addAll(parts);
			return list;
		}
		Collections.addAll(list, stubIdsToPortMapping);
		return list;
	}

	private static boolean containsRange(String s) {
		return s.contains("[") || s.contains("(");
	}

	private static boolean containsClosingRange(String s) {
		return s.contains("]") || s.contains(")");
	}

	public StubRunnerOptionsBuilder withStubs(String... stubs) {
		addStub(stubsToList(stubs));
		return this;
	}

	public StubRunnerOptionsBuilder withStubs(List<String> stubs) {
		for (String stub : stubs) {
			withStubs(stub);
		}
		return this;
	}

	public StubRunnerOptionsBuilder withMinMaxPort(Integer minPortValue, Integer maxPortValue) {
		this.minPortValue = minPortValue;
		this.maxPortValue = maxPortValue;
		return this;
	}

	public StubRunnerOptionsBuilder withMinPort(int minPortValue) {
		this.minPortValue = minPortValue;
		return this;
	}

	public StubRunnerOptionsBuilder withMaxPort(int maxPortValue) {
		this.maxPortValue = maxPortValue;
		return this;
	}

	public StubRunnerOptionsBuilder withStubRepositoryRoot(StubResource stubRepositoryRoot) {
		this.stubRepositoryRoot = stubRepositoryRoot;
		return this;
	}

	public StubRunnerOptionsBuilder withStubRepositoryRoot(String stubRepositoryRoot) {
		if (stubRepositoryRoot != null && !stubRepositoryRoot.isBlank()) {
			this.stubRepositoryRoot = ResourceResolver.resource(stubRepositoryRoot);
		}
		return this;
	}

	public StubRunnerOptionsBuilder withStubsMode(StubsMode stubsMode) {
		if (stubsMode == null) {
			return this;
		}
		this.stubsMode = stubsMode;
		return this;
	}

	public StubRunnerOptionsBuilder withStubsMode(String stubsMode) {
		if (stubsMode == null) {
			return this;
		}
		this.stubsMode = StubsMode.valueOf(stubsMode);
		return this;
	}

	public StubRunnerOptionsBuilder withStubsClassifier(String stubsClassifier) {
		this.stubsClassifier = stubsClassifier;
		return this;
	}

	public StubRunnerOptionsBuilder withPort(Integer port) {
		String lastStub = this.stubs.peekLast();
		addPort(lastStub + DELIMITER + port);
		return this;
	}

	public StubRunnerOptionsBuilder withOptions(StubRunnerOptions options) {
		this.minPortValue = options.minPortValue;
		this.maxPortValue = options.maxPortValue;
		this.stubRepositoryRoot = options.stubRepositoryRoot;
		this.stubsMode = options.stubsMode;
		this.stubsClassifier = options.stubsClassifier;
		this.username = options.username;
		this.password = options.password;
		this.stubRunnerProxyOptions = options.getStubRunnerProxyOptions();
		this.stubsPerConsumer = options.isStubsPerConsumer();
		this.consumerName = options.getConsumerName();
		this.mappingsOutputFolder = options.getMappingsOutputFolder();
		this.stubConfigurations = options.dependencies != null ? new ArrayList<>(options.dependencies)
				: new ArrayList<>();
		this.stubIdsToPortMapping = options.stubIdsToPortMapping != null ? options.stubIdsToPortMapping
				: new LinkedHashMap<>();
		this.deleteStubsAfterTest = options.isDeleteStubsAfterTest();
		this.generateStubs = options.isGenerateStubs();
		this.failOnNoStubs = options.isFailOnNoStubs();
		this.properties = options.getProperties();
		this.httpServerStubConfigurer = options.getHttpServerStubConfigurer();
		this.serverId = options.getServerId();
		return this;
	}

	public StubRunnerOptionsBuilder withMappingsOutputFolder(String mappingsOutputFolder) {
		this.mappingsOutputFolder = mappingsOutputFolder;
		return this;
	}

	public StubRunnerOptionsBuilder withDeleteStubsAfterTest(boolean deleteStubsAfterTest) {
		this.deleteStubsAfterTest = deleteStubsAfterTest;
		return this;
	}

	public StubRunnerOptionsBuilder withGenerateStubs(boolean generateStubs) {
		this.generateStubs = generateStubs;
		return this;
	}

	public StubRunnerOptionsBuilder withFailOnNoStubs(boolean failOnNoStubs) {
		this.failOnNoStubs = failOnNoStubs;
		return this;
	}

	public StubRunnerOptionsBuilder withProperties(Map<String, String> properties) {
		this.properties = properties;
		return this;
	}

	public StubRunnerOptionsBuilder withHttpServerStubConfigurer(Class httpServerStubConfigurer) {
		this.httpServerStubConfigurer = httpServerStubConfigurer;
		return this;
	}

	public StubRunnerOptionsBuilder withServerId(String serverId) {
		this.serverId = serverId;
		return this;
	}

	public StubRunnerOptions build() {
		return new StubRunnerOptions(this.minPortValue, this.maxPortValue, this.stubRepositoryRoot, this.stubsMode,
				this.stubsClassifier, buildDependencies(), this.stubIdsToPortMapping, this.username, this.password,
				this.stubRunnerProxyOptions, this.stubsPerConsumer, this.consumerName, this.mappingsOutputFolder,
				this.deleteStubsAfterTest, this.generateStubs, this.failOnNoStubs, this.properties,
				this.httpServerStubConfigurer, this.serverId);
	}

	private Collection<StubConfiguration> buildDependencies() {
		List<StubConfiguration> stubConfigurations = StubsParser.fromString(this.stubs, this.stubsClassifier);
		this.stubConfigurations.addAll(stubConfigurations);
		return this.stubConfigurations;
	}

	private void addStub(List<String> notations) {
		for (String notation : notations) {
			addStub(notation);
		}
	}

	private void addStub(String notation) {
		if (StubsParser.hasPort(notation)) {
			addPort(notation);
			this.stubs.add(StubsParser.ivyFromStringWithPort(notation));
		}
		else {
			this.stubs.add(notation);
		}
	}

	private void addPort(String notation) {
		putStubIdsToPortMapping(StubsParser.fromStringWithPort(notation));
	}

	private void putStubIdsToPortMapping(Map<StubConfiguration, Integer> stubIdsToPortMapping) {
		this.stubIdsToPortMapping.putAll(stubIdsToPortMapping);
	}

	public StubRunnerOptionsBuilder withUsername(final String username) {
		this.username = username;
		return this;
	}

	public StubRunnerOptionsBuilder withPassword(final String password) {
		this.password = password;
		return this;
	}

	public StubRunnerOptionsBuilder withProxy(final String proxyHost, final int proxyPort) {
		this.stubRunnerProxyOptions = new StubRunnerOptions.StubRunnerProxyOptions(proxyHost, proxyPort);
		return this;
	}

	public StubRunnerOptionsBuilder withStubPerConsumer(boolean stubPerConsumer) {
		this.stubsPerConsumer = stubPerConsumer;
		return this;
	}

	public StubRunnerOptionsBuilder withConsumerName(String consumerName) {
		this.consumerName = consumerName;
		return this;
	}

}
