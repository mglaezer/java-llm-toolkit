# Java LLM Toolkit

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Java Version](https://img.shields.io/badge/Java-17%2B-orange)](https://openjdk.java.net/)

## Introduction

A Java toolkit that simplifies working with Large Language Models through powerful templating, structured outputs with additional semantic context, and robust JSON/YAML handling. 

Works standalone or integrates with LangChain4j for features like RAG, tools and memory.

### JTE (Java Template Engine) based Prompt composition

The toolkit provides a comprehensive templating system that goes beyond basic string templates:

- **[JTE (Java Template Engine)](https://github.com/casid/jte) Integration**:
  - JTE supports complex template composition with includes, loops, and conditionals
  - IDE plugins provide validation of template parameters
  - Templates are stored as `.jte` files alongside Java code or in the resource folder
  - Uses annotation-based binding of method parameters to template variables
  - Enforces strict validation of parameter-to-template mapping
  
### Structured output with additional semantic context

The toolkit transforms Java code structure directly into LLM output instructions:

- **Java Record-based Output Format**:
  - Uses Java bean definitions as LLM output instructions
  - Leverages annotations to provide additional semantic context to LLMs
  - LLMs tend to produce better results with annotated Java beans used in output instructions compared to JSON Schema or examples approach

### JSON and YAML Processing

Robust handling of LLM responses with automatic repair capabilities:

- **Smart Parser and Repair**:
  - Handles malformed JSON from various LLM providers
  - Fixes syntax issues like missing commas, unmatched parentheses, and incomplete JSON structures
  - Removes markdown artifacts
  - Provides convenient abstractions for conversion between JSON, YAML, and Java objects

### LangChain4j Integration

Optional integration with LangChain4j ecosystem:

- **Flexible Usage**:
  - Works as standalone toolkit or LangChain4j extension
  - Supports LangChain4j chat models
  - Enables LangChain4j features like RAG (Retrieval Augmented Generation), memory, and tools


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


