package org.llmtoolkit.util;

import java.util.*;
import spoon.Launcher;
import spoon.reflect.declaration.*;
import spoon.reflect.reference.CtTypeReference;

public class ClassStructureGenerator {

    private final Set<String> processedClasses = new HashSet<>();
    private final String ignoredPackagePrefix = "java.";
    private final StringBuilder output = new StringBuilder();

    // Common default values to omit
    private static final Map<String, Map<String, String>> DEFAULT_VALUES = new HashMap<>();

    static {
        Map<String, String> validateDefaults = new HashMap<>();
        validateDefaults.put("maxLength", "2147483647");
        validateDefaults.put("minLength", "0");
        validateDefaults.put("nullable", "false");
        DEFAULT_VALUES.put("Validate", validateDefaults);

        Map<String, String> infoDefaults = new HashMap<>();
        infoDefaults.put("priority", "0");
        // Don't add default for tags since we want to show empty arrays
        DEFAULT_VALUES.put("Info", infoDefaults);
    }

    public String generateClassStructure(Class<?> clazz) {
        Launcher spoon = new Launcher();
        spoon.getEnvironment().setNoClasspath(true);
        spoon.addInputResource(
                clazz.getProtectionDomain().getCodeSource().getLocation().getPath());
        spoon.buildModel();

        processType(spoon.getFactory().Type().get(clazz));
        return output.toString();
    }

    private void processType(CtType<?> type) {
        String qualifiedName = type.getQualifiedName();
        String simpleName = type.getSimpleName();

        // Skip processing if already handled, in java packages, is an annotation/annotation type,
        // or is a type parameter
        if (processedClasses.contains(qualifiedName)
                || qualifiedName.startsWith(ignoredPackagePrefix)
                || type instanceof CtAnnotation
                || type instanceof CtAnnotationType
                || type instanceof CtTypeParameter) {
            return;
        }

        processedClasses.add(qualifiedName);
        generateTypeDeclaration(type);
        processDependencies(type);
    }

