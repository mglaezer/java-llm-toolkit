package org.llmtoolkit.core;

import dev.langchain4j.service.AiServices;
import gg.jte.CodeResolver;
import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import gg.jte.output.StringOutput;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.llmtoolkit.core.annotations.PP;
import org.llmtoolkit.core.annotations.PT;
import org.llmtoolkit.util.Do;
import org.llmtoolkit.util.json.JsonUtils;

@Slf4j
@Builder
public class TemplatedLLMServiceFactory {

    private Consumer<AiServices<StringAnswer>> builderCustomizer;
    private CodeResolver codeResolver;
    private boolean isToPrintPrompt;
    private boolean isToPrintAnswer;

    private StringAnswer stringAnswer;
    private TemplateEngine templateEngine;

    private synchronized void initializeIfNeeded() {
        if (templateEngine == null) {
            templateEngine = TemplateEngine.create(codeResolver, ContentType.Plain);
        }
        if (stringAnswer == null) {
            AiServices<StringAnswer> baseBuilder = AiServices.builder(StringAnswer.class);
            builderCustomizer.accept(baseBuilder);
            stringAnswer = baseBuilder.build();
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T create(Class<T> serviceInterface) {
        initializeIfNeeded();
        return (T) Proxy.newProxyInstance(
                serviceInterface.getClassLoader(),
                new Class<?>[] {serviceInterface},
                new PromptInvocationHandler(serviceInterface));
    }

    private class PromptInvocationHandler implements InvocationHandler {
        private final Class<?> serviceInterface;

        private PromptInvocationHandler(Class<?> serviceInterface) {
            this.serviceInterface = serviceInterface;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (method.getDeclaringClass() == Object.class) {
                return method.invoke(this, args);
            }

            Type returnType = method.getGenericReturnType();
            Class<?> valueType = extractValueType(returnType);

            boolean isList = returnType instanceof ParameterizedType
                    && ((ParameterizedType) returnType).getRawType() == List.class;
            boolean isString = returnType == String.class;

            String prompt = preparePrompt(method, args, serviceInterface.getPackage());
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
            throw new UnsupportedOperationException("Return type must be either a class or List<Class>");
        }

        // TODO: ensure records or strings
        private void validateReturnType(Class<?> type) {
            if (type.isPrimitive()) {
                throw new UnsupportedOperationException("Primitive return types are not supported");
            }
            if (type != String.class
                    && (type.getName().startsWith("java.lang.")
                            || type.getName().startsWith("java.util."))) {
                throw new UnsupportedOperationException(
                        "Java language and util types (except String) are not supported");
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
    }

    private static void printPrompt(String wholePrompt) {
        log.info("Prompt:\n" + wholePrompt);
    }

    private static void printAnswer(String answer) {
        log.info("Answer:\n" + answer);
    }

    private String preparePrompt(Method method, Object[] args, Package aPackage) {
        String packagePath = aPackage.getName().replace('.', '/');

        PT promptAnnotation = method.getAnnotation(PT.class);
        if (promptAnnotation == null) {
            throw new IllegalStateException("Method must be annotated with @" + PT.class.getSimpleName());
        }

        String templatePath = packagePath + "/" + promptAnnotation.templatePath();

        Map<String, Object> params = new HashMap<>();
        Parameter[] parameters = method.getParameters();
        for (int i = 0; i < parameters.length; i++) {
            PP paramAnnotation = parameters[i].getAnnotation(PP.class);
            if (paramAnnotation != null) {
                params.put(paramAnnotation.value(), args[i]);
            }
        }

        validateTemplateParameters(templatePath, params);

        StringOutput output = new StringOutput();
        templateEngine.render(templatePath, params, output);
        return output.toString();
    }

    private void validateTemplateParameters(String templatePath, Map<String, Object> providedParams) {
        Map<String, Class<?>> paramInfo = templateEngine.getParamInfo(templatePath);

        Set<String> missingParams = new HashSet<>(paramInfo.keySet());
        missingParams.removeAll(providedParams.keySet());

        Set<String> extraParams = new HashSet<>(providedParams.keySet());
        extraParams.removeAll(paramInfo.keySet());

        if (!missingParams.isEmpty() || !extraParams.isEmpty()) {
            StringBuilder err = new StringBuilder("Template parameter mismatch for " + templatePath + ":");
            if (!missingParams.isEmpty()) {
                err.append("\n  Missing required parameters: ").append(String.join(", ", missingParams));
            }
            if (!extraParams.isEmpty()) {
                err.append("\n  Extra parameters provided: ").append(String.join(", ", extraParams));
            }
            throw new IllegalArgumentException(err.toString());
        }
    }
}
