package org.llmtoolkit.core;

import static org.llmtoolkit.core.TemplatedLLMServiceFactory.extractValueType;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.service.AiServices;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
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
            Consumer<AiServices<StringAnswer>> adapter = customizer::accept;
            adapter.accept(baseBuilder);
        }

        return baseBuilder.build();
    }

    @Override
    public String augmentPrompt(String resolvedTemplate, Method method, Type returnType) {
        Class<?> valueType = extractValueType(returnType);

        boolean isList =
                returnType instanceof ParameterizedType && ((ParameterizedType) returnType).getRawType() == List.class;
        boolean isString = returnType == String.class;

        if (isString) {
            return resolvedTemplate;
        } else {
            return resolvedTemplate + "\n"
                    + (isList
                            ? OutputInstructions.arrayInstructions(valueType)
                            : OutputInstructions.singleObjectInstructions(valueType));
        }
    }

    @Override
    public Method resolveServiceMethod(Object service, Method originalMethod) throws NoSuchMethodException {
        // StringAnswer always uses the standard "answer" method
        return service.getClass().getInterfaces()[0].getMethod("answer", String.class);
    }

    @Override
    public Object processResult(Object result, Type originalReturnType) {
        if (result == null) {
            return null;
        }

        String answer = (String) result;

        Class<?> valueType = extractValueType(originalReturnType);
        boolean isList = originalReturnType instanceof ParameterizedType
                && ((ParameterizedType) originalReturnType).getRawType() == List.class;
        boolean isString = originalReturnType == String.class;

        if (isString) {
            return answer;
        } else if (isList) {
            return JsonUtils.parseJsonOrYamlArray(answer, valueType);
        } else {
            return JsonUtils.parseJsonOrYamlObject(answer, valueType);
        }
    }
}
