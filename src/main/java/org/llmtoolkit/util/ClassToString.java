package org.llmtoolkit.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;
import java.util.stream.Collectors;

@SuppressWarnings({"DuplicatedCode", "SpellCheckingInspection"})
public class ClassToString {
    // Constants for indentation
    private static final String BASE_INDENT = "";
    private static final String SINGLE_INDENT = "    ";
    private static final String DOUBLE_INDENT = "        ";
    private static final String JAVA_PACKAGE_PREFIX = "java.";
    private static final String[] RECORD_GENERATED_METHODS = {"toString", "equals", "hashCode"};
    private static final String VALUE_METHOD = "value";

    public static String onlyRecords(Class<?> clazz) {
        return toString(clazz, false, false);
    }

    public static String toString(Class<?> clazz, boolean printMethods, boolean qualifyNestedClassNames) {
        StringBuilder sb = new StringBuilder();
        Set<Class<?>> processed = new HashSet<>();
        Queue<Class<?>> toProcess = new LinkedList<>();
        toProcess.add(clazz);

        while (!toProcess.isEmpty()) {
            Class<?> current = toProcess.poll();
            if (processed.add(current)) {
                addDependentTypes(current, toProcess, printMethods);
                if (current.isRecord()) {
                    generateClassDefinition(current, sb, printMethods, qualifyNestedClassNames);
                } else if (current.isInterface()) {
                    generateInterfaceDefinition(current, sb, printMethods, qualifyNestedClassNames);
                } else if (!current.isEnum()) {
                    generateRegularClassDefinition(current, sb, printMethods, qualifyNestedClassNames);
                }
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    private static void addDependentTypes(Class<?> clazz, Queue<Class<?>> toProcess, boolean includeMethods) {
        // Add interface methods return types and parameter types
        if (includeMethods) {
            for (Method method : clazz.getDeclaredMethods()) {
                if (!method.isSynthetic() && !isRecordGeneratedMethod(method, clazz)) {
                    addTypeAndGenerics(method.getGenericReturnType(), toProcess);
                    for (Parameter param : method.getParameters()) {
                        addTypeAndGenerics(param.getParameterizedType(), toProcess);
                    }
                }
            }
        }

        // Add record components if it's a record
        if (clazz.isRecord() && clazz.getRecordComponents() != null) {
            for (RecordComponent component : clazz.getRecordComponents()) {
                Type genericType = component.getGenericType();
                addTypeAndGenerics(genericType, toProcess);
            }
        }

        for (Class<?> nested : clazz.getDeclaredClasses()) {
            if (!nested.isSynthetic()) {
                toProcess.add(nested);
            }
        }
    }

    private static void addTypeAndGenerics(Type type, Queue<Class<?>> toProcess) {
        if (type instanceof Class<?> clazz) {
            // Skip void type, primitive types, and array classes
            if (clazz == void.class || clazz.isPrimitive() || clazz.isArray()) {
                return;
            }
            if ((clazz.isRecord() || clazz.isInterface() || !clazz.isEnum())
                    && !clazz.getName().startsWith(JAVA_PACKAGE_PREFIX)) {
                toProcess.add(clazz);
            }
        } else if (type instanceof ParameterizedType paramType) {
            Type rawType = paramType.getRawType();
            if (rawType instanceof Class<?> rawClass && !rawClass.getName().startsWith(JAVA_PACKAGE_PREFIX)) {
                if (rawClass.isRecord() || rawClass.isInterface() || !rawClass.isEnum()) {
                    toProcess.add(rawClass);
                }
            }

            for (Type typeArg : paramType.getActualTypeArguments()) {
                addTypeAndGenerics(typeArg, toProcess);
            }
        }
    }

    private static void generateClassDefinition(
            Class<?> clazz, StringBuilder sb, boolean printMethods, boolean qualifyNestedClassNames) {
        // Add ALL annotations, including inherited ones
        appendAnnotations(clazz.getAnnotations(), sb);
        appendModifiers(clazz.getModifiers(), sb, false);
        sb.append("record ").append(getTypeName(clazz, qualifyNestedClassNames));
        appendTypeParameters(clazz.getTypeParameters(), sb);

        RecordComponent[] components = clazz.getRecordComponents();
        sb.append("(\n");
        if (components != null && components.length > 0) {
            for (int i = 0; i < components.length; i++) {
                sb.append(DOUBLE_INDENT).append(formatRecordComponent(components[i], qualifyNestedClassNames));
                if (i < components.length - 1) {
                    sb.append(",");
                }
                sb.append("\n");
            }
            sb.append(SINGLE_INDENT); // Single indent for closing parenthesis
        }
        sb.append(")");

        appendInterfaces(clazz.getInterfaces(), sb, "implements");
        sb.append(" {\n");

        if (printMethods) {
            Arrays.stream(clazz.getDeclaredMethods())
                    .filter(method -> !method.isSynthetic() && !isRecordGeneratedMethod(method, clazz))
                    .forEach(method -> appendMethod(method, sb, qualifyNestedClassNames, false));
        }

        sb.append(BASE_INDENT).append("}\n");
    }

    private static String formatRecordComponent(RecordComponent component, boolean qualifyNestedClassNames) {
        StringBuilder sb = new StringBuilder();
        // Get ALL annotations using helper method to include field and parameter annotations
        Set<Annotation> allAnnotations = collectRecordComponentAnnotations(component);

        for (Annotation annotation : allAnnotations) {
            sb.append(formatAnnotation(annotation)).append(" ");
        }

        Type type = component.getGenericType();
        sb.append(getTypeName(type, qualifyNestedClassNames)).append(" ").append(component.getName());
        return sb.toString();
    }

    private static Set<Annotation> collectRecordComponentAnnotations(RecordComponent component) {
        Set<Annotation> allAnnotations = new LinkedHashSet<>();
        addComponentAnnotations(component, allAnnotations);
        addFieldAnnotations(component, allAnnotations);
        addTypeAnnotations(component, allAnnotations);
        addConstructorParameterAnnotations(component, allAnnotations);
        addAccessorMethodAnnotations(component, allAnnotations);
        return allAnnotations;
    }

    private static void addComponentAnnotations(RecordComponent component, Set<Annotation> annotations) {
        Collections.addAll(annotations, component.getAnnotations());
    }

    private static void addFieldAnnotations(RecordComponent component, Set<Annotation> annotations) {
        try {
            Field field = component.getDeclaringRecord().getDeclaredField(component.getName());
            Collections.addAll(annotations, field.getAnnotations());
        } catch (NoSuchFieldException e) {
            // Ignore if field not found
        }
    }

    private static void addTypeAnnotations(RecordComponent component, Set<Annotation> annotations) {
        Type genericType = component.getGenericType();
        if (genericType instanceof AnnotatedType annotatedType) {
            Collections.addAll(annotations, annotatedType.getAnnotations());
        }
    }

    private static void addConstructorParameterAnnotations(RecordComponent component, Set<Annotation> annotations) {
        try {
            for (Constructor<?> constructor : component.getDeclaringRecord().getDeclaredConstructors()) {
                Parameter[] parameters = constructor.getParameters();
                RecordComponent[] components = component.getDeclaringRecord().getRecordComponents();

                if (parameters.length == components.length) {
                    for (int i = 0; i < components.length; i++) {
                        if (components[i].getName().equals(component.getName())) {
                            Collections.addAll(annotations, parameters[i].getAnnotations());
                            addParameterTypeAnnotations(parameters[i], annotations);
                            break;
                        }
                    }
                    break;
                }
            }
        } catch (Exception e) {
            // Ignore if we can't get constructor parameters
        }
    }

    private static void addParameterTypeAnnotations(Parameter parameter, Set<Annotation> annotations) {
        AnnotatedType annotatedParamType = parameter.getAnnotatedType();
        if (annotatedParamType != null) {
            Collections.addAll(annotations, annotatedParamType.getAnnotations());
        }
    }

    private static void addAccessorMethodAnnotations(RecordComponent component, Set<Annotation> annotations) {
        try {
            Method accessorMethod = component.getDeclaringRecord().getDeclaredMethod(component.getName());
            Collections.addAll(annotations, accessorMethod.getAnnotations());
        } catch (NoSuchMethodException e) {
            // Ignore if accessor method not found
        }
    }

    private static String formatAnnotation(Annotation annotation) {
        String annotationClassName = annotation.annotationType().getSimpleName();
        Method[] methods = annotation.annotationType().getDeclaredMethods();

        if (methods.length == 0) {
            return "@" + annotationClassName;
        }

        StringBuilder sb = new StringBuilder("@").append(annotationClassName);

        try {
            // Sort methods to ensure consistent ordering
            methods = methods.clone();
            Arrays.sort(methods, (a, b) -> {
                // Put "value" first, then sort alphabetically
                if (a.getName().equals(VALUE_METHOD)) return -1;
                if (b.getName().equals(VALUE_METHOD)) return 1;
                return a.getName().compareTo(b.getName());
            });

            Map<String, Object> nonDefaultValues = new LinkedHashMap<>();
            for (Method method : methods) {
                method.setAccessible(true);
                Object value = method.invoke(annotation);
                Object defaultValue = method.getDefaultValue();
                if (value != null && (!value.equals(defaultValue))) {
                    nonDefaultValues.put(method.getName(), value);
                }
            }

            if (!nonDefaultValues.isEmpty()) {
                sb.append("(");
                if (nonDefaultValues.size() == 1 && nonDefaultValues.containsKey(VALUE_METHOD)) {
                    // If "value" is the only parameter, just print its value without the name
                    sb.append(formatAnnotationValue(nonDefaultValues.get(VALUE_METHOD)));
                } else {
                    // Print all parameters with their names
                    boolean first = true;
                    for (Map.Entry<String, Object> entry : nonDefaultValues.entrySet()) {
                        if (!first) {
                            sb.append(", ");
                        }
                        sb.append(entry.getKey()).append(" = ").append(formatAnnotationValue(entry.getValue()));
                        first = false;
                    }
                }
                sb.append(")");
            }
        } catch (Exception e) {
            return "@" + annotationClassName;
        }

        return sb.toString();
    }

    private static String formatAnnotationValue(Object value) {
        if (value instanceof String) {
            return "\"" + value + "\"";
        } else if (value instanceof Class<?>) {
            return ((Class<?>) value).getSimpleName() + ".class";
        } else if (value instanceof Enum<?>) {
            return value.toString();
        } else if (value.getClass().isArray()) {
            return Arrays.deepToString((Object[]) value);
        }
        return value.toString();
    }

    private static String getTypeName(Type type, boolean qualifyNestedClassNames) {
        if (type instanceof Class<?> clazz) {
            if (clazz.isArray()) {
                return getTypeName(clazz.getComponentType(), qualifyNestedClassNames) + "[]";
            }
            // For nested classes, include the enclosing class name if configured
            if (qualifyNestedClassNames && clazz.getEnclosingClass() != null && !clazz.isAnonymousClass()) {
                return getTypeName(clazz.getEnclosingClass(), true) + "." + clazz.getSimpleName();
            }
            return clazz.getSimpleName();
        } else if (type instanceof ParameterizedType paramType) {
            Type rawType = paramType.getRawType();
            Type[] typeArgs = paramType.getActualTypeArguments();

            return getTypeName(rawType, qualifyNestedClassNames) + "<"
                    + Arrays.stream(typeArgs)
                            .map(t -> getTypeName(t, qualifyNestedClassNames))
                            .collect(Collectors.joining(", "))
                    + ">";
        }
        return type.getTypeName();
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private static boolean isRecordGeneratedMethod(Method method, Class<?> recordClass) {
        if (Arrays.asList(RECORD_GENERATED_METHODS).contains(method.getName())) {
            return true;
        }

        RecordComponent[] components = recordClass.getRecordComponents();
        if (components == null) {
            return false;
        }

        return Arrays.stream(components).anyMatch(comp -> isComponentAccessor(comp, method));
    }

    private static boolean isComponentAccessor(RecordComponent component, Method method) {
        return component.getName().equals(method.getName())
                && method.getParameterCount() == 0
                && method.getReturnType().equals(component.getType());
    }

    private static void generateInterfaceDefinition(
            Class<?> interfaceClass, StringBuilder sb, boolean printMethods, boolean qualifyNestedClassNames) {
        appendAnnotations(interfaceClass.getAnnotations(), sb);
        appendModifiers(interfaceClass.getModifiers(), sb, true);
        sb.append(interfaceClass.getSimpleName());
        appendTypeParameters(interfaceClass.getTypeParameters(), sb);
        appendInterfaces(interfaceClass.getInterfaces(), sb, "extends");
        sb.append(" {\n");

        if (printMethods) {
            Method[] methods = interfaceClass.getDeclaredMethods();
            for (int i = 0; i < methods.length; i++) {
                Method method = methods[i];
                if (!method.isSynthetic()) {
                    appendMethod(method, sb, qualifyNestedClassNames, true);
                    if (i < methods.length - 1) {
                        sb.append("\n");
                    }
                }
            }
        }

        sb.append(BASE_INDENT).append("}\n");
    }

    private static void generateRegularClassDefinition(
            Class<?> clazz, StringBuilder sb, boolean printMethods, boolean qualifyNestedClassNames) {
        appendAnnotations(clazz.getAnnotations(), sb);
        appendModifiers(clazz.getModifiers(), sb, false);
        sb.append("class ").append(getTypeName(clazz, qualifyNestedClassNames));
        appendTypeParameters(clazz.getTypeParameters(), sb);

        Class<?> superclass = clazz.getSuperclass();
        if (superclass != null && superclass != Object.class) {
            sb.append(" extends ").append(getTypeName(superclass, qualifyNestedClassNames));
        }

        appendInterfaces(clazz.getInterfaces(), sb, "implements");
        sb.append(" {\n");

        if (printMethods) {
            // Print constructors
            Arrays.stream(clazz.getDeclaredConstructors())
                    .filter(constructor -> !constructor.isSynthetic())
                    .forEach(constructor -> appendConstructor(constructor, sb, qualifyNestedClassNames));

            // Print methods
            Arrays.stream(clazz.getDeclaredMethods())
                    .filter(method -> !method.isSynthetic())
                    .forEach(method -> appendMethod(method, sb, qualifyNestedClassNames, false));
        }

        sb.append(BASE_INDENT).append("}\n");
    }

    private static void appendConstructor(
            Constructor<?> constructor, StringBuilder sb, boolean qualifyNestedClassNames) {
        String methodIndent = SINGLE_INDENT;

        // Get all annotations including inherited ones
        Set<Annotation> allAnnotations = new LinkedHashSet<>();
        Collections.addAll(allAnnotations, constructor.getAnnotations());

        for (Annotation annotation : allAnnotations) {
            sb.append(methodIndent).append(formatAnnotation(annotation)).append("\n");
        }

        sb.append(methodIndent);

        String modifierStr = Modifier.toString(constructor.getModifiers());
        if (!modifierStr.isEmpty()) {
            sb.append(modifierStr).append(" ");
        }

        // Use simple name for constructor
        sb.append(constructor.getDeclaringClass().getSimpleName());

        // Handle parameters
        sb.append("(");
        Parameter[] parameters = constructor.getParameters();
        for (int i = 0; i < parameters.length; i++) {
            sb.append(formatParameter(parameters[i], qualifyNestedClassNames));
            if (i < parameters.length - 1) {
                sb.append(", ");
            }
        }
        sb.append(") {}\n");
    }

    // Helper methods for common operations

    private static void appendAnnotations(Annotation[] annotations, StringBuilder sb) {
        // Get all annotations including inherited ones
        Set<Annotation> allAnnotations = new LinkedHashSet<>();
        Collections.addAll(allAnnotations, annotations);

        for (Annotation annotation : allAnnotations) {
            sb.append(ClassToString.BASE_INDENT)
                    .append(formatAnnotation(annotation))
                    .append("\n");
        }
    }

    private static void appendModifiers(int modifiers, StringBuilder sb, boolean isInterface) {
        String modifierStr = Modifier.toString(modifiers);
        // Remove redundant 'abstract' modifier for interfaces
        if (isInterface) {
            modifierStr = modifierStr.replace("abstract interface", "interface");
        }

        if (!modifierStr.isEmpty()) {
            sb.append(ClassToString.BASE_INDENT).append(modifierStr).append(" ");
        }
    }

    private static void appendTypeParameters(TypeVariable<?>[] typeParameters, StringBuilder sb) {
        if (typeParameters.length > 0) {
            sb.append("<");
            sb.append(Arrays.stream(typeParameters).map(Type::getTypeName).collect(Collectors.joining(", ")));
            sb.append(">");
        }
    }

    private static void appendInterfaces(Class<?>[] interfaces, StringBuilder sb, String keyword) {
        if (interfaces.length > 0) {
            sb.append(" ").append(keyword).append(" ");
            sb.append(Arrays.stream(interfaces).map(Class::getSimpleName).collect(Collectors.joining(", ")));
        }
    }

    private static void appendMethod(
            Method method, StringBuilder sb, boolean qualifyNestedClassNames, boolean isInterface) {
        String methodIndent = SINGLE_INDENT;

        // Get all annotations including inherited ones
        Set<Annotation> allAnnotations = new LinkedHashSet<>();
        Collections.addAll(allAnnotations, method.getAnnotations());

        // Check for @Override
        Class<?> declaringClass = method.getDeclaringClass();
        String methodName = method.getName();
        Class<?>[] paramTypes = method.getParameterTypes();

        // Check superclass for override
        Class<?> superclass = declaringClass.getSuperclass();
        boolean isOverride = false;
        while (superclass != null && !isOverride) {
            try {
                superclass.getDeclaredMethod(methodName, paramTypes);
                isOverride = true;
            } catch (NoSuchMethodException e) {
                superclass = superclass.getSuperclass();
            }
        }

        // Check interfaces for override
        if (!isOverride) {
            for (Class<?> iface : declaringClass.getInterfaces()) {
                try {
                    iface.getDeclaredMethod(methodName, paramTypes);
                    isOverride = true;
                    break;
                } catch (NoSuchMethodException ignored) {
                }
            }
        }

        // Add @Override if method is actually overriding
        if (isOverride) {
            sb.append(methodIndent).append("@Override\n");
        }

        // Add other annotations
        for (Annotation annotation : allAnnotations) {
            sb.append(methodIndent).append(formatAnnotation(annotation)).append("\n");
        }

        sb.append(methodIndent);

        int methodModifiers = method.getModifiers();
        String modifierStr;
        if (isInterface) {
            modifierStr = Modifier.toString(methodModifiers & ~Modifier.ABSTRACT);
        } else {
            modifierStr = Modifier.toString(methodModifiers);
        }
        if (!modifierStr.isEmpty()) {
            sb.append(modifierStr).append(" ");
        }

        sb.append(getTypeName(method.getGenericReturnType(), qualifyNestedClassNames))
                .append(" ")
                .append(method.getName());

        sb.append("(");
        Parameter[] parameters = method.getParameters();
        for (int i = 0; i < parameters.length; i++) {
            Parameter param = parameters[i];
            for (Annotation annotation : param.getAnnotations()) {
                sb.append(formatAnnotation(annotation)).append(" ");
            }
            sb.append(getTypeName(param.getParameterizedType(), qualifyNestedClassNames))
                    .append(" ")
                    .append(param.getName());
            if (i < parameters.length - 1) {
                sb.append(", ");
            }
        }
        sb.append(")");

        if (isInterface) {
            sb.append(";\n");
        } else {
            sb.append(" {}\n");
        }
    }

    private static String formatParameter(Parameter param, boolean qualifyNestedClassNames) {
        if (!param.isNamePresent()) {
            throw new UnsupportedOperationException("Parameter names are not present in "
                    + param.getDeclaringExecutable().getDeclaringClass().getName()
                    + ". Please compile with '-parameters' flag or use Java 21+");
        }

        StringBuilder sb = new StringBuilder();

        // Get all annotations including type annotations
        Set<Annotation> allAnnotations = new LinkedHashSet<>();
        Collections.addAll(allAnnotations, param.getAnnotations());
        Collections.addAll(allAnnotations, param.getAnnotatedType().getAnnotations());

        for (Annotation annotation : allAnnotations) {
            sb.append(formatAnnotation(annotation)).append(" ");
        }

        sb.append(getTypeName(param.getParameterizedType(), qualifyNestedClassNames))
                .append(" ")
                .append(param.getName());
        return sb.toString();
    }
}
