package org.llmtoolkit.core;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.service.AiServices;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import org.llmtoolkit.core.annotations.PT;

public class NativeTypeStrategy implements LlmServiceStrategy {

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
                // Keep original return types but use String parameter
                builder = builder.defineMethod(method.getName(), method.getReturnType(), Visibility.PUBLIC)
                        .withParameter(String.class, "prompt")
                        .withoutCode();
            }
        }

        try (DynamicType.Unloaded<?> unloaded = builder.make()) {
            return unloaded.load(originalInterface.getClassLoader(), ClassLoadingStrategy.Default.INJECTION)
                    .getLoaded();
        }
    }

    @Override
    public Method resolveServiceMethod(Object service, Method originalMethod) throws NoSuchMethodException {
        Class<?>[] interfaces = service.getClass().getInterfaces();
        if (interfaces.length == 0) throw new RuntimeException("Service has no interfaces");
        return interfaces[0].getMethod(originalMethod.getName(), String.class);
    }

    @Override
    public String augmentPromptWithOutputInstructions(String prompt, Method method, ReturnTypeInfo typeInfo) {
        // Native type strategy doesn't modify the prompt as it relies on langchain4j's built-in type handling
        return prompt;
    }

    @Override
    public Object convertResult(Object result, ReturnTypeInfo typeInfo) {
        // Native type strategy doesn't need to process the result as langchain4j handles the conversion
        return result;
    }
}
