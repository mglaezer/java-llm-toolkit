// File: java/org/llmtoolkit/core/JteTemplateLlmServiceFactory.java
package org.llmtoolkit.core;

import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import gg.jte.CodeResolver;
import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import gg.jte.output.StringOutput;
import gg.jte.resolve.DirectoryCodeResolver;
import org.llmtoolkit.core.annotations.PP;
import org.llmtoolkit.core.annotations.PT;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public class JteTemplateLlmServiceFactory<R> {

    private final Function<String, R> llmResponseProvider;
    private final TemplateEngine templateEngine;

    public JteTemplateLlmServiceFactory(Function<String, R> llmResponseProvider, CodeResolver codeResolver) {
        this.llmResponseProvider = llmResponseProvider;
        this.templateEngine = TemplateEngine.create(codeResolver, ContentType.Plain);
    }

    @SuppressWarnings("unchecked")
    public <T> T create(Class<T> serviceInterface) {
        return (T) Proxy.newProxyInstance(
                serviceInterface.getClassLoader(),
                new Class<?>[]{serviceInterface},
                new JteTemplateLlmInvocationHandler(serviceInterface)
        );
    }

    private class JteTemplateLlmInvocationHandler implements InvocationHandler {
        private final Class<?> serviceInterface;

        private JteTemplateLlmInvocationHandler(Class<?> serviceInterface) {
            this.serviceInterface = serviceInterface;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (method.getDeclaringClass() == Object.class) {
                return method.invoke(this, args);
            }
            String prompt = preparePrompt(method, args, serviceInterface.getPackage());
            return llmResponseProvider.apply(prompt);
        }
    }

    private String preparePrompt(Method method, Object[] args, Package pkg) {
        String packagePath = pkg.getName().replace('.', '/');
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

    // Plain functional interface for converting a prompt String into a Poem
    @FunctionalInterface
    public interface PoemAiService {
        Poem compose(String prompt);
    }

    // Example service interface to demonstrate usage. All methods must return the same type R.
    public interface ExamplePoemService {
        @PT(templatePath = "compose_poem.jte")
        Poem composePoem(@PP("theme") String theme, @PP("stanzas") int stanzas);
    }

    // Structured output class.
    @SuppressWarnings("unused")
    public static class Poem {
        private String title;
        private String content;

        public Poem() {
        }

        public Poem(String title, String content) {
            this.title = title;
            this.content = content;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        @Override
        public String toString() {
            return "Poem{title='" + title + "', content='" + content + "'}";
        }
    }

    // Main method demonstrating type safe usage with inline adapter.
    public static void main(String[] args) {
        OpenAiChatModel model = OpenAiChatModel.builder()
                .apiKey(System.getenv("OPENAI_API_KEY"))
                .modelName("gpt-4")
                .maxTokens(2000)
                .build();

        // Create the PoemAiService via AiServices.
        PoemAiService poemAiService = AiServices.builder(PoemAiService.class)
                .chatLanguageModel(model)
                .build();

        // Inline adapter: wrap the PoemAiService as a Function<String, Poem>.
        Function<String, Poem> aiService = poemAiService::compose;

        // Use a CodeResolver for templates (e.g., templates under src/test/java).
        CodeResolver codeResolver = new DirectoryCodeResolver(Path.of("src/test/java"));

        // Create a type safe factory instantiated with Poem.
        JteTemplateLlmServiceFactory<Poem> factory = new JteTemplateLlmServiceFactory<>(aiService, codeResolver);

        // Create an instance of the ExamplePoemService from the factory.
        ExamplePoemService service = factory.create(ExamplePoemService.class);

        // Call the service method; the factory stamps the prompt and returns a Poem.
        Poem poem = service.composePoem("Nature", 3);

        System.out.println("Received Poem:");
        System.out.println("Title: " + poem.getTitle());
        System.out.println("Content: " + poem.getContent());
    }
}