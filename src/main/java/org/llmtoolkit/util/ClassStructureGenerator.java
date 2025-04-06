package org.llmtoolkit.util;

import java.util.*;
import spoon.Launcher;
import spoon.reflect.declaration.*;
import spoon.reflect.reference.CtTypeReference;

public class ClassStructureGenerator {

    private final Set<String> processedClasses = new HashSet<>();
    private final String ignoredPackagePrefix = "java.";
    private final StringBuilder output = new StringBuilder();

    // No longer caching annotation default values

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

        // Skip annotation definitions as requested
        if (type instanceof CtAnnotationType) {
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
        } else if (type instanceof CtAnnotationType) {
            typeKeyword = "@interface";
        } else {
            typeKeyword = "unknown"; // Handle other cases if necessary
        }

        // Add modifiers
        output.append("public ");

        // Add static for nested types, but not for interfaces or annotations
        if (type.isParentInitialized()
                && !(type.getParent() instanceof CtPackage)
                && !(type instanceof CtInterface)
                && !(type instanceof CtAnnotationType)) {
            output.append("static ");
        }

        // Add final for records only if explicitly marked as final
        if (isRecordClass && type.getModifiers().contains(ModifierKind.FINAL)) {
            output.append("final ");
        }

        // Add abstract for abstract classes
        if (type instanceof CtClass && ((CtClass<?>) type).isAbstract()) {
            output.append("abstract ");
        }

