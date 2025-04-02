package org.llmtoolkit.util.json;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

@SuppressWarnings("SpellCheckingInspection")
class JSONRepairTest {

    @Test
    void testValidJSON() {
        String input =
                """
            {
                "name": "John",
                "age": 30,
                "city": "New York"
            }""";
        String result = JSONRepair.repairJSON(input);
        assertTrue(result.contains("\"name\""));
        assertTrue(result.contains("\"John\""));
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "{'name': 'John'}", // Single quotes
                "{name: 'John'}", // Unquoted keys
                "{\"name\": \"John\",}", // Trailing comma
                "{\nname:\n'John'\n}", // Newlines and spaces
                "{'name': 'John', age: 30,}", // Mixed quotes and unquoted
                "{\"name\": \"John\", \"age\": 30,}" // Standard JSON with trailing comma
            })
    void testVariousQuotingStyles(String input) {
        String result = JSONRepair.repairJSON(input);
        assertNotNull(result);
        assertTrue(result.contains("name"));
    }

    @Test
    void testNestedObjects() {
        String input =
                """
            {
                person: {
                    name: 'John',
                    address: {
                        city: "New York",
                        zip: 10001,
                    }
                }
            }""";
        String result = JSONRepair.repairJSON(input);
        assertTrue(result.contains("\"person\""));
        assertTrue(result.contains("\"address\""));
    }

    @Test
    void testArrays() {
        String input =
                """
            {
                "numbers": [1, 2, 3,],
                "names": ['John', "Jane",],
                "mixed": [1, "two", {'three': 3,},],
            }""";
        String result = JSONRepair.repairJSON(input);
        assertTrue(result.contains("[1,2,3]"));
        assertTrue(result.contains("\"John\""));
    }

    @Test
    void testSpecialCharacters() {
        String input =
                """
            {
                "null_char": "\\u0000",
                "escaped": "\\"\\\\",
                "unicode": "\\u1234"
            }""";
        String result = JSONRepair.repairJSON(input);
        assertTrue(result.contains("\\u0000"));
        assertTrue(result.contains("\\\"\\\\"));
        assertTrue(result.contains("\\u1234"));
    }

    @Test
    void testNumbers() {
        String input =
                """
            {
                "integer": 42,
                "negative": -17,
                "float": 3.14,
                "scientific": 1.23e-4,
                "special": .5,
            }""";
        String result = JSONRepair.repairJSON(input);
        assertTrue(result.contains("42"));
        assertTrue(result.contains("-17"));
        assertTrue(result.contains("3.14"));
    }

    @Test
    void testBooleanAndNull() {
        String input =
                """
            {
                "bool1": true,
                "bool2": false,
                "empty": null,
                "True": TRUE,
                "False": FALSE,
                "Null": NULL
            }""";
        String result = JSONRepair.repairJSON(input);
        assertTrue(result.contains("true"));
        assertTrue(result.contains("false"));
        assertTrue(result.contains("null"));
    }

    @Test
    void testEdgeCases() {
        // Remove this test as it's covered by @ParameterizedTest below
    }

    @ParameterizedTest
    @MethodSource("provideRepairCases")
    void testJSONRepair(String input, String expectedStructure) throws JsonProcessingException {
        String result = JSONRepair.repairJSON(input);

        // Parse both to compare structure, not exact string matching
        ObjectMapper mapper = new ObjectMapper();
        assertEquals(mapper.readTree(expectedStructure), mapper.readTree(result), "Failed to repair: " + input);
    }

    private static Stream<Arguments> provideRepairCases() {
        return Stream.of(
                // Basic cases
                Arguments.of("{key: value}", "{\"key\": \"value\"}"),
                Arguments.of("{'key': 'value'}", "{\"key\": \"value\"}"),
                Arguments.of("{\"key\": \"value\",}", "{\"key\": \"value\"}"),

                // Whitespace handling - properly escaped
                Arguments.of("{\nkey\n:\nvalue\n}", "{\"key\":\"value\"}"),
                Arguments.of("{ key :  value }", "{\"key\": \"value\"}"),
                Arguments.of("{\"key\":\"value\nwith\nnewlines\"}", "{\"key\":\"value\\nwith\\nnewlines\"}"),

                // Newlines in values - properly escaped
                Arguments.of("{\"key\": \"value\\nwith\\nnewlines\"}", "{\"key\": \"value\\nwith\\nnewlines\"}"),
                Arguments.of("{\"key\":\"line1\\nline2\"}", "{\"key\": \"line1\\nline2\"}"),

                // Missing or extra delimiters - parser treats missing values as empty strings
                Arguments.of("{key: value,}", "{\"key\": \"value\"}"),
                Arguments.of("{\"key\":}", "{\"key\": \"\"}"),
                Arguments.of("{\"key\",}", "{\"key\": \"\"}"),
                Arguments.of("{\"key\":", "{\"key\": \"\"}"),

                // Array repairs
                Arguments.of("[1, 2, 3,]", "[1, 2, 3]"),
                Arguments.of("[1 2 3]", "[1, 2, 3]"),
                Arguments.of("[,1,2,,3,]", "[1, 2, 3]"),

                // Nested structures
                Arguments.of("{key: {nested: value}}", "{\"key\": {\"nested\": \"value\"}}"),
                Arguments.of("[{key: value}, {key: value,}]", "[{\"key\": \"value\"}, {\"key\": \"value\"}]"),

                // Mixed quotes and no quotes
                Arguments.of("{key: 'value', \"key2\": value2}", "{\"key\": \"value\", \"key2\": \"value2\"}"),

                // Numbers and special values
                Arguments.of("{key: 123}", "{\"key\": 123}"),
                Arguments.of("{key: -12.34}", "{\"key\": -12.34}"),
                Arguments.of("{key: true}", "{\"key\": true}"),
                Arguments.of("{key: false}", "{\"key\": false}"),
                Arguments.of("{key: null}", "{\"key\": null}"),

                // Empty structures
                Arguments.of("{}", "{}"),
                Arguments.of("[]", "[]"),
                Arguments.of("[{}]", "[{}]"),
                Arguments.of("{\"arr\":[]}", "{\"arr\":[]}"),

                // Special characters
                Arguments.of("{\"key\": \"value\\nwith\\tescapes\"}", "{\"key\": \"value\\nwith\\tescapes\"}"),

                // Complex mixed cases
                Arguments.of(
                        "{key: [1,2,], nested: {a: 'b',}, 'quoted': true, \"double\": null,}",
                        "{\"key\": [1,2], \"nested\": {\"a\": \"b\"}, \"quoted\": true, \"double\": null}"));
    }

    @Test
    void testErrorCases() {
        assertEquals("{}", JSONRepair.repairJSON("{"));
        assertEquals("\"\"", JSONRepair.repairJSON("}"));
        assertEquals("[]", JSONRepair.repairJSON("["));
        assertEquals("\"\"", JSONRepair.repairJSON("]"));
        // null input should still throw as it's not a valid string
        assertThrows(Exception.class, () -> JSONRepair.repairJSON(null));
    }

    @Test
    void testLargeJSON() {
        StringBuilder large = new StringBuilder("{\"array\":[");
        for (int i = 0; i < 1000; i++) {
            large.append(i).append(",");
        }
        large.append("]}");

        String result = JSONRepair.repairJSON(large.toString());
        assertNotNull(result);
        assertTrue(result.length() > 1000);
    }

    @Test
    void testMarkdownJSON() throws JsonProcessingException {
        String input =
                """
            ```json
            {
                "name": "John"
            }
            ```""";
        String result = JSONRepair.repairJSON(input);
        ObjectMapper mapper = new ObjectMapper();
        assertEquals(mapper.readTree("{\"name\": \"John\"}"), mapper.readTree(result));
    }

    @Test
    void testEmptyInput() {
        assertEquals("\"\"", JSONRepair.repairJSON(""));
        assertEquals("\"\"", JSONRepair.repairJSON("   "));
    }

    @Test
    void testNullInput() {
        assertThrows(Exception.class, () -> JSONRepair.repairJSON(null));
    }

    @Test
    void testComplexStructures() throws JsonProcessingException {
        String input =
                """
            {
                "array_of_objects": [
                    {"id": 1, "value": "first"},
                    {"id": 2, "value": "second",},
                ],
                "object_with_arrays": {
                    "numbers": [1,2,3],
                    "strings": ["a","b","c",],
                    "mixed": [1,"two",{"three":3},null,true,false],
                }
            }""";
        String result = JSONRepair.repairJSON(input);

        // Parse result to verify it's valid JSON
        ObjectMapper mapper = new ObjectMapper();
        @SuppressWarnings("unchecked")
        Map<String, Object> parsed = mapper.readValue(result, Map.class);

        assertTrue(parsed.containsKey("array_of_objects"));
        assertTrue(parsed.containsKey("object_with_arrays"));

        List<?> arrayOfObjects = (List<?>) parsed.get("array_of_objects");
        assertEquals(2, arrayOfObjects.size());
    }

    @Test
    void testSpecialEscapes() {
        String input =
                """
            {
                "newlines": "line1
                line2",
                "tabs": "col1    col2"
            }""";
        String result = JSONRepair.repairJSON(input);
        assertTrue(result.contains("line1"));
        assertTrue(result.contains("line2"));
        assertTrue(result.contains("col1"));
    }

    @Test
    void testWhitespaceHandling() {
        String input =
                """
            {
                "spaces":    "multiple    spaces",
                "tabs":\t"tab\tcharacters",
                "newlines":"multiple

            lines",
                "mixed":    \t  \n"mixed    \t
            whitespace"   \t

            }""";
        String result = JSONRepair.repairJSON(input);
        assertTrue(result.contains("\"spaces\""));
        assertTrue(result.contains("\"tabs\""));
        assertTrue(result.contains("\"newlines\""));
        assertTrue(result.contains("\"mixed\""));
    }

    @Test
    void testPerformance() {
        // Generate a large JSON string
        StringBuilder large = new StringBuilder("{\"items\":[");
        for (int i = 0; i < 10000; i++) {
            large.append("{\"id\":")
                    .append(i)
                    .append(",\"name\":\"item")
                    .append(i)
                    .append("\"")
                    .append(",\"value\":")
                    .append(Math.random())
                    .append("},");
        }
        large.append("]}");

        // Measure parsing time
        long startTime = System.nanoTime();
        String result = JSONRepair.repairJSON(large.toString());
        long endTime = System.nanoTime();

        long duration = (endTime - startTime) / 1_000_000; // Convert to milliseconds
        assertTrue(duration < 5000, "Parsing took too long: " + duration + "ms");
        assertNotNull(result);
    }

    @Test
    void testMarkdownAndComments() throws JsonProcessingException {
        String[] inputs = {
            // Markdown code blocks
            "```json\n{\"key\": \"value\"}\n```",
            "```javascript\n{\"key\": \"value\"}\n```",
            // Comments and whitespace
            "// This is a comment\n{\"key\": \"value\"}",
            "/* Multi-line\ncomment */\n{\"key\": \"value\"}",
            // Mixed markdown and comments
            "```json\n// Comment\n{\"key\": \"value\"}\n```"
        };

        for (String input : inputs) {
            String result = JSONRepair.repairJSON(input);
            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(result);
            assertEquals("value", node.get("key").asText());
        }
    }

    @Test
    void testUnquotedSpecialValues() throws JsonProcessingException {
        String input =
                """
            {
                "nullValue": null,
                "boolTrue": true,
                "boolFalse": false,
                "mixedCase": NULL,
                "unquotedTrue": TRUE,
                "unquotedFalse": FALSE,
                "numberValue": 123
            }""";

        String result = JSONRepair.repairJSON(input);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(result);

        assertNull(node.get("nullValue").asText(null));
        assertTrue(node.get("boolTrue").asBoolean());
        assertFalse(node.get("boolFalse").asBoolean());
        assertNull(node.get("mixedCase").asText(null));
        assertTrue(node.get("unquotedTrue").asBoolean());
        assertFalse(node.get("unquotedFalse").asBoolean());
        assertEquals(123, node.get("numberValue").asInt());
    }

    @Test
    void testComplexNestedStructures() throws JsonProcessingException {
        String input =
                """
            {
                "array": [1 2 3],
                "objects": [
                    {key: value} {key: value}
                ],
                "mixed": [
                    1,
                    {nestedKey: nestedValue},
                    "string",
                    true,
                    null
                ]
            }""";

        String result = JSONRepair.repairJSON(input);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(result);

        // Check array repair
        assertEquals(3, node.get("array").size());

        // Check objects array repair
        JsonNode objects = node.get("objects");
        assertEquals(2, objects.size());
        assertEquals("value", objects.get(0).get("key").asText());

        // Check mixed array repair
        JsonNode mixed = node.get("mixed");
        assertEquals(5, mixed.size());
        assertEquals(1, mixed.get(0).asInt());
        assertEquals("nestedValue", mixed.get(1).get("nestedKey").asText());
        assertEquals("string", mixed.get(2).asText());
        assertTrue(mixed.get(3).asBoolean());
        assertTrue(mixed.get(4).isNull());
    }

    @Test
    void testMalformedArraysAndObjects() {
        String[] inputs = {
            // Missing commas in arrays
            "[1 2 3]",
            "[\"a\" \"b\" \"c\"]",
            // Extra commas
            "[1,,,2,,,3]",
            "{\"a\":1,,,\"b\":2}",
            // Mixed issues
            "[1 2,,,3 4,5 6,,,]",
            // Nested structures
            "{\"arr\":[1 2 3],\"obj\":{a:1 b:2}}"
        };

        ObjectMapper mapper = new ObjectMapper();
        for (String input : inputs) {
            String result = JSONRepair.repairJSON(input);
            // Verify the result is valid JSON
            assertDoesNotThrow(() -> mapper.readTree(result));
        }
    }

    @Test
    void testEdgeCasesWithWhitespace() {
        String[] inputs = {
            // Multiple spaces between values
            "{key:    value}",
            // Tabs and newlines
            "{\tkey\t:\t\"value\"\t}",
            "{\nkey\n:\n\"value\"\n}",
            // Mixed whitespace
            "{ key :  \n  value ,\t\"key2\" : value2 }",
            // Around brackets and braces
            "[ 1 , 2 , { key : value } , 4 ]"
        };

        for (String input : inputs) {
            String result = JSONRepair.repairJSON(input);
            assertNotNull(result);
            // Verify the result is valid JSON
            ObjectMapper mapper = new ObjectMapper();
            assertDoesNotThrow(() -> mapper.readTree(result));
        }
    }

    @Test
    void testEmptyVsNullValues() throws JsonProcessingException {
        String input =
                """
            {
                "emptyString": "",
                "nullValue": null,
                "emptyArray": [],
                "nullArray": null,
                "emptyObject": {},
                "nullObject": null,
                "spaceString": "   ",
                "nested": {
                    "empty": "",
                    "null": null
                }
            }""";

        String result = JSONRepair.repairJSON(input);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(result);

        // Check empty string vs null
        assertTrue(node.get("emptyString").isTextual());
        assertEquals("", node.get("emptyString").asText());
        assertTrue(node.get("nullValue").isNull());

        // Check empty array vs null
        assertTrue(node.get("emptyArray").isArray());
        assertEquals(0, node.get("emptyArray").size());
        assertTrue(node.get("nullArray").isNull());

        // Check empty object vs null
        assertTrue(node.get("emptyObject").isObject());
        assertEquals(0, node.get("emptyObject").size());
        assertTrue(node.get("nullObject").isNull());

        // Check space string is preserved
        assertEquals("   ", node.get("spaceString").asText());

        // Check nested values
        assertTrue(node.get("nested").get("empty").isTextual());
        assertEquals("", node.get("nested").get("empty").asText());
        assertTrue(node.get("nested").get("null").isNull());
    }

    @Test
    void testAdvancedNumberParsing() throws JsonProcessingException {
        String input =
                """
            {
                "integer": 42,
                "negative": -17,
                "decimal": 3.14,
                "decimalNoLeading": .789,
                "scientificPos": 1.23e4,
                "scientificNeg": 1.23e-4,
                "scientificCap": 1.23E4,
                "scientificPosSign": 1.23e+4,
                "largeNumber": 9007199254740991,
                "invalidNumber": 12.34.56
            }""";

        String result = JSONRepair.repairJSON(input);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(result);

        // Test integer parsing
        assertEquals(42, node.get("integer").asInt());
        assertEquals(-17, node.get("negative").asInt());

        // Test decimal parsing
        assertEquals(3.14, node.get("decimal").asDouble(), 0.0001);
        assertEquals(0.789, node.get("decimalNoLeading").asDouble(), 0.0001);

        // Test scientific notation
        assertEquals(12300, node.get("scientificPos").asDouble(), 0.0001);
        assertEquals(0.000123, node.get("scientificNeg").asDouble(), 0.0000001);
        assertEquals(12300, node.get("scientificCap").asDouble(), 0.0001);
        assertEquals(12300, node.get("scientificPosSign").asDouble(), 0.0001);

        // Test large number handling
        assertEquals(9007199254740991L, node.get("largeNumber").asLong());

        // Test invalid number handling (should be converted to string)
        assertTrue(node.get("invalidNumber").isTextual());
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "{\"num\": .5}",
                "{\"num\": -.5}",
                "{\"num\": 1.}",
                "{\"num\": 1e5}",
                "{\"num\": 1e+5}",
                "{\"num\": 1e-5}",
                "{\"num\": -1e-5}"
            })
    void testNumberEdgeCases(String input) throws JsonProcessingException {
        String result = JSONRepair.repairJSON(input);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(result);

        assertTrue(node.get("num").isNumber(), "Failed to parse number from input: " + input);
    }
}
