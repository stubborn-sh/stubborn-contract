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

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Reflection-driven mutation tests for the generated {@code equals}/{@code hashCode}/
 * {@code toString} of {@link YamlContract} and all its nested value classes. For every
 * class it verifies that two default instances are equal, that differing in each single
 * field breaks equality (killing the per-field {@code Objects.equals} conditionals), and
 * that a fully populated instance has a distinct hash and a non-blank string form.
 */
class YamlContractEqualsMutationTests {

	@Test
	void yaml_contract_and_all_nested_value_classes_honour_equals_hashCode_toString() throws Exception {
		List<Class<?>> classes = new ArrayList<>();
		classes.add(YamlContract.class);
		for (Class<?> nested : YamlContract.class.getDeclaredClasses()) {
			classes.add(nested);
		}
		int verified = 0;
		for (Class<?> clazz : classes) {
			if (!declaresEquals(clazz) || clazz.isEnum() || Modifier.isAbstract(clazz.getModifiers())) {
				continue;
			}
			verifyClass(clazz);
			verified++;
		}
		assertThat(verified).as("number of value classes verified").isGreaterThanOrEqualTo(15);
	}

	private static boolean declaresEquals(Class<?> clazz) {
		try {
			clazz.getDeclaredMethod("equals", Object.class);
			return true;
		}
		catch (NoSuchMethodException ex) {
			return false;
		}
	}

	private void verifyClass(Class<?> clazz) throws Exception {
		Object a = clazz.getDeclaredConstructor().newInstance();
		Object b = clazz.getDeclaredConstructor().newInstance();

		assertThat(a).as("%s: two defaults equal", clazz.getSimpleName()).isEqualTo(b);
		assertThat(a.hashCode()).as("%s: equal objects share hashCode", clazz.getSimpleName()).isEqualTo(b.hashCode());
		assertThat(a).as("%s: reflexive", clazz.getSimpleName()).isEqualTo(a);
		assertThat(a.equals(null)).as("%s: not equal to null", clazz.getSimpleName()).isFalse();
		assertThat(a.equals("a different type")).as("%s: not equal to other type", clazz.getSimpleName()).isFalse();

		List<Field> fields = new ArrayList<>();
		for (Field field : clazz.getFields()) {
			if (Modifier.isStatic(field.getModifiers()) || Modifier.isFinal(field.getModifiers())) {
				continue;
			}
			fields.add(field);
		}

		for (Field field : fields) {
			Object base = clazz.getDeclaredConstructor().newInstance();
			Object current = field.get(base);
			Object distinct = distinctValue(field, current);
			if (distinct == null && current == null) {
				continue;
			}
			Object mutated = clazz.getDeclaredConstructor().newInstance();
			field.set(mutated, distinct);
			assertThat(mutated)
				.as("%s: differs by field %s (mutated.equals(default))", clazz.getSimpleName(), field.getName())
				.isNotEqualTo(base);
			assertThat(base)
				.as("%s: differs by field %s (default.equals(mutated))", clazz.getSimpleName(), field.getName())
				.isNotEqualTo(mutated);
		}

		// fully populated instance for hashCode / toString distinctness
		Object populated = clazz.getDeclaredConstructor().newInstance();
		boolean anySet = false;
		for (Field field : fields) {
			Object distinct = distinctValue(field, field.get(populated));
			if (distinct != null) {
				field.set(populated, distinct);
				anySet = true;
			}
		}
		if (anySet) {
			assertThat(populated.hashCode()).as("%s: populated hashCode differs from empty", clazz.getSimpleName())
				.isNotEqualTo(a.hashCode());
		}
		if (declaresToString(clazz)) {
			assertThat(a.toString()).as("%s: empty toString not blank", clazz.getSimpleName()).isNotBlank();
			assertThat(populated.toString()).as("%s: populated toString not blank", clazz.getSimpleName()).isNotBlank();
		}
	}

	private static boolean declaresToString(Class<?> clazz) {
		try {
			clazz.getDeclaredMethod("toString");
			return true;
		}
		catch (NoSuchMethodException ex) {
			return false;
		}
	}

	private Object distinctValue(Field field, Object current) throws Exception {
		Class<?> type = field.getType();
		if (type == boolean.class || type == Boolean.class) {
			return (current != null) ? !((Boolean) current) : Boolean.TRUE;
		}
		if (type == int.class || type == Integer.class) {
			return (current != null) ? ((Integer) current) + 1 : Integer.valueOf(999);
		}
		if (type == long.class || type == Long.class) {
			return (current != null) ? ((Long) current) + 1L : Long.valueOf(999L);
		}
		if (CharSequence.class.isAssignableFrom(type)) {
			return (current != null) ? current + "-x" : "distinct-" + field.getName();
		}
		if (Map.class.isAssignableFrom(type)) {
			Map<Object, Object> map = new LinkedHashMap<>();
			map.put("distinctKey-" + field.getName(), "distinctValue");
			if (map.equals(current)) {
				map.put("second", "value");
			}
			return map;
		}
		if (List.class.isAssignableFrom(type)) {
			List<Object> list = new ArrayList<>();
			list.add("distinct-" + field.getName());
			if (list.equals(current)) {
				list.add("second");
			}
			return list;
		}
		if (type == byte[].class) {
			return new byte[] { 1, 2, 3 };
		}
		if (type.isEnum()) {
			for (Object constant : type.getEnumConstants()) {
				if (!constant.equals(current)) {
					return constant;
				}
			}
			return null;
		}
		if (current != null) {
			// non-null default -> null is a distinct value (fields use Objects.equals)
			return null;
		}
		// null default -> a fresh instance is distinct from null
		try {
			return type.getDeclaredConstructor().newInstance();
		}
		catch (Exception ex) {
			if (type == Object.class) {
				return new Object();
			}
			return null;
		}
	}

}
