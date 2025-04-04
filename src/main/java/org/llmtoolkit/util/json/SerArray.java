package org.llmtoolkit.util.json;

import static org.llmtoolkit.util.json.SerObject.DEFAULT_YAML_WIDTH;

import java.util.List;

/**
 * Serializes arrays to/from JSON and YAML with format validation.
 * Ensures round-trip consistency by verifying that re-parsing the serialized output
 * produces identical results.
 *
 * @param <T> type of elements in the array
 */
public class SerArray<T> {
    private final List<T> array;
    private final Class<T> clazz;

    private SerArray(List<T> array, Class<T> clazz) {
        if (array == null) {
            throw new IllegalStateException("Array is not initialized");
        }
        this.array = array;
        this.clazz = clazz;
    }

    public static <T> SerArray<T> from(String jsonOrYaml, Class<T> clazz) {
        List<T> array = JsonUtils.parseJsonOrYamlArray(jsonOrYaml, clazz);
        return new SerArray<>(array, clazz);
    }

    public static <T> SerArray<T> from(List<T> array, Class<T> clazz) {
        return new SerArray<>(array, clazz);
    }

    public List<T> toArray() {
        return array;
    }

    public String toJson() {
        return JsonUtils.formatJsonArray(array);
    }

    public String toYaml() {
        return toYaml(DEFAULT_YAML_WIDTH);
    }

    public String toYaml(int width) {
        String serializedJson = toJson();
        String yaml = JsonUtils.convertJsonToYaml(serializedJson, width);
        String reSerializedJson = SerArray.from(yaml, clazz).toJson();
        if (!serializedJson.equals(reSerializedJson)) throw new RuntimeException();
        return yaml;
    }
}
