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

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import sh.stubborn.contract.spec.Contract;
import sh.stubborn.contract.spec.internal.Response;
import sh.stubborn.contract.verifier.file.SingleContractMetadata;
import sh.stubborn.contract.verifier.template.HandlebarsTemplateProcessor;
import sh.stubborn.contract.verifier.template.TemplateProcessor;

class GenericHttpBodyThen implements Then, BodyMethodVisitor {

	private final BlockBuilder blockBuilder;

	private final BodyParser bodyParser;

	private final TemplateProcessor templateProcessor;

	private final ComparisonBuilder comparisonBuilder;

	private final List<Then> thens = new LinkedList<>();

	GenericHttpBodyThen(BlockBuilder blockBuilder, GeneratedClassMetaData metaData, BodyParser bodyParser,
			ComparisonBuilder comparisonBuilder) {
		this.blockBuilder = blockBuilder;
		this.bodyParser = bodyParser;
		this.comparisonBuilder = comparisonBuilder;
		this.templateProcessor = new HandlebarsTemplateProcessor();
		this.thens
			.addAll(Arrays.asList(new GenericBinaryBodyThen(blockBuilder, metaData, this.bodyParser, comparisonBuilder),
					new GenericTextBodyThen(blockBuilder, metaData, this.bodyParser, this.comparisonBuilder),
					new GenericJsonBodyThen(blockBuilder, metaData, this.bodyParser, this.comparisonBuilder),
					new GenericXmlBodyThen(blockBuilder, this.bodyParser)));
	}

	@Override
	public MethodVisitor<Then> apply(SingleContractMetadata metadata) {
		endBodyBlock(this.blockBuilder);
		this.blockBuilder.addEmptyLine();
		startBodyBlock(this.blockBuilder, "and:");
		this.thens.stream().filter((then) -> then.accept(metadata)).forEach((then) -> {
			var unused = then.apply(metadata);
		});
		return this;
	}

	@Override
	public boolean accept(SingleContractMetadata metadata) {
		Contract contract = metadata.getContract();
		if (contract == null) {
			return false;
		}
		Response response = contract.getResponse();
		if (response == null) {
			return false;
		}
		return response.getBody() != null;
	}

}
