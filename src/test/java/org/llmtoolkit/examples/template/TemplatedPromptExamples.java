package org.llmtoolkit.examples.template;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.service.Result;
import gg.jte.CodeResolver;
import gg.jte.resolve.DirectoryCodeResolver;
import java.nio.file.Path;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.llmtoolkit.basicllm.CommonLLMs;
import org.llmtoolkit.core.*;
import org.llmtoolkit.core.annotations.PP;
import org.llmtoolkit.core.annotations.PT;
import org.llmtoolkit.examples.ProgrammingLanguages;
import org.llmtoolkit.util.Env;
import org.llmtoolkit.util.json.SerArray;
import org.llmtoolkit.util.json.SerObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * Examples below demonstrate two approaches to LLM interaction, both using JTE templates:
 *
 * 1. Basic approach with BasicLLM usage.
 * 2. Integration with langchain4j:
 *    - Uses langchain4j's chat model construction instead of BasicLLM class
 *    - Enables additional features like Memory, RAG, Tools
 */
@Slf4j
public class TemplatedPromptExamples {
    private static final Logger LOG = LoggerFactory.getLogger(TemplatedPromptExamples.class);

    private static final CodeResolver TEMPLATES_ROOT = new DirectoryCodeResolver(Path.of("src/test/java"));
    public static final ChatLanguageModel MODEL = GoogleAiGeminiChatModel.builder()
            .modelName(CommonLLMs.GEMINI_2_0_FLASH)
            .apiKey(Env.getRequired("GEMINI_API_KEY"))
            .build();

    // Used to demo how complex Java objects can be used in JTE templates
    public record ChooseFrom(String adjective, List<String> options) {
        private static final ChooseFrom CHOOSE_FROM = new ChooseFrom(
                "glorious",
                List.of("Java", "JavaScript", "Python", "Ruby", "Go", "Rust", "C++", "C#", "Scala", "Fortran"));
    }

    interface ProgrammingLanguagesServiceAsObjectAndString {

        @PT(templatePath = "programming_languages_prompt.jte")
        ProgrammingLanguages getBestLanguagesAsObject(
                @PP("count") int count,
                @PP("examplesCount") int examplesCount1,
                @PP("chooseFrom") ChooseFrom chooseFrom);

        @PT(templatePath = "programming_languages_prompt.jte")
        String getBestLanguagesAsString(
                @PP("count") int count,
                @PP("examplesCount") int examplesCount,
                @PP("chooseFrom") ChooseFrom chooseFrom);
    }

    interface ProgrammingLanguagesServiceWithLangChain4jResult {
        @PT(templatePath = "programming_languages_prompt.jte")
        Result<ProgrammingLanguages> getBestLanguagesAsObject(
                @PP("count") int count,
                @PP("examplesCount") int examplesCount1,
                @PP("chooseFrom") ChooseFrom chooseFrom);
    }

    interface ProgrammingLanguagesServiceAsList {

        @PT(templatePath = "programming_languages_prompt.jte")
        List<ProgrammingLanguages.Language> getBestLanguagesAsList(
                @PP("count") int count,
                @PP("examplesCount") int examplesCount1,
                @PP("chooseFrom") ChooseFrom chooseFrom);
    }

    /*
     * When creating a service using TemplatedLLMServiceFactory:
     * - @PT annotation specifies which template to use for the prompt
     * - @PP annotations map method parameters to template variables
     *
     * JTE (java template engine) is used as it is more powerful than LangChain4j templates.
     * JTE supports includes, loops, conditionals, etc.
     *
     * For non-String return types, the toolkit:
     * - Adds output instructions to the prompt by converting the return type's Java source (via reflection) into a Jackson bean definition
     * - Parses LLM's response back into the requested type using lenient JSON parser,
     *   allowing some inferior LLMs that do not support json output to produce slightly malformed JSON.
     *
     * Unlike langchain4j's JSON schema approach, this method allows passing additional context
     * through annotations (like @Cue) on all elements of the target class/record.
     * Many LLMs seem to follow this approach better than JSON schema.
     */

