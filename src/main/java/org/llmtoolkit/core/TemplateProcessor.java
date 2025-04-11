package org.llmtoolkit.core;

import java.lang.reflect.Method;

public interface TemplateProcessor {
    /**
     * Validates template existence and structure without processing parameters
     *
     * @param method Method to extract template path from
     * @param basePackage Base package for template resolution
     * @throws IllegalArgumentException if template is invalid
     */
    void validateTemplate(Method method, Package basePackage);

    /**
     * Prepares a prompt by processing template and parameters
     *
     * @param method Method to extract template path and parameters from
     * @param args Method invocation arguments
     * @param basePackage Base package for template resolution
     * @return Processed template as string
     * @throws IllegalArgumentException if parameters are invalid
     */
    String preparePrompt(Method method, Object[] args, Package basePackage);
}
