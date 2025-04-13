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
    private ResponseStructuringStrategy serviceStrategy = new JacksonSourceResponseStructuringStrategy();

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
        ReturnTypeInfo.validateType(method.getGenericReturnType());
        templateProcessor.validateTemplate(method);
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
            ReturnTypeInfo typeInfo = ReturnTypeInfo.from(method.getGenericReturnType());

            final String processedPrompt =
                    serviceStrategy.augmentPromptWithOutputInstructions(templatePrompt, method, typeInfo);

            Do printPrompt = Do.once(() -> printPrompt(processedPrompt), isToPrintPrompt);

            Object rawResult = withPrintOnError(
                    () -> serviceStrategy.invokeService(service, processedPrompt, method), printPrompt);

            final Object processedResult = serviceStrategy.convertResult(rawResult, typeInfo);

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
