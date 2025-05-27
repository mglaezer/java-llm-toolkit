package org.llmtoolkit.core;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import java.lang.reflect.Method;
import java.util.function.Consumer;
import org.llmtoolkit.util.json.JsonUtils;

public class JacksonSourceResponseStructuringStrategy implements ResponseStructuringStrategy {

    @Override
    public <T> Object createService(Class<T> serviceInterface, ChatModel model, Consumer<AiServices<?>> customizer) {
        // Create a single StringAnswer service to handle all method calls
        AiServices<StringAnswer> baseBuilder =
                AiServices.builder(StringAnswer.class).chatModel(model);

        if (customizer != null) {
            customizer.accept(baseBuilder);
        }

        return baseBuilder.build();
    }

    @Override
    public String augmentPromptWithOutputInstructions(String prompt, Method method, ReturnTypeInfo typeInfo) {
        if (typeInfo.isString()) {
            return prompt;
        }

        return prompt + "\n"
                + (typeInfo.isList()
                        ? OutputInstructions.arrayInstructions(typeInfo.getValueType())
                        : OutputInstructions.singleObjectInstructions(typeInfo.getValueType()));
    }

    @Override
    public Object invokeService(Object service, String prompt, Method originalMethod) {
        return ((StringAnswer) service).answer(prompt);
    }

    @Override
    public Object convertResult(Object result, ReturnTypeInfo typeInfo) {
        if (result == null) {
            return null;
        }

        String answer = (String) result;

        if (typeInfo.isString()) {
            return answer;
        } else if (typeInfo.isList()) {
            return JsonUtils.parseJsonOrYamlArray(answer, typeInfo.getValueType());
        } else {
            return JsonUtils.parseJsonOrYamlObject(answer, typeInfo.getValueType());
        }
    }
}
