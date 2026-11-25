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

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.avro.Conversions;
import org.apache.avro.Schema;
import org.apache.avro.data.TimeConversions;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.generic.GenericRecordBuilder;
import org.apache.avro.specific.SpecificData;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.avro.util.Utf8;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GenericRecordAssertTests {

	private static final String ORDER_CREATED_JSON = """
			{"type": "record", "name": "OrderCreated", "namespace": "com.example.orders", "fields": [
			  {"name": "orderId", "type": "string"},
			  {"name": "quantity", "type": "int"},
			  {"name": "createdAt", "type": {"type": "long", "logicalType": "timestamp-millis"}},
			  {"name": "total", "type": {"type": "bytes", "logicalType": "decimal", "precision": 10, "scale": 2}},
			  {"name": "status", "type": {"type": "enum", "name": "OrderStatus", "symbols": ["NEW", "PAID"]}},
			  {"name": "customer", "type": {"type": "record", "name": "Customer", "fields": [
			    {"name": "name", "type": "string"}]}},
			  {"name": "lines", "type": {"type": "array", "items": {"type": "record", "name": "OrderLine", "fields": [
			    {"name": "sku", "type": "string"}, {"name": "price", "type": "double"}]}}},
			  {"name": "attributes", "type": {"type": "map", "values": "string"}},
			  {"name": "note", "type": ["null", "string"], "default": null},
			  {"name": "coupon", "type": ["null", "string"], "default": null}
			]}
			""";

	private static final Schema ORDER_CREATED = new Schema.Parser().parse(ORDER_CREATED_JSON);

	@Test
	void should_pass_value_assertions_for_every_named_field_read_with_the_contract_schema() {
		GenericRecordAssert order = GenericRecordAssert.assertThatAvro(orderCreated()).readWith(ORDER_CREATED_JSON);

		assertThatCode(() -> {
			order.field("orderId").isEqualTo("o-1");
			order.field("quantity").isEqualTo(2);
			order.field("createdAt").isEqualTo(1700000000000L);
			order.field("total").isEqualTo(new BigDecimal("19.99"));
			order.field("status").isEqualTo("PAID");
			order.field("customer.name").isEqualTo("Ada");
			order.field("lines[0].sku").matches("A-[0-9]+");
			order.field("lines[0].price").isEqualTo(9.5d);
			order.field("lines").isNotNull().hasSizeBetween(1, 1);
			order.field("attributes['channel']").isEqualTo("web");
			order.field("note").isNull();
			order.field("coupon").isEqualTo("WELCOME");
		}).doesNotThrowAnyException();
	}

	@ParameterizedTest(name = "writer {0} read as {1}")
	@CsvSource(delimiter = '|',
			textBlock = """
					{"type": "string", "logicalType": "uuid"} | "string"                                       | 123e4567-e89b-42d3-a456-426614174000 | 123e4567-e89b-42d3-a456-426614174000
					"int"                                     | "long"                                         | 2                                    | 2
					"bytes"                                   | "string"                                       | abc                                  | abc
					"long"                                    | {"type": "long", "logicalType": "timestamp-millis"} | 1700000000000                   | 1700000000000
					""")
	void should_read_a_field_change_that_schema_registry_accepts(String writerType, String readerType, String written,
			String expected) {
		GenericRecord record = new GenericRecordBuilder(single(writerType))
			.set("value", written(single(writerType).getField("value").schema(), written))
			.build();

		GenericRecordAssert.FieldAssert value = GenericRecordAssert.assertThatAvro(record)
			.readWith(single(readerType).toString())
			.field("value");

		assertThat(String.valueOf(value.value())).isEqualTo(expected);
	}

	@ParameterizedTest(name = "writer {0} read as {1}")
	@CsvSource(delimiter = '|', textBlock = """
			"long"   | "string"
			"long"   | "int"
			"string" | {"type": "enum", "name": "E", "symbols": ["A"]}
			""")
	void should_fail_on_a_field_change_that_schema_registry_rejects(String writerType, String readerType) {
		GenericRecord record = new GenericRecordBuilder(single(writerType)).set("value", sample(writerType)).build();

		assertThatThrownBy(() -> GenericRecordAssert.assertThatAvro(record).readWith(single(readerType).toString()))
			.isInstanceOf(AssertionError.class)
			.hasMessageStartingWith("Record [com.example.Single] cannot be read with the contract's Avro schema: ")
			.hasMessageContaining("/fields/0/type")
			.hasMessageEndingWith("(TYPE_MISMATCH)");
	}

	@Test
	void should_fail_when_the_writer_dropped_a_field_the_reader_needs() {
		Schema reader = new Schema.Parser().parse("""
				{"type": "record", "name": "OrderCreated", "namespace": "com.example.orders", "fields": [
				  {"name": "orderId", "type": "string"}, {"name": "channel", "type": "string"}]}
				""");

		assertThatThrownBy(() -> GenericRecordAssert.assertThatAvro(orderCreated()).readWith(reader.toString()))
			.isInstanceOf(AssertionError.class)
			.hasMessageEndingWith(": channel (READER_FIELD_MISSING_DEFAULT_VALUE)");
	}

	@Test
	void should_drop_writer_fields_the_reader_does_not_know() {
		Schema reader = new Schema.Parser().parse("""
				{"type": "record", "name": "OrderCreated", "namespace": "com.example.orders", "fields": [
				  {"name": "orderId", "type": "string"}]}
				""");

		GenericRecordAssert order = GenericRecordAssert.assertThatAvro(orderCreated()).readWith(reader.toString());

		order.field("orderId").isEqualTo("o-1");
		assertThatThrownBy(() -> order.field("coupon")).isInstanceOf(AssertionError.class)
			.hasMessage("Field [coupon] not found: record [com.example.orders.OrderCreated] has no field [coupon]");
	}

	@Test
	void should_compare_a_specific_record_with_conversions_and_a_java_enum_as_plain_avro() {
		Paid paid = new Paid();
		paid.paidAt = Instant.ofEpochMilli(1700000000000L);
		paid.status = Paid.Status.PAID;

		GenericRecordAssert record = GenericRecordAssert.assertThatAvro(paid).readWith(Paid.SCHEMA$.toString());

		record.field("paidAt").isEqualTo(1700000000000L);
		record.field("status").isEqualTo("PAID");
	}

	@Test
	void should_compare_converted_values_inside_a_union_as_plain_avro() {
		Schema schema = new Schema.Parser().parse("""
				{"type": "record", "name": "Shipped", "fields": [
				  {"name": "shippedAt", "type": ["null", {"type": "long", "logicalType": "timestamp-millis"}]},
				  {"name": "parcel", "type": ["null", {"type": "string", "logicalType": "uuid"}]}]}
				""");
		UUID parcel = UUID.fromString("123e4567-e89b-42d3-a456-426614174000");
		GenericRecord shipped = new GenericRecordBuilder(schema).set("shippedAt", Instant.ofEpochMilli(1700000000000L))
			.set("parcel", parcel)
			.build();

		GenericRecordAssert record = GenericRecordAssert.assertThatAvro(shipped);

		record.field("shippedAt").isEqualTo(1700000000000L);
		record.field("parcel").isEqualTo(parcel.toString());
	}

	@ParameterizedTest(name = "{0}")
	@CsvSource(delimiter = ';',
			textBlock = """
					{"type": "long", "logicalType": "timestamp-millis"} ; 1700000000123   ; ([0-9]{4})-(1[0-2]|0[1-9])-(3[01]|0[1-9]|[12][0-9])T(2[0-3]|[01][0-9]):([0-5][0-9]):([0-5][0-9]) ; 2023-11-14T22:13:20
					{"type": "long", "logicalType": "timestamp-micros"} ; 1700000000123456 ; 2023-11-14T22:13:20 ; 2023-11-14T22:13:20
					{"type": "int", "logicalType": "date"}              ; 19675           ; (\\d\\d\\d\\d)-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01]) ; 2023-11-14
					{"type": "int", "logicalType": "time-millis"}       ; 80000123        ; (2[0-3]|[01][0-9]):([0-5][0-9]):([0-5][0-9]) ; 22:13:20
					""")
	void should_match_a_temporal_field_against_its_iso_text_as_by_timestamp_by_date_and_by_time_expect(String type,
			long written, String regex, String text) {
		Schema schema = single(type);
		Object value = (schema.getField("value").schema().getType() == Schema.Type.INT) ? (Object) (int) written
				: (Object) written;
		GenericRecord record = new GenericRecordBuilder(schema).set("value", value).build();

		assertThatCode(() -> GenericRecordAssert.assertThatAvro(record).field("value").matches(regex))
			.doesNotThrowAnyException();
		assertThatThrownBy(() -> GenericRecordAssert.assertThatAvro(record).field("value").matches("x"))
			.hasMessageEndingWith("but was <" + text + ">");
	}

	@Test
	void should_fail_when_the_value_differs() {
		assertThatThrownBy(() -> GenericRecordAssert.assertThatAvro(orderCreated()).field("quantity").isEqualTo(3))
			.isInstanceOf(AssertionError.class)
			.hasMessage("Field [quantity] expected <3> but was <2>");
	}

	@Test
	void should_fail_when_the_value_does_not_match_the_pattern() {
		assertThatThrownBy(
				() -> GenericRecordAssert.assertThatAvro(orderCreated()).field("orderId").matches("x-[0-9]+"))
			.isInstanceOf(AssertionError.class)
			.hasMessage("Field [orderId] expected to match [x-[0-9]+] but was <o-1>");
	}

	@Test
	void should_fail_when_a_value_is_expected_but_the_field_is_null() {
		assertThatThrownBy(() -> GenericRecordAssert.assertThatAvro(orderCreated()).field("note").isNotNull())
			.isInstanceOf(AssertionError.class)
			.hasMessage("Field [note] expected a value but was null");
	}

	@Test
	void should_fail_when_an_array_size_is_out_of_range() {
		assertThatThrownBy(
				() -> GenericRecordAssert.assertThatAvro(orderCreated()).field("lines").hasSizeGreaterThanOrEqualTo(2))
			.isInstanceOf(AssertionError.class)
			.hasMessage("Field [lines] expected between 2 and 2147483647 elements but had 1");
		assertThatThrownBy(
				() -> GenericRecordAssert.assertThatAvro(orderCreated()).field("orderId").hasSizeLessThanOrEqualTo(2))
			.isInstanceOf(AssertionError.class)
			.hasMessage("Field [orderId] expected an array but was of Avro type [string]");
	}

	@Test
	void should_fail_when_the_field_is_absent() {
		assertThatThrownBy(() -> GenericRecordAssert.assertThatAvro(orderCreated()).field("customer.email"))
			.isInstanceOf(AssertionError.class)
			.hasMessage("Field [customer.email] not found: record [com.example.orders.Customer] has no field [email]");
	}

	@Test
	void should_fail_when_the_path_goes_through_a_null_field() {
		assertThatThrownBy(() -> GenericRecordAssert.assertThatAvro(orderCreated()).field("note.text"))
			.isInstanceOf(AssertionError.class)
			.hasMessage("Field [note.text] not found: [note] is null");
	}

	@Test
	void should_fail_when_the_path_does_not_fit_the_record_shape() {
		assertThatThrownBy(() -> GenericRecordAssert.assertThatAvro(orderCreated()).field("customer[0]"))
			.isInstanceOf(AssertionError.class)
			.hasMessage("Field [customer[0]] not found: [customer] is of Avro type [record]");
	}

	@Test
	void should_fail_when_the_payload_is_not_a_generic_record() {
		assertThatThrownBy(() -> GenericRecordAssert.assertThatAvro(new byte[] { 0, 0, 0, 0, 1 }))
			.isInstanceOf(AssertionError.class)
			.hasMessageStartingWith("Expected the received payload to be an Avro GenericRecord but was [byte[]]");
	}

	@Test
	void should_expose_the_normalized_value_for_execute_assertions() {
		assertThat(GenericRecordAssert.assertThatAvro(orderCreated()).field("customer.name").value()).isEqualTo("Ada");
	}

	private static Schema single(String type) {
		return new Schema.Parser().parse("""
				{"type": "record", "name": "Single", "namespace": "com.example", "fields": [
				  {"name": "value", "type": %s}]}
				""".formatted(type));
	}

	private static Object written(Schema schema, String text) {
		return switch (schema.getType()) {
			case INT -> Integer.parseInt(text);
			case LONG -> Long.parseLong(text);
			case BYTES -> ByteBuffer.wrap(text.getBytes(StandardCharsets.UTF_8));
			default -> new Utf8(text);
		};
	}

	private static Object sample(String writerType) {
		return written(single(writerType).getField("value").schema(),
				"long".equals(writerType.replace("\"", "")) ? "1" : "A");
	}

	private static GenericRecord orderCreated() {
		Schema customerSchema = ORDER_CREATED.getField("customer").schema();
		Schema lineSchema = ORDER_CREATED.getField("lines").schema().getElementType();
		Schema totalSchema = ORDER_CREATED.getField("total").schema();
		ByteBuffer total = new Conversions.DecimalConversion().toBytes(new BigDecimal("19.99"), totalSchema,
				totalSchema.getLogicalType());
		return new GenericRecordBuilder(ORDER_CREATED).set("orderId", new Utf8("o-1"))
			.set("quantity", 2)
			.set("createdAt", 1700000000000L)
			.set("total", total)
			.set("status", new GenericData.EnumSymbol(ORDER_CREATED.getField("status").schema(), "PAID"))
			.set("customer", new GenericRecordBuilder(customerSchema).set("name", new Utf8("Ada")).build())
			.set("lines", List
				.of(new GenericRecordBuilder(lineSchema).set("sku", new Utf8("A-1")).set("price", 9.5d).build()))
			.set("attributes", Map.of(new Utf8("channel"), new Utf8("web")))
			.set("note", null)
			.set("coupon", new Utf8("WELCOME"))
			.build();
	}

	public static class Paid extends SpecificRecordBase {

		public static final Schema SCHEMA$ = new Schema.Parser()
			.parse("""
					{"type": "record", "name": "Paid", "namespace": "sh.stubborn.contract.verifier.messaging.avro.GenericRecordAssertTests", "fields": [
					  {"name": "paidAt", "type": {"type": "long", "logicalType": "timestamp-millis"}},
					  {"name": "status", "type": {"type": "enum", "name": "Status", "symbols": ["NEW", "PAID"]}}]}
					""");

		public static final SpecificData MODEL$ = new SpecificData();

		static {
			MODEL$.addLogicalTypeConversion(new TimeConversions.TimestampMillisConversion());
		}

		Instant paidAt;

		Status status;

		@Override
		public Schema getSchema() {
			return SCHEMA$;
		}

		@Override
		public SpecificData getSpecificData() {
			return MODEL$;
		}

		@Override
		public Object get(int field) {
			return (field == 0) ? this.paidAt : this.status;
		}

		@Override
		public void put(int field, Object value) {
			if (field == 0) {
				this.paidAt = (Instant) value;
			}
			else {
				this.status = (Status) value;
			}
		}

		public enum Status {

			NEW, PAID

		}

	}

}
