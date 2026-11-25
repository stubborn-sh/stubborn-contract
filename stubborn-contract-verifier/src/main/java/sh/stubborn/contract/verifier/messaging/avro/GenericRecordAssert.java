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

package sh.stubborn.contract.verifier.messaging.avro;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.avro.AvroRuntimeException;
import org.apache.avro.Conversions;
import org.apache.avro.LogicalType;
import org.apache.avro.LogicalTypes;
import org.apache.avro.Schema;
import org.apache.avro.SchemaCompatibility;
import org.apache.avro.SchemaCompatibility.SchemaPairCompatibility;
import org.apache.avro.data.TimeConversions;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericEnumSymbol;
import org.apache.avro.generic.GenericFixed;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificData;
import org.apache.avro.specific.SpecificRecord;
import org.jspecify.annotations.Nullable;

/**
 * Assertions on a received Avro record, called by tests generated with
 * {@code assertAvroTypes} enabled.
 *
 * <p>
 * The record is first written with its own (writer) schema and read back as plain Avro
 * data, so a {@code SpecificRecord}, Java enums and logical-type conversions such as
 * {@code Instant} or {@code UUID} compare the same way as a {@code GenericRecord}.
 * {@link #readWith(String)} then reads it with the contract's (reader) schema under the
 * Avro schema resolution rules that Schema Registry applies, so a change that a consumer
 * holding the contract's schema can still read passes, and any other change fails.
 *
 * <p>
 * Field paths use dots for nested records, {@code [n]} for array elements and
 * {@code ['key']} for map entries, for example {@code lines[0].sku} or
 * {@code attributes['channel']}.
 *
 * <p>
 * Avro is an optional dependency of this module: only tests generated for contracts that
 * declare {@code metadata.kafka.avro.schema} reference this class.
 *
 * @author Jakub Havlik
 * @since 0.2.5
 */
public final class GenericRecordAssert {

	private static final Pattern PATH_SEGMENT = Pattern
		.compile("\\.?([A-Za-z_][A-Za-z0-9_]*)|\\[(\\d+)]|\\['((?:[^'\\\\]|\\\\.)*)']");

	private static final GenericData CONVERTING = converting();

	private final GenericRecord record;

	private GenericRecordAssert(GenericRecord record) {
		this.record = record;
	}

	/**
	 * Starts assertions on a received message payload.
	 * @param payload the payload, expected to be a {@link GenericRecord} (a
	 * {@code SpecificRecord} is one too)
	 * @return the assertion
	 * @throws AssertionError when the payload is not a {@link GenericRecord}
	 */
	public static GenericRecordAssert assertThatAvro(@Nullable Object payload) {
		if (payload instanceof GenericRecord genericRecord) {
			return new GenericRecordAssert(
					resolve(genericRecord, genericRecord.getSchema(), genericRecord.getSchema()));
		}
		throw new AssertionError("Expected the received payload to be an Avro GenericRecord but was ["
				+ ((payload != null) ? payload.getClass().getSimpleName() : "null")
				+ "]. Configure the message receiver to deserialize Avro, for example with KafkaAvroDeserializer.");
	}

	/**
	 * Reads the record the way a consumer holding the given schema reads it: fields are
	 * resolved by name, promoted ({@code int} to {@code long}, {@code bytes} to
	 * {@code string}, ...) and defaulted, and fields the reader does not know are
	 * dropped. Logical types are not part of the check, as in Schema Registry.
	 * @param readerSchema the reader schema, as Avro JSON
	 * @return the assertion on the record as the reader sees it
	 * @throws AssertionError when the record's schema cannot be read with the reader
	 * schema
	 */
	public GenericRecordAssert readWith(String readerSchema) {
		Schema reader = new Schema.Parser().parse(readerSchema);
		Schema writer = this.record.getSchema();
		SchemaPairCompatibility compatibility = SchemaCompatibility.checkReaderWriterCompatibility(reader, writer);
		if (compatibility.getType() != SchemaCompatibility.SchemaCompatibilityType.COMPATIBLE) {
			throw new AssertionError(
					"Record [" + writer.getFullName() + "] cannot be read with the contract's Avro schema: "
							+ compatibility.getResult()
								.getIncompatibilities()
								.stream()
								.map((incompatibility) -> incompatibility.getLocation() + ": "
										+ incompatibility.getMessage() + " (" + incompatibility.getType() + ")")
								.collect(Collectors.joining("; ")));
		}
		return new GenericRecordAssert(resolve(this.record, writer, reader));
	}

