package telran.json;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;

/**
 * This is simple JSON Parser, providing recursive parsing of JSONs representing
 * objects and arrays. The supposed format of all values are quoted strings,
 * inheritance and generic types are not covered.
 * 
 * @author Daniel Z.
 */
public class Json {
	static Map<Class<?>, Function<String, ?>> parsersByType = new IdentityHashMap<>();

	static {
		parsersByType.put(byte.class, Byte::parseByte);
		parsersByType.put(Byte.class, Byte::parseByte);
		parsersByType.put(short.class, Short::parseShort);
		parsersByType.put(Short.class, Short::parseShort);
		parsersByType.put(int.class, Integer::parseInt);
		parsersByType.put(Integer.class, Integer::parseInt);
		parsersByType.put(long.class, Long::parseLong);
		parsersByType.put(Long.class, Long::parseLong);

		parsersByType.put(float.class, Float::parseFloat);
		parsersByType.put(Float.class, Float::parseFloat);
		parsersByType.put(double.class, Double::parseDouble);
		parsersByType.put(Double.class, Double::parseDouble);

		parsersByType.put(boolean.class, Boolean::parseBoolean);
		parsersByType.put(Boolean.class, Boolean::parseBoolean);

		parsersByType.put(String.class, Function.identity());

		parsersByType.put(LocalDate.class, v -> LocalDate.parse(v, DateTimeFormatter.ofPattern("dd/MM/yyyy")));

	}

	public <T> T parse(String json, Class<T> type) throws Exception {
		SimpleJsonTokenizer tokenizer = new SimpleJsonTokenizer(json);
		T res = parseValue(tokenizer, type);
		if (tokenizer.hasNext()) {
			throw new RuntimeException("Json has excessive fields");
		}
		return res;
	}

	@SuppressWarnings("unchecked")
	private <T> T parseValue(SimpleJsonTokenizer tokenizer, Class<T> type) throws Exception {
		Function<String, ?> parser = parsersByType.get(type);
		if (parser != null) {
			return (T) parser.apply(stripQuotes(tokenizer.next()));
		}
		if (type.isArray()) {
			return parseArray(tokenizer, type);
		}
		return parseObject(tokenizer, type);
	}

	@SuppressWarnings("unchecked")
	private <T> T parseArray(SimpleJsonTokenizer tokenizer, Class<T> arrType) throws Exception {
		List<Object> list = new ArrayList<>();
		Class<?> eltType = arrType.getComponentType();
		assertNextToken(tokenizer, "[");
		while (!tokenizer.peek().equals("]")) {
			if (list.size() > 0) { // non-first element
				assertNextToken(tokenizer, ",");
			}
			Object elt = parseValue(tokenizer, eltType);
			list.add(elt);
		}
		tokenizer.next(); // extract ]
		int eltCount = list.size();
		Object actualArray = Array.newInstance(eltType, eltCount); // creation of generic array using reflection
		return (T) list.toArray((Object[]) actualArray);
	}

	private <T> T parseObject(SimpleJsonTokenizer tokenizer, Class<T> objType) throws Exception {
		T res = objType.getDeclaredConstructor().newInstance();
		Map<String, String> specialFieldNames = new HashMap<>();
		Arrays.stream(objType.getDeclaredFields()).filter(f -> f.isAnnotationPresent(JsonField.class)).forEach(f -> {
			String fName = f.getName();
			String specialJsonName = f.getAnnotation(JsonField.class).value();
			specialFieldNames.put(specialJsonName, fName);
		});
		assertNextToken(tokenizer, "{");
		int parsedFields = 0;
		while (!tokenizer.peek().equals("}")) {

			if (parsedFields > 0) {
				assertNextToken(tokenizer, ",");
			}

			String key = tokenizer.next();
			String fieldName = stripQuotes(key);

			if (specialFieldNames.containsKey(fieldName)) {
				fieldName = specialFieldNames.get(fieldName);
			}

			Field field = objType.getDeclaredField(fieldName);
			assertNextToken(tokenizer, ":");
			Class<?> fieldType = field.getType();

			if (field.isAnnotationPresent(JsonFormat.class)) {
				String pattern = field.getAnnotation(JsonFormat.class).value();
				parsersByType.put(JsonFormat.class, v -> LocalDate.parse(v, DateTimeFormatter.ofPattern(pattern)));
				fieldType = JsonFormat.class;
			}

			Object value = parseValue(tokenizer, fieldType);
			field.setAccessible(true);
			field.set(res, value);
			parsedFields++;
		}
		tokenizer.next(); // extract }
		return res;
	}

	private void assertNextToken(SimpleJsonTokenizer tokenizer, String expectedToken) {
		String nextToken = tokenizer.next();
		if (!nextToken.equals(expectedToken)) {
			throw new RuntimeException("Unexpected token " + nextToken + " instead of " + expectedToken);
		}
	}

	private String stripQuotes(String quotedString) {
		if (!quotedString.matches("\".*\"")) {
			throw new RuntimeException("Invalid token instead of quoted string: " + quotedString);
		}
		return quotedString.substring(1, quotedString.length() - 1);
	}

}
