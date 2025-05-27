package org.llmtoolkit.core;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import java.lang.reflect.Method;
import java.util.function.Consumer;

public interface ResponseStructuringStrategy {

    /**
     * Creates a service implementation that will handle LLM interactions
     */
    <T> Object createService(Class<T> serviceInterface, ChatModel model, Consumer<AiServices<?>> customizer);

    /**
     * Invokes the service method with the given prompt
     */
    Object invokeService(Object service, String prompt, Method originalMethod);

    /**
     * Prepares the prompt before sending it to the LLM
     */
    String augmentPromptWithOutputInstructions(String prompt, Method method, ReturnTypeInfo typeInfo);

    /**
     * Processes the result returned from the LLM service
     */
    Object convertResult(Object result, ReturnTypeInfo typeInfo);
}
