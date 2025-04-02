package org.llmtoolkit.util.json;

import static com.fasterxml.jackson.core.json.JsonReadFeature.ALLOW_TRAILING_COMMA;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.util.StringUtils;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

public class JsonUtils {

    private static final ObjectMapper YAML_OBJECT_MAPPER = new ObjectMapper(new YAMLFactory()
            .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
            .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES));

    private static final ObjectMapper JSON_OBJECT_MAPPER = new ObjectMapper()
            .enable(ALLOW_TRAILING_COMMA.mappedFeature())
            .enable(SerializationFeature.INDENT_OUTPUT)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);

    public static <T> T parseJsonOrYamlObject(String inputString, Class<T> clazz) {

        // First attempt: Parse as YAML
        try {
            return YAML_OBJECT_MAPPER.readValue(inputString, clazz);
        } catch (JsonProcessingException ignored) {
        }

        // Second attempt: Parse JSON
        String json = trimJson(inputString);
        try {
            return JSON_OBJECT_MAPPER.readValue(json, clazz);
        } catch (JsonProcessingException ignored) {
        }

        // Third attempt: Repair and parse JSON
        try {
            return JSON_OBJECT_MAPPER.readValue(JSONRepair.repairJSON(json), clazz);
        } catch (Exception e) {
            throw new RuntimeException("Cannot parse json to class " + clazz.getName() + " json: \n" + json, e);
        }
    }

    public static <T> List<T> parseJsonOrYamlArray(String inputString, Class<T> clazz) {
        // First attempt: Parse as YAML
        try {
            return YAML_OBJECT_MAPPER.readValue(
                    inputString, YAML_OBJECT_MAPPER.getTypeFactory().constructCollectionType(List.class, clazz));
        } catch (JsonProcessingException ignored) {
        }

        // Second attempt: Parse JSON
        String json = trimJson(inputString);
        try {
            return JSON_OBJECT_MAPPER.readValue(
                    json, JSON_OBJECT_MAPPER.getTypeFactory().constructCollectionType(List.class, clazz));
        } catch (JsonProcessingException ignored) {
        }

        // Third attempt: Repair and parse JSON
        try {
            return JSON_OBJECT_MAPPER.readValue(
                    JSONRepair.repairJSON(json),
                    JSON_OBJECT_MAPPER.getTypeFactory().constructCollectionType(List.class, clazz));
        } catch (Exception e) {
            throw new RuntimeException(
                    "Cannot parse json array to class " + clazz.getName() + " input: \n" + inputString, e);
        }
    }

    public static <T> String formatJsonObject(T object) {
        try {
            return JSON_OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> String formatJsonArray(List<T> list) {
        try {
            return JSON_OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(list);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static String trimJson(String input) {
        if (!StringUtils.hasText(input)) return "";
        input = getMarkdownBlockIfPresent(input);
        return extractJson(input);
    }

    private static String getMarkdownBlockIfPresent(String input) {
        Pattern markdownPattern = Pattern.compile("```json\\s*(.*?)\\s*```", Pattern.DOTALL);
        Matcher markdownMatcher = markdownPattern.matcher(input);

        if (markdownMatcher.find()) {
            return markdownMatcher.group(1);
        }
        return input;
    }

    private static String extractJson(String input) {
        String arrayPattern = "\\[.*](?!.*])";
        String objectPattern = "\\{.*}(?!.*})";

        int objectStart = input.indexOf('{');
        int arrayStart = input.indexOf('[');

        if (objectStart == -1 && arrayStart == -1) {
            return input.trim();
        }

        Pattern pattern;
        if (objectStart == -1) pattern = Pattern.compile(arrayPattern, Pattern.DOTALL);
        else if (arrayStart == -1) pattern = Pattern.compile(objectPattern, Pattern.DOTALL);
        else
            pattern = objectStart < arrayStart
                    ? Pattern.compile(objectPattern, Pattern.DOTALL)
                    : Pattern.compile(arrayPattern, Pattern.DOTALL);

        Matcher matcher = pattern.matcher(input);
        String longest = "";
        while (matcher.find()) {
            String match = matcher.group();
            if (match.length() > longest.length()) {
                longest = match;
            }
        }
        return longest.isEmpty() ? input.trim() : longest.trim();
    }

    /*
     * Much better output than direct YAML printing
     * Honors width parameter
     */
    static String convertJsonToYaml(String json, int width) {

        try {
            ObjectMapper jsonMapper = new ObjectMapper();
            JsonNode jsonNode = jsonMapper.readTree(json);

            DumperOptions options = new DumperOptions();
            options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
            options.setDefaultScalarStyle(DumperOptions.ScalarStyle.PLAIN);
            options.setIndent(4);
            options.setWidth(width);

            if (jsonNode.isObject()) {
                var map = jsonMapper.convertValue(jsonNode, LinkedHashMap.class);
                return new Yaml(options).dump(map);
            } else if (jsonNode.isArray()) {
                var list = jsonMapper.convertValue(jsonNode, List.class);
                return new Yaml(options).dump(list);
            } else {
                throw new IllegalArgumentException("Input JSON must be an object or array");
            }
        } catch (JsonProcessingException | IllegalArgumentException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unused")
    public static String preventYamlQuotation(String text) {
        return text
                // Normalize line breaks
                .replace("\r\n", "\n")
                // Remove trailing whitespace on lines
                .replaceAll("(?m)[ \\t]+$", "")
                // Normalize excessive blank lines to maintain readability
                .replaceAll("\n{2,}", "\n")
                // Escape backslashes to prevent YAML interpretation issues
                .replace("\\", "\\\\");
    }
}
