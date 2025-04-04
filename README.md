# Java LLM Toolkit

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Java Version](https://img.shields.io/badge/Java-17%2B-orange)](https://openjdk.java.net/)

## Introduction

A Java toolkit that simplifies working with Large Language Models through powerful templating, structured outputs, and robust JSON/YAML handling. Works standalone or integrates with LangChain4j for advanced features like RAG and memory.

### JTE (Java Template Engine) based Prompt composition

The toolkit provides a comprehensive templating system that goes beyond basic string templates:

- **[JTE (Java Template Engine)](https://github.com/casid/jte) Integration**:
  - Supports complex template composition with includes, loops, and conditionals
  - Provides IDE-assisted validation of template parameters
  - Stores templates as `.jte` files alongside Java code
  - Uses annotation-based binding of method parameters to template variables
  - Enforces strict validation of parameter-to-template mapping
  
### Structured Output Handling

The toolkit transforms Java code structure directly into LLM output instructions:

- **Java Record-based Output Format**:
  - Uses Java bean definitions as LLM output instructions
  - Generates format specifications from Java record structure
  - Leverages annotations to provide additional semantic context to LLMs
  - Offers better accuracy compared to JSON Schema approach
  - Supports field-level output validation and repair

### JSON and YAML Processing

Robust handling of LLM responses with automatic repair capabilities:

- **Smart Parser and Repair**:
  - Handles malformed JSON from various LLM providers
  - Fixes syntax issues like missing commas, unmatched parentheses, and incomplete JSON structures
  - Removes markdown artifacts and formatting inconsistencies
  - Provides clean conversion between JSON, YAML, and Java objects
  - Includes validation to ensure parsing consistency

### LangChain4j Integration

Optional integration with LangChain4j ecosystem:

- **Flexible Usage**:
  - Works as standalone toolkit or LangChain4j extension
  - Supports LangChain4j chat models
  - Enables RAG (Retrieval Augmented Generation)
  - Provides access to conversation memory
  - Integrates with LangChain4j tools


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