    private void generateTypeDeclaration(CtType<?> type) {
        output.append(formatAnnotations(type.getAnnotations())).append("\n");

        // Check if this is a class that extends Record
        boolean isRecordClass = false;
        if (type instanceof CtClass) {
            CtClass<?> ctClass = (CtClass<?>) type;
            if (ctClass.getSuperclass() != null
                    && ctClass.getSuperclass().getSimpleName().equals("Record")) {
                isRecordClass = true;
            }
        }

        // Determine whether the type is a class, interface, or record
        String typeKeyword;
        if (isRecordClass) {
            typeKeyword = "record";
        } else if (type instanceof CtClass) {
            typeKeyword = "class";
        } else if (type instanceof CtInterface) {
            typeKeyword = "interface";
        } else {
            typeKeyword = "unknown"; // Handle other cases if necessary
        }

        output.append(formatModifiers(type.getModifiers()))
                .append(" ")
                .append(typeKeyword)
                .append(" ")
                .append(type.getSimpleName());

        // Type parameters
        if (!type.getFormalCtTypeParameters().isEmpty()) {
            output.append("<")
                    .append(String.join(
                            ", ",
                            type.getFormalCtTypeParameters().stream()
                                    .map(CtTypeParameter::getSimpleName)
                                    .toArray(String[]::new)))
                    .append(">");
        }

        // Add inheritance information for non-record classes
        if (!isRecordClass) {
            if (!type.getSuperInterfaces().isEmpty()) {
                if (type instanceof CtClass) {
                    CtClass<?> ctClass = (CtClass<?>) type;
                    if (ctClass.getSuperclass() != null
                            && !ctClass.getSuperclass().getQualifiedName().equals("java.lang.Object")) {
                        output.append(" extends ").append(formatTypeReference(ctClass.getSuperclass()));
                    }

                    if (!type.getSuperInterfaces().isEmpty()) {
                        output.append(" implements ");
                    }
                } else {
                    output.append(" extends ");
                }

                if (!type.getSuperInterfaces().isEmpty()) {
                    output.append(type.getSuperInterfaces().stream()
                            .map(this::formatTypeReference)
                            .collect(java.util.stream.Collectors.joining(", ")));
                }
            } else if (type instanceof CtClass) {
                CtClass<?> ctClass = (CtClass<?>) type;
                if (ctClass.getSuperclass() != null
                        && !ctClass.getSuperclass().getQualifiedName().equals("java.lang.Object")) {
                    output.append(" extends ").append(formatTypeReference(ctClass.getSuperclass()));
                }
            }
        }

        // Special handling for record classes
        if (isRecordClass) {
            // For records, we want to show the constructor-like format
            output.append("(\n");

            // Record components (fields)
            List<CtField<?>> fields = type.getFields();
            for (int i = 0; i < fields.size(); i++) {
                CtField<?> field = fields.get(i);
                output.append("        ")
                        .append(formatAnnotations(field.getAnnotations()))
                        .append(" ")
                        .append(formatTypeReference(field.getType()))
                        .append(" ")
                        .append(field.getSimpleName());

                if (i < fields.size() - 1) {
                    output.append(",\n");
                } else {
                    output.append("\n    ");
                }
            }
            output.append(") {\n\n");

            // Only show non-accessor methods for records
            List<CtMethod<?>> methods = new ArrayList<>(type.getMethods());

            // Sort methods to match ClassToString order (by name)
            methods.sort(Comparator.comparing(CtMethod::getSimpleName));

            for (CtMethod<?> method : methods) {
                String methodName = method.getSimpleName();
                boolean isAccessor =
                        fields.stream().anyMatch(field -> field.getSimpleName().equals(methodName));
                boolean isAutoGenerated =
                        methodName.equals("equals") || methodName.equals("hashCode") || methodName.equals("toString");

                if (!isAccessor && !isAutoGenerated) {
                    output.append("    ")
                            .append(formatAnnotations(method.getAnnotations()))
                            .append("\n    ")
                            .append(formatModifiers(method.getModifiers()))
                            .append(" ");

                    // Add generic type parameters if any
                    if (!method.getFormalCtTypeParameters().isEmpty()) {
                        output.append("<")
                                .append(method.getFormalCtTypeParameters().stream()
                                        .map(CtTypeParameter::getSimpleName)
                                        .collect(java.util.stream.Collectors.joining(", ")))
                                .append("> ");
                    }

                    output.append(formatTypeReference(method.getType()))
                            .append(" ")
                            .append(method.getSimpleName())
                            .append("(");

                    // Parameters
                    List<CtParameter<?>> parameters = method.getParameters();
                    for (int i = 0; i < parameters.size(); i++) {
                        CtParameter<?> param = parameters.get(i);
                        output.append(formatAnnotations(param.getAnnotations()))
                                .append(" ")
                                .append(formatTypeReference(param.getType()))
                                .append(" ")
                                .append(param.getSimpleName());

                        if (i < parameters.size() - 1) {
                            output.append(", ");
                        }
                    }
                    output.append(")");

                    // Add throws clause if present
                    if (!method.getThrownTypes().isEmpty()) {
                        output.append(" throws ")
                                .append(method.getThrownTypes().stream()
                                        .map(this::formatTypeReference)
                                        .collect(java.util.stream.Collectors.joining(", ")));
                    }

                    output.append(" {}\n\n");
                }
            }
        } else {
            output.append(" {\n\n");

            // Fields (for non-records) - only show public fields
            List<CtField<?>> publicFields = type.getFields().stream()
                    .filter(field -> field.getModifiers().contains(ModifierKind.PUBLIC))
                    .collect(java.util.stream.Collectors.toList());

            for (CtField<?> field : publicFields) {
                output.append("    ")
                        .append(formatAnnotations(field.getAnnotations()))
                        .append(" ")
                        .append(formatModifiers(field.getModifiers()))
                        .append(" ")
                        .append(formatTypeReference(field.getType()))
                        .append(" ")
                        .append(field.getSimpleName())
                        .append(";\n\n");
            }

            // Constructors (for classes)
            if (type instanceof CtClass) {
                CtClass<?> ctClass = (CtClass<?>) type;
                List<CtConstructor<?>> constructors = new ArrayList<>(ctClass.getConstructors());

                // Sort constructors by parameter count
                constructors.sort(Comparator.comparingInt(c -> c.getParameters().size()));

                for (CtConstructor<?> constructor : constructors) {
                    output.append("    ")
                            .append(formatAnnotations(constructor.getAnnotations()))
                            .append(" ")
                            .append(formatModifiers(constructor.getModifiers()))
                            .append(" ")
                            .append(type.getSimpleName())
                            .append("(");

                    // Constructor parameters
                    List<CtParameter<?>> parameters = constructor.getParameters();
                    for (int i = 0; i < parameters.size(); i++) {
                        CtParameter<?> param = parameters.get(i);
                        output.append(formatAnnotations(param.getAnnotations()))
                                .append(" ")
                                .append(formatTypeReference(param.getType()))
                                .append(" ")
                                .append(param.getSimpleName());

                        if (i < parameters.size() - 1) {
                            output.append(", ");
                        }
                    }
                    output.append(") {}");

                    // Add throws clause if present
                    if (!constructor.getThrownTypes().isEmpty()) {
                        output.append(" throws ")
                                .append(constructor.getThrownTypes().stream()
                                        .map(this::formatTypeReference)
                                        .collect(java.util.stream.Collectors.joining(", ")));
                    }

                    output.append("\n\n");
                }
            }

            // Methods (for non-records)
            List<CtMethod<?>> methods = new ArrayList<>(type.getMethods());

            // Sort methods to match ClassToString order (by name)
            methods.sort(Comparator.comparing(CtMethod::getSimpleName));

            for (CtMethod<?> method : methods) {
                output.append("    ")
                        .append(formatAnnotations(method.getAnnotations()))
                        .append(" ")
                        .append(formatModifiersForMethod(method, type))
                        .append(" ");

                // Add generic type parameters if any
                if (!method.getFormalCtTypeParameters().isEmpty()) {
                    output.append("<")
                            .append(method.getFormalCtTypeParameters().stream()
                                    .map(CtTypeParameter::getSimpleName)
                                    .collect(java.util.stream.Collectors.joining(", ")))
                            .append("> ");
                }

                output.append(formatTypeReference(method.getType()))
                        .append(" ")
                        .append(method.getSimpleName())
                        .append("(");

                // Parameters
                List<CtParameter<?>> parameters = method.getParameters();
                for (int i = 0; i < parameters.size(); i++) {
                    CtParameter<?> param = parameters.get(i);
                    output.append(formatAnnotations(param.getAnnotations()))
                            .append(" ")
                            .append(formatTypeReference(param.getType()))
                            .append(" ")
                            .append(param.getSimpleName());

                    if (i < parameters.size() - 1) {
                        output.append(", ");
                    }
                }
                output.append(")");

                // Add throws clause if present
                if (!method.getThrownTypes().isEmpty()) {
                    output.append(" throws ")
                            .append(method.getThrownTypes().stream()
                                    .map(this::formatTypeReference)
                                    .collect(java.util.stream.Collectors.joining(", ")));
                }

                // Add empty braces for methods with bodies
                output.append(" {}");

                output.append("\n\n");
            }
        }

        output.append("}\n\n");
    }

