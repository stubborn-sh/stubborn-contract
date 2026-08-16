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

package sh.stubborn.contract.verifier.file;

import java.io.File;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sh.stubborn.contract.spec.Contract;
import sh.stubborn.contract.spec.internal.DslProperty;
import sh.stubborn.contract.spec.internal.Header;
import sh.stubborn.contract.spec.internal.Headers;
import sh.stubborn.contract.spec.internal.OutputMessage;
import sh.stubborn.contract.spec.internal.Request;
import sh.stubborn.contract.spec.internal.Response;
import sh.stubborn.contract.verifier.util.ContentType;
import sh.stubborn.contract.verifier.util.ContentUtils;
import sh.stubborn.contract.verifier.util.NamesUtil;

/**
 * Metadata describing a single contract contained within a contract file.
 *
 * @author Marcin Grzejszczak
 * @since 1.0.0
 */
public class SingleContractMetadata {

	private static final Logger log = LoggerFactory.getLogger(SingleContractMetadata.class);

	private final ContractMetadata contractMetadata;

	private final Path stubsPath;

	private final Contract contract;

	private final List<Contract> allContracts;

	private final String definedInputStubContentType;

	private final ContentType inputStubContentType;

	private final ContentType evaluatedInputStubContentType;

	private final String definedOutputStubContentType;

	private final ContentType outputStubContentType;

	private final ContentType evaluatedOutputStubContentType;

	private final String definedInputTestContentType;

	private final ContentType inputTestContentType;

	private final ContentType evaluatedInputTestContentType;

	private final String definedOutputTestContentType;

	private final ContentType outputTestContentType;

	private final ContentType evaluatedOutputTestContentType;

	private @Nullable String methodName;

	private final boolean http;

	public SingleContractMetadata(Contract currentContract, ContractMetadata contractMetadata) {
		Objects.requireNonNull(currentContract, "Contract must not be null");
		this.allContracts = contractMetadata.getConvertedContract();
		this.contract = currentContract;
		this.contractMetadata = contractMetadata;
		Headers inputHeaders = inputHeaders(currentContract);
		DslProperty<?> inputBody = inputBody(currentContract);
		Headers outputHeaders = outputHeaders(currentContract);
		DslProperty<?> outputBody = outputBody(currentContract);
		Header inputContentType = contentTypeHeader(inputHeaders);
		Header outputContentType = contentTypeHeader(outputHeaders);
		this.definedInputTestContentType = Optional.ofNullable(inputContentType)
			.map(DslProperty::getServerValue)
			.map(Object::toString)
			.orElse("");
		this.evaluatedInputTestContentType = tryToEvaluateTestContentType(inputHeaders, inputBody);
		this.inputTestContentType = (inputBody != null) ? this.evaluatedInputTestContentType : ContentType.UNKNOWN;
		this.definedOutputTestContentType = Optional.ofNullable(outputContentType)
			.map(DslProperty::getServerValue)
			.map(Object::toString)
			.orElse("");
		this.evaluatedOutputTestContentType = tryToEvaluateTestContentType(outputHeaders, outputBody);
		this.outputTestContentType = (outputBody != null) ? this.evaluatedOutputTestContentType : ContentType.UNKNOWN;
		this.definedInputStubContentType = Optional.ofNullable(inputContentType)
			.map(DslProperty::getClientValue)
			.map(Object::toString)
			.orElse("");
		this.evaluatedInputStubContentType = tryToEvaluateStubContentType(inputHeaders, inputBody);
		this.inputStubContentType = (inputBody != null) ? this.evaluatedInputStubContentType : ContentType.UNKNOWN;
		this.definedOutputStubContentType = Optional.ofNullable(outputContentType)
			.map(DslProperty::getClientValue)
			.map(Object::toString)
			.orElse("");
		this.evaluatedOutputStubContentType = tryToEvaluateStubContentType(outputHeaders, outputBody);
		this.outputStubContentType = (outputBody != null) ? this.evaluatedOutputStubContentType : ContentType.UNKNOWN;
		this.http = currentContract.getRequest() != null;
		this.stubsPath = contractMetadata.getPath();
	}

	private @Nullable Header contentTypeHeader(@Nullable Headers headers) {
		return (headers != null) ? headers.getEntries()
			.stream()
			.filter((it) -> "Content-Type".equalsIgnoreCase(it.getName()))
			.findFirst()
			.orElse(null) : null;
	}

	private ContentType tryToEvaluateStubContentType(Headers mainHeaders, @Nullable DslProperty<?> body) {
		Object clientValue = (body != null) ? Objects.requireNonNullElse(body.getClientValue(), "") : "";
		ContentType contentType = ContentUtils.evaluateClientSideContentType(mainHeaders, clientValue);
		if (contentType == ContentType.DEFINED || contentType == ContentType.UNKNOWN) {
			// try to retrieve from the other side (e.g. stub side was a regex, but test
			// side is concrete)
			Object serverValue = (body != null) ? Objects.requireNonNullElse(body.getServerValue(), "") : "";
			return ContentUtils.evaluateServerSideContentType(mainHeaders, serverValue);
		}
		return contentType;
	}

