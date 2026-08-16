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

package sh.stubborn.contract.verifier.converter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import org.jspecify.annotations.Nullable;
import sh.stubborn.contract.spec.Contract;

/**
 * YAML representation of a {@link Contract}.
 *
 * @author Marcin Grzejszczak
 * @author Tim Ysewyn
 * @since 1.0.0
 */
public class YamlContract {

	public @Nullable Request request;

	public @Nullable Response response;

	public @Nullable Input input;

	public @Nullable OutputMessage outputMessage;

	public @Nullable String description;

	public @Nullable String label;

	public @Nullable String name;

	public @Nullable Integer priority;

	public boolean ignored;

	public boolean inProgress;

	public Map<String, Object> metadata = new HashMap<>();

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		YamlContract that = (YamlContract) o;
		return this.ignored == that.ignored && this.inProgress == that.inProgress
				&& Objects.equals(this.request, that.request) && Objects.equals(this.response, that.response)
				&& Objects.equals(this.input, that.input) && Objects.equals(this.outputMessage, that.outputMessage)
				&& Objects.equals(this.description, that.description) && Objects.equals(this.label, that.label)
				&& Objects.equals(this.name, that.name) && Objects.equals(this.priority, that.priority)
				&& this.metadata.equals(that.metadata);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.request, this.response, this.input, this.outputMessage, this.description, this.label,
				this.name, this.priority, this.ignored, this.inProgress, this.metadata);
	}

	/**
	 * YAML representation of a contract's HTTP request.
	 *
	 * @author Marcin Grzejszczak
	 * @since 1.0.0
	 */
	public static class Request {

		public @Nullable String method;

		public @Nullable String url;

		public @Nullable String urlPath;

		public Map<String, Object> queryParameters = new LinkedHashMap<String, Object>();

		public @Nullable Map<String, Object> headers = new LinkedHashMap<String, Object>();

		public @Nullable Map<String, Object> cookies = new LinkedHashMap<String, Object>();

		public @Nullable Object body;

		public @Nullable String bodyFromFile;

		public @Nullable String bodyFromFileAsBytes;

		public StubMatchers matchers = new StubMatchers();

		public @Nullable Multipart multipart;

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			Request request = (Request) o;
			return Objects.equals(this.method, request.method) && Objects.equals(this.url, request.url)
					&& Objects.equals(this.urlPath, request.urlPath)
					&& Objects.equals(this.queryParameters, request.queryParameters)
					&& Objects.equals(this.headers, request.headers) && Objects.equals(this.cookies, request.cookies)
					&& Objects.equals(this.body, request.body)
					&& Objects.equals(this.bodyFromFile, request.bodyFromFile)
					&& Objects.equals(this.bodyFromFileAsBytes, request.bodyFromFileAsBytes)
					&& Objects.equals(this.matchers, request.matchers)
					&& Objects.equals(this.multipart, request.multipart);
		}

		@Override
		public int hashCode() {
			return Objects.hash(this.method, this.url, this.urlPath, this.queryParameters, this.headers, this.cookies,
					this.body, this.bodyFromFile, this.bodyFromFileAsBytes, this.matchers, this.multipart);
		}

		@Override
		public String toString() {
			return "Request{" + "method='" + this.method + '\'' + ", url='" + this.url + '\'' + ", urlPath='"
					+ this.urlPath + '\'' + ", queryParameters=" + this.queryParameters + ", headers=" + this.headers
					+ ", cookies=" + this.cookies + ", body=" + this.body + ", bodyFromFile='" + this.bodyFromFile
					+ '\'' + ", bodyFromFileAsBytes='" + this.bodyFromFileAsBytes + '\'' + ", matchers=" + this.matchers
					+ ", multipart=" + this.multipart + '}';
		}

	}

	/**
	 * YAML representation of a multipart request entry.
	 *
	 * @author Marcin Grzejszczak
	 * @since 1.0.0
	 */
	public static class Multipart {

		public Map<String, String> params = new LinkedHashMap<String, String>();

		public List<Named> named = new ArrayList<Named>();

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			Multipart multipart = (Multipart) o;
			return Objects.equals(this.params, multipart.params) && Objects.equals(this.named, multipart.named);
		}

		@Override
		public int hashCode() {
			return Objects.hash(this.params, this.named);
		}

		@Override
		public String toString() {
			return "Multipart{" + "params=" + this.params + ", named=" + this.named + '}';
		}

	}

	/**
	 * YAML representation of a named multipart part.
	 *
	 * @author Marcin Grzejszczak
	 * @since 1.0.0
	 */
	public static class Named {

		public @Nullable String paramName;

		public @Nullable String fileName;

		public @Nullable String fileContent;

		public @Nullable String fileContentAsBytes;

		public @Nullable String fileContentFromFileAsBytes;

		public @Nullable String contentType;

		public @Nullable String fileNameCommand;

		public @Nullable String fileContentCommand;

		public @Nullable String contentTypeCommand;

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			Named named = (Named) o;
			return Objects.equals(this.paramName, named.paramName) && Objects.equals(this.fileName, named.fileName)
					&& Objects.equals(this.fileContent, named.fileContent)
					&& Objects.equals(this.fileContentAsBytes, named.fileContentAsBytes)
					&& Objects.equals(this.fileContentFromFileAsBytes, named.fileContentFromFileAsBytes)
					&& Objects.equals(this.contentType, named.contentType)
					&& Objects.equals(this.fileNameCommand, named.fileNameCommand)
					&& Objects.equals(this.fileContentCommand, named.fileContentCommand)
					&& Objects.equals(this.contentTypeCommand, named.contentTypeCommand);
		}

		@Override
		public int hashCode() {
			return Objects.hash(this.paramName, this.fileName, this.fileContent, this.fileContentAsBytes,
					this.fileContentFromFileAsBytes, this.contentType, this.fileNameCommand, this.fileContentCommand,
					this.contentTypeCommand);
		}

		@Override
		public String toString() {
			return "Named{" + "paramName='" + this.paramName + '\'' + ", fileName='" + this.fileName + '\''
					+ ", fileContent='" + this.fileContent + '\'' + ", fileContentAsBytes='" + this.fileContentAsBytes
					+ '\'' + ", fileContentFromFileAsBytes='" + this.fileContentFromFileAsBytes + '\''
					+ ", contentType='" + this.contentType + '\'' + ", fileNameCommand='" + this.fileNameCommand + '\''
					+ ", fileContentCommand='" + this.fileContentCommand + '\'' + ", contentTypeCommand='"
					+ this.contentTypeCommand + '\'' + '}';
		}

	}

	/**
	 * YAML matchers applied to the stub (request) side.
	 *
	 * @author Marcin Grzejszczak
	 * @since 1.0.0
	 */
	public static class StubMatchers {

		public @Nullable KeyValueMatcher url;

		public List<BodyStubMatcher> body = new ArrayList<BodyStubMatcher>();

		public List<KeyValueMatcher> headers = new ArrayList<KeyValueMatcher>();

		public List<QueryParameterMatcher> queryParameters = new ArrayList<QueryParameterMatcher>();

		public List<KeyValueMatcher> cookies = new ArrayList<KeyValueMatcher>();

		public @Nullable MultipartStubMatcher multipart;

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			StubMatchers that = (StubMatchers) o;
			return Objects.equals(this.url, that.url) && Objects.equals(this.body, that.body)
					&& Objects.equals(this.headers, that.headers)
					&& Objects.equals(this.queryParameters, that.queryParameters)
					&& Objects.equals(this.cookies, that.cookies) && Objects.equals(this.multipart, that.multipart);
		}

		@Override
		public int hashCode() {
			return Objects.hash(this.url, this.body, this.headers, this.queryParameters, this.cookies, this.multipart);
		}

		@Override
		public String toString() {
			return "StubMatchers{" + "url=" + this.url + ", body=" + this.body + ", headers=" + this.headers
					+ ", queryParameters=" + this.queryParameters + ", cookies=" + this.cookies + ", multipart="
					+ this.multipart + '}';
		}

	}

	/**
	 * Type of matcher applied to a stub or test value.
	 *
	 * @author Marcin Grzejszczak
	 * @since 1.0.0
	 */
	public enum MatchingType {

		equal_to, containing, matching, not_matching, equal_to_json, equal_to_xml, absent, binary_equal_to;

		static @Nullable MatchingType from(String string) {
			return Arrays.stream(values())
				.filter((matchingType) -> matchingType.name()
					.replace("_", "")
					.equalsIgnoreCase(string.toLowerCase(Locale.ROOT).replace("_", "")))
				.findFirst()
				.orElse(null);
		}

	}

	/**
	 * YAML matcher applied to the request body.
	 *
	 * @author Marcin Grzejszczak
	 * @since 1.0.0
	 */
	public static class BodyStubMatcher {

		public @Nullable String path;

		public @Nullable StubMatcherType type;

		public @Nullable String value;

		public @Nullable PredefinedRegex predefined;

		public @Nullable Integer minOccurrence;

		public @Nullable Integer maxOccurrence;

		public @Nullable RegexType regexType;

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			BodyStubMatcher that = (BodyStubMatcher) o;
			return Objects.equals(this.path, that.path) && this.type == that.type
					&& Objects.equals(this.value, that.value) && this.predefined == that.predefined
					&& Objects.equals(this.minOccurrence, that.minOccurrence)
					&& Objects.equals(this.maxOccurrence, that.maxOccurrence) && this.regexType == that.regexType;
		}

		@Override
		public int hashCode() {
			return Objects.hash(this.path, this.type, this.value, this.predefined, this.minOccurrence,
					this.maxOccurrence, this.regexType);
		}

		@Override
		public String toString() {
			return "BodyStubMatcher{" + "path='" + this.path + '\'' + ", type=" + this.type + ", value='" + this.value
					+ '\'' + ", predefined=" + this.predefined + ", minOccurrence=" + this.minOccurrence
					+ ", maxOccurrence=" + this.maxOccurrence + ", regexType=" + this.regexType + '}';
		}

	}

	/**
	 * Predefined regular-expression type for a matcher.
	 *
	 * @author Marcin Grzejszczak
	 * @since 1.0.0
	 */
	public enum RegexType {

		as_integer, as_double, as_float, as_long, as_short, as_boolean, as_string

	}

	/**
	 * YAML matcher applied to a multipart request part.
	 *
	 * @author Marcin Grzejszczak
	 * @since 1.0.0
	 */
	public static class MultipartStubMatcher {

		public List<KeyValueMatcher> params = new ArrayList<KeyValueMatcher>();

		public List<MultipartNamedStubMatcher> named = new ArrayList<MultipartNamedStubMatcher>();

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			MultipartStubMatcher that = (MultipartStubMatcher) o;
			return Objects.equals(this.params, that.params) && Objects.equals(this.named, that.named);
		}

		@Override
		public int hashCode() {
			return Objects.hash(this.params, this.named);
		}

		@Override
		public String toString() {
			return "MultipartStubMatcher{" + "params=" + this.params + ", named=" + this.named + '}';
		}

	}

	/**
	 * YAML matcher applied to a named multipart request part.
	 *
	 * @author Marcin Grzejszczak
	 * @since 1.0.0
	 */
	public static class MultipartNamedStubMatcher {

		public @Nullable String paramName;

		public @Nullable ValueMatcher fileName;

		public @Nullable ValueMatcher fileContent;

		public @Nullable ValueMatcher contentType;

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			MultipartNamedStubMatcher that = (MultipartNamedStubMatcher) o;
			return Objects.equals(this.paramName, that.paramName) && Objects.equals(this.fileName, that.fileName)
					&& Objects.equals(this.fileContent, that.fileContent)
					&& Objects.equals(this.contentType, that.contentType);
		}

		@Override
		public int hashCode() {
			return Objects.hash(this.paramName, this.fileName, this.fileContent, this.contentType);
		}

		@Override
		public String toString() {
			return "MultipartNamedStubMatcher{" + "paramName='" + this.paramName + '\'' + ", fileName=" + this.fileName
					+ ", fileContent=" + this.fileContent + ", contentType=" + this.contentType + '}';
		}

	}

	/**
	 * YAML matcher describing an expected value by regular expression.
	 *
	 * @author Marcin Grzejszczak
	 * @since 1.0.0
	 */
	public static class ValueMatcher {

		public @Nullable String regex;

		public @Nullable PredefinedRegex predefined;

		public ValueMatcher() {
		}

		public ValueMatcher(String regex) {
			this.regex = regex;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			ValueMatcher that = (ValueMatcher) o;
			return Objects.equals(this.regex, that.regex) && this.predefined == that.predefined;
		}

		@Override
		public int hashCode() {
			return Objects.hash(this.regex, this.predefined);
		}

		@Override
		public String toString() {
			return "ValueMatcher{" + "regex='" + this.regex + '\'' + ", predefined=" + this.predefined + '}';
		}

	}

	/**
	 * YAML matcher applied to the response body.
	 *
	 * @author Marcin Grzejszczak
	 * @since 1.0.0
	 */
	public static class BodyTestMatcher {

		public @Nullable String path;

		public @Nullable TestMatcherType type;

		public @Nullable String value;

		public @Nullable Integer minOccurrence;

		public @Nullable Integer maxOccurrence;

		public @Nullable PredefinedRegex predefined;

		public @Nullable RegexType regexType;

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			BodyTestMatcher that = (BodyTestMatcher) o;
			return Objects.equals(this.path, that.path) && this.type == that.type
					&& Objects.equals(this.value, that.value) && Objects.equals(this.minOccurrence, that.minOccurrence)
					&& Objects.equals(this.maxOccurrence, that.maxOccurrence) && this.predefined == that.predefined
					&& this.regexType == that.regexType;
		}

		@Override
		public int hashCode() {
			return Objects.hash(this.path, this.type, this.value, this.minOccurrence, this.maxOccurrence,
					this.predefined, this.regexType);
		}

		@Override
		public String toString() {
			return "BodyTestMatcher{" + "path='" + this.path + '\'' + ", type=" + this.type + ", value='" + this.value
					+ '\'' + ", minOccurrence=" + this.minOccurrence + ", maxOccurrence=" + this.maxOccurrence
					+ ", predefined=" + this.predefined + ", regexType=" + this.regexType + '}';
		}

	}

	/**
	 * YAML matcher applied to a keyed value such as a header or cookie.
	 *
	 * @author Marcin Grzejszczak
	 * @since 1.0.0
	 */
	public static class KeyValueMatcher {

		public @Nullable String key;

		public @Nullable String regex;

		public @Nullable PredefinedRegex predefined;

		public @Nullable String command;

		public @Nullable RegexType regexType;

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			KeyValueMatcher that = (KeyValueMatcher) o;
			return Objects.equals(this.key, that.key) && Objects.equals(this.regex, that.regex)
					&& this.predefined == that.predefined && Objects.equals(this.command, that.command)
					&& this.regexType == that.regexType;
		}

		@Override
		public int hashCode() {
			return Objects.hash(this.key, this.regex, this.predefined, this.command, this.regexType);
		}

		@Override
		public String toString() {
			return "KeyValueMatcher{" + "key='" + this.key + '\'' + ", regex='" + this.regex + '\'' + ", predefined="
					+ this.predefined + ", command='" + this.command + '\'' + ", regexType=" + this.regexType + '}';
		}

	}

	/**
	 * YAML matcher applied to headers.
	 *
	 * @author Marcin Grzejszczak
	 * @since 1.0.0
	 */
	public static class HeadersMatcher extends KeyValueMatcher {

	}

	/**
	 * YAML matcher applied to a query parameter.
	 *
	 * @author Marcin Grzejszczak
	 * @since 1.0.0
	 */
	public static class QueryParameterMatcher {

		public @Nullable String key;

		public @Nullable MatchingType type;

		public @Nullable Object value;

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			QueryParameterMatcher that = (QueryParameterMatcher) o;
			return Objects.equals(this.key, that.key) && this.type == that.type
					&& Objects.equals(this.value, that.value);
		}

		@Override
		public int hashCode() {
			return Objects.hash(this.key, this.type, this.value);
		}

		@Override
		public String toString() {
			return "QueryParameterMatcher{" + "key='" + this.key + '\'' + ", type=" + this.type + ", value="
					+ this.value + '}';
		}

	}

	/**
	 * YAML matcher applied to a response header.
	 *
	 * @author Marcin Grzejszczak
	 * @since 1.0.0
	 */
	public static class TestHeaderMatcher {

		public @Nullable String key;

		public @Nullable String regex;

		public @Nullable String command;

		public @Nullable PredefinedRegex predefined;

		public @Nullable RegexType regexType;

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			TestHeaderMatcher that = (TestHeaderMatcher) o;
			return Objects.equals(this.key, that.key) && Objects.equals(this.regex, that.regex)
					&& Objects.equals(this.command, that.command) && this.predefined == that.predefined
					&& this.regexType == that.regexType;
		}

		@Override
		public int hashCode() {
			return Objects.hash(this.key, this.regex, this.command, this.predefined, this.regexType);
		}

		@Override
		public String toString() {
			return "TestHeaderMatcher{" + "key='" + this.key + '\'' + ", regex='" + this.regex + '\'' + ", command='"
					+ this.command + '\'' + ", predefined=" + this.predefined + ", regexType=" + this.regexType + '}';
		}

	}

	/**
	 * YAML matcher applied to a response cookie.
	 *
	 * @author Marcin Grzejszczak
	 * @since 1.0.0
	 */
	public static class TestCookieMatcher {

		public @Nullable String key;

		public @Nullable String regex;

		public @Nullable String command;

		public @Nullable PredefinedRegex predefined;

		public @Nullable RegexType regexType;

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			TestCookieMatcher that = (TestCookieMatcher) o;
			return Objects.equals(this.key, that.key) && Objects.equals(this.regex, that.regex)
					&& Objects.equals(this.command, that.command) && this.predefined == that.predefined
					&& this.regexType == that.regexType;
		}

		@Override
		public int hashCode() {
			return Objects.hash(this.key, this.regex, this.command, this.predefined, this.regexType);
		}

		@Override
		public String toString() {
			return "TestCookieMatcher{" + "key='" + this.key + '\'' + ", regex='" + this.regex + '\'' + ", command='"
					+ this.command + '\'' + ", predefined=" + this.predefined + ", regexType=" + this.regexType + '}';
		}

	}

	/**
	 * Predefined regular expressions available to matchers.
	 *
	 * @author Marcin Grzejszczak
	 * @since 1.0.0
	 */
	public enum PredefinedRegex {

		only_alpha_unicode, number, any_double, any_boolean, ip_address, hostname, email, url, uuid, iso_date,
		iso_date_time, iso_time, iso_8601_with_offset, non_empty, non_blank

	}

	/**
	 * Type of matcher applicable to the stub (request) side.
	 *
	 * @author Marcin Grzejszczak
	 * @since 1.0.0
	 */
	public enum StubMatcherType {

		by_date, by_time, by_timestamp, by_regex, by_equality, by_type, by_null

	}

	/**
	 * Type of matcher applicable to the test (response) side.
	 *
	 * @author Marcin Grzejszczak
	 * @since 1.0.0
	 */
	public enum TestMatcherType {

		by_date, by_time, by_timestamp, by_regex, by_equality, by_type, by_command, by_null

	}

	/**
	 * YAML representation of a contract's HTTP response.
	 *
	 * @author Marcin Grzejszczak
	 * @since 1.0.0
	 */
	public static class Response {

		public @Nullable Integer status;

		public @Nullable Map<String, Object> headers = new LinkedHashMap<String, Object>();

		public @Nullable Map<String, Object> cookies = new LinkedHashMap<String, Object>();

		public @Nullable Object body;

		public @Nullable String bodyFromFile;

		public @Nullable String bodyFromFileAsBytes;

		public TestMatchers matchers = new TestMatchers();

		public @Nullable Boolean async;

		public @Nullable Integer fixedDelayMilliseconds;

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			Response response = (Response) o;
			return Objects.equals(this.status, response.status) && Objects.equals(this.headers, response.headers)
					&& Objects.equals(this.cookies, response.cookies) && Objects.equals(this.body, response.body)
					&& Objects.equals(this.bodyFromFile, response.bodyFromFile)
					&& Objects.equals(this.bodyFromFileAsBytes, response.bodyFromFileAsBytes)
					&& Objects.equals(this.matchers, response.matchers) && Objects.equals(this.async, response.async)
					&& Objects.equals(this.fixedDelayMilliseconds, response.fixedDelayMilliseconds);
		}

		@Override
		public int hashCode() {
			return Objects.hash(this.status, this.headers, this.cookies, this.body, this.bodyFromFile,
					this.bodyFromFileAsBytes, this.matchers, this.async, this.fixedDelayMilliseconds);
		}

		@Override
		public String toString() {
			return "Response{" + "status=" + this.status + ", headers=" + this.headers + ", cookies=" + this.cookies
					+ ", body=" + this.body + ", bodyFromFile='" + this.bodyFromFile + '\'' + ", bodyFromFileAsBytes='"
					+ this.bodyFromFileAsBytes + '\'' + ", matchers=" + this.matchers + ", async=" + this.async
					+ ", fixedDelayMilliseconds=" + this.fixedDelayMilliseconds + '}';
		}

	}

	/**
	 * YAML matchers applied to the test (response) side.
	 *
	 * @author Marcin Grzejszczak
	 * @since 1.0.0
	 */
	public static class TestMatchers {

		public List<BodyTestMatcher> body = new ArrayList<BodyTestMatcher>();

		public List<TestHeaderMatcher> headers = new ArrayList<TestHeaderMatcher>();

		public List<TestCookieMatcher> cookies = new ArrayList<TestCookieMatcher>();

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			TestMatchers that = (TestMatchers) o;
			return Objects.equals(this.body, that.body) && Objects.equals(this.headers, that.headers)
					&& Objects.equals(this.cookies, that.cookies);
		}

		@Override
		public int hashCode() {
			return Objects.hash(this.body, this.headers, this.cookies);
		}

		@Override
		public String toString() {
			return "TestMatchers{" + "body=" + this.body + ", headers=" + this.headers + ", cookies=" + this.cookies
					+ '}';
		}

	}

	/**
	 * YAML representation of a messaging contract's input.
	 *
	 * @author Marcin Grzejszczak
	 * @since 1.0.0
	 */
	public static class Input {

		public @Nullable String triggeredBy;

		public @Nullable String assertThat;

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			Input input = (Input) o;
			return Objects.equals(this.triggeredBy, input.triggeredBy)
					&& Objects.equals(this.assertThat, input.assertThat);
		}

		@Override
		public int hashCode() {
			return Objects.hash(this.triggeredBy, this.assertThat);
		}

		@Override
		public String toString() {
			return "Input{" + "triggeredBy='" + this.triggeredBy + '\'' + ", assertThat='" + this.assertThat + '\''
					+ '}';
		}

	}

	/**
	 * YAML representation of a messaging contract's output message.
	 *
	 * @author Marcin Grzejszczak
	 * @since 1.0.0
	 */
	public static class OutputMessage {

		public @Nullable String sentTo;

		public @Nullable Map<String, Object> headers = new LinkedHashMap<String, Object>();

		public @Nullable Object body;

		public @Nullable String bodyFromFile;

		public @Nullable String bodyFromFileAsBytes;

		public @Nullable String assertThat;

		public TestMatchers matchers = new TestMatchers();

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			OutputMessage that = (OutputMessage) o;
			return Objects.equals(this.sentTo, that.sentTo) && Objects.equals(this.headers, that.headers)
					&& Objects.equals(this.body, that.body) && Objects.equals(this.bodyFromFile, that.bodyFromFile)
					&& Objects.equals(this.bodyFromFileAsBytes, that.bodyFromFileAsBytes)
					&& Objects.equals(this.assertThat, that.assertThat) && Objects.equals(this.matchers, that.matchers);
		}

		@Override
		public int hashCode() {
			return Objects.hash(this.sentTo, this.headers, this.body, this.bodyFromFile, this.bodyFromFileAsBytes,
					this.assertThat, this.matchers);
		}

		@Override
		public String toString() {
			return "OutputMessage{" + "sentTo='" + this.sentTo + '\'' + ", headers=" + this.headers + ", body="
					+ this.body + ", bodyFromFile='" + this.bodyFromFile + '\'' + ", bodyFromFileAsBytes='"
					+ this.bodyFromFileAsBytes + '\'' + ", assertThat='" + this.assertThat + '\'' + ", matchers="
					+ this.matchers + '}';
		}

	}

}
