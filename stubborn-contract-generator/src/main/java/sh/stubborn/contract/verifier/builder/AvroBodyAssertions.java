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

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.avro.LogicalType;
import org.apache.avro.LogicalTypes;
import org.apache.avro.Schema;
import org.apache.commons.text.StringEscapeUtils;
import org.jspecify.annotations.Nullable;
import sh.stubborn.contract.spec.internal.BodyMatcher;
import sh.stubborn.contract.spec.internal.BodyMatchers;
import sh.stubborn.contract.spec.internal.ExecutionProperty;
import sh.stubborn.contract.spec.internal.MatchingType;
import sh.stubborn.contract.spec.internal.RegexProperty;

/**
 * Walks a contract body together with its Avro schema and renders the assertions on the
 * received record: one line that reads the record with the contract's schema, which
 * checks every field's type the way Schema Registry does, then one value line per named
 * field.
 *
 * @author Jakub Havlik
 * @since 0.2.5
 */
final class AvroBodyAssertions {

	private static final String CLASSPATH_PREFIX = "classpath:";

	static final String RECORD = "avroRecord";

	private static final Set<String> TEMPORAL = Set.of("timestamp-millis", "timestamp-micros", "local-timestamp-millis",
			"local-timestamp-micros", "date");

	private final BodyParser bodyParser;

	private final List<BodyMatcher> matchers;

	private final Set<BodyMatcher> usedMatchers = new LinkedHashSet<>();

	private final List<String> lines = new ArrayList<>();

	private AvroBodyAssertions(BodyParser bodyParser, @Nullable BodyMatchers bodyMatchers) {
		this.bodyParser = bodyParser;
		this.matchers = (bodyMatchers != null) ? bodyMatchers.matchers() : List.of();
	}

	static List<String> lines(BodyParser bodyParser, String schemaLocation, Path contractPath, Object body,
			@Nullable BodyMatchers bodyMatchers) {
		try {
			Schema schema = parseSchema(schemaLocation, contractPath);
			AvroBodyAssertions assertions = new AvroBodyAssertions(bodyParser, bodyMatchers);
			assertions.lines.add("GenericRecordAssert " + RECORD + " = assertThatAvro(response.getPayload()).readWith("
					+ bodyParser.quotedLongText(schema.toString()) + ")");
			assertions.walk(schema, body, FieldPath.ROOT, true);
			assertions.failOnUnusedMatchers();
			return assertions.lines;
		}
		catch (RuntimeException ex) {
			throw new IllegalStateException("Contract [" + contractPath + "]: " + ex.getMessage(), ex);
		}
	}

	private void failOnUnusedMatchers() {
		List<String> unused = this.matchers.stream()
			.filter((matcher) -> !this.usedMatchers.contains(matcher))
			.map(BodyMatcher::path)
			.toList();
		if (!unused.isEmpty()) {
			throw new IllegalStateException("Body matchers " + unused
					+ " match no field of the contract body. With Avro assertions a matcher path uses dots and [*], for example $.lines[*].sku, and must name a field the body holds.");
		}
	}

	private void walk(Schema schema, @Nullable Object value, FieldPath path, boolean compareValues) {
		BodyMatcher matcher = matcherFor(path);
		Schema resolved = branch(schema, value, path);
		boolean matched = false;
		if (matcher != null && matcher.matchingType() != MatchingType.EQUALITY) {
			this.lines.add(matched(resolved, matcher, path));
			matched = true;
		}
		boolean compare = compareValues && !matched;
		if (value == null) {
			if (compare) {
				this.lines.add(field(path) + ".isNull()");
			}
		}
		else if (value instanceof Map<?, ?> empty && empty.isEmpty() && !FieldPath.ROOT.equals(path)) {
			if (compare) {
				this.lines.add(field(path) + ".isNotNull()");
			}
		}
		else if (resolved.getType() == Schema.Type.RECORD && value instanceof Map<?, ?> fields) {
			fields.forEach((key, child) -> walk(recordField(resolved, key.toString(), path).schema(), child,
					path.child(key.toString()), compare));
		}
		else if (resolved.getType() == Schema.Type.MAP && value instanceof Map<?, ?> entries) {
			entries.forEach((key, child) -> walk(resolved.getValueType(), child, path.key(key.toString()), compare));
		}
		else if (resolved.getType() == Schema.Type.ARRAY && value instanceof List<?> list) {
			for (int index = 0; index < list.size(); index++) {
				walk(resolved.getElementType(), list.get(index), path.index(index), compare);
			}
		}
		else if (compare) {
			this.lines.add(leaf(resolved, value, path));
		}
	}

