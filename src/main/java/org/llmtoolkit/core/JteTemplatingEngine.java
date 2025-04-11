package org.llmtoolkit.core;

import gg.jte.CodeResolver;
import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import gg.jte.output.StringOutput;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.llmtoolkit.core.annotations.PP;
import org.llmtoolkit.core.annotations.PT;

@RequiredArgsConstructor
public class JteTemplatingEngine implements TemplatingEngine {

    public static JteTemplatingEngine create(CodeResolver codeResolver) {
        return new JteTemplatingEngine(codeResolver);
    }

    private final CodeResolver codeResolver;
    private TemplateEngine templateEngine;

    private void initializeIfNeeded() {
        if (templateEngine == null) {
            templateEngine = TemplateEngine.create(codeResolver, ContentType.Plain);
        }
    }

    @Override
    public String preparePrompt(Method method, Object[] args, Package basePackage) {
        initializeIfNeeded();

        String packagePath = basePackage.getName().replace('.', '/');
        String templatePath = resolveTemplatePath(method, packagePath);
        Map<String, Object> params = extractParameters(method, args);

        validateTemplateParameters(templatePath, params);

        StringOutput output = new StringOutput();
        templateEngine.render(templatePath, params, output);
        return output.toString();
    }

    private String resolveTemplatePath(Method method, String packagePath) {
        PT promptAnnotation = method.getAnnotation(PT.class);
        if (promptAnnotation == null) {
            throw new IllegalStateException("Method must be annotated with @" + PT.class.getSimpleName());
        }
        return packagePath + "/" + promptAnnotation.templatePath();
    }

    private Map<String, Object> extractParameters(Method method, Object[] args) {
        Map<String, Object> params = new HashMap<>();
        Parameter[] parameters = method.getParameters();
        for (int i = 0; i < parameters.length; i++) {
            PP paramAnnotation = parameters[i].getAnnotation(PP.class);
            if (paramAnnotation != null) {
                params.put(paramAnnotation.value(), args[i]);
            }
        }
        return params;
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
