package org.llmtoolkit.core;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.service.AiServices;
import java.lang.reflect.Method;
import java.util.function.Consumer;

public interface LlmServiceStrategy {

    /**
     * Creates a service implementation that will handle LLM interactions
     */
    <T> Object createService(Class<T> serviceInterface, ChatLanguageModel model, Consumer<AiServices<?>> customizer);

    /**
     * Resolves the method to be called on the underlying service
     */
    Method resolveServiceMethod(Object service, Method originalMethod) throws NoSuchMethodException;

    /**
     * Prepares the prompt before sending it to the LLM
     */
    String augmentPromptWithOutputInstructions(String prompt, Method method, ReturnTypeInfo typeInfo);

    /**
     * Processes the result returned from the LLM service
     */
    Object convertResult(Object result, ReturnTypeInfo typeInfo);
}
