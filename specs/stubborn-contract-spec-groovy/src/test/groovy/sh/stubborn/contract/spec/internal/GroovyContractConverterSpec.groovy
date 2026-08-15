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

import java.nio.file.Files
import java.nio.file.Path

import spock.lang.Specification
import spock.lang.TempDir

import sh.stubborn.contract.spec.Contract

/**
 * Mutation-focused coverage for {@link GroovyContractConverter}.
 */
class GroovyContractConverterSpec extends Specification {

	@TempDir
	Path tempDir

	private static final String SINGLE_DSL = '''
sh.stubborn.contract.spec.Contract.make {
    request { method 'GET'; url '/foo' }
    response { status 200 }
}
'''

	private static final String LIST_DSL = '''
[sh.stubborn.contract.spec.Contract.make {
    request { method 'GET'; url '/a' }
    response { status 200 }
},
sh.stubborn.contract.spec.Contract.make {
    request { method 'GET'; url '/b' }
    response { status 200 }
}]
'''

	def 'INSTANCE is non-null and usable'() {
		expect:
			GroovyContractConverter.INSTANCE != null
			GroovyContractConverter.INSTANCE instanceof GroovyContractConverter
	}

	def 'convertAsCollection(rootFolder, dsl) parses a single contract'() {
		when:
			Collection<Contract> contracts = GroovyContractConverter
					.convertAsCollection(tempDir.toFile(), SINGLE_DSL)
		then:
			contracts.size() == 1
			contracts.iterator().next() instanceof Contract
	}

	def 'convertAsCollection(rootFolder, dsl) parses a list of two contracts'() {
		when:
			Collection<Contract> contracts = GroovyContractConverter
					.convertAsCollection(tempDir.toFile(), LIST_DSL)
		then:
			contracts.size() == 2
	}

	def 'convertAsCollection(rootFolder, dsl) throws when dsl does not return a contract'() {
		when:
			GroovyContractConverter.convertAsCollection(tempDir.toFile(), '"hello"')
		then:
			IllegalStateException ex = thrown()
			ex.message.contains('not returning a Contract or list of Contracts')
	}

	def 'convertAsCollection(rootFolder, dsl) wraps invalid groovy in IllegalStateException'() {
		when:
			GroovyContractConverter.convertAsCollection(tempDir.toFile(), 'this is not valid groovy ][')
		then:
			thrown(Exception)
	}

	def 'convertAsCollection(File) parses file and defaults name to filename without extension'() {
		given:
			File dsl = writeFile('mycontract.groovy', SINGLE_DSL)
		when:
			Collection<Contract> contracts = GroovyContractConverter.convertAsCollection(dsl)
		then:
			contracts.size() == 1
			contracts.iterator().next().name == 'mycontract'
	}

	def 'convertAsCollection(rootFolder, File) parses file and defaults name to filename without extension'() {
		given:
			File dsl = writeFile('single.groovy', SINGLE_DSL)
		when:
			Collection<Contract> contracts = GroovyContractConverter
					.convertAsCollection(tempDir.toFile(), dsl)
		then:
			contracts.size() == 1
			contracts.iterator().next().name == 'single'
	}

	def 'convertAsCollection(File) names two unnamed contracts with index suffixes'() {
		given:
			File dsl = writeFile('multi.groovy', LIST_DSL)
		when:
			List<Contract> contracts = GroovyContractConverter.convertAsCollection(dsl) as List
		then:
			contracts.size() == 2
			contracts[0].name == 'multi_0'
			contracts[1].name == 'multi_1'
	}

	def 'convertAsCollection(File) keeps an explicitly named contract'() {
		given:
			String named = '''
sh.stubborn.contract.spec.Contract.make {
    name('explicit')
    request { method 'GET'; url '/foo' }
    response { status 200 }
}
'''
			File dsl = writeFile('named.groovy', named)
		when:
			Collection<Contract> contracts = GroovyContractConverter.convertAsCollection(dsl)
		then:
			contracts.iterator().next().name == 'explicit'
	}

	def 'convertAsCollection(File) returns empty list when file evaluates to null'() {
		given:
			File dsl = writeFile('nullish.groovy', 'null')
		when:
			Collection<Contract> contracts = GroovyContractConverter.convertAsCollection(dsl)
		then:
			contracts.isEmpty()
	}

	def 'convertAsCollection(rootFolder, File) throws when file does not return a contract'() {
		given:
			File dsl = writeFile('nope.groovy', '"hello"')
		when:
			GroovyContractConverter.convertAsCollection(tempDir.toFile(), dsl)
		then:
			IllegalStateException ex = thrown()
			ex.message.contains('not returning a Contract or list of Contracts')
	}

	def 'convertAsCollection(rootFolder, File) throws when file returns a list of non-contracts'() {
		given:
			File dsl = writeFile('nonContractList.groovy', '["a", "b"]')
		when:
			GroovyContractConverter.convertAsCollection(tempDir.toFile(), dsl)
		then:
			IllegalStateException ex = thrown()
			ex.message.contains('not returning a Contract or list of Contracts')
	}

	def 'convertAsCollection(rootFolder, File) wraps a checked failure (missing file) with the file path'() {
		given:
			File dsl = new File(tempDir.toFile(), 'missing.groovy')
		when:
			GroovyContractConverter.convertAsCollection(tempDir.toFile(), dsl)
		then:
			IllegalStateException ex = thrown()
			ex.message.contains(dsl.path)
	}

	def 'isAccepted is true for #name and false otherwise'() {
		expect:
			GroovyContractConverter.INSTANCE.isAccepted(new File(name)) == accepted
		where:
			name          || accepted
			'a.groovy'    || true
			'a.gvy'       || true
			'a.json'      || false
			'a.txt'       || false
	}

	def 'convertTo returns the same collection instance'() {
		given:
			Collection<Contract> input = GroovyContractConverter
					.convertAsCollection(tempDir.toFile(), SINGLE_DSL)
		expect:
			GroovyContractConverter.INSTANCE.convertTo(input).is(input)
	}

	def 'convertFrom delegates to convertAsCollection(File)'() {
		given:
			File dsl = writeFile('fromfile.groovy', SINGLE_DSL)
		when:
			Collection<Contract> contracts = GroovyContractConverter.INSTANCE.convertFrom(dsl)
		then:
			contracts.size() == 1
			contracts.iterator().next().name == 'fromfile'
	}

	private File writeFile(String name, String content) {
		File f = tempDir.resolve(name).toFile()
		Files.writeString(f.toPath(), content)
		return f
	}

}
