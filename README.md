# Java LLM Toolkit

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Java Version](https://img.shields.io/badge/Java-17%2B-orange)](https://openjdk.java.net/)

## Introduction

Java LLM Toolkit provides capabilities for working with Large Language Models in Java applications, with optional LangChain4j integration.

### JTE (Java Template Engine) based Prompt composition

The toolkit provides a comprehensive templating system that goes beyond LangChain4j's capabilities:

- **[JTE (Java Template Engine)](https://github.com/casid/jte) Integration**: Uses the JTE for structured prompt templates
  - JTE supports complex template composition with includes, loops, conditionals, etc
  - JTE IDE plugin provides validation of template parameters
  - Templates are stored as separate files with `.jte` extension near java code or in resources directory
  - Annotation-based binding of method parameters to template variables
  - Strict validation of 1:1 mapping between method parameters and template variables
  
### Structured Output Handling

The toolkit offers a unique approach to structured output handling:

- **Java Record-based Output Format Instructions**:
  - Uses Java bean definitions directly as output format instructions for LLMs
  - Automatically generates output instructions based on Java record structure
  - Provides better results compared to JSON Schema or examples approach
  - Annotations on classes, records, and fields are visible to the LLM
  - Annotations provide more direct and effective field-level semantic instructions compared to JSON Schema or other approaches

### JSON and YAML Handling

The toolkit provides robust JSON and YAML handling capabilities:

  - Falls back to JSON repair mechanisms if standard parsing fails
  - Provides advanced repair functionality for malformed JSON returned by some LLMs
  - Fixes common LLM response issues like missing parts of the syntax, duplicate commas, trailing commas, etc
  - Removes markdown wrappers and cleans up formatting issues
  - Provides convenient abstractions for converting java objects to JSON and YAML and back.

### Additional Features

  - Can work with or without LangChain4j, either replacing it or extending it.
  - Compatible with LangChain4j's chat models, memory, RAG, and tools


## Quick Start

See [examples](src/test/java/org/llmtoolkit/examples) for common usage patterns and best practices. The examples demonstrate:
- Template-based prompts with JTE
- Structured output handling using Java records
- Integration with LangChain4j features
- Low-level API usage

## Installation

Follow these instructions on JitPack:

[![](https://jitpack.io/v/mglaezer/java-llm-toolkit.svg)](https://jitpack.io/#mglaezer/java-llm-toolkit)

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## License

This project is licensed under the Apache License 2.0.

## Requirements

### Java Version
- Java 17 or higher

### Compiler Settings

> **Note**: This requirement is automatically satisfied in Java 21+ without additional configuration.

When using Java 17-20,  might get "Parameter names are not present in ... Please compile with '-parameters' flag or use Java 21+" exception.

If that happens, you need to compile your code with parameter names retained:

#### Gradle
```groovy
tasks.withType(JavaCompile) {
    options.compilerArgs += ['-parameters']
}
```

#### Maven
```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
        <parameters>true</parameters>
    </configuration>
</plugin>
```