    /**
     * Format modifiers for methods, handling interface methods specially
     */
    private String formatModifiersForMethod(CtMethod<?> method, CtType<?> containingType) {
        Set<ModifierKind> modifiers = new HashSet<>(method.getModifiers());

        // For interface methods, don't show "abstract" modifier
        if (containingType instanceof CtInterface && method.getBody() == null) {
            modifiers.remove(ModifierKind.ABSTRACT);
        }

        return formatModifiers(modifiers);
    }

    private void processDependencies(CtType<?> type) {
        // Process field types
        type.getFields().forEach(field -> processTypeReference(field.getType()));

        // Process method parameter/return types
        type.getMethods().forEach(method -> {
            processTypeReference(method.getType());
            method.getParameters().forEach(param -> processTypeReference(param.getType()));
        });

        // Process nested types
        type.getNestedTypes().forEach(this::processType);
    }

    private void processTypeReference(CtTypeReference<?> ref) {
        if (ref == null || ref.isPrimitive()) return;

        // Skip all java packages
        String qualifiedName = ref.getQualifiedName();
        if (qualifiedName.startsWith(ignoredPackagePrefix)) {
            return;
        }

        try {
            CtType<?> referencedType = ref.getTypeDeclaration();
            if (referencedType != null
                    && !(referencedType instanceof CtAnnotation)
                    && !(referencedType instanceof CtAnnotationType)) {

                // Skip type parameters
                if (!(referencedType instanceof CtTypeParameter)) {
                    processType(referencedType);
                }
            }
        } catch (spoon.SpoonException e) {
            // Handle unresolved types in no-classpath mode
        }
    }

