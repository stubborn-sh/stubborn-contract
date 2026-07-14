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

package sh.stubborn.contract.stubrunner

import spock.lang.Specification

/**
 * @author Marcin Grzejszczak
 */
class GitStubDownloaderPropertiesSpec extends Specification {

	def "should parse only the URL after protocol if it doesn't start with git"() {
		given:
			StubResource resource = resource("git://https://foo.com")
		when:
			GitStubDownloaderProperties props = new GitStubDownloaderProperties(resource, new StubRunnerOptionsBuilder().build())
		then:
			props.url == URI.create("https://foo.com")
	}

	def "should return the whole address if it starts with git@ but doesn't finish with dot git"() {
		given:
			StubResource resource = resource("git://git@foo.com/foo")
		when:
			GitStubDownloaderProperties props = new GitStubDownloaderProperties(resource, new StubRunnerOptionsBuilder().build())
		then:
			props.url == URI.create("git:git@foo.com/foo")
	}

	StubResource resource(String resourceUri) {
		return [
			getDescription: { null },
			getInputStream: { null },
			getURI: { java.net.URI.create(resourceUri) },
			getURL: { null },
			getFile: { null },
			getFilename: { null },
			exists: { false }
		] as StubResource
	}
}
