package org.llmtoolkit.core;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.service.AiServices;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.function.Consumer;

public interface LlmServiceStrategy {
    /**
     * Creates a service implementation that will handle LLM interactions
     */
    <T> Object createService(Class<T> serviceInterface, ChatLanguageModel model, Consumer<AiServices<?>> customizer);

    /**
     * Prepares the prompt before sending it to the LLM
     */
    String augmentPrompt(String resolvedTemplate, Method method, Type returnType);

    Method resolveServiceMethod(Object service, Method originalMethod) throws NoSuchMethodException;
    /**
     * Processes the result returned from the LLM service
     */
    Object processResult(Object result, Type originalReturnType);
}
