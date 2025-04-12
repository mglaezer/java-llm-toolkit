package org.llmtoolkit.core;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.service.AiServices;
import java.lang.reflect.*;
import java.util.function.Consumer;
import java.util.function.Supplier;
import lombok.Builder;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.llmtoolkit.core.annotations.PT;
import org.llmtoolkit.util.Do;

@Slf4j
@Builder
public class TemplatedLLMServiceFactory {
    @NonNull
    private final ChatLanguageModel model;

    @NonNull
    private final TemplateProcessor templateProcessor;

    private Consumer<AiServices<?>> aiServiceCustomizer;
    private boolean isToPrintPrompt;
    private boolean isToPrintAnswer;

    @Builder.Default
    private LlmServiceStrategy serviceStrategy = new StringAnswerStrategy();

    @SuppressWarnings("unchecked")
    public <T> T create(Class<T> serviceInterface) {
        if (!serviceInterface.isInterface()) {
            throw new IllegalArgumentException("Only interfaces are supported, got: " + serviceInterface.getName());
        }

        validateInterface(serviceInterface);

        Object service = serviceStrategy.createService(serviceInterface, model, aiServiceCustomizer);

        return (T) Proxy.newProxyInstance(
                serviceInterface.getClassLoader(),
                new Class<?>[] {serviceInterface},
                new ServiceInvocationHandler(service));
    }

    private <T> void validateInterface(Class<T> serviceInterface) {
        for (Method method : serviceInterface.getDeclaredMethods()) {
            if (method.getDeclaringClass() != Object.class && method.isAnnotationPresent(PT.class)) {
                validateMethod(method);
            }
        }
    }

    private void validateMethod(Method method) {
        extractValueType(method.getGenericReturnType());
        templateProcessor.validateTemplate(method);
    }

    static Class<?> extractValueType(Type returnType) {
        //noinspection DuplicatedCode
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

    private static void validateReturnType(Class<?> type) {
        if (type.isPrimitive()) {
            throw new UnsupportedOperationException("Primitive return types are not supported");
        }
        if (type != String.class
                && (type.getName().startsWith("java.lang.") || type.getName().startsWith("java.util."))) {
            throw new UnsupportedOperationException("Java language and util types (except String) are not supported");
        }
    }

    private class ServiceInvocationHandler implements InvocationHandler {
        private final Object service;

        public ServiceInvocationHandler(Object service) {
            this.service = service;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (method.getDeclaringClass() == Object.class) {
                return method.invoke(this, args);
            }

            String templatePrompt = templateProcessor.preparePrompt(method, args);

            final String processedPrompt =
                    serviceStrategy.augmentPrompt(templatePrompt, method, method.getGenericReturnType());

            Do printPrompt = Do.once(() -> printPrompt(processedPrompt), isToPrintPrompt);

            Method serviceMethod = serviceStrategy.resolveServiceMethod(service, method);
            Object rawResult = withPrintOnError(
                    () -> {
                        try {
                            return serviceMethod.invoke(service, processedPrompt);
                        } catch (Exception e) {
                            throw new RuntimeException("Error invoking service", e);
                        }
                    },
                    printPrompt);

            final Object processedResult = serviceStrategy.processResult(rawResult, method.getGenericReturnType());

            Do printAnswer = Do.once(
                    () -> {
                        if (processedResult != null) {
                            printAnswer(processedResult.toString());
                        }
                    },
                    isToPrintAnswer);

            return withPrintOnError(() -> processedResult, printPrompt, printAnswer);
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
    }

    private static void printPrompt(String prompt) {
        log.info("Prompt:\n{}", prompt);
    }

    private static void printAnswer(String answer) {
        log.info("Answer:\n{}", answer);
    }
}
