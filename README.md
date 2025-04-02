# Java LLM Toolkit

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Java Version](https://img.shields.io/badge/Java-17%2B-orange)](https://openjdk.java.net/)


## Features

## Installation

### Gradle

```gradle
implementation 'org.llmtoolkit:java-llm-toolkit:0.1.0'
```

### Maven

```xml
<dependency>
    <groupId>org.llmtoolkit</groupId>
    <artifactId>llmtoolkit-java</artifactId>
    <version>0.1.0</version>
</dependency>
```

## Quick Start



## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## License

This project is licensed under the Apache License 2.0.

## Requirements

### Java Version
- Java 17 or higher

### Compiler Settings

> **Note**: This requirement is automatically satisfied in Java 21+ without additional configuration.

When using Java 17-20, you need to compile your code with parameter names retained:

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