	/**
	 * Selects a field of the record.
	 * @param path the field path, for example {@code customer.name}
	 * @return the field assertion
	 * @throws AssertionError when the path does not exist in the record
	 */
	public FieldAssert field(String path) {
		@Nullable Object value = this.record;
		Schema schema = this.record.getSchema();
		Matcher matcher = PATH_SEGMENT.matcher(path);
		int position = 0;
		while (position < path.length()) {
			if (!matcher.region(position, path.length()).lookingAt()) {
				throw new AssertionError("Field [" + path + "] is not a valid path");
			}
			if (value == null) {
				throw new AssertionError(
						"Field [" + path + "] not found: [" + path.substring(0, position) + "] is null");
			}
			schema = resolveUnion(schema, value);
			if (matcher.group(1) != null && value instanceof GenericRecord parent) {
				Schema.Field field = schema.getField(matcher.group(1));
				if (field == null) {
					throw new AssertionError("Field [" + path + "] not found: record [" + schema.getFullName()
							+ "] has no field [" + matcher.group(1) + "]");
				}
				value = parent.get(field.pos());
				schema = field.schema();
			}
			else if (matcher.group(2) != null && value instanceof List<?> list) {
				int index = Integer.parseInt(matcher.group(2));
				if (index >= list.size()) {
					throw new AssertionError("Field [" + path + "] not found: array has " + list.size() + " elements");
				}
				value = list.get(index);
				schema = schema.getElementType();
			}
			else if (matcher.group(3) != null && value instanceof Map<?, ?> map) {
				value = mapEntry(map, matcher.group(3).replace("\\'", "'"), path);
				schema = schema.getValueType();
			}
			else {
				throw new AssertionError("Field [" + path + "] not found: [" + path.substring(0, position)
						+ "] is of Avro type [" + schema.getType().getName() + "]");
			}
			position = matcher.end();
		}
		return new FieldAssert(path, schema, value);
	}