    /**
     * Formats a list of annotations to remove package prefixes and brackets
     * @param annotations The list of annotations to format
     * @return A string representation of the annotations without package prefixes
     */
    private String formatAnnotations(List<CtAnnotation<?>> annotations) {
        if (annotations == null || annotations.isEmpty()) {
            return "";
        }

        return annotations.stream()
                .map(annotation -> {
                    String simpleName = annotation.getAnnotationType().getSimpleName();

                    // Get the annotation type declaration to check for default values
                    final CtType<?> annotationType;
                    try {
                        annotationType = annotation.getAnnotationType().getTypeDeclaration();
                    } catch (Exception e) {
                        // Ignore exceptions when trying to get type declaration
                        return "@" + simpleName;
                    }

                    // Sort annotation values to match ClassToString order (value first, then others)
                    Map<String, Object> sortedValues = new LinkedHashMap<>();

                    // Add "value" first if it exists
                    if (annotation.getValues().containsKey("value")) {
                        sortedValues.put("value", annotation.getValues().get("value"));
                    }

                    // Add other values in alphabetical order
                    annotation.getValues().entrySet().stream()
                            .filter(entry -> !entry.getKey().equals("value"))
                            .sorted(Map.Entry.comparingByKey())
                            .forEach(entry -> sortedValues.put(entry.getKey(), entry.getValue()));

                    // Check if all values are default values
                    boolean allDefaultValues = true;
                    Map<String, String> defaultsForType = DEFAULT_VALUES.get(simpleName);

                    if (defaultsForType != null) {
                        for (Map.Entry<String, Object> entry : sortedValues.entrySet()) {
                            String key = entry.getKey();
                            String value = formatAnnotationValue(entry.getValue());

                            // If this value is not a default value, we need to show the annotation
                            if (!defaultsForType.containsKey(key)
                                    || !defaultsForType.get(key).equals(value)) {
                                allDefaultValues = false;
                                break;
                            }
                        }

                        // If all values are default values and we have at least one value, just show the annotation
                        // name
                        if (allDefaultValues && !sortedValues.isEmpty()) {
                            return "@" + simpleName;
                        }
                    }

                    String values = sortedValues.entrySet().stream()
                            .map(entry -> {
                                String key = entry.getKey();
                                Object value = entry.getValue();

                                // Skip null values
                                if (value == null) {
                                    return null;
                                }

                                // For empty arrays, show as []
                                if (value.toString().equals("{}")) {
                                    return key + " = []";
                                }

                                // Check if this is a default value for annotation methods
                                if (annotationType instanceof CtAnnotationType) {
                                    CtAnnotationType annotationTypeDecl = (CtAnnotationType) annotationType;

                                    // Try to find the annotation method with this name
                                    for (Object methodObj : annotationTypeDecl.getMethods()) {
                                        if (methodObj instanceof CtMethod) {
                                            CtMethod<?> method = (CtMethod<?>) methodObj;
                                            if (method.getSimpleName().equals(key)
                                                    && method instanceof CtAnnotationMethod) {
                                                CtAnnotationMethod<?> annotationMethod = (CtAnnotationMethod<?>) method;
                                                if (annotationMethod.getDefaultExpression() != null) {
                                                    String defaultValue = annotationMethod
                                                            .getDefaultExpression()
                                                            .toString();
                                                    String currentValue = value.toString();

                                                    // If the current value equals the default value, skip it
                                                    if (defaultValue.equals(currentValue)
                                                            || (defaultValue
                                                                    .replaceAll("\\s", "")
                                                                    .equals(currentValue.replaceAll("\\s", "")))) {
                                                        return null;
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                // Check if this is a default value for this annotation type
                                if (defaultsForType != null
                                        && defaultsForType.containsKey(key)
                                        && defaultsForType.get(key).equals(formatAnnotationValue(value))) {
                                    return null;
                                }

                                return key + " = " + formatAnnotationValue(value);
                            })
                            .filter(s -> s != null)
                            .collect(java.util.stream.Collectors.joining(", "));

                    return "@" + simpleName + (values.isEmpty() ? "" : "(" + values + ")");
                })
                .collect(java.util.stream.Collectors.joining(" "));
    }

    /**
     * Formats an annotation value to be more readable
     * @param value The annotation value to format
     * @return A formatted string representation of the value
     */
    private String formatAnnotationValue(Object value) {
        if (value == null) {
            return "null";
        }

        String valueStr = value.toString();

        // Handle arrays
        if (valueStr.startsWith("{") && valueStr.endsWith("}")) {
            // Empty array
            if (valueStr.equals("{}")) {
                return "[]";
            }

            // Format array values with proper quotes
            String content = valueStr.substring(1, valueStr.length() - 1).trim();
            if (content.isEmpty()) {
                return "[]";
            }

            // Split by comma, but handle quoted strings properly
            String[] elements = content.split(",");
            StringBuilder result = new StringBuilder("[");

            for (int i = 0; i < elements.length; i++) {
                String element = elements[i].trim();

                // Add quotes for string elements if they don't already have them
                if (!element.startsWith("\"")
                        && !element.endsWith("\"")
                        && !element.equals("true")
                        && !element.equals("false")
                        && !element.matches("\\d+")) {
                    result.append("\"").append(element).append("\"");
                } else {
                    result.append(element);
                }

                if (i < elements.length - 1) {
                    result.append(", ");
                }
            }

            result.append("]");
            return result.toString();
        }

        return valueStr;
    }

    /**
     * Formats a set of modifiers to be more readable without commas
     * @param modifiers The set of modifiers to format
     * @return A formatted string representation of the modifiers
     */
    private String formatModifiers(Set<ModifierKind> modifiers) {
        if (modifiers == null || modifiers.isEmpty()) {
            return "";
        }

        return modifiers.stream()
                .map(ModifierKind::toString)
                .map(String::toLowerCase)
                .collect(java.util.stream.Collectors.joining(" "));
    }

    /**
     * Formats a type reference to include generic type information
     * @param typeRef The type reference to format
     * @return A formatted string representation of the type reference
     */
    private String formatTypeReference(CtTypeReference<?> typeRef) {
        if (typeRef == null) {
            return "void";
        }

        String simpleName = typeRef.getSimpleName();

        // Handle arrays
        if (typeRef.isArray()) {
            return typeRef.getSimpleName().replace("[", "").replace("]", "") + "[]";
        }

        // Handle generic types
        if (!typeRef.getActualTypeArguments().isEmpty()) {
            return simpleName + "<"
                    + typeRef.getActualTypeArguments().stream()
                            .map(this::formatTypeReference)
                            .collect(java.util.stream.Collectors.joining(", "))
                    + ">";
        }

        return simpleName;
    }
}
