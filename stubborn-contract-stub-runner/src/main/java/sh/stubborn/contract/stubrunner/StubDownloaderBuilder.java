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

/**
 * Builder for a {@link StubDownloader}. Instances must have a no-arg constructor so they
 * can be registered via {@link java.util.ServiceLoader} in
 * {@code META-INF/services/sh.stubborn.contract.stubrunner.StubDownloaderBuilder}. The
 * {@link StubDownloader} itself is built from options after discovery.
 *
 * <p>
 * Implementations may also override {@link #resolve(String)} to tell the
 * {@link ResourceResolver} how to convert a protocol-specific URL string into a
 * {@link StubResource}.
 *
 * @author Marcin Grzejszczak
 * @since 1.1.0
 */
public interface StubDownloaderBuilder {

	/**
	 * @param stubRunnerOptions options of Stub Runner
	 * @return {@link StubDownloader} instance or {@code null} if current parameters don't
	 * allow building the instance
	 */
	StubDownloader build(StubRunnerOptions stubRunnerOptions);

	/**
	 * Converts a URL string into a {@link StubResource} for protocols understood by this
	 * builder. Returns {@code null} if the URL is not handled.
	 * @param location URL string
	 * @return resolved {@link StubResource} or {@code null}
	 */
	default StubResource resolve(String location) {
		return null;
	}

}