	private static GenericRecord resolve(GenericRecord record, Schema writer, Schema reader) {
		try {
			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			BinaryEncoder encoder = EncoderFactory.get().directBinaryEncoder(bytes, null);
			new GenericDatumWriter<>(writer, writingModel(record)).write(record, encoder);
			encoder.flush();
			return new GenericDatumReader<GenericRecord>(writer, reader, GenericData.get()).read(null,
					DecoderFactory.get().binaryDecoder(bytes.toByteArray(), null));
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
		catch (AvroRuntimeException | ClassCastException ex) {
			throw new AssertionError(
					"Record [" + writer.getFullName() + "] does not match its own Avro schema: " + ex.getMessage(), ex);
		}
	}

	private static GenericData writingModel(GenericRecord record) {
		return (record instanceof SpecificRecord) ? SpecificData.getForClass(record.getClass()) : CONVERTING;
	}

	private static GenericData converting() {
		GenericData data = new GenericData();
		List.of(new Conversions.DecimalConversion(), new Conversions.BigDecimalConversion(),
				new Conversions.UUIDConversion(), new Conversions.DurationConversion(),
				new TimeConversions.DateConversion(), new TimeConversions.TimeMillisConversion(),
				new TimeConversions.TimeMicrosConversion(), new TimeConversions.TimestampMillisConversion(),
				new TimeConversions.TimestampMicrosConversion(), new TimeConversions.TimestampNanosConversion(),
				new TimeConversions.LocalTimestampMillisConversion(),
				new TimeConversions.LocalTimestampMicrosConversion(),
				new TimeConversions.LocalTimestampNanosConversion())
			.forEach(data::addLogicalTypeConversion);
		return data;
	}

	private static Schema resolveUnion(Schema schema, @Nullable Object value) {
		if (schema.getType() != Schema.Type.UNION) {
			return schema;
		}
		return schema.getTypes().get(GenericData.get().resolveUnion(schema, value));
	}

	private static @Nullable Object mapEntry(Map<?, ?> map, String key, String path) {
		for (Map.Entry<?, ?> entry : map.entrySet()) {
			if (key.equals(String.valueOf(entry.getKey()))) {
				return entry.getValue();
			}
		}
		throw new AssertionError("Field [" + path + "] not found: map has no key [" + key + "]");
	}

	/**
	 * Assertions on one field of the record.
	 *
	 * @since 0.2.5
	 */
	public static final class FieldAssert {

		private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss")
			.withZone(ZoneOffset.UTC);

		private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

		private final String path;

		private final Schema schema;

		private final @Nullable Object value;

		private FieldAssert(String path, Schema schema, @Nullable Object value) {
			this.path = path;
			this.schema = resolveUnion(schema, value);
			this.value = value;
		}

		/**
		 * Checks the field value. Avro strings and enum symbols compare as
		 * {@link String}, decimals as {@link BigDecimal} by numeric value, everything
		 * else by {@link Object#equals(Object)}.
		 * @param expected the expected value
		 * @return this assertion
		 */
		public FieldAssert isEqualTo(Object expected) {
			Object actual = value();
			boolean equal = (expected instanceof BigDecimal expectedDecimal
					&& actual instanceof BigDecimal actualDecimal) ? expectedDecimal.compareTo(actualDecimal) == 0
							: expected.equals(actual);
			if (!equal) {
				throw new AssertionError(
						"Field [" + this.path + "] expected <" + expected + "> but was <" + actual + ">");
			}
			return this;
		}

		/**
		 * Checks that the field value, as text, matches a regular expression. A
		 * timestamp, date or time field is written as ISO-8601 text at second precision
		 * in UTC ({@code 2023-11-14T22:13:20}, {@code 2023-11-14}, {@code 22:13:20}), the
		 * forms {@code byTimestamp()}, {@code byDate()} and {@code byTime()} expect.
		 * @param regex the regular expression
		 * @return this assertion
		 */
		public FieldAssert matches(String regex) {
			String actual = text();
			if (actual == null || !Pattern.compile(regex).matcher(actual).matches()) {
				throw new AssertionError(
						"Field [" + this.path + "] expected to match [" + regex + "] but was <" + actual + ">");
			}
			return this;
		}

		/**
		 * Checks that the field is {@code null}.
		 * @return this assertion
		 */
		public FieldAssert isNull() {
			if (this.value != null) {
				throw new AssertionError("Field [" + this.path + "] expected null but was <" + value() + ">");
			}
			return this;
		}

		/**
		 * Checks that the field is not {@code null}.
		 * @return this assertion
		 */
		public FieldAssert isNotNull() {
			if (this.value == null) {
				throw new AssertionError("Field [" + this.path + "] expected a value but was null");
			}
			return this;
		}

		/**
		 * Checks the number of elements of an array field.
		 * @param min the minimum size, inclusive
		 * @param max the maximum size, inclusive
		 * @return this assertion
		 */
		public FieldAssert hasSizeBetween(int min, int max) {
			int size = size();
			if (size < min || size > max) {
				throw new AssertionError("Field [" + this.path + "] expected between " + min + " and " + max
						+ " elements but had " + size);
			}
			return this;
		}

		/**
		 * Checks the minimum number of elements of an array field.
		 * @param min the minimum size, inclusive
		 * @return this assertion
		 */
		public FieldAssert hasSizeGreaterThanOrEqualTo(int min) {
			return hasSizeBetween(min, Integer.MAX_VALUE);
		}

		/**
		 * Checks the maximum number of elements of an array field.
		 * @param max the maximum size, inclusive
		 * @return this assertion
		 */
		public FieldAssert hasSizeLessThanOrEqualTo(int max) {
			return hasSizeBetween(0, max);
		}

		/**
		 * Returns the field value normalized as {@link #isEqualTo(Object)} compares it.
		 * @return the normalized value
		 */
		public @Nullable Object value() {
			if (this.value instanceof CharSequence || this.value instanceof GenericEnumSymbol<?>) {
				return this.value.toString();
			}
			if (this.schema.getLogicalType() instanceof LogicalTypes.Decimal decimal) {
				Conversions.DecimalConversion conversion = new Conversions.DecimalConversion();
				if (this.value instanceof ByteBuffer bytes) {
					return conversion.fromBytes(bytes.duplicate(), this.schema, decimal);
				}
				if (this.value instanceof GenericFixed fixed) {
					return conversion.fromFixed(fixed, this.schema, decimal);
				}
			}
			return this.value;
		}

		private @Nullable String text() {
			Object actual = value();
			LogicalType logicalType = this.schema.getLogicalType();
			if (!(actual instanceof Number number) || logicalType == null) {
				return (actual != null) ? actual.toString() : null;
			}
			long amount = number.longValue();
			return switch (logicalType.getName()) {
				case "timestamp-millis", "local-timestamp-millis" -> DATE_TIME.format(Instant.ofEpochMilli(amount));
				case "timestamp-micros", "local-timestamp-micros" ->
					DATE_TIME.format(Instant.EPOCH.plus(amount, ChronoUnit.MICROS));
				case "date" -> LocalDate.ofEpochDay(amount).toString();
				case "time-millis" -> TIME.format(LocalTime.ofNanoOfDay(amount * 1_000_000));
				case "time-micros" -> TIME.format(LocalTime.ofNanoOfDay(amount * 1_000));
				default -> actual.toString();
			};
		}

		private int size() {
			if (this.value instanceof List<?> list) {
				return list.size();
			}
			throw new AssertionError("Field [" + this.path + "] expected an array but was of Avro type ["
					+ this.schema.getType().getName() + "]");
		}

	}

}
