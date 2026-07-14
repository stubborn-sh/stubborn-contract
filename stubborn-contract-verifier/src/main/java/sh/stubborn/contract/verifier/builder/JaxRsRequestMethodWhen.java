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

package sh.stubborn.contract.verifier.builder;

import java.util.Locale;

import sh.stubborn.contract.spec.internal.ExecutionProperty;
import sh.stubborn.contract.spec.internal.FromFileProperty;
import sh.stubborn.contract.spec.internal.Request;
import sh.stubborn.contract.verifier.file.SingleContractMetadata;
import sh.stubborn.contract.verifier.util.ContentType;

class JaxRsRequestMethodWhen implements When, JaxRsBodyParser {

	private final BlockBuilder blockBuilder;

	private final BodyReader bodyReader;

	JaxRsRequestMethodWhen(BlockBuilder blockBuilder, GeneratedClassMetaData metaData) {
		this.blockBuilder = blockBuilder;
		this.bodyReader = new BodyReader(metaData);
	}

	@Override
	public MethodVisitor<When> apply(SingleContractMetadata metadata) {
		appendMethodAndBody(metadata);
		return this;
	}

	void appendMethodAndBody(SingleContractMetadata metadata) {
		Request request = metadata.getContract().getRequest();
		ContentType type = metadata.getInputTestContentType();
		String method = request.getMethod().getServerValue().toString().toLowerCase(Locale.ROOT);
		if (request.getBody() != null) {
			String definedContentType = metadata.getDefinedInputTestContentType();
			String contentType = (definedContentType != null && !definedContentType.isBlank()) ? definedContentType
					: type.getMimeType();
			Object body = request.getBody().getServerValue();
			String value;
			if (body instanceof ExecutionProperty) {
				value = body.toString();
			}
			else if (body instanceof FromFileProperty) {
				FromFileProperty fileProperty = (FromFileProperty) body;
				value = fileProperty.isByte()
						? this.bodyReader.readBytesFromFileString(metadata, fileProperty, CommunicationType.REQUEST)
						: this.bodyReader.readStringFromFileString(metadata, fileProperty, CommunicationType.REQUEST);
			}
			else {
				value = "\"" + requestBodyAsString(metadata) + "\"";
			}
			this.blockBuilder.addIndented(".build(\"" + method.toUpperCase(Locale.ROOT) + "\", entity(" + value + ", \""
					+ contentType + "\"))");
		}
		else {
			this.blockBuilder.addIndented(".build(\"" + method.toUpperCase(Locale.ROOT) + "\")");
		}
	}

	@Override
	public boolean accept(SingleContractMetadata metadata) {
		return true;
	}

}
