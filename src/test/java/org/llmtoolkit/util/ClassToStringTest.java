package org.llmtoolkit.util;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
import org.junit.jupiter.api.Test;

@SuppressWarnings("unused")
public class ClassToStringTest {

    @Retention(RetentionPolicy.RUNTIME)
    @interface TestAnnotation {
        String value() default "";

        int number() default 0;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @interface SimpleAnnotation {}

    @Retention(RetentionPolicy.RUNTIME)
    @interface InheritedAnn {
        String value() default "";
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Inherited
    @interface InheritableAnn {
        String value() default "";
    }

    @InheritableAnn("parent")
    static class Parent {
        @InheritedAnn("parentMethod")
        public void parentMethod() {}
    }

    @InheritedAnn("child")
    static class ChildWithAnnotations extends Parent {
        @TestAnnotation("constructor")
        @InheritedAnn("constructor")
        public ChildWithAnnotations(@SimpleAnnotation @InheritedAnn("param") String name) {}

        @Override
        @TestAnnotation("method")
        @InheritedAnn("method")
        public void parentMethod() {}

        @TestAnnotation("newMethod")
        public void newMethod(@SimpleAnnotation @InheritedAnn("methodParam") String param) {}
    }

    // Test records
    @TestAnnotation("test")
    record SimpleRecord(String name, int value) {}

    record GenericRecord<T>(T data, List<T> items) {}

    record NestedRecord(@TestAnnotation SimpleRecord record, @SimpleAnnotation List<String> list) {}

    // Test interfaces
    interface SimpleInterface {
        void doSomething();

        String getValue();
    }

    interface GenericInterface<T> {
        T process(T input);

        List<T> processAll(List<T> inputs);
    }

    interface ExtendingInterface extends SimpleInterface {
        void additionalMethod();
    }

    @SuppressWarnings("LombokGetterMayBeUsed")
    @TestAnnotation("test")
    static class SimpleClass {
        private final String name;

        @SuppressWarnings("FieldCanBeLocal")
        private final int value;

        SimpleClass(String name, int value) {
            this.name = name;
            this.value = value;
        }

        public String getName() {
            return name;
        }
    }

    static class GenericClass<T> {
        @SuppressWarnings("FieldCanBeLocal")
        private final T data;

        private final List<T> items;

        GenericClass(T data, List<T> items) {
            this.data = data;
            this.items = items;
        }

        @TestAnnotation("process")
        public List<T> processData(@SimpleAnnotation T input) {
            return items;
        }
    }

    static class ExtendingClass extends SimpleClass implements SimpleInterface {
        ExtendingClass(String name, int value) {
            super(name, value);
        }

        @Override
        public void doSomething() {}

        @Override
        public String getValue() {
            return getName();
        }
    }

    @Test
    void testSimpleRecord() {
        String result = ClassToString.toString(SimpleRecord.class, false, false);
        assertTrue(result.contains("@TestAnnotation(\"test\")"));
        assertTrue(result.contains("record SimpleRecord("));
        assertTrue(result.contains("String name"));
        assertTrue(result.contains("int value"));
    }

    @Test
    void testGenericRecord() {
        String result = ClassToString.toString(GenericRecord.class, false, false);
        assertTrue(result.contains("record GenericRecord<T>("));
        assertTrue(result.contains("T data"));
        assertTrue(result.contains("List<T> items"));
    }

    @Test
    void testNestedRecord() {
        String result = ClassToString.toString(NestedRecord.class, false, false);
        assertTrue(result.contains("@TestAnnotation SimpleRecord record"));
        assertTrue(result.contains("@SimpleAnnotation List<String> list"));
    }

    @Test
    void testSimpleInterface() {
        String result = ClassToString.toString(SimpleInterface.class, true, true);
        assertTrue(result.contains("interface SimpleInterface"));
        assertTrue(result.contains("void doSomething();"));
        assertTrue(result.contains("String getValue();"));
    }

    @Test
    void testGenericInterface() {
        String result = ClassToString.toString(GenericInterface.class, true, true);
        assertTrue(result.contains("interface GenericInterface<T>"));
        assertTrue(result.contains("T process("));
        assertTrue(result.contains("List<T> processAll("));
    }

    @Test
    void testExtendingInterface() {
        String result = ClassToString.toString(ExtendingInterface.class, true, true);
        assertTrue(result.contains("interface ExtendingInterface extends SimpleInterface"));
        assertTrue(result.contains("void additionalMethod();"));
    }

    @Test
    void testOnlyRecords() {
        record Inner(String value) {}
        record Outer(Inner inner, String name) {}

        String result = ClassToString.onlyRecords(Outer.class);
        assertTrue(result.contains("record Inner("));
        assertTrue(result.contains("record Outer("));
        assertFalse(result.contains("interface"));
    }

    @Test
    void testQualifiedNestedClassName() {
        String result = ClassToString.toString(OuterClass.InnerRecord.class, true, true);
        assertTrue(result.contains("OuterClass.InnerRecord"));
    }

    // Define this at class level
    static class OuterClass {
        record InnerRecord(String value) {}
    }

