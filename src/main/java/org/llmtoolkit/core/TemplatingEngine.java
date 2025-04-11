package org.llmtoolkit.core;

import java.lang.reflect.Method;

public interface TemplatingEngine {
    /**
     * Prepares a prompt by processing template and parameters
     *
     * @param method Method to extract template path and parameters from
     * @param args Method invocation arguments
     * @param basePackage Base package for template resolution
     * @return Processed template as string
     */
    String preparePrompt(Method method, Object[] args, Package basePackage);
}
