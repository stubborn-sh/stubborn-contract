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

package sh.stubborn.contract.stubrunner.spring;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.jspecify.annotations.Nullable;
import sh.stubborn.contract.stubrunner.BatchStubRunner;
import sh.stubborn.contract.stubrunner.BatchStubRunnerFactory;
import sh.stubborn.contract.stubrunner.RunningStubs;
import sh.stubborn.contract.stubrunner.StubConfiguration;
import sh.stubborn.contract.stubrunner.StubDownloaderBuilderProvider;
import sh.stubborn.contract.stubrunner.StubRunnerOptions;
import sh.stubborn.contract.stubrunner.StubRunnerOptionsBuilder;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.util.StringUtils;

/**
 * Configuration that initializes a {@link BatchStubRunner} that runs
 * {@link sh.stubborn.contract.stubrunner.StubRunner} instance for each stub.
 *
 * @author Marcin Grzejszczak
 * @author Eddú Meléndez
 * @since 1.0.0
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(StubRunnerProperties.class)
@ConditionalOnMissingBean(type = "sh.stubborn.contract.wiremock.WiremockServerConfiguration")
@Import(StubRunnerPortBeanPostProcessor.class)
public class StubRunnerConfiguration {

	static final String STUBRUNNER_PREFIX = "stubborn.contract.stubrunner.runningstubs";

	private StubDownloaderBuilderProvider provider = new StubDownloaderBuilderProvider();

	@Autowired
	private ConfigurableEnvironment environment;

	/**
	 * Bean that initializes stub runners, runs them and on shutdown closes them. Upon its
	 * instantiation JAR with stubs is downloaded and unpacked to a temporary folder and
	 * WireMock server are started for each of those stubs
	 * @param beanFactory bean factory
	 * @return the batch stub runner bean
	 */
	@Bean
	public BatchStubRunner batchStubRunner(BeanFactory beanFactory) {
		StubRunnerProperties props = beanFactory.getBean(StubRunnerProperties.class);
		StubRunnerOptionsBuilder builder = builder(props);
		String proxyHost = props.getProxyHost();
		if (proxyHost != null) {
			builder.withProxy(proxyHost, Objects.requireNonNull(props.getProxyPort()));
		}
		StubRunnerOptions stubRunnerOptions = stubRunnerOptions(builder);
		BatchStubRunner batchStubRunner = new BatchStubRunnerFactory(stubRunnerOptions,
				this.provider.get(stubRunnerOptions), new LazyMessageVerifier(beanFactory))
			.buildBatchStubRunner();
		// TODO: Consider running it in a separate thread
		RunningStubs runningStubs = batchStubRunner.runStubs();
		registerPort(runningStubs);
		return batchStubRunner;
	}

	private StubRunnerOptions stubRunnerOptions(StubRunnerOptionsBuilder builder) {
		return builder.build();
	}

	@Bean
	public BeanPostProcessor batchStubRunnerBeanPostProcessor(BatchStubRunner runner) {
		return new BeanPostProcessor() {
		};
	}

	private StubRunnerOptionsBuilder builder(StubRunnerProperties props) {
		StubRunnerOptionsBuilder builder = new StubRunnerOptionsBuilder()
			.withMinMaxPort(Integer.valueOf(resolvePlaceholder(props.getMinPort(), props.getMinPort())),
					Integer.valueOf(resolvePlaceholder(props.getMaxPort(), props.getMaxPort())))
			.withStubRepositoryRoot(props.getRepositoryRoot())
			.withStubsClassifier(this.environment.resolvePlaceholders(props.getClassifier()))
			.withStubs(resolvePlaceholder(props.getIds()))
			.withUsername(resolvePlaceholder(props.getUsername()))
			.withPassword(resolvePlaceholder(props.getPassword()))
			.withStubPerConsumer(Boolean.parseBoolean(resolvePlaceholder(props.isStubsPerConsumer())))
			.withConsumerName(consumerName(props))
			.withMappingsOutputFolder(resolvePlaceholder(props.getMappingsOutputFolder()))
			.withDeleteStubsAfterTest(Boolean.parseBoolean(resolvePlaceholder(props.isDeleteStubsAfterTest())))
			.withGenerateStubs(Boolean.parseBoolean(resolvePlaceholder(props.isGenerateStubs())))
			.withProperties(props.getProperties())
			.withHttpServerStubConfigurer(props.getHttpServerStubConfigurer())
			.withFailOnNoStubs(props.isFailOnNoStubs());
		String stubsMode = resolvePlaceholder(props.getStubsMode());
		if (stubsMode != null) {
			builder.withStubsMode(stubsMode);
		}
		String serverId = resolvePlaceholder(props.getServerId());
		if (serverId != null) {
			builder.withServerId(serverId);
		}
		return builder;
	}

	private String[] resolvePlaceholder(String[] string) {
		return Arrays.stream(string).map(this.environment::resolvePlaceholders).toArray(String[]::new);
	}

	private @Nullable String resolvePlaceholder(@Nullable Object string) {
		if (string == null) {
			return null;
		}
		return this.environment.resolvePlaceholders(string.toString());
	}

	private String resolvePlaceholder(Object string, Object defaultValue) {
		if (string == null) {
			return defaultValue.toString();
		}
		return this.environment.resolvePlaceholders(string.toString());
	}

	private @Nullable String consumerName(StubRunnerProperties props) {
		if (StringUtils.hasText(props.getConsumerName())) {
			return resolvePlaceholder(props.getConsumerName());
		}
		return this.environment.getProperty("spring.application.name");
	}

	private void registerPort(RunningStubs runStubs) {
		MutablePropertySources propertySources = this.environment.getPropertySources();
		if (!propertySources.contains(STUBRUNNER_PREFIX)) {
			propertySources.addFirst(new MapPropertySource(STUBRUNNER_PREFIX, new HashMap<>()));
		}
		Map<String, Object> source = ((MapPropertySource) Objects
			.requireNonNull(propertySources.get(STUBRUNNER_PREFIX))).getSource();
		for (Map.Entry<StubConfiguration, Integer> entry : runStubs.validNamesAndPorts().entrySet()) {
			source.put(STUBRUNNER_PREFIX + "." + entry.getKey().getArtifactId() + ".port", entry.getValue());
			// there are projects where artifact id is the same, what differs is the group
			// id
			source.put(STUBRUNNER_PREFIX + "." + entry.getKey().getGroupId() + "." + entry.getKey().getArtifactId()
					+ ".port", entry.getValue());
		}
	}

}
