package org.llmtoolkit.util.json;

public class SerObject<T> {
    static final int DEFAULT_YAML_WIDTH = 120;

    private final T object;
    private final Class<T> clazz;

    private SerObject(T object, Class<T> clazz) {
        if (object == null) {
            throw new IllegalStateException("Object is not initialized");
        }
        this.object = object;
        this.clazz = clazz;
    }

    public static <T> SerObject<T> from(String jsonOrYaml, Class<T> clazz) {
        T object = JsonUtils.parseJsonOrYamlObject(jsonOrYaml, clazz);
        return new SerObject<>(object, clazz);
    }

    public static <T> SerObject<T> from(T object) {
        @SuppressWarnings("unchecked")
        Class<T> clazz = (Class<T>) object.getClass();
        return new SerObject<>(object, clazz);
    }

    public T toObject() {
        return object;
    }

    public String toJson() {
        return JsonUtils.formatJsonObject(object);
    }

    public String toYaml() {
        return toYaml(DEFAULT_YAML_WIDTH);
    }

    public String toYaml(int width) {
        String serializedJson = toJson();
        String yaml = JsonUtils.convertJsonToYaml(serializedJson, width);
        String reSerializedJson = SerObject.from(yaml, clazz).toJson();
        if (!serializedJson.equals(reSerializedJson)) throw new RuntimeException();
        return yaml;
    }
}