        output.append(typeKeyword).append(" ").append(type.getSimpleName());

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
                        .append(" @Validate ")
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
                    if (parameters.size() > 1) {
                        // Multi-line parameter format for methods with multiple parameters
                        for (int i = 0; i < parameters.size(); i++) {
                            CtParameter<?> param = parameters.get(i);
                            if (i > 0) {
                                output.append(",\n                ");
                            }
                            output.append(formatAnnotations(param.getAnnotations()))
                                    .append(" ")
                                    .append(formatTypeReference(param.getType()))
                                    .append(" ")
                                    .append(param.getSimpleName());
                        }
                    } else if (parameters.size() == 1) {
                        // Single-line format for methods with one parameter
                        CtParameter<?> param = parameters.get(0);
                        output.append(formatAnnotations(param.getAnnotations()))
                                .append(" ")
                                .append(formatTypeReference(param.getType()))
                                .append(" ")
                                .append(param.getSimpleName());
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
                    if (parameters.size() > 1) {
                        // Multi-line parameter format for constructors with multiple parameters
                        for (int i = 0; i < parameters.size(); i++) {
                            CtParameter<?> param = parameters.get(i);
                            if (i > 0) {
                                output.append(",\n                ");
                            }
                            output.append(formatAnnotations(param.getAnnotations()))
                                    .append(" ")
                                    .append(formatTypeReference(param.getType()))
                                    .append(" ")
                                    .append(param.getSimpleName());
                        }
                    } else if (parameters.size() == 1) {
                        // Single-line format for constructors with one parameter
                        CtParameter<?> param = parameters.get(0);
                        output.append(formatAnnotations(param.getAnnotations()))
                                .append(" ")
                                .append(formatTypeReference(param.getType()))
                                .append(" ")
                                .append(param.getSimpleName());
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
                if (parameters.size() > 1) {
                    // Multi-line parameter format for methods with multiple parameters
                    for (int i = 0; i < parameters.size(); i++) {
                        CtParameter<?> param = parameters.get(i);
                        if (i > 0) {
                            output.append(",\n                ");
                        }
                        output.append(formatAnnotations(param.getAnnotations()))
                                .append(" ")
                                .append(formatTypeReference(param.getType()))
                                .append(" ")
                                .append(param.getSimpleName());
                    }
                } else if (parameters.size() == 1) {
                    // Single-line format for methods with one parameter
                    CtParameter<?> param = parameters.get(0);
                    output.append(formatAnnotations(param.getAnnotations()))
                            .append(" ")
                            .append(formatTypeReference(param.getType()))
                            .append(" ")
                            .append(param.getSimpleName());
                }
                output.append(")");

                // Add throws clause if present
                if (!method.getThrownTypes().isEmpty()) {
                    output.append(" throws ")
                            .append(method.getThrownTypes().stream()
                                    .map(this::formatTypeReference)
                                    .collect(java.util.stream.Collectors.joining(", ")));
                }

                // For interface methods, add semicolons instead of braces
                if (type instanceof CtInterface) {
                    output.append(";");
                } else {
                    output.append(" {}");
                }

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

        // For interface methods, don't show any modifiers as they're implicitly public
        if (containingType instanceof CtInterface) {
            modifiers.remove(ModifierKind.ABSTRACT);
            modifiers.remove(ModifierKind.PUBLIC);
            return "";
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

                    // Get the annotation's default values
                    final Map<String, String> defaultValues;
                    if (annotationType instanceof CtAnnotationType) {
                        defaultValues = getDefaultValuesForAnnotation(annotationType);
                    } else {
                        defaultValues = new HashMap<>();
                    }

                    // Sort annotation values to match ClassToString order (value first, then others)
                    Map<String, Object> sortedValues = new LinkedHashMap<>();

                    // Add "value" first if it exists
                    if (annotation.getValues().containsKey("value")) {
                        sortedValues.put("value", annotation.getValues().get("value"));
                    }

                    // Add other values in alphabetical order, but skip default values
                    annotation.getValues().entrySet().stream()
                            .filter(entry -> !entry.getKey().equals("value"))
                            .filter(entry -> {
                                // Skip default values
                                String key = entry.getKey();
                                Object value = entry.getValue();
                                String valueStr = formatAnnotationValue(value);

                                // If this is a default value, skip it
                                if (defaultValues.containsKey(key)
                                        && defaultValues.get(key).equals(valueStr)) {
                                    return false;
                                }

                                // If this is an empty array, skip it (common default)
                                if (valueStr.equals("{}") || valueStr.equals("[]")) {
                                    return false;
                                }

                                return true;
                            })
                            .sorted(Map.Entry.comparingByKey())
                            .forEach(entry -> sortedValues.put(entry.getKey(), entry.getValue()));

                    // We've already filtered out default values above, so we can use sortedValues directly

                    // If no parameters are specified, just show the annotation name
                    if (sortedValues.isEmpty()) {
                        return "@" + simpleName;
                    }

                    String values = sortedValues.entrySet().stream()
                            .map(entry -> {
                                String key = entry.getKey();
                                Object value = entry.getValue();
                                return key + " = " + formatAnnotationValue(value);
                            })
                            .collect(java.util.stream.Collectors.joining(", "));

                    return "@" + simpleName + "(" + values + ")";
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

        // Handle special cases for common values
        if (valueStr.equals("2147483647")) {
            // This is Integer.MAX_VALUE, often used as default for maxLength
            return "Integer.MAX_VALUE";
        } else if (valueStr.equals("0")) {
            // Common default for minLength and priority
            return "0";
        } else if (valueStr.equals("false")) {
            // Common default for boolean flags
            return "false";
        } else if (valueStr.equals("true")) {
            // Common default for boolean flags
            return "true";
        } else if (valueStr.equals("{}")) {
            // Empty array
            return "{}";
        }

        // Handle arrays
        if (valueStr.startsWith("{") && valueStr.endsWith("}")) {
            // Empty array
            if (valueStr.equals("{}")) {
                return "{}";
            }

            // Format array values with proper quotes
            String content = valueStr.substring(1, valueStr.length() - 1).trim();
            if (content.isEmpty()) {
                return "{}";
            }

            // Split by comma, but handle quoted strings properly
            String[] elements = content.split(",");
            StringBuilder result = new StringBuilder("{");

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

            result.append("}");
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
     * Gets the default values for an annotation type
     * @param annotationType The annotation type
     * @return A map of default values for the annotation type
     */
    private Map<String, String> getDefaultValuesForAnnotation(CtType<?> annotationType) {
        if (!(annotationType instanceof CtAnnotationType)) {
            return Collections.emptyMap();
        }

        // Compute default values directly without caching
        return computeDefaultValuesForAnnotationType((CtAnnotationType<?>) annotationType);
    }

    /**
     * Computes the default values for an annotation type
     * @param annotationType The annotation type to compute default values for
     * @return A map of default values for the annotation type
     */
    private Map<String, String> computeDefaultValuesForAnnotationType(CtAnnotationType<?> annotationType) {
        Map<String, String> defaultValues = new HashMap<>();

        for (Object methodObj : annotationType.getMethods()) {
            if (methodObj instanceof CtMethod && methodObj instanceof CtAnnotationMethod) {
                CtAnnotationMethod<?> annotationMethod = (CtAnnotationMethod<?>) methodObj;
                if (annotationMethod.getDefaultExpression() != null) {
                    String methodName = annotationMethod.getSimpleName();
                    // Get the default value and format it properly
                    Object defaultExpr = annotationMethod.getDefaultExpression();
                    String defaultValue = formatAnnotationValue(defaultExpr.toString());

                    // Store the default value without any special handling
                    defaultValues.put(methodName, defaultValue);
                }
            }
        }

        return defaultValues;
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
