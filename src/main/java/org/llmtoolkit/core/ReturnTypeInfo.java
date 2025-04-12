package org.llmtoolkit.core;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

/**
 * Represents return type information with validation logic
 */
public class ReturnTypeInfo {
    private final Class<?> valueType;
    private final boolean isList;
    private final boolean isString;

    private ReturnTypeInfo(Type originalType) {
        this.valueType = extractValueType(originalType);
        this.isList = originalType instanceof ParameterizedType
                && ((ParameterizedType) originalType).getRawType() == List.class;
        this.isString = originalType == String.class;
    }

    public static ReturnTypeInfo from(Type type) {
        return new ReturnTypeInfo(type);
    }

    public static void validateType(Type type) {
        extractValueType(type); // Will throw if invalid
    }

    private static Class<?> extractValueType(Type returnType) {
        //noinspection DuplicatedCode
        if (returnType instanceof Class<?> clazz) {
            validateReturnType(clazz);
            return clazz;
        } else if (returnType instanceof ParameterizedType paramType) {
            Type[] typeArgs = paramType.getActualTypeArguments();
            if (typeArgs.length == 1 && typeArgs[0] instanceof Class<?> elementType) {
                validateReturnType(elementType);
                return elementType;
            }
        }
        throw new UnsupportedOperationException(
                "Return type must be either a class (e.g., String, CustomClass) or List<Class> (e.g., List<String>). "
                        + "Unsupported types include: Map<K,V>, List<List<T>>, List<?>, generic type parameters.");
    }

    private static void validateReturnType(Class<?> type) {
        if (type.isPrimitive()) {
            throw new UnsupportedOperationException("Primitive return types are not supported");
        }
        if (type != String.class
                && (type.getName().startsWith("java.lang.") || type.getName().startsWith("java.util."))) {
            throw new UnsupportedOperationException("Java language and util types (except String) are not supported");
        }
    }

    public Class<?> getValueType() {
        return valueType;
    }

    public boolean isList() {
        return isList;
    }

    public boolean isString() {
        return isString;
    }
}
