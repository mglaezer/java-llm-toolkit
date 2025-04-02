package org.llmtoolkit.util.json;

import static com.fasterxml.jackson.core.json.JsonWriteFeature.ESCAPE_NON_ASCII;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class JSONRepair {
    private static final String NULL_STRING = "null";
    private static final String EMPTY_STRING = "";
    private static final String MARKDOWN_JSON_PREFIX = "```json";

    // Regex patterns
    private static final String DUPLICATE_COMMAS_PATTERN = ",\\s*,";
    private static final String TRAILING_OBJECT_COMMA_PATTERN = ",\\s*}";
    private static final String TRAILING_ARRAY_COMMA_PATTERN = ",\\s*]";

    // Common characters
    private static final char OBJECT_START = '{';
    private static final char OBJECT_END = '}';
    private static final char ARRAY_START = '[';
    private static final char ARRAY_END = ']';
    private static final char DOUBLE_QUOTE = '"';
    private static final char SINGLE_QUOTE = '\'';
    private static final char COMMA = ',';
    private static final char COLON = ':';
    private static final char ESCAPE_CHAR = '\\';

    private static final Map<Character, Character> ESCAPE_CHARS = Map.of(
            'n', '\n',
            't', '\t',
            'r', '\r',
            'b', '\b',
            'f', '\f',
            '\\', '\\');

    public static String repairJSON(String src) {
        if (src == null) {
            throw new IllegalArgumentException("Input cannot be null");
        }
        try {
            String cleaned = cleanupCommas(src.trim());
            String unwrapped = removeMarkdownWrapper(cleaned);

            JSONParser jp = new JSONParser(unwrapped);
            Object result = jp.parseJSON();
            return jsonMarshal(result);
        } catch (Exception e) {
            throw new RuntimeException("Repair JSON error: " + e.getMessage(), e);
        }
    }

    private static String cleanupCommas(String input) {
        return input.replaceAll(DUPLICATE_COMMAS_PATTERN, ",")
                .replaceAll(TRAILING_OBJECT_COMMA_PATTERN, "}")
                .replaceAll(TRAILING_ARRAY_COMMA_PATTERN, "]");
    }

    private static String removeMarkdownWrapper(String input) {
        if (input.startsWith(MARKDOWN_JSON_PREFIX)) {
            return input.substring(MARKDOWN_JSON_PREFIX.length()).trim();
        }
        return input;
    }

    private static String jsonMarshal(Object obj) throws JsonProcessingException {
        if (obj == null) {
            return NULL_STRING;
        }
        ObjectMapper mapper = createObjectMapper();
        return mapper.writeValueAsString(obj);
    }

    private static ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(ESCAPE_NON_ASCII.mappedFeature(), false);
        return mapper;
    }

    private static class JSONParser {
        private final String container;
        private int index;
        private final List<String> marker;

        public JSONParser(String in) {
            this.container = in;
            this.index = 0;
            this.marker = new ArrayList<>();
        }

        public Object parseJSON() {
            skipWhitespaces();
            Character c = peek(0);
            if (c == null) {
                return EMPTY_STRING;
            }

            boolean isInMarkers = !marker.isEmpty();

            if (c == OBJECT_START) {
                advance();
                return parseObject();
            } else if (c == ARRAY_START) {
                advance();
                return parseArray();
            } else if (c == OBJECT_END || c == ARRAY_END) {
                return EMPTY_STRING;
            } else if (isInMarkers && (isQuote(c) || Character.isLetter(c))) {
                if (isNullStart(c)) {
                    Object nullValue = parseNull();
                    if (nullValue == null) {
                        return null;
                    }
                }
                String value = parseString().toString();
                return parseLiteral(value);
            } else if (isInMarkers && (Character.isDigit(c) || c == '-' || c == '.')) {
                return parseNumber();
            } else if (isInMarkers && isNullStart(c)) {
                return parseNull();
            }

            advance();
            return parseJSON();
        }

        private Object parseLiteral(String value) {
            if (value.equalsIgnoreCase("true")) {
                return Boolean.TRUE;
            } else if (value.equalsIgnoreCase("false")) {
                return Boolean.FALSE;
            } else if (value.equalsIgnoreCase("null")) {
                return null;
            }
            return value;
        }

        private boolean isQuote(char c) {
            return c == DOUBLE_QUOTE || c == SINGLE_QUOTE;
        }

        private boolean isNullStart(Character c) {
            return c == 'n' || c == 'N';
        }

        private Object parseNull() {
            if (container == null || index >= container.length()) {
                return EMPTY_STRING;
            }
            String remaining = container.substring(index).toLowerCase();
            if (remaining.startsWith(NULL_STRING)) {
                index += 4;
                return null;
            }
            return EMPTY_STRING;
        }

        private Map<String, Object> parseObject() {
            Map<String, Object> result = new LinkedHashMap<>();
            Character c = peek(0);

            while (c != null && c != OBJECT_END) {
                skipWhitespaces();

                c = peek(0);
                if (c != null && c == COLON) {
                    advance();
                }

                setMarker("object_key");
                skipWhitespaces();

                String key = parseObjectKey();
                if (key.isEmpty()) {
                    key = "empty_placeholder";
                }

                skipWhitespaces();
                c = peek(0);

                if (c != null && c == OBJECT_END) {
                    result.put(key, EMPTY_STRING);
                    continue;
                }

                if (c != null && c == COLON) {
                    advance();
                }

                skipWhitespaces();
                c = peek(0);

                if (c == null) {
                    result.put(key, EMPTY_STRING);
                    break;
                }

                resetMarker();
                setMarker("object_value");
                Object value = parseJSON();

                resetMarker();
                result.put(key, value);

                c = peek(0);
                if (c != null && (c == COMMA || isQuote(c))) {
                    advance();
                }

                skipWhitespaces();
                c = peek(0);
            }

            if (c != null) {
                advance();
            }
            return result;
        }

        private String parseObjectKey() {
            String key = EMPTY_STRING;
            Character c = peek(0);

            while (key.isEmpty() && c != null) {
                int currentIndex = index;
                Object parsedString = parseString();
                key = parsedString.toString();

                c = peek(0);
                if (key.isEmpty() && c != null && (c == COLON || c == COMMA || c == OBJECT_END)) {
                    break;
                } else if (key.isEmpty() && index == currentIndex) {
                    advance();
                }
            }

            return key;
        }

        private List<Object> parseArray() {
            List<Object> result = new ArrayList<>();
            Character c = peek(0);
            setMarker("array");

            while (c != null && c != ARRAY_END) {
                skipWhitespaces();
                Object value = parseJSON();

                if (value == null) {
                    result.add(null);
                } else if (!value.equals(EMPTY_STRING)) {
                    Character prevC = peek(-1);
                    if (!value.equals("...") || prevC == null || prevC != '.') {
                        result.add(value);
                    }
                }

                c = peek(0);
                while (c != null && (Character.isWhitespace(c) || c == COMMA)) {
                    advance();
                    c = peek(0);
                }

                if (getMarker() != null && getMarker().equals("object_value") && c != null && c == OBJECT_END) {
                    break;
                }
            }

            c = peek(0);
            if (c != null && c != ARRAY_END) {
                index--;
            }

            advance();
            resetMarker();
            return result;
        }

        private Object parseString() {
            StringBuilder result = new StringBuilder();
            Character c = peek(0);

            if (c == null) {
                return EMPTY_STRING;
            }

            boolean isQuoted = isQuote(c);
            char quote = isQuoted ? c : DOUBLE_QUOTE;

            if (isQuoted) {
                advance();
            }

            while ((c = peek(0)) != null) {
                Character prev = peek(-1);
                if (isQuoted && c == quote && (prev == null || prev != ESCAPE_CHAR)) {
                    advance();
                    break;
                }

                if (!isQuoted) {
                    if (c == COMMA || c == OBJECT_END || c == ARRAY_END || c == COLON || Character.isWhitespace(c)) {
                        break;
                    }
                }

                if (c == ESCAPE_CHAR) {
                    advance();
                    Character next = peek(0);
                    if (next != null) {
                        handleEscapeSequence(result, next);
                    }
                } else {
                    result.append(c);
                    advance();
                }
            }

            return result.toString();
        }

        private void handleEscapeSequence(StringBuilder result, char next) {
            if (ESCAPE_CHARS.containsKey(next)) {
                result.append(ESCAPE_CHARS.get(next));
            } else if (next == 'u') {
                result.append("\\u");
                advance();
                for (int i = 0; i < 4; i++) {
                    Character hex = peek(0);
                    if (hex != null) {
                        result.append(hex);
                        advance();
                    }
                }
            } else {
                result.append(next);
            }
            advance();
        }

        private Object parseNumber() {
            StringBuilder result = new StringBuilder();
            Character c;

            while ((c = peek(0)) != null) {
                if (Character.isDigit(c) || c == '-' || c == '.' || c == 'e' || c == 'E' || c == '+') {
                    result.append(c);
                    advance();
                } else {
                    break;
                }
            }

            String numStr = result.toString();
            try {
                if (numStr.contains(".") || numStr.toLowerCase().contains("e")) {
                    return Double.parseDouble(numStr);
                } else {
                    return Long.parseLong(numStr);
                }
            } catch (NumberFormatException e) {
                return numStr;
            }
        }

        private Character peek(int offset) {
            int targetIndex = index + offset;
            if (container == null || targetIndex < 0 || targetIndex >= container.length()) {
                return null;
            }
            return container.charAt(targetIndex);
        }

        private void advance() {
            index++;
        }

        private void skipWhitespaces() {
            Character c = peek(0);
            while (c != null && Character.isWhitespace(c)) {
                advance();
                c = peek(0);
            }
        }

        private void setMarker(String in) {
            if (in != null && !in.isEmpty()) {
                marker.add(in);
            }
        }

        private void resetMarker() {
            if (!marker.isEmpty()) {
                marker.removeLast();
            }
        }

        private String getMarker() {
            return !marker.isEmpty() ? marker.getLast() : null;
        }
    }
}
