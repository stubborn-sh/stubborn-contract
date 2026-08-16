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

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.junit.jupiter.api.Test
import sh.stubborn.contract.spec.Contract
import java.io.File

import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.test.context.SpringBootTest

/**
 * Regression guard for the Spring Boot 4.1 (Kotlin ~2.3) vs project Kotlin 2.4 scripting
 * dependency skew — driven from inside a running Spring Boot 4.1 application context.
 *
 * This module inherits `spring-boot-dependencies` (Boot 4.1) from the reactor root and then
 * realigns every Kotlin jar to `${kotlin.version}` (2.4.x) via the `kotlin-bom` import in this
 * module's own `pom.xml` (`dependencyManagement`). Boot's BOM would otherwise pin
 * `kotlin-scripting-*` back to its own (older) Kotlin while `kotlin-scripting-jsr223` and
 * `kotlin-scripting-compiler-embeddable` stay at 2.4.x — a mixed classpath on which the JVM
 * scripting host throws
 * `NoSuchMethodError: kotlin.script.experimental.jvm.util.JvmScriptCachingKt.getDisableCompilationCache`
 * at eval time.
 *
 * `@SpringBootTest` boots a Boot 4.1 application context so the `.kts` contract is compiled the
 * way a real Spring Boot 4.1 consumer application would compile it. If the `kotlin-bom` import is
 * ever removed, this test fails with that `NoSuchMethodError` from the scripting host — which is
 * exactly the skew we are guarding against.
 */
@SpringBootTest(classes = [KotlinScriptingVersionSkewRegressionTests.Boot41App::class])
class KotlinScriptingVersionSkewRegressionTests {

	@Test
	fun `compiles a kts contract through the scripting host inside a Boot 4_1 application`() {
		val converter = KotlinContractConverter()
		val kts = File(javaClass.classLoader.getResource("contracts/singleDefinition.kts")!!.toURI())

		val contracts = ArrayList<Contract>()
		assertThatCode { contracts.addAll(converter.convertFrom(kts)) }
			.withFailMessage(
				"Compiling a .kts contract via BasicJvmScriptingHost().eval failed inside a Spring Boot " +
					"4.1 application. This is the Boot/Kotlin scripting version skew — realign every Kotlin " +
					"jar to \${kotlin.version} via the kotlin-bom import in this module's pom.xml."
			)
			.doesNotThrowAnyException()

		assertThat(contracts).isNotEmpty()
		contracts.forEach(Contract::assertContract)
	}

	/**
	 * A minimal Spring Boot 4.1 application so the guard runs inside a booted Boot context,
	 * matching how a real Boot 4.1 consumer compiles Kotlin DSL contracts.
	 */
	@SpringBootConfiguration
	open class Boot41App

}
