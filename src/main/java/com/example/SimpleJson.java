package com.example;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class SimpleJson {
    private SimpleJson() {
    }

    public static Object parse(String json) {
        Parser parser = new Parser(json);
        Object value = parser.parseValue();
        parser.skipWhitespace();
        if (!parser.isAtEnd()) {
            throw new IllegalArgumentException("Unexpected trailing JSON content at position " + parser.index);
        }
        return value;
    }

    @SuppressWarnings("unchecked")
    public static List<Object> asArray(Object value) {
        if (value instanceof List<?> list) {
            return (List<Object>) list;
        }
        throw new IllegalArgumentException("Expected a JSON array.");
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> asObject(Object value) {
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        throw new IllegalArgumentException("Expected a JSON object.");
    }

    public static String getString(Map<String, Object> object, String key) {
        return asString(object.get(key));
    }

    public static String asString(Object value) {
        if (value == null) {
            return "";
        }
        return String.valueOf(value).trim();
    }

    public static int getInt(Map<String, Object> object, String key, int fallback) {
        return asInt(object.get(key), fallback);
    }

    public static int asInt(Object value, int fallback) {
        if (value == null) {
            return fallback;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        String text = asString(value);
        return text.isBlank() ? fallback : Integer.parseInt(text);
    }

    public static double getDouble(Map<String, Object> object, String key, double fallback) {
        return asDouble(object.get(key), fallback);
    }

    public static double asDouble(Object value, double fallback) {
        if (value == null) {
            return fallback;
        }
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        String text = asString(value);
        return text.isBlank() ? fallback : Double.parseDouble(text);
    }

    public static boolean getBoolean(Map<String, Object> object, String key, boolean fallback) {
        return asBoolean(object.get(key), fallback);
    }

    public static boolean asBoolean(Object value, boolean fallback) {
        if (value == null) {
            return fallback;
        }
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        String text = asString(value);
        return text.isBlank() ? fallback : Boolean.parseBoolean(text);
    }

    private static final class Parser {
        private final String input;
        private int index;

        private Parser(String input) {
            this.input = input == null ? "" : input;
        }

        private Object parseValue() {
            skipWhitespace();
            if (isAtEnd()) {
                throw new IllegalArgumentException("Unexpected end of JSON input.");
            }

            return switch (current()) {
                case '{' -> parseObject();
                case '[' -> parseArray();
                case '"' -> parseString();
                case 't' -> parseLiteral("true", Boolean.TRUE);
                case 'f' -> parseLiteral("false", Boolean.FALSE);
                case 'n' -> parseLiteral("null", null);
                default -> parseNumber();
            };
        }

        private Map<String, Object> parseObject() {
            expect('{');
            skipWhitespace();

            Map<String, Object> object = new LinkedHashMap<>();
            if (peek('}')) {
                expect('}');
                return object;
            }

            while (true) {
                skipWhitespace();
                String key = parseString();
                skipWhitespace();
                expect(':');
                Object value = parseValue();
                object.put(key, value);
                skipWhitespace();

                if (peek('}')) {
                    expect('}');
                    return object;
                }

                expect(',');
            }
        }

        private List<Object> parseArray() {
            expect('[');
            skipWhitespace();

            List<Object> list = new ArrayList<>();
            if (peek(']')) {
                expect(']');
                return list;
            }

            while (true) {
                list.add(parseValue());
                skipWhitespace();

                if (peek(']')) {
                    expect(']');
                    return list;
                }

                expect(',');
            }
        }

        private String parseString() {
            expect('"');
            StringBuilder builder = new StringBuilder();

            while (!isAtEnd()) {
                char current = current();
                index++;

                if (current == '"') {
                    return builder.toString();
                }

                if (current == '\\') {
                    if (isAtEnd()) {
                        throw new IllegalArgumentException("Unterminated escape sequence in JSON string.");
                    }

                    char escaped = current();
                    index++;
                    switch (escaped) {
                        case '"', '\\', '/' -> builder.append(escaped);
                        case 'b' -> builder.append('\b');
                        case 'f' -> builder.append('\f');
                        case 'n' -> builder.append('\n');
                        case 'r' -> builder.append('\r');
                        case 't' -> builder.append('\t');
                        case 'u' -> builder.append(parseUnicodeEscape());
                        default -> throw new IllegalArgumentException("Unsupported escape sequence: \\" + escaped);
                    }
                    continue;
                }

                builder.append(current);
            }

            throw new IllegalArgumentException("Unterminated JSON string.");
        }

        private char parseUnicodeEscape() {
            if (index + 4 > input.length()) {
                throw new IllegalArgumentException("Incomplete unicode escape sequence.");
            }
            String hex = input.substring(index, index + 4);
            index += 4;
            return (char) Integer.parseInt(hex, 16);
        }

        private Object parseLiteral(String literal, Object value) {
            if (input.startsWith(literal, index)) {
                index += literal.length();
                return value;
            }
            throw new IllegalArgumentException("Invalid JSON token at position " + index);
        }

        private Number parseNumber() {
            int start = index;
            if (peek('-')) {
                index++;
            }

            while (!isAtEnd() && Character.isDigit(current())) {
                index++;
            }

            boolean decimal = false;
            if (!isAtEnd() && current() == '.') {
                decimal = true;
                index++;
                while (!isAtEnd() && Character.isDigit(current())) {
                    index++;
                }
            }

            if (!isAtEnd() && (current() == 'e' || current() == 'E')) {
                decimal = true;
                index++;
                if (!isAtEnd() && (current() == '+' || current() == '-')) {
                    index++;
                }
                while (!isAtEnd() && Character.isDigit(current())) {
                    index++;
                }
            }

            String numberText = input.substring(start, index);
            if (numberText.isBlank()) {
                throw new IllegalArgumentException("Invalid JSON number at position " + start);
            }

            if (decimal) {
                return new BigDecimal(numberText);
            }

            try {
                return Long.parseLong(numberText);
            } catch (NumberFormatException exception) {
                return new BigDecimal(numberText);
            }
        }

        private void expect(char expected) {
            skipWhitespace();
            if (isAtEnd() || current() != expected) {
                throw new IllegalArgumentException("Expected '" + expected + "' at position " + index);
            }
            index++;
        }

        private boolean peek(char expected) {
            skipWhitespace();
            return !isAtEnd() && current() == expected;
        }

        private char current() {
            return input.charAt(index);
        }

        private void skipWhitespace() {
            while (!isAtEnd() && Character.isWhitespace(input.charAt(index))) {
                index++;
            }
        }

        private boolean isAtEnd() {
            return index >= input.length();
        }
    }
}
