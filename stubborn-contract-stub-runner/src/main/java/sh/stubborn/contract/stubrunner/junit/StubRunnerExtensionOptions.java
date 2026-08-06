/*
 * Copyright 2018-present the original author or authors.
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

package sh.stubborn.contract.stubrunner.junit;

import java.util.List;
import java.util.Map;

import sh.stubborn.contract.stubrunner.HttpServerStubConfigurer;
import sh.stubborn.contract.stubrunner.StubRunnerOptions;
import sh.stubborn.contract.stubrunner.StubsMode;
import sh.stubborn.contract.verifier.messaging.MessageVerifierReceiver;
import sh.stubborn.contract.verifier.messaging.MessageVerifierSender;

/**
 * Fluent options for the {@link StubRunnerExtension}.
 *
 * @author Olga Maciaszek-Sharma
 * @since 2.1.0
 */
interface StubRunnerExtensionOptions {

	/**
	 * Pass the {@link MessageVerifierSender} that this rule should use. If you don't pass
	 * anything a {@link ExceptionThrowingMessageVerifier} will be used. That means that
	 * an exception will be thrown whenever you try to do sth messaging related.
	 * @param messageVerifier message verifier implementation
	 * @return the stub runner extension
	 */
	StubRunnerExtension messageVerifierSender(MessageVerifierSender messageVerifier);

	/**
	 * Pass the {@link MessageVerifierReceiver} that this rule should use. If you don't
	 * pass anything a {@link ExceptionThrowingMessageVerifier} will be used. That means
	 * that an exception will be thrown whenever you try to do sth messaging related.
	 * @param messageVerifier message verifier implementation
	 * @return the stub runner extension
	 */
	StubRunnerExtension messageVerifierReceiver(MessageVerifierReceiver messageVerifier);

	/**
	 * Override all options.
	 * @param stubRunnerOptions options of Stub Runner
	 * @return the stub runner extension
	 * @see StubRunnerOptions
	 */
	StubRunnerExtension options(StubRunnerOptions stubRunnerOptions);

	/**
	 * Sets the minimum port value.
	 * @param minPort min value of port for WireMock server
	 * @return the stub runner extension
	 */
	StubRunnerExtension minPort(int minPort);

	/**
	 * Sets the maximum port value.
	 * @param maxPort max value of port for WireMock server
	 * @return the stub runner extension
	 */
	StubRunnerExtension maxPort(int maxPort);

	/**
	 * Sets the repository root.
	 * @param repoRoot string URI of repository containing stubs
	 * @return the stub runner extension
	 */
	StubRunnerExtension repoRoot(String repoRoot);

	/**
	 * Sets the stubs mode.
	 * @param stubsMode stubs mode that should be used
	 * @return the stub runner extension
	 */
	StubRunnerExtension stubsMode(StubsMode stubsMode);

	/**
	 * Downloads the stub with the given coordinates.
	 * @param groupId group id of the stub
	 * @param artifactId artifact id of the stub
	 * @param version version of the stub
	 * @param classifier classifier of the stub
	 * @return the stub runner extension with ports
	 */
	PortStubRunnerExtensionOptions downloadStub(String groupId, String artifactId, String version, String classifier);

	/**
	 * Downloads the latest stub with the given coordinates.
	 * @param groupId group id of the stub
	 * @param artifactId artifact id of the stub
	 * @param classifier classifier of the stub
	 * @return the stub runner extension with ports
	 */
	PortStubRunnerExtensionOptions downloadLatestStub(String groupId, String artifactId, String classifier);

	/**
	 * Downloads the stub with the given coordinates.
	 * @param groupId group id of the stub
	 * @param artifactId artifact id of the stub
	 * @param version version of the stub
	 * @return the stub runner extension with ports
	 */
	PortStubRunnerExtensionOptions downloadStub(String groupId, String artifactId, String version);

	/**
	 * Downloads the stub with the given coordinates.
	 * @param groupId group id of the stub
	 * @param artifactId artifact id of the stub
	 * @return the stub runner extension with ports
	 */
	PortStubRunnerExtensionOptions downloadStub(String groupId, String artifactId);

	/**
	 * Downloads a single stub described by the given ivy notation.
	 * @param ivyNotation ivy notation of a single stub to download
	 * @return the stub runner extension with ports
	 */
	PortStubRunnerExtensionOptions downloadStub(String ivyNotation);

	/**
	 * Downloads the stubs described by the given ivy notations.
	 * @param ivyNotations stubs to download in ivy notations
	 * @return the stub runner extension
	 */
	StubRunnerExtension downloadStubs(String... ivyNotations);

	/**
	 * Downloads the stubs described by the given ivy notations.
	 * @param ivyNotations stubs to download in ivy notations
	 * @return the stub runner extension
	 */
	StubRunnerExtension downloadStubs(List<String> ivyNotations);

	/**
	 * Enables stubs per consumer.
	 * @param stubPerConsumer allows stub per consumer
	 * @return the stub runner extension
	 */
	StubRunnerExtension withStubPerConsumer(boolean stubPerConsumer);

	/**
	 * Sets the consumer name.
	 * @param consumerName given consumer name
	 * @return the stub runner extension
	 */
	StubRunnerExtension withConsumerName(String consumerName);

	/**
	 * Sets the mappings output folder.
	 * @param mappingsOutputFolder allows setting the output folder for mappings
	 * @return the stub runner extension
	 */
	StubRunnerExtension withMappingsOutputFolder(String mappingsOutputFolder);

	/**
	 * Configures whether to delete stubs after the tests.
	 * @param deleteStubsAfterTest if set to {@code false} will NOT delete stubs from a
	 * temporary folder after running tests
	 * @return the stub runner extension
	 */
	StubRunnerExtension withDeleteStubsAfterTest(boolean deleteStubsAfterTest);

	/**
	 * Configures whether to generate stubs at runtime.
	 * @param generateStubs if set to {@code true} will NOT load generated stubs but will
	 * generate stubs from contract definitions at runtime
	 * @return the rule
	 */
	StubRunnerExtension withGenerateStubs(boolean generateStubs);

	/**
	 * Configures whether to fail when no stubs are found.
	 * @param failOnNoStubs when enabled, this flag will tell stub runner to throw an
	 * exception when no stubs / contracts were found.
	 * @return the rule
	 */
	StubRunnerExtension failOnNoStubs(boolean failOnNoStubs);

	/**
	 * Sets custom properties.
	 * @param properties map of properties that can be passed to custom
	 * {@link sh.stubborn.contract.stubrunner.StubDownloaderBuilder}
	 * @return the stub runner extension
	 */
	StubRunnerExtension withProperties(Map<String, String> properties);

	/**
	 * Sets the HTTP server stub configurer.
	 * @param httpServerStubConfigurer configuration for an HTTP server stub
	 * @return the stub runner extension
	 */
	StubRunnerExtension withHttpServerStubConfigurer(
			Class<? extends HttpServerStubConfigurer> httpServerStubConfigurer);

}