    public static void main(String[] args) {
        demo_returningObject_jsonSchemaStructure();
        demo_returningObject_jacksonBeanStructure();
        demo_returningObject_withLangChain4jResult();
        demo_returningList_jacksonBeanStructure();
        demo_returningString_jacksonBeanStructure();
        demo_returningString_jsonSchemaStructure();
    }

    private static void demo_returningObject_jsonSchemaStructure() {
        demoReturningObject(new LangChainJsonResponseStructuringStrategy());
    }

    private static void demo_returningObject_jacksonBeanStructure() {
        demoReturningObject(new JacksonSourceResponseStructuringStrategy());
    }

    private static void demoReturningObject(ResponseStructuringStrategy strategy) {
        ProgrammingLanguagesServiceAsObjectAndString service = TemplatedLLMServiceFactory.builder()
                .serviceStrategy(strategy)
                .model(MODEL)
                .templateProcessor(JteTemplateProcessor.create(TEMPLATES_ROOT))
                .aiServiceCustomizer(aiServices -> {
                    /* Put rag, memory, tools, etc. here */
                })
                .build()
                .create(ProgrammingLanguagesServiceAsObjectAndString.class);

        ProgrammingLanguages languages = service.getBestLanguagesAsObject(2, 2, ChooseFrom.CHOOSE_FROM);
        LOG.info("\nlanguages as Object: \n{}", SerObject.from(languages).toYaml());
    }

    private static void demo_returningObject_withLangChain4jResult() {
        ProgrammingLanguagesServiceWithLangChain4jResult service = TemplatedLLMServiceFactory.builder()
                .serviceStrategy(new LangChainJsonResponseStructuringStrategy())
                .model(MODEL)
                .templateProcessor(JteTemplateProcessor.create(TEMPLATES_ROOT))
                .build()
                .create(ProgrammingLanguagesServiceWithLangChain4jResult.class);

        Result<ProgrammingLanguages> languages = service.getBestLanguagesAsObject(2, 2, ChooseFrom.CHOOSE_FROM);
        LOG.info("\nToken usage: \n{}", languages.tokenUsage());
    }

    private static void demo_returningList_jacksonBeanStructure() {
        ProgrammingLanguagesServiceAsList service = TemplatedLLMServiceFactory.builder()
                .serviceStrategy(new JacksonSourceResponseStructuringStrategy())
                .model(MODEL)
                .templateProcessor(JteTemplateProcessor.create(TEMPLATES_ROOT))
                .build()
                .create(ProgrammingLanguagesServiceAsList.class);

        List<ProgrammingLanguages.Language> languages = service.getBestLanguagesAsList(2, 2, ChooseFrom.CHOOSE_FROM);
        LOG.info(
                "\nlanguages as List: \n{}",
                SerArray.from(languages, ProgrammingLanguages.Language.class).toYaml());
    }

    private static void demo_returningString_jacksonBeanStructure() {
        demoReturningString(new JacksonSourceResponseStructuringStrategy());
    }

    private static void demo_returningString_jsonSchemaStructure() {
        demoReturningString(new LangChainJsonResponseStructuringStrategy());
    }

    private static void demoReturningString(ResponseStructuringStrategy strategy) {
        ProgrammingLanguagesServiceAsObjectAndString service = TemplatedLLMServiceFactory.builder()
                .serviceStrategy(strategy)
                .model(MODEL)
                .templateProcessor(JteTemplateProcessor.create(TEMPLATES_ROOT))
                .build()
                .create(ProgrammingLanguagesServiceAsObjectAndString.class);

        String languages = service.getBestLanguagesAsString(2, 2, ChooseFrom.CHOOSE_FROM);
        LOG.info("\nlanguages as String: \n{}", languages);
    }
}
