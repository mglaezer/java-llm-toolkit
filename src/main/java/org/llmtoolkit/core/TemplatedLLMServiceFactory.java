package org.llmtoolkit.core;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.service.AiServices;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;
import lombok.Builder;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.llmtoolkit.core.annotations.PT;
import org.llmtoolkit.util.Do;
import org.llmtoolkit.util.json.JsonUtils;

@Slf4j
@Builder
public class TemplatedLLMServiceFactory {
    @NonNull
    private final ChatLanguageModel model;

    @NonNull
    private final TemplateProcessor templateProcessor;

    private Consumer<AiServices<StringAnswer>> aiServiceCustomizer;
    private boolean isToPrintPrompt;
    private boolean isToPrintAnswer;

    private StringAnswer stringAnswer;

    private void initializeIfNeeded() {
        if (stringAnswer == null) {
            AiServices<StringAnswer> baseBuilder =
                    AiServices.builder(StringAnswer.class).chatLanguageModel(model);
            if (aiServiceCustomizer != null) {
                aiServiceCustomizer.accept(baseBuilder);
                stringAnswer = baseBuilder.build();
            } else {
                stringAnswer = model::chat;
            }
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T create(Class<T> serviceInterface) {

        if (!serviceInterface.isInterface()) {
            throw new IllegalArgumentException("Only interfaces are supported, got: " + serviceInterface.getName());
        }

        initializeIfNeeded();
        validateInterface(serviceInterface);
        return (T) Proxy.newProxyInstance(
                serviceInterface.getClassLoader(), new Class<?>[] {serviceInterface}, new PromptInvocationHandler());
    }

    private <T> void validateInterface(Class<T> serviceInterface) {
        for (Method method : serviceInterface.getDeclaredMethods()) {
            if (method.getDeclaringClass() != Object.class && method.isAnnotationPresent(PT.class)) {
                validateMethod(method);
            }
        }
    }

    private void validateMethod(Method method) {
        extractValueType(method.getGenericReturnType()); // Will throw if invalid
        templateProcessor.validateTemplate(method);
    }

    private class PromptInvocationHandler implements InvocationHandler {

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (method.getDeclaringClass() == Object.class) {
                return method.invoke(this, args);
            }

            String prompt = templateProcessor.preparePrompt(method, args);
            return processPrompt(method, prompt);
        }

        private Object processPrompt(Method method, String prompt) {
            Type returnType = method.getGenericReturnType();
            Class<?> valueType = extractValueType(returnType);

            boolean isList = returnType instanceof ParameterizedType
                    && ((ParameterizedType) returnType).getRawType() == List.class;
            boolean isString = returnType == String.class;

            String wholePrompt = isString
                    ? prompt
                    : prompt + "\n"
                            + (isList
                                    ? OutputInstructions.arrayInstructions(valueType)
                                    : OutputInstructions.singleObjectInstructions(valueType));

            Do printPrompt = Do.once(() -> printPrompt(wholePrompt), isToPrintPrompt);
            String answer = withPrintOnError(() -> stringAnswer.answer(wholePrompt), printPrompt);
            Do printAnswer = Do.once(() -> printAnswer(answer), isToPrintAnswer);

            return withPrintOnError(
                    () -> isList
                            ? JsonUtils.parseJsonOrYamlArray(answer, valueType)
                            : isString ? answer : JsonUtils.parseJsonOrYamlObject(answer, valueType),
                    printPrompt,
                    printAnswer);
        }
    }

    private Class<?> extractValueType(Type returnType) {
        if (returnType instanceof Class<?> clazz) {
            validateReturnType(clazz);
            return clazz;
        } else if (returnType instanceof ParameterizedType paramType) {
            Type[] typeArgs = paramType.getActualTypeArguments();
            if (typeArgs.length == 1 && typeArgs[0] instanceof Class<?> elementType) {
                validateReturnType(elementType);
                return elementType;
            }
        }
        throw new UnsupportedOperationException(
                "Return type must be either a class (e.g., String, CustomClass) or List<Class> (e.g., List<String>). "
                        + "Unsupported types include: Map<K,V>, List<List<T>>, List<?>, generic type parameters.");
    }

    private void validateReturnType(Class<?> type) {
        if (type.isPrimitive()) {
            throw new UnsupportedOperationException("Primitive return types are not supported");
        }
        if (type != String.class
                && (type.getName().startsWith("java.lang.") || type.getName().startsWith("java.util."))) {
            throw new UnsupportedOperationException("Java language and util types (except String) are not supported");
        }
    }

    private <T> T withPrintOnError(Supplier<T> action, Do... printActions) {
        try {
            return action.get();
        } catch (RuntimeException e) {
            for (Do printAction : printActions) {
                printAction.once();
            }
            throw e;
        }
    }

    private static void printPrompt(String wholePrompt) {
        log.info("Prompt:\n{}", wholePrompt);
    }

    private static void printAnswer(String answer) {
        log.info("Answer:\n{}", answer);
    }
}