	private ContentType tryToEvaluateTestContentType(Headers mainHeaders, @Nullable DslProperty<?> body) {
		Object serverValue = (body != null) ? Objects.requireNonNullElse(body.getServerValue(), "") : "";
		ContentType contentType = ContentUtils.evaluateClientSideContentType(mainHeaders, serverValue);
		if (contentType == ContentType.DEFINED || contentType == ContentType.UNKNOWN) {
			// try to retrieve from the other side (e.g. stub side was a regex, but test
			// side is concrete)
			Object clientValue = (body != null) ? Objects.requireNonNullElse(body.getClientValue(), "") : "";
			return ContentUtils.evaluateServerSideContentType(mainHeaders, clientValue);
		}
		return contentType;
	}

	public boolean isJson() {
		return this.inputTestContentType.equals(ContentType.JSON) || this.outputTestContentType.equals(ContentType.JSON)
				|| this.inputStubContentType.equals(ContentType.JSON)
				|| this.outputStubContentType.equals(ContentType.JSON);
	}

	public boolean evaluatesToJson() {
		return isJson() || this.evaluatedInputTestContentType.equals(ContentType.JSON)
				|| this.evaluatedOutputTestContentType.equals(ContentType.JSON)
				|| this.evaluatedInputStubContentType.equals(ContentType.JSON)
				|| this.evaluatedOutputStubContentType.equals(ContentType.JSON);
	}

	public boolean isIgnored() {
		return this.contract.getIgnored() || this.contractMetadata.getIgnored();
	}

	public boolean isXml() {
		return this.inputTestContentType.equals(ContentType.XML) || this.outputTestContentType.equals(ContentType.XML)
				|| this.inputStubContentType.equals(ContentType.XML)
				|| this.outputStubContentType.equals(ContentType.XML);
	}

	public boolean isHttp() {
		return this.http;
	}

	public boolean isInProgress() {
		return this.contract.isInProgress();
	}

	public boolean isMessaging() {
		return !isHttp();
	}

	private @Nullable DslProperty<?> inputBody(Contract contract) {
		return Optional.ofNullable(contract.getRequest())
			.map(Request::getBody)
			.map(DslProperty.class::cast)
			.orElse(null);
	}

	private Headers inputHeaders(Contract contract) {
		Headers found = Optional.ofNullable(contract.getRequest()).map(Request::getHeaders).orElse(null);
		return (found != null) ? found : new Headers();
	}

	private @Nullable DslProperty<?> outputBody(Contract contract) {
		return Optional.ofNullable(contract.getResponse())
			.map(Response::getBody)
			.map(DslProperty.class::cast)
			.orElseGet(() -> Optional.ofNullable(contract.getOutputMessage()).map(OutputMessage::getBody).orElse(null));
	}

	private Headers outputHeaders(Contract contract) {
		Headers found = Optional.ofNullable(contract.getResponse())
			.map(Response::getHeaders)
			.orElseGet(
					() -> Optional.ofNullable(contract.getOutputMessage()).map(OutputMessage::getHeaders).orElse(null));
		return (found != null) ? found : new Headers();
	}

	public String methodName() {
		if (this.methodName == null) {
			this.methodName = calculateMethodName();
		}
		return this.methodName;
	}

	private String calculateMethodName() {
		String contractName = this.contract.getName();
		if (contractName != null && !NamesUtil.isEmpty(contractName)) {
			String name = NamesUtil.camelCase(NamesUtil.convertIllegalPackageChars(contractName));
			log.trace("Overriding the default test name with [{}]", name);
			return name;
		}
		if (this.allContracts.size() > 1) {
			int index = this.allContracts.indexOf(getContract());
			String name = String.format("%s_%d", camelCasedMethodFromFileName(this.stubsPath), index);
			log.trace("Scenario found. The method name will be [{}]", name);
			return name;
		}
		String name = camelCasedMethodFromFileName(this.stubsPath);
		log.trace("The method name will be [{}]", name);
		return name;
	}

	private static String camelCasedMethodFromFileName(Path stubsPath) {
		return NamesUtil.camelCase(NamesUtil.convertIllegalMethodNameChars(
				NamesUtil.toLastDot(NamesUtil.afterLast(stubsPath.toString(), File.separator))));
	}

	public ContractMetadata getContractMetadata() {
		return this.contractMetadata;
	}

	public Contract getContract() {
		return this.contract;
	}

	public Collection<Contract> getAllContracts() {
		return this.allContracts;
	}

	public String getDefinedInputStubContentType() {
		return this.definedInputStubContentType;
	}

	public ContentType getInputStubContentType() {
		return this.inputStubContentType;
	}

	public ContentType getEvaluatedInputStubContentType() {
		return this.evaluatedInputStubContentType;
	}

	public String getDefinedOutputStubContentType() {
		return this.definedOutputStubContentType;
	}

	public ContentType getEvaluatedOutputStubContentType() {
		return this.evaluatedOutputStubContentType;
	}

	public String getDefinedInputTestContentType() {
		return this.definedInputTestContentType;
	}

	public ContentType getInputTestContentType() {
		return this.inputTestContentType;
	}

	public String getDefinedOutputTestContentType() {
		return this.definedOutputTestContentType;
	}

	public ContentType getOutputTestContentType() {
		return this.outputTestContentType;
	}

	public ContentType getEvaluatedOutputTestContentType() {
		return this.evaluatedOutputTestContentType;
	}

}
