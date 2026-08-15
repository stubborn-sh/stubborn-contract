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

package sh.stubborn.contract.spec.internal

import java.util.regex.Pattern

import org.codehaus.groovy.runtime.GStringImpl
import spock.lang.Specification

/**
 * Mutation-focused coverage for {@link GroovyDslPropertyConverter}.
 */
class GroovyDslPropertyConverterSpec extends Specification {

	GroovyDslPropertyConverter converter = new GroovyDslPropertyConverter()

	def 'testSide returns the same object for a non-GString input'() {
		given:
			Object input = 'plain string'
		expect:
			converter.testSide(input).is(input)
	}

	def 'testSide returns the same GString when it holds no RegexProperty'() {
		given:
			GStringImpl gstring = new GStringImpl(['x'] as Object[], ['a', 'b'] as String[])
		expect:
			converter.testSide(gstring).is(gstring)
	}

	def 'testSide converts a GString holding a RegexProperty into a Url with the generated value'() {
		given:
			RegexProperty regexProperty = new RegexProperty(Pattern.compile('[0]'))
			GStringImpl gstring = new GStringImpl([regexProperty] as Object[], ['/foo/', ''] as String[])
		when:
			Object result = converter.testSide(gstring)
		then:
			result instanceof Url
			Url url = (Url) result
			url.clientValue != null
			url.clientValue.toString() == '/foo/0'
	}

}