	private String matched(Schema schema, BodyMatcher matcher, FieldPath path) {
		MatchingType type = Objects.requireNonNull(matcher.matchingType());
		return switch (type) {
			case TYPE -> field(path) + ".isNotNull()" + size(schema, matcher);
			case NULL -> field(path) + ".isNull()";
			case COMMAND -> execute((ExecutionProperty) Objects.requireNonNull(matcher.value()), path);
			default -> field(path) + matches(Objects.requireNonNull(matcher.value()));
		};
	}

	private static String size(Schema schema, BodyMatcher matcher) {
		Integer min = matcher.minTypeOccurrence();
		Integer max = matcher.maxTypeOccurrence();
		if (schema.getType() != Schema.Type.ARRAY || (min == null && max == null)) {
			return "";
		}
		if (min != null && max != null) {
			return ".hasSizeBetween(" + min + ", " + max + ")";
		}
		return (min != null) ? ".hasSizeGreaterThanOrEqualTo(" + min + ")" : ".hasSizeLessThanOrEqualTo(" + max + ")";
	}

	private String leaf(Schema schema, Object value, FieldPath path) {
		if (value instanceof ExecutionProperty executionProperty) {
			return execute(executionProperty, path);
		}
		if (value instanceof Pattern || value instanceof RegexProperty) {
			return field(path) + matches(value);
		}
		String literal = literal(schema, value);
		return field(path) + ((literal != null) ? ".isEqualTo(" + literal + ")" : ".isNotNull()");
	}

	private @Nullable String literal(Schema schema, Object value) {
		if (schema.getLogicalType() instanceof LogicalTypes.Decimal) {
			return "new java.math.BigDecimal(" + quoted(new BigDecimal(value.toString()).toPlainString()) + ")";
		}
		if (value instanceof CharSequence text && isTemporal(schema)) {
			Long epoch = Objects.requireNonNull(epoch(schema, text.toString()));
			return (schema.getType() == Schema.Type.INT) ? String.valueOf(epoch) : epoch + "L";
		}
		return switch (schema.getType()) {
			case STRING, ENUM -> quoted(value.toString());
			case INT -> String.valueOf(((Number) value).intValue());
			case LONG -> ((Number) value).longValue() + "L";
			case FLOAT -> ((Number) value).floatValue() + "f";
			case DOUBLE -> ((Number) value).doubleValue() + "d";
			case BOOLEAN -> value.toString();
			default -> null;
		};
	}

	private String field(FieldPath path) {
		return RECORD + ".field(" + quoted(path.avro()) + ")";
	}

	private String execute(ExecutionProperty executionProperty, FieldPath path) {
		return executionProperty.insertValue(field(path) + ".value()");
	}

	private String matches(Object regex) {
		String pattern = (regex instanceof Pattern compiled) ? compiled.pattern()
				: (regex instanceof RegexProperty property) ? property.pattern() : regex.toString();
		return ".matches(" + this.bodyParser.quotedEscapedShortText(StringEscapeUtils.escapeJava(pattern)) + ")";
	}

	private String quoted(String text) {
		return this.bodyParser.quotedShortText(text);
	}

	private @Nullable BodyMatcher matcherFor(FieldPath path) {
		BodyMatcher found = this.matchers.stream()
			.filter((matcher) -> Pattern.matches(Pattern.quote(matcher.path()).replace("[*]", "\\E\\[\\d+]\\Q"),
					path.json()))
			.findFirst()
			.orElse(null);
		if (found != null) {
			this.usedMatchers.add(found);
		}
		return found;
	}

	private static Schema branch(Schema schema, @Nullable Object value, FieldPath path) {
		List<Schema> candidates = (schema.getType() == Schema.Type.UNION) ? schema.getTypes() : List.of(schema);
		return candidates.stream()
			.filter((candidate) -> fits(candidate, value))
			.findFirst()
			.orElseThrow(() -> new IllegalStateException("Contract body field [" + path.avro()
					+ "] does not fit Avro type [" + describe(schema) + "]: " + value));
	}

	private static boolean fits(Schema schema, @Nullable Object value) {
		if (value == null) {
			return schema.getType() == Schema.Type.NULL;
		}
		if (value instanceof ExecutionProperty || value instanceof Pattern || value instanceof RegexProperty) {
			return schema.getType() != Schema.Type.NULL && !isContainer(schema);
		}
		if (schema.getLogicalType() instanceof LogicalTypes.Decimal) {
			return value instanceof Number;
		}
		if (value instanceof CharSequence text && isTemporal(schema)) {
			return epoch(schema, text.toString()) != null;
		}
		return switch (schema.getType()) {
			case RECORD, MAP -> value instanceof Map;
			case ARRAY -> value instanceof List;
			case STRING -> value instanceof CharSequence;
			case ENUM -> value instanceof CharSequence && schema.getEnumSymbols().contains(value.toString());
			case BOOLEAN -> value instanceof Boolean;
			case INT -> isIntegral(value) && ((Number) value).longValue() == ((Number) value).intValue();
			case LONG -> isIntegral(value);
			case FLOAT, DOUBLE -> value instanceof Number;
			case BYTES, FIXED -> value instanceof CharSequence;
			default -> false;
		};
	}

