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

import org.jspecify.annotations.Nullable;

/**
 * Arguments passed to the {@link StubRunner} application.
 *
 * @author Marcin Grzejszczak
 * @since 1.0.0
 * @see StubRunner
 */
class Arguments {

	private final StubRunnerOptions stubRunnerOptions;

	private final String repositoryPath;

	private final @Nullable StubConfiguration stub;

	Arguments(StubRunnerOptions stubRunnerOptions) {
		this(stubRunnerOptions, "", null);
	}

	Arguments(StubRunnerOptions stubRunnerOptions, @Nullable String repositoryPath, @Nullable StubConfiguration stub) {
		this.stubRunnerOptions = stubRunnerOptions;
		this.repositoryPath = (repositoryPath != null) ? repositoryPath : "";
		this.stub = stub;
	}

	StubRunnerOptions getStubRunnerOptions() {
		return this.stubRunnerOptions;
	}

	String getRepositoryPath() {
		return this.repositoryPath;
	}

	@Nullable StubConfiguration getStub() {
		return this.stub;
	}

	@Override
	public String toString() {
		return "Arguments{" + "stubRunnerOptions=" + this.stubRunnerOptions + ", repositoryPath='" + this.repositoryPath
				+ '\'' + ", stub=" + this.stub + '}';
	}

}
