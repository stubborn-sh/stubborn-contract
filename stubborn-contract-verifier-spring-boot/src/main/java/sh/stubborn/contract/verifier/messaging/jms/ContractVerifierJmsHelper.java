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

package sh.stubborn.contract.verifier.messaging.jms;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.ObjectMessage;
import jakarta.jms.StreamMessage;
import jakarta.jms.TextMessage;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;
import sh.stubborn.contract.verifier.messaging.MessageVerifierReceiver;
import sh.stubborn.contract.verifier.messaging.MessageVerifierSender;
import sh.stubborn.contract.verifier.messaging.internal.ContractVerifierMessage;
import sh.stubborn.contract.verifier.messaging.internal.ContractVerifierMessaging;

/**
 * JMS specific {@link ContractVerifierMessaging} implementation.
 *
 * @author Marcin Grzejszczak
 */
class ContractVerifierJmsHelper extends ContractVerifierMessaging<Message> {

	private static final Log log = LogFactory.getLog(ContractVerifierJmsHelper.class);

	ContractVerifierJmsHelper(MessageVerifierSender<Message> sender, MessageVerifierReceiver<Message> receiver) {
		super(sender, receiver);
	}

	@Override
	protected ContractVerifierMessage convert(@Nullable Message message) {
		try {
			Map<String, Object> headers = headers(message);
			return new ContractVerifierMessage(getPayload(message), headers);
		}
		catch (JMSException ex) {
			log.warn("An exception occurred while trying to convert the JMS message", ex);
			throw new IllegalStateException(ex);
		}
	}

	private Map<String, Object> headers(@Nullable Message message) throws JMSException {
		Map<String, Object> headers = new HashMap<>();
		if (message == null) {
			return headers;
		}
		Enumeration enumeration = message.getPropertyNames();
		while (enumeration.hasMoreElements()) {
			Object element = enumeration.nextElement();
			String asString = element.toString();
			Object property = message.getObjectProperty(asString);
			headers.put(asString, property);
		}
		return headers;
	}

	private @Nullable Object getPayload(@Nullable Message message) throws JMSException {
		if (message == null) {
			return null;
		}
		else if (message instanceof TextMessage) {
			return ((TextMessage) message).getText();
		}
		else if (message instanceof StreamMessage) {
			return ((StreamMessage) message).readObject();
		}
		else if (message instanceof ObjectMessage) {
			return ((ObjectMessage) message).getObject();
		}
		return message.getBody(Object.class);
	}

}