	private static String describe(Schema schema) {
		if (schema.getType() == Schema.Type.UNION) {
			return schema.getTypes().stream().map(AvroBodyAssertions::describe).collect(Collectors.joining(", "));
		}
		return (schema.getLogicalType() != null) ? schema.getLogicalType().getName() : schema.getType().getName();
	}

	private static boolean isTemporal(Schema schema) {
		LogicalType logicalType = schema.getLogicalType();
		return logicalType != null && TEMPORAL.contains(logicalType.getName());
	}

	private static @Nullable Long epoch(Schema schema, String text) {
		try {
			return switch (Objects.requireNonNull(schema.getLogicalType()).getName()) {
				case "timestamp-millis" -> Instant.parse(text).toEpochMilli();
				case "timestamp-micros" -> ChronoUnit.MICROS.between(Instant.EPOCH, Instant.parse(text));
				case "local-timestamp-millis" -> LocalDateTime.parse(text).toInstant(ZoneOffset.UTC).toEpochMilli();
				case "local-timestamp-micros" ->
					ChronoUnit.MICROS.between(Instant.EPOCH, LocalDateTime.parse(text).toInstant(ZoneOffset.UTC));
				default -> LocalDate.parse(text).toEpochDay();
			};
		}
		catch (DateTimeParseException ex) {
			return null;
		}
	}

	private static boolean isContainer(Schema schema) {
		return schema.getType() == Schema.Type.RECORD || schema.getType() == Schema.Type.MAP
				|| schema.getType() == Schema.Type.ARRAY;
	}

	private static boolean isIntegral(Object value) {
		return value instanceof Integer || value instanceof Long || value instanceof Short || value instanceof Byte
				|| value instanceof BigInteger;
	}

	private static Schema.Field recordField(Schema record, String name, FieldPath parent) {
		Schema.Field field = record.getField(name);
		if (field == null) {
			throw new IllegalStateException("Contract body field [" + parent.child(name).avro()
					+ "] is not in Avro record [" + record.getFullName() + "]");
		}
		return field;
	}

	private static Schema parseSchema(String location, Path contractPath) {
		try {
			if (location.trim().startsWith("{")) {
				return new Schema.Parser().parse(location);
			}
			if (location.startsWith(CLASSPATH_PREFIX)) {
				return parseClasspathSchema(location.substring(CLASSPATH_PREFIX.length()));
			}
			Path schemaPath = Path.of(location);
			Path contractDirectory = contractPath.toAbsolutePath().getParent();
			if (!Files.exists(schemaPath) && contractDirectory != null) {
				schemaPath = contractDirectory.resolve(location);
			}
			try (InputStream stream = Files.newInputStream(schemaPath)) {
				return new Schema.Parser().parse(stream);
			}
		}
		catch (IOException ex) {
			throw new UncheckedIOException("Failed to read Avro schema [" + location + "]", ex);
		}
	}

	private static Schema parseClasspathSchema(String resource) throws IOException {
		ClassLoader classLoader = Objects.requireNonNullElse(Thread.currentThread().getContextClassLoader(),
				AvroBodyAssertions.class.getClassLoader());
		try (InputStream stream = classLoader.getResourceAsStream(resource)) {
			if (stream == null) {
				throw new IllegalStateException("Avro schema [" + CLASSPATH_PREFIX + resource
						+ "] is not on the test generator classpath. Use a file path, or add the jar that holds the schema as a dependency of the contract plugin.");
			}
			return new Schema.Parser().parse(stream);
		}
	}

	private record FieldPath(String avro, String json) {

		static final FieldPath ROOT = new FieldPath("", "$");

		FieldPath child(String name) {
			return new FieldPath(this.avro.isEmpty() ? name : this.avro + "." + name, this.json + "." + name);
		}

		FieldPath index(int index) {
			return new FieldPath(this.avro + "[" + index + "]", this.json + "[" + index + "]");
		}

		FieldPath key(String key) {
			return new FieldPath(this.avro + "['" + key.replace("'", "\\'") + "']", this.json + "." + key);
		}

	}

}
