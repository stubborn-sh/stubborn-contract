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

package sh.stubborn.contract.verifier.messaging.rabbit;

import tools.jackson.databind.json.JsonMapper;

import org.springframework.amqp.support.converter.DefaultJacksonJavaTypeMapper;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Out-of-the-box JSON conversion for a consumer's {@code @RabbitListener} in a contract
 * test. Restores the zero-config developer experience users had with Spring Cloud
 * Contract: a consumer running {@code @AutoConfigureStubRunner} messaging against a real
 * RabbitMQ broker no longer has to hand-configure a {@code JacksonJsonMessageConverter}
 * to bind a typed listener parameter.
 *
 * <p>
 * The stub-runner Rabbit backend ({@code StubbornRabbitMessageVerifierSender}) publishes
 * a contract's {@code outputMessage} to the broker as raw JSON bytes with a
 * {@code contentType=application/json} property. Without a JSON converter Spring AMQP's
 * default {@code SimpleMessageConverter} yields a {@code String}, which fails to bind to
 * a typed listener record. This config registers a Jackson 3
 * {@link JacksonJsonMessageConverter} as the single {@link MessageConverter} bean that
 * Spring Boot wires into the {@code @RabbitListener} container factory. Its type mapper
 * is configured to ignore the Spring {@code __TypeId__} header (see
 * {@link #stubbornContractRabbitJsonMessageConverter()}) — a transport-neutral message
 * may carry {@code __TypeId__=java.lang.String}, which the default mapper would honour to
 * deserialize the JSON into a {@code String} that cannot bind to the typed listener. With
 * the header ignored the JSON is read as a generic tree and Spring's messaging layer
 * binds it to the listener's method-parameter type.
 *
 * <h3>Guards</h3>
 * <ul>
 * <li>{@link ConditionalOnClass @ConditionalOnClass(MessageConverter.class)} — only
 * active when Spring AMQP is on the classpath, so it is inert without Rabbit
 * messaging.</li>
 * <li>{@link ConditionalOnMissingBean @ConditionalOnMissingBean(MessageConverter.class)}
 * — it <em>never</em> overrides a converter the user has already defined.</li>
 * <li>{@link ConditionalOnProperty @ConditionalOnProperty} on
 * {@code stubborn.contract.messaging.consumer-converters.enabled} (default {@code true})
 * — set it to {@code false} to opt out of the magic entirely.</li>
 * </ul>
 *
 * <p>
 * The configuration is imported only through {@code @AutoConfigureMessageVerifier} (and
 * hence {@code @AutoConfigureStubRunner}), so it is harmless in production.
 *
 * @author Marcin Grzejszczak
 * @since 1.0.0
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(MessageConverter.class)
@ConditionalOnProperty(name = "stubborn.contract.messaging.consumer-converters.enabled", havingValue = "true",
		matchIfMissing = true)
public class ContractVerifierRabbitConsumerConverterConfiguration {

	@Bean
	@ConditionalOnMissingBean(MessageConverter.class)
	JacksonJsonMessageConverter stubbornContractRabbitJsonMessageConverter() {
		JacksonJsonMessageConverter converter = new JacksonJsonMessageConverter(new JsonMapper());
		converter.setJavaTypeMapper(ignoreTypeIdTypeMapper());
		return converter;
	}

	/**
	 * A type mapper that never reads the Spring {@code __TypeId__} header. A
	 * transport-neutral contract message may carry {@code __TypeId__=java.lang.String}
	 * (reflecting the JSON wire payload). The default mapper honours that header and
	 * deserializes the body into a {@code String}, which then cannot bind to the typed
	 * {@code @RabbitListener} parameter. By overriding the class-id field name to a
	 * header that is never present, the converter deserializes the JSON to a generic tree
	 * instead, and Spring's messaging layer binds it to the listener's method-parameter
	 * type — the same zero-config outcome as Kafka. All packages are trusted, since
	 * contract messages come from trusted stubs.
	 * @return a type mapper that ignores the {@code __TypeId__} header and trusts all
	 * packages
	 */
	private static DefaultJacksonJavaTypeMapper ignoreTypeIdTypeMapper() {
		DefaultJacksonJavaTypeMapper typeMapper = new DefaultJacksonJavaTypeMapper() {
			@Override
			public String getClassIdFieldName() {
				return "__stubborn_ignored_type_id__";
			}
		};
		typeMapper.setTrustedPackages("*");
		return typeMapper;
	}

}
