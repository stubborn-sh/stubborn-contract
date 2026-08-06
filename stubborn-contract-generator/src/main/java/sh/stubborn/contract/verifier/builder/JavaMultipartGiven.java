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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import sh.stubborn.contract.spec.internal.Multipart;
import sh.stubborn.contract.spec.internal.NamedProperty;
import sh.stubborn.contract.spec.internal.Request;
import sh.stubborn.contract.verifier.config.TestFramework;
import sh.stubborn.contract.verifier.file.SingleContractMetadata;
import sh.stubborn.contract.verifier.util.ContentUtils;
import sh.stubborn.contract.verifier.util.MapConverter;

class JavaMultipartGiven implements Given, RestAssuredAcceptor {

	private final BlockBuilder blockBuilder;

	private final GeneratedClassMetaData generatedClassMetaData;

	private final BodyParser bodyParser;

	JavaMultipartGiven(BlockBuilder blockBuilder, GeneratedClassMetaData generatedClassMetaData,
			BodyParser bodyParser) {
		this.blockBuilder = blockBuilder;
		this.bodyParser = bodyParser;
		this.generatedClassMetaData = generatedClassMetaData;
	}

	@Override
	public MethodVisitor<Given> apply(SingleContractMetadata metadata) {
		multipartLines(metadata, this.generatedClassMetaData, this.bodyParser).forEach(this.blockBuilder::addLine);
		return this;
	}

	/**
	 * The multipart continuation lines ({@code .multiPart(...)} for a named file part,
	 * {@code .param(...)} for a plain form parameter), in the exact order and form the
	 * legacy MockMvc/Explicit builder emits. Reused by {@link RequestModelBuilder} so the
	 * structured request path stays byte-identical to the legacy output.
	 * @param metadata the contract whose multipart to render
	 * @param generatedClassMetaData the class metadata (used to read file-based parts)
	 * @param bodyParser the body parser used to quote plain parameters (the Java parser
	 * for MockMvc/Explicit)
	 * @return the multipart continuation lines (no statement terminators)
	 */
	static List<String> multipartLines(SingleContractMetadata metadata, GeneratedClassMetaData generatedClassMetaData,
			BodyParser bodyParser) {
		BodyReader bodyReader = new BodyReader(generatedClassMetaData);
		List<String> lines = new ArrayList<>();
		getMultipartParameters(metadata)
			.forEach((key, value) -> lines.add(multipartParameterLine(metadata, key, value, bodyReader, bodyParser)));
		return lines;
	}

	private static String multipartParameterLine(SingleContractMetadata metadata, String key, Object value,
			BodyReader bodyReader, BodyParser bodyParser) {
		if (value instanceof NamedProperty) {
			return ".multiPart(" + getMultipartFileParameterContent(metadata, key, (NamedProperty) value, bodyReader)
					+ ")";
		}
		return getParameterString(key, value, bodyParser);
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> getMultipartParameters(SingleContractMetadata metadata) {
		Request request = Objects.requireNonNull(metadata.getContract().getRequest());
		Multipart multipart = Objects.requireNonNull(request.getMultipart());
		return (Map<String, Object>) Objects.requireNonNull(multipart.getServerValue());
	}

	private static String getMultipartFileParameterContent(SingleContractMetadata metadata, String propertyName,
			NamedProperty propertyValue, BodyReader bodyReader) {
		return ContentUtils.getJavaMultipartFileParameterContent(propertyName, propertyValue,
				(fileProp) -> bodyReader.readBytesFromFileString(metadata, fileProp, CommunicationType.REQUEST));
	}

	private static String getParameterString(String key, Object value, BodyParser bodyParser) {
		return ".param(" + bodyParser.quotedShortText(key) + ", "
				+ bodyParser.quotedShortText(MapConverter.getTestSideValuesForNonBody(value)) + ")";
	}

	@Override
	public boolean accept(SingleContractMetadata metadata) {
		Request request = metadata.getContract().getRequest();
		return request != null && request.getMultipart() != null && acceptType(this.generatedClassMetaData, metadata)
				&& this.generatedClassMetaData.configProperties.getTestFramework() != TestFramework.SPOCK;
	}

}
