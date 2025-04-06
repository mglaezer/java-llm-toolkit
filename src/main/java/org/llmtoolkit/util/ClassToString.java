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
                } else if (current.isEnum()) {
                    generateEnumDefinition(current, sb, printMethods, qualifyNestedClassNames);
                } else {
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
        // Use a set to track types we've already seen to prevent infinite recursion
        addTypeAndGenerics(type, toProcess, new HashSet<>());
    }

    private static void addTypeAndGenerics(Type type, Queue<Class<?>> toProcess, Set<Type> visited) {
        // Prevent infinite recursion by tracking visited types
        if (visited.contains(type)) {
            return;
        }
        visited.add(type);

        if (type instanceof Class<?> clazz) {
            // Skip void type, primitive types
            if (clazz == void.class || clazz.isPrimitive()) {
                return;
            }
            if (clazz.isArray()) {
                addTypeAndGenerics(clazz.getComponentType(), toProcess, visited);
                return;
            }
            if (!clazz.getName().startsWith(JAVA_PACKAGE_PREFIX)) {
                toProcess.add(clazz);

                // Add nested classes
                for (Class<?> nestedClass : clazz.getDeclaredClasses()) {
                    if (!nestedClass.isSynthetic()) {
                        toProcess.add(nestedClass);
                    }
                }
            }
        } else if (type instanceof ParameterizedType paramType) {
            Type rawType = paramType.getRawType();
            if (rawType instanceof Class<?> rawClass && !rawClass.getName().startsWith(JAVA_PACKAGE_PREFIX)) {
                toProcess.add(rawClass);
            }

            for (Type typeArg : paramType.getActualTypeArguments()) {
                addTypeAndGenerics(typeArg, toProcess, visited);
            }
        } else if (type instanceof WildcardType wildcardType) {
            // Process upper and lower bounds of wildcard types
            for (Type upperBound : wildcardType.getUpperBounds()) {
                if (!upperBound.equals(Object.class)) { // Skip Object.class to prevent recursion
                    addTypeAndGenerics(upperBound, toProcess, visited);
                }
            }
            for (Type lowerBound : wildcardType.getLowerBounds()) {
                addTypeAndGenerics(lowerBound, toProcess, visited);
            }
        } else if (type instanceof GenericArrayType genericArrayType) {
            // Process component type of generic arrays
            addTypeAndGenerics(genericArrayType.getGenericComponentType(), toProcess, visited);
        } else if (type instanceof TypeVariable<?> typeVariable) {
            // Process bounds of type variables
            for (Type bound : typeVariable.getBounds()) {
                if (!bound.equals(Object.class)) { // Skip Object.class to prevent recursion
                    addTypeAndGenerics(bound, toProcess, visited);
                }
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

        appendInterfaces(clazz.getInterfaces(), sb);
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
                if (a.getName().equals(VALUE_METHOD)) return -1;
                if (b.getName().equals(VALUE_METHOD)) return 1;
                return a.getName().compareTo(b.getName());
            });

            Map<String, Object> nonDefaultValues = new LinkedHashMap<>();
            for (Method method : methods) {
                method.setAccessible(true);
                Object value = method.invoke(annotation);
                Object defaultValue = method.getDefaultValue();

                // NOTE: Skipping empty arrays is not strictly correct as they might not be default values.
                // We do this for cleaner output, trading complete accuracy for better readability.
                // In practice, empty arrays in annotations are rarely meaningful when different from null.
                if (value != null && value.getClass().isArray() && Array.getLength(value) == 0) {
                    continue;
                }

                if (value != null && (!value.equals(defaultValue))) {
                    nonDefaultValues.put(method.getName(), value);
                }
            }

            if (!nonDefaultValues.isEmpty()) {
                sb.append("(");
                if (nonDefaultValues.size() == 1 && nonDefaultValues.containsKey(VALUE_METHOD)) {
                    sb.append(formatAnnotationValue(nonDefaultValues.get(VALUE_METHOD)));
                } else {
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
        if (value == null) {
            return "null";
        } else if (value instanceof String) {
            return "\"" + value + "\"";
        } else if (value instanceof Class<?>) {
            return ((Class<?>) value).getSimpleName() + ".class";
        } else if (value instanceof Enum<?>) {
            return value.toString();
        } else if (value.getClass().isArray()) {
            if (value.getClass().getComponentType().isPrimitive()) {
                // Handle primitive arrays
                if (value instanceof boolean[]) {
                    return formatPrimitiveArray(value);
                } else if (value instanceof byte[]) {
                    return formatPrimitiveArray(value);
                } else if (value instanceof char[]) {
                    return formatPrimitiveArray(value);
                } else if (value instanceof double[]) {
                    return formatPrimitiveArray(value);
                } else if (value instanceof float[]) {
                    return formatPrimitiveArray(value);
                } else if (value instanceof int[]) {
                    return formatPrimitiveArray(value);
                } else if (value instanceof long[]) {
                    return formatPrimitiveArray(value);
                } else if (value instanceof short[]) {
                    return formatPrimitiveArray(value);
                }
                return "{}";
            }

            // Handle object arrays with proper formatting
            Object[] array = (Object[]) value;
            if (array.length == 0) {
                return "{}";
            }

            StringBuilder sb = new StringBuilder("{");
            for (int i = 0; i < array.length; i++) {
                Object element = array[i];
                sb.append(formatAnnotationValue(element));
                if (i < array.length - 1) {
                    sb.append(", ");
                }
            }
            sb.append("}");
            return sb.toString();
        }
        return value.toString();
    }

    private static String formatPrimitiveArray(Object array) {
        int length = Array.getLength(array);
        if (length == 0) {
            return "{}";
        }

        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i < length; i++) {
            Object element = Array.get(array, i);
            sb.append(element);
            if (i < length - 1) {
                sb.append(", ");
            }
        }
        sb.append("}");
        return sb.toString();
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
        } else if (type instanceof WildcardType wildcardType) {
            StringBuilder sb = new StringBuilder("?");
            Type[] upperBounds = wildcardType.getUpperBounds();
            Type[] lowerBounds = wildcardType.getLowerBounds();

            if (lowerBounds.length > 0) {
                sb.append(" super ").append(getTypeName(lowerBounds[0], qualifyNestedClassNames));
            } else if (upperBounds.length > 0 && !upperBounds[0].equals(Object.class)) {
                sb.append(" extends ").append(getTypeName(upperBounds[0], qualifyNestedClassNames));
            }

            return sb.toString();
        } else if (type instanceof GenericArrayType genericArrayType) {
            return getTypeName(genericArrayType.getGenericComponentType(), qualifyNestedClassNames) + "[]";
        } else if (type instanceof TypeVariable<?> typeVariable) {
            return typeVariable.getName();
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
        // Skip if this is an annotation interface or extends one
        if (isAnnotationInterface(interfaceClass)) {
            return;
        }

        appendAnnotations(interfaceClass.getAnnotations(), sb);
        appendModifiers(interfaceClass.getModifiers(), sb, true);
        sb.append(interfaceClass.getSimpleName());
        appendTypeParameters(interfaceClass.getTypeParameters(), sb);

        // Handle generic interfaces
        Type[] genericInterfaces = interfaceClass.getGenericInterfaces();
        if (genericInterfaces.length > 0) {
            sb.append(" extends ");
            for (int i = 0; i < genericInterfaces.length; i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(getTypeName(genericInterfaces[i], qualifyNestedClassNames));
            }
        }
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

    private static boolean isAnnotationInterface(Class<?> clazz) {
        if (!clazz.isInterface()) {
            return false;
        }

        // Check if it's an annotation interface directly
        if (clazz.equals(Annotation.class) || clazz.isAnnotation()) {
            return true;
        }

        // Check if it extends an annotation interface
        for (Class<?> iface : clazz.getInterfaces()) {
            if (isAnnotationInterface(iface)) {
                return true;
            }
        }

        return false;
    }

    private static void generateEnumDefinition(
            Class<?> enumClass, StringBuilder sb, boolean printMethods, boolean qualifyNestedClassNames) {
        appendAnnotations(enumClass.getAnnotations(), sb);
        appendModifiers(enumClass.getModifiers() & ~Modifier.FINAL, sb, false);
        sb.append("enum ").append(getTypeName(enumClass, qualifyNestedClassNames));
        appendInterfaces(enumClass.getInterfaces(), sb);
        sb.append(" {\n");

        // Add enum constants
        Object[] constants = enumClass.getEnumConstants();
        if (constants != null && constants.length > 0) {
            for (int i = 0; i < constants.length; i++) {
                sb.append(SINGLE_INDENT).append(constants[i].toString());
                if (i < constants.length - 1) {
                    sb.append(",\n");
                } else {
                    sb.append(";\n");
                }
            }
            sb.append("\n");
        }

        // Add fields
        for (Field field : enumClass.getDeclaredFields()) {
            if (!field.isSynthetic() && !field.isEnumConstant()) {
                appendField(field, sb);
            }
        }

        // Add methods
        if (printMethods) {
            // Add constructors
            for (Constructor<?> constructor : enumClass.getDeclaredConstructors()) {
                if (!constructor.isSynthetic()) {
                    appendConstructor(constructor, sb, qualifyNestedClassNames);
                }
            }

            // Add methods
            for (Method method : enumClass.getDeclaredMethods()) {
                if (!method.isSynthetic()
                        && !method.getName().equals("values")
                        && !method.getName().equals("valueOf")) {
                    appendMethod(method, sb, qualifyNestedClassNames, false);
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

        // Handle generic superclass
        Type genericSuperclass = clazz.getGenericSuperclass();
        if (genericSuperclass != null && !genericSuperclass.equals(Object.class)) {
            sb.append(" extends ").append(getTypeName(genericSuperclass, qualifyNestedClassNames));
        }

        // Handle generic interfaces
        Type[] genericInterfaces = clazz.getGenericInterfaces();
        if (genericInterfaces.length > 0) {
            sb.append(" implements ");
            for (int i = 0; i < genericInterfaces.length; i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(getTypeName(genericInterfaces[i], qualifyNestedClassNames));
            }
        }
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
            for (int i = 0; i < typeParameters.length; i++) {
                TypeVariable<?> typeVar = typeParameters[i];
                sb.append(typeVar.getName());

                // Add bounds if present and not just Object
                Type[] bounds = typeVar.getBounds();
                if (bounds.length > 0 && !bounds[0].equals(Object.class)) {
                    sb.append(" extends ");
                    for (int j = 0; j < bounds.length; j++) {
                        sb.append(getTypeName(bounds[j], true));
                        if (j < bounds.length - 1) {
                            sb.append(" & ");
                        }
                    }
                }

                if (i < typeParameters.length - 1) {
                    sb.append(", ");
                }
            }
            sb.append(">");
        }
    }

    private static void appendInterfaces(Class<?>[] interfaces, StringBuilder sb) {
        if (interfaces.length > 0) {
            sb.append(" ").append("implements").append(" ");
            sb.append(Arrays.stream(interfaces).map(Class::getSimpleName).collect(Collectors.joining(", ")));
        }
    }

    private static void appendField(Field field, StringBuilder sb) {
        // Skip synthetic fields
        if (field.isSynthetic()) {
            return;
        }

        // Add annotations
        appendAnnotations(field.getAnnotations(), sb);

        // Add modifiers
        sb.append(SINGLE_INDENT);
        appendModifiers(field.getModifiers(), sb, false);

        // Add type and name
        sb.append(getTypeName(field.getGenericType(), false))
                .append(" ")
                .append(field.getName())
                .append(";\n");
    }

    private static void appendMethod(
            Method method, StringBuilder sb, boolean qualifyNestedClassNames, boolean isInterface) {
        String methodIndent = SINGLE_INDENT;
        int modifiers = method.getModifiers();
        Class<?> declaringClass = method.getDeclaringClass();

        // Skip record component accessors
        if (declaringClass.isRecord() && isRecordGeneratedMethod(method, declaringClass)) {
            return;
        }

        // Get all annotations including inherited ones
        Set<Annotation> allAnnotations = new LinkedHashSet<>();
        Collections.addAll(allAnnotations, method.getAnnotations());

        // Check for @Override
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

        // Remove TRANSIENT as it's not valid for methods, but appears in some cases
        modifiers &= ~Modifier.TRANSIENT;

        // Handle default methods in interfaces
        boolean isDefault = isInterface
                && (modifiers & Modifier.PUBLIC) != 0
                && !Modifier.isStatic(modifiers)
                && !Modifier.isAbstract(modifiers);

        if (isInterface && !isDefault) {
            modifiers &= ~Modifier.ABSTRACT;
        }

        String modifierStr = Modifier.toString(modifiers);
        if (!modifierStr.isEmpty()) {
            sb.append(modifierStr).append(" ");
        }

        // Add 'default' keyword for default methods in interfaces
        if (isDefault) {
            sb.append("default ");
        }

        // Handle generic type parameters for methods
        TypeVariable<?>[] typeParameters = method.getTypeParameters();
        if (typeParameters.length > 0) {
            sb.append("<");
            for (int i = 0; i < typeParameters.length; i++) {
                TypeVariable<?> typeVar = typeParameters[i];
                sb.append(typeVar.getName());

                // Add bounds if present
                Type[] bounds = typeVar.getBounds();
                if (bounds.length > 0 && !bounds[0].equals(Object.class)) {
                    sb.append(" extends ");
                    for (int j = 0; j < bounds.length; j++) {
                        sb.append(getTypeName(bounds[j], qualifyNestedClassNames));
                        if (j < bounds.length - 1) {
                            sb.append(" & ");
                        }
                    }
                }

                if (i < typeParameters.length - 1) {
                    sb.append(", ");
                }
            }
            sb.append("> ");
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
            sb.append(getTypeName(param.getParameterizedType(), qualifyNestedClassNames));

            // Handle varargs parameters
            if (i == parameters.length - 1 && method.isVarArgs()) {
                // Replace the last [] with ... for varargs
                int lastBracketIndex = sb.lastIndexOf("[");
                if (lastBracketIndex >= 0) {
                    sb.delete(lastBracketIndex, lastBracketIndex + 2);
                    sb.append("...");
                }
            }

            sb.append(" ").append(param.getName());
            if (i < parameters.length - 1) {
                sb.append(", ");
            }
        }
        sb.append(")");

        // Determine if method should have implementation
        boolean needsImplementation = !isInterface
                || // Regular class methods always have implementation
                isDefault
                || // Interface default methods
                Modifier.isStatic(modifiers)
                || // Static methods
                Modifier.isPrivate(modifiers); // Private interface methods

        // Abstract methods never have implementation
        if (Modifier.isAbstract(modifiers)) {
            needsImplementation = false;
        }

        if (needsImplementation) {
            sb.append(" { /* impl */ }");
        } else {
            sb.append(";");
        }
        sb.append("\n");
    }

    private static String formatParameter(Parameter param, boolean qualifyNestedClassNames) {
        if (!param.isNamePresent()) {
            throw new UnsupportedOperationException("Parameter names are not present in "
                    + param.getDeclaringExecutable().getDeclaringClass().getName()
                    + ". Please compile with '-parameters' flag or use Java 21+ "
                    + "(see #compiler-settings in README.md)");
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
