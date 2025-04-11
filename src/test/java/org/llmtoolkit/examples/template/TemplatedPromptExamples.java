package org.llmtoolkit.examples.template;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import gg.jte.CodeResolver;
import gg.jte.resolve.DirectoryCodeResolver;
import java.nio.file.Path;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.llmtoolkit.basicllm.CommonLLMs;
import org.llmtoolkit.core.JteTemplateProcessor;
import org.llmtoolkit.core.TemplatedLLMServiceFactory;
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

    interface ProgrammingLanguagesService {

        @PT(templatePath = "programming_languages_prompt.jte")
        ProgrammingLanguages getBestLanguagesAsObject(
                @PP("count") int count,
                @PP("examplesCount") int examplesCount1,
                @PP("chooseFrom") ChooseFrom chooseFrom);

        @PT(templatePath = "programming_languages_prompt.jte")
        List<ProgrammingLanguages.Language> getBestLanguagesAsList(
                @PP("count") int count,
                @PP("examplesCount") int examplesCount1,
                @PP("chooseFrom") ChooseFrom chooseFrom);

        @PT(templatePath = "programming_languages_prompt.jte")
        String getBestLanguagesAsString(
                @PP("count") int count,
                @PP("examplesCount") int examplesCount,
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
        demoLangchainAiServices_returningObject();
        demoLangchainAiServices_returningList();
        demoBasicLLM_returningString();
    }

    private static void demoLangchainAiServices_returningObject() {
        ProgrammingLanguagesService service = TemplatedLLMServiceFactory.builder()
                .model(MODEL)
                .templateProcessor(JteTemplateProcessor.create(TEMPLATES_ROOT))
                .isToPrintPrompt(true)
                .aiServiceCustomizer(aiServices -> {
                    /* Put rag, memory, tools, etc. here */
                })
                .build()
                .create(ProgrammingLanguagesService.class);

        ProgrammingLanguages languages = service.getBestLanguagesAsObject(2, 2, ChooseFrom.CHOOSE_FROM);
        LOG.info("\nlanguages as Object: \n{}", SerObject.from(languages).toYaml());
    }

    private static void demoLangchainAiServices_returningList() {
        ProgrammingLanguagesService service = TemplatedLLMServiceFactory.builder()
                .model(MODEL)
                .templateProcessor(JteTemplateProcessor.create(TEMPLATES_ROOT))
                .build()
                .create(ProgrammingLanguagesService.class);

        List<ProgrammingLanguages.Language> languages = service.getBestLanguagesAsList(2, 2, ChooseFrom.CHOOSE_FROM);
        LOG.info(
                "\nlanguages as List: \n{}",
                SerArray.from(languages, ProgrammingLanguages.Language.class).toYaml());
    }

    private static void demoBasicLLM_returningString() {
        ProgrammingLanguagesService service = TemplatedLLMServiceFactory.builder()
                .model(MODEL)
                .templateProcessor(JteTemplateProcessor.create(TEMPLATES_ROOT))
                .build()
                .create(ProgrammingLanguagesService.class);

        String languages = service.getBestLanguagesAsString(2, 2, ChooseFrom.CHOOSE_FROM);
        LOG.info("\nlanguages as String: \n{}", languages);
    }
}
