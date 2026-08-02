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

import java.util.Objects;

import sh.stubborn.contract.spec.internal.ExecutionProperty;
import sh.stubborn.contract.spec.internal.FromFileProperty;
import sh.stubborn.contract.spec.internal.Request;
import sh.stubborn.contract.verifier.file.SingleContractMetadata;
import sh.stubborn.contract.verifier.util.ContentType;

class MockMvcBodyGiven implements Given {

	private final BlockBuilder blockBuilder;

	private final GeneratedClassMetaData generatedClassMetaData;

	private final BodyParser bodyParser;

	MockMvcBodyGiven(BlockBuilder blockBuilder, GeneratedClassMetaData generatedClassMetaData, BodyParser bodyParser) {
		this.blockBuilder = blockBuilder;
		this.generatedClassMetaData = generatedClassMetaData;
		this.bodyParser = bodyParser;
	}

	@Override
	public MethodVisitor<Given> apply(SingleContractMetadata metadata) {
		this.blockBuilder.addIndented(bodyLine(metadata, this.generatedClassMetaData, this.bodyParser));
		return this;
	}

	/**
	 * The {@code .body(...)} continuation line for the request, in the exact form the
	 * legacy MockMvc/Explicit builders emit. Reused by {@link RequestModelBuilder} so the
	 * structured request path stays byte-identical to the legacy output.
	 * @param metadata the contract whose request body to render
	 * @param generatedClassMetaData the class-level metadata (needed to resolve
	 * file-based bodies)
	 * @param bodyParser the body parser used to quote/escape the body (the Java parser
	 * for MockMvc/Explicit)
	 * @return the {@code .body(...)} line (no statement terminator)
	 */
	static String bodyLine(SingleContractMetadata metadata, GeneratedClassMetaData generatedClassMetaData,
			BodyParser bodyParser) {
		Object body;
		Request request = Objects.requireNonNull(metadata.getContract().getRequest());
		Object serverValue = Objects.requireNonNull(request.getBody()).getServerValue();
		if (serverValue instanceof ExecutionProperty || serverValue instanceof FromFileProperty) {
			body = serverValue;
		}
		else {
			body = bodyParser.requestBodyAsString(metadata);
		}
		return getBodyString(metadata, body, new BodyReader(generatedClassMetaData), bodyParser);
	}

	private static String getBodyString(SingleContractMetadata metadata, Object body, BodyReader bodyReader,
			BodyParser bodyParser) {
		String value;
		if (body instanceof ExecutionProperty) {
			value = body.toString();
		}
		else if (body instanceof FromFileProperty) {
			FromFileProperty fileProperty = (FromFileProperty) body;
			value = fileProperty.isByte()
					? bodyReader.readBytesFromFileString(metadata, fileProperty, CommunicationType.REQUEST)
					: bodyReader.readStringFromFileString(metadata, fileProperty, CommunicationType.REQUEST);
		}
		else {
			String escaped = escapeRequestSpecialChars(metadata, body.toString());
			value = bodyParser.quotedEscapedLongText(escaped);
		}
		return ".body(" + value + ")";
	}

	private static String escapeRequestSpecialChars(SingleContractMetadata metadata, String string) {
		if (metadata.getInputTestContentType() == ContentType.JSON) {
			return string.replaceAll("\\\\n", "\\\\\\\\n");
		}
		return string;
	}

	@Override
	public boolean accept(SingleContractMetadata metadata) {
		Request request = metadata.getContract().getRequest();
		return request != null && request.getBody() != null;
	}

}
