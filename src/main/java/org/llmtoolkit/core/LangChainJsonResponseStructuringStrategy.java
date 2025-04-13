package org.llmtoolkit.core;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.StubMethod;
import org.llmtoolkit.core.annotations.PT;

public class LangChainJsonResponseStructuringStrategy implements ResponseStructuringStrategy {

    private static final AtomicInteger counter = new AtomicInteger(0);

    @Override
    public <T> Object createService(
            Class<T> serviceInterface, ChatLanguageModel model, Consumer<AiServices<?>> customizer) {
        Class<?> modifiedInterface = createModifiedInterface(serviceInterface);

        AiServices<?> aiServicesBuilder = AiServices.builder(modifiedInterface).chatLanguageModel(model);

        if (customizer != null) {
            customizer.accept(aiServicesBuilder);
        }

        return aiServicesBuilder.build();
    }

    private <T> Class<?> createModifiedInterface(Class<T> originalInterface) {
        var builder = new ByteBuddy()
                .makeInterface()
                .name(originalInterface.getPackage().getName() + ".Modified" + originalInterface.getSimpleName()
                        + counter.incrementAndGet());

        for (Method method : originalInterface.getDeclaredMethods()) {
            if (method.isAnnotationPresent(PT.class)) {
                // Define annotations similar to StringAnswer to avoid langchain4j's template resolution
                AnnotationDescription userMessageAnnotation = AnnotationDescription.Builder.ofType(UserMessage.class)
                        .defineArray("value", "{{raw}}")
                        .build();

                AnnotationDescription vAnnotation = AnnotationDescription.Builder.ofType(V.class)
                        .define("value", "raw")
                        .build();

                // Define method with annotations
                builder = builder.defineMethod(method.getName(), method.getReturnType(), Visibility.PUBLIC)
                        .withParameter(String.class, "prompt")
                        .intercept(StubMethod.INSTANCE)
                        .annotateMethod(userMessageAnnotation)
                        .annotateParameter(0, vAnnotation);
            }
        }

        try (DynamicType.Unloaded<?> unloaded = builder.make()) {
            return unloaded.load(originalInterface.getClassLoader(), ClassLoadingStrategy.Default.INJECTION)
                    .getLoaded();
        }
    }

    @Override
    public String augmentPromptWithOutputInstructions(String prompt, Method method, ReturnTypeInfo typeInfo) {
        // Native type strategy doesn't modify the prompt as it relies on langchain4j's built-in type handling
        return prompt;
    }

    @Override
    public Object invokeService(Object service, String prompt, Method originalMethod) {
        try {
            return service.getClass()
                    .getInterfaces()[0]
                    .getMethod(originalMethod.getName(), String.class)
                    .invoke(service, prompt);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Object convertResult(Object result, ReturnTypeInfo typeInfo) {
        // Native type strategy doesn't need to process the result as langchain4j handles the conversion
        return result;
    }
}