    @Test
    void testUnqualifiedNestedClassName() {
        class LocalOuter {
            record LocalInner(String value) {}
        }

        String result = ClassToString.toString(LocalOuter.LocalInner.class, true, false);
        assertTrue(result.contains("LocalInner"));
        assertFalse(result.contains("LocalOuter.LocalInner"));
    }

    @Test
    void testComplexRecord() {
        record ComplexRecord<T, U>(
                @TestAnnotation(value = "test", number = 42) T first,
                @SimpleAnnotation List<U> second,
                List<List<T>> nested) {}

        String result = ClassToString.toString(ComplexRecord.class, true, false);
        assertTrue(result.contains("record ComplexRecord<T, U>"));
        assertTrue(result.contains("@TestAnnotation(value = \"test\", number = 42)"));
        assertTrue(result.contains("@SimpleAnnotation List<U> second"));
        assertTrue(result.contains("List<List<T>> nested"));
    }

    @Test
    void testRecordWithMethods() {
        record RecordWithMethods(String name) {
            public String upperCase() {
                return name.toUpperCase();
            }
        }

        String result = ClassToString.toString(RecordWithMethods.class, true, false);
        assertTrue(result.contains("record RecordWithMethods("));
        assertTrue(result.contains("public String upperCase()"));
        assertFalse(result.contains("public String toString()")); // Should not include generated methods
    }

    @Test
    void testInterfaceHierarchy() {

        interface Base {
            void baseMethod();
        }
        interface Middle extends Base {
            void middleMethod();
        }
        interface Child extends Middle {
            void childMethod();
        }

        String result = ClassToString.toString(Child.class, true, true);
        assertTrue(result.contains("interface Child extends Middle"));
        assertTrue(result.contains("void childMethod();"));
    }

    @Test
    void testSimpleClass() {
        String result = ClassToString.toString(SimpleClass.class, true, false);
        assertTrue(result.contains("@TestAnnotation(\"test\")"));
        assertTrue(result.contains("class SimpleClass"));
        assertTrue(result.contains("public String getName()"));
    }

    @Test
    void testGenericClass() {
        String result = ClassToString.toString(GenericClass.class, true, false);
        assertTrue(result.contains("class GenericClass<T>"));
        assertTrue(result.contains("@TestAnnotation(\"process\")"));
        assertTrue(result.contains("public List<T> processData("));
        assertTrue(result.contains("@SimpleAnnotation T input"));
    }

    @Test
    void testExtendingClass() {
        String result = ClassToString.toString(ExtendingClass.class, true, false);
        assertTrue(result.contains("class ExtendingClass extends SimpleClass implements SimpleInterface"));
        assertTrue(result.contains("public void doSomething()"));
        assertTrue(result.contains("public String getValue()"));
    }

    public static class ClassWithStaticMethods {
        @TestAnnotation("factory")
        public static ClassWithStaticMethods create(String value) {
            return new ClassWithStaticMethods();
        }

        public static void staticHelper(@SimpleAnnotation String input) {}

        public void instanceMethod() {}
    }

    @Test
    void testClassWithStaticMethods() {
        String result = ClassToString.toString(ClassWithStaticMethods.class, true, false);
        assertTrue(result.contains("class ClassWithStaticMethods"));
        assertTrue(result.contains("@TestAnnotation(\"factory\")"));
        assertTrue(result.contains("public static ClassWithStaticMethods create(String value)"));
        assertTrue(result.contains("public static void staticHelper(@SimpleAnnotation String input)"));
        assertTrue(result.contains("public void instanceMethod()"));
    }

    static class ClassWithConstructors {
        @TestAnnotation("constructor")
        public ClassWithConstructors(@SimpleAnnotation String name, int value) {}

        protected ClassWithConstructors() {}
    }

    @Test
    void testClassWithConstructors() {
        String result = ClassToString.toString(ClassWithConstructors.class, true, false);
        assertTrue(result.contains("class ClassWithConstructors"));
        assertTrue(result.contains("@TestAnnotation(\"constructor\")"));
        assertTrue(result.contains("public ClassWithConstructors(@SimpleAnnotation String name, int value)"));
        assertTrue(result.contains("protected ClassWithConstructors()"));
    }

    @Test
    void testAllAnnotations() {
        String result = ClassToString.toString(ChildWithAnnotations.class, true, false);
        // Class annotations (including inherited)
        assertTrue(result.contains("@InheritableAnn(\"parent\")"));
        assertTrue(result.contains("@InheritedAnn(\"child\")"));

        // Constructor annotations
        assertTrue(result.contains("@TestAnnotation(\"constructor\")"));
        assertTrue(result.contains("@InheritedAnn(\"constructor\")"));

        // Constructor parameter annotations
        assertTrue(result.contains("@SimpleAnnotation @InheritedAnn(\"param\") String name"));

        // Method annotations (including inherited and override)
        assertTrue(result.contains("@TestAnnotation(\"method\")"));
        assertTrue(result.contains("@InheritedAnn(\"method\")"));

        // Method parameter annotations
        assertTrue(result.contains("@SimpleAnnotation @InheritedAnn(\"methodParam\") String param"));
    }
}
