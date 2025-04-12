package org.llmtoolkit.core;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.service.AiServices;
import java.lang.reflect.Method;
import java.util.function.Consumer;
import org.llmtoolkit.util.json.JsonUtils;

public class StringAnswerStrategy implements LlmServiceStrategy {

    @Override
    public <T> Object createService(
            Class<T> serviceInterface, ChatLanguageModel model, Consumer<AiServices<?>> customizer) {
        // Create a single StringAnswer service to handle all method calls
        AiServices<StringAnswer> baseBuilder =
                AiServices.builder(StringAnswer.class).chatLanguageModel(model);

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
    public Method resolveServiceMethod(Object service, Method originalMethod) throws NoSuchMethodException {
        // StringAnswer always uses the standard "answer" method
        return service.getClass().getInterfaces()[0].getMethod("answer", String.class);
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
