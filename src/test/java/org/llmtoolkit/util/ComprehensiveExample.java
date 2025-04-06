package org.llmtoolkit.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

/**
 * A comprehensive example demonstrating ClassToString2 with various Java features:
 * - Records with annotations
 * - Interfaces with default and static methods
 * - Classes with static and instance methods
 * - Annotations on classes, methods, parameters, and fields
 * - Generic types
 */
public class ComprehensiveExample {

    // Define some annotations for our example
    @Retention(RetentionPolicy.RUNTIME)
    @Target({
        ElementType.TYPE,
        ElementType.METHOD,
        ElementType.FIELD,
        ElementType.PARAMETER,
        ElementType.RECORD_COMPONENT
    })
    public @interface Info {
        String value();

        String[] tags() default {};

        int priority() default 0;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD, ElementType.PARAMETER})
    public @interface Validate {
        boolean nullable() default false;

        int minLength() default 0;

        int maxLength() default Integer.MAX_VALUE;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE})
    public @interface Entity {
        String table() default "";

        boolean audited() default false;
    }

    // Generic interface with annotations, default and static methods
    @Info(
            value = "Repository interface for data access",
            tags = {"data", "persistence"})
    public interface Repository<T, ID> {
        @Info(value = "Find entity by ID", priority = 1)
        Optional<T> findById(@Validate(nullable = false) ID id);

        @Info(value = "Save entity", priority = 2)
        T save(@Validate T entity);

        @Info(value = "Delete entity", priority = 1)
        void delete(@Validate T entity);

        @Info(value = "Find all entities", priority = 3)
        @Validate(nullable = false)
        List<T> findAll();

        @Info(value = "Count entities", priority = 4)
        default long count() {
            return findAll().size();
        }

        @Info(value = "Check if entity exists", priority = 2)
        default boolean exists(@Validate(nullable = false) ID id) {
            return findById(id).isPresent();
        }

        @Info(value = "Create repository instance", priority = 1)
        static <T, ID> Repository<T, ID> create(
                @Info(value = "Entity finder function") Function<ID, Optional<T>> finder,
                @Info(value = "Entity saver function") Function<T, T> saver) {
            return new Repository<>() {
                @Override
                public Optional<T> findById(ID id) {
                    return finder.apply(id);
                }

                @Override
                public T save(T entity) {
                    return saver.apply(entity);
                }

                @Override
                public void delete(T entity) {
                    // Implementation omitted
                }

                @Override
                public List<T> findAll() {
                    return List.of();
                }
            };
        }
    }

    // Record with annotations on record, components, and methods
    @Entity(table = "persons", audited = true)
    @Info(
            value = "Person record representing a user",
            tags = {"user", "entity"},
            priority = 1)
    public record Person(
            @Info(value = "Unique identifier", priority = 1) @Validate(nullable = false) Long id,
            @Info(value = "Person's full name", priority = 2) @Validate(minLength = 2, maxLength = 100) String name,
            @Info(value = "Person's email address", priority = 2) @Validate(nullable = false) String email,
            @Info(value = "Person's birth date", priority = 3) LocalDate birthDate,
            @Info(value = "Person's roles", priority = 4) List<String> roles) {
        // Validate method with annotations
        @Info(value = "Validate person data", priority = 1)
        public void validate() {
            if (name == null || name.length() < 2) {
                throw new IllegalArgumentException("Name must be at least 2 characters");
            }
            if (email == null || !email.contains("@")) {
                throw new IllegalArgumentException("Invalid email format");
            }
        }

        // Static factory method with annotations
        @Info(value = "Create a new person", priority = 2)
        public static Person create(
                @Info(value = "Person's name") @Validate(minLength = 2) String name,
                @Info(value = "Person's email") @Validate(nullable = false) String email,
                @Info(value = "Person's birth date") LocalDate birthDate) {
            return new Person(null, name, email, birthDate, List.of());
        }

        // Method with generic return type
        @Info(value = "Convert person to map", priority = 3)
        public <K, V> Map<K, V> toMap(
                @Info(value = "Key mapper") Function<String, K> keyMapper,
                @Info(value = "Value mapper") Function<Object, V> valueMapper) {
            return Map.of(
                    keyMapper.apply("id"), valueMapper.apply(id),
                    keyMapper.apply("name"), valueMapper.apply(name),
                    keyMapper.apply("email"), valueMapper.apply(email),
                    keyMapper.apply("birthDate"), valueMapper.apply(birthDate),
                    keyMapper.apply("roles"), valueMapper.apply(roles));
        }
    }

    // Class implementing the interface with annotations
    @Entity(table = "person_repository")
    @Info(
            value = "Implementation of Person repository",
            tags = {"implementation", "repository"},
            priority = 2)
    public static class PersonRepository implements Repository<Person, Long> {
        @Info(value = "In-memory storage", priority = 1)
        private final Map<Long, Person> storage;

        @Info(value = "Next ID for auto-increment", priority = 2)
        private static long nextId = 1;

        public PersonRepository(@Info(value = "Initial data", priority = 1) @Validate Map<Long, Person> initialData) {
            this.storage = initialData;
        }

        @Override
        @Info(value = "Find person by ID", priority = 1)
        public Optional<Person> findById(@Validate(nullable = false) Long id) {
            return Optional.ofNullable(storage.get(id));
        }

        @Override
        @Info(value = "Save person", priority = 2)
        public Person save(@Validate Person person) {
            if (person.id() == null) {
                // Create new person with ID
                Person newPerson =
                        new Person(getNextId(), person.name(), person.email(), person.birthDate(), person.roles());
                storage.put(newPerson.id(), newPerson);
                return newPerson;
            } else {
                // Update existing person
                storage.put(person.id(), person);
                return person;
            }
        }

        @Override
        @Info(value = "Delete person", priority = 1)
        public void delete(@Validate Person person) {
            if (person.id() != null) {
                storage.remove(person.id());
            }
        }

        @Override
        @Info(value = "Find all persons", priority = 3)
        @Validate(nullable = false)
        public List<Person> findAll() {
            return List.copyOf(storage.values());
        }

        @Info(value = "Get next ID", priority = 4)
        private static synchronized long getNextId() {
            return nextId++;
        }

        @Info(value = "Create empty repository", priority = 2)
        public static PersonRepository createEmpty() {
            return new PersonRepository(Map.of());
        }

        @Info(value = "Find by email", priority = 2)
        public Optional<Person> findByEmail(@Info(value = "Email to search") @Validate(nullable = false) String email) {
            return storage.values().stream()
                    .filter(p -> email.equals(p.email()))
                    .findFirst();
        }
    }

    @Test
    void testComprehensiveExample() {

        System.out.println(ClassToString.toString(ComprehensiveExample.class, true, false));
    }
}
