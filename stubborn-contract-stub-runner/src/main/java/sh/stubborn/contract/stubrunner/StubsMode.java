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
 * Stub resolution modes.
 *
 * @author Marcin Grzejszczak
 */
public enum StubsMode {

	/**
	 * Pick the stubs from classpath.
	 */
	CLASSPATH,

	/**
	 * Fetch the stubs from local .m2.
	 */
	LOCAL,

	/**
	 * Fetch the stubs from a remote location.
	 */
	REMOTE,

}
