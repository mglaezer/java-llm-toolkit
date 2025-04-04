package org.llmtoolkit.examples.template;

import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import gg.jte.CodeResolver;
import gg.jte.resolve.DirectoryCodeResolver;
import java.nio.file.Path;
import java.util.List;
import org.llmtoolkit.core.CommonLLMs;
import org.llmtoolkit.core.StringAnswer;
import org.llmtoolkit.core.TemplatedLLMServiceFactory;
import org.llmtoolkit.core.annotations.PP;
import org.llmtoolkit.core.annotations.PT;
import org.llmtoolkit.examples.ProgrammingLanguages;
import org.llmtoolkit.util.Env;
import org.llmtoolkit.util.json.SerArray;
import org.llmtoolkit.util.json.SerObject;

public class TemplatedPromptExamples {

    private static final CodeResolver TEMPLATES_ROOT = new DirectoryCodeResolver(Path.of("src/test/java"));

    // Used to demo how complex Java objects can be used in JTE templates
    public record ChooseFrom(String adjective, List<String> options) {
        private static final ChooseFrom CHOOSE_FROM = new ChooseFrom(
                "glorious",
                List.of("Java", "JavaScript", "Python", "Ruby", "Go", "Rust", "C++", "C#", "Scala", "Fortran"));
    }

    interface ProgrammingLanguagesService {
        @PT(templatePath = "programming_languages_prompt.jte")
        List<ProgrammingLanguages.Language> getBestLanguagesAsList(
                @PP("count") int count,
                @PP("examplesCount") int examplesCount1,
                @PP("chooseFrom") ChooseFrom chooseFrom);

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

    public static void main(String[] args) {
        demoBasicLLM_returningObject();
        demoBasicLLM_returningList();
        demoBasicLLM_returningString();
        demoLangchainAiServices_returningObject();
        demoLangchainAiServices_returningList();
    }

    private static void demoBasicLLM_returningObject() {

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
         * Many inferior LLMs seem to follow this approach better than JSON schema.
         */
        ProgrammingLanguagesService service = new TemplatedLLMServiceFactory(
                        CommonLLMs.GEMINI_2_0_FLASH.get(), TEMPLATES_ROOT)
                .create(ProgrammingLanguagesService.class);
        ProgrammingLanguages languages = service.getBestLanguagesAsObject(2, 2, ChooseFrom.CHOOSE_FROM);
        System.out.println(
                "languages as Object = \n" + SerObject.from(languages).toYaml());
    }

    private static void demoBasicLLM_returningList() {
        ProgrammingLanguagesService service = new TemplatedLLMServiceFactory(
                        CommonLLMs.GEMINI_2_0_FLASH.get(), TEMPLATES_ROOT)
                .create(ProgrammingLanguagesService.class);

        List<ProgrammingLanguages.Language> languages = service.getBestLanguagesAsList(2, 2, ChooseFrom.CHOOSE_FROM);
        System.out.println("languages as List = \n"
                + SerArray.from(languages, ProgrammingLanguages.Language.class).toYaml());
    }

    private static void demoBasicLLM_returningString() {

        ProgrammingLanguagesService service = new TemplatedLLMServiceFactory(
                        CommonLLMs.GEMINI_2_0_FLASH.get(), TEMPLATES_ROOT)
                .create(ProgrammingLanguagesService.class);

        String languages = service.getBestLanguagesAsString(2, 2, ChooseFrom.CHOOSE_FROM);
        System.out.println("languages as String = \n" + languages);
    }

    /*
     * These examples demonstrate using langchain4j mechanisms for LLM interaction.
     * This approach is preferred when:
     * - You want to construct chat models the langchain way
     * - You need langchain-compatible services like Memory, RAG, Tools, etc.:
     *
     * The examples show how to integrate these langchain4j services
     * with java-llm-toolkit's templating capabilities
     */
    private static void demoLangchainAiServices_returningObject() {
        GoogleAiGeminiChatModel model = GoogleAiGeminiChatModel.builder()
                .apiKey(Env.getRequired("GEMINI_API_KEY"))
                .modelName(CommonLLMs.GEMINI_2_0_FLASH.get().getModel())
                .temperature(0.5)
                .build();

        // using langchain4j's AiServices to create a StringAnswer, compatible with java-llm-toolkit
        // this builder allows adding Memory, RAG, Tools, etc
        StringAnswer stringAnswer =
                AiServices.builder(StringAnswer.class).chatLanguageModel(model).build();

        // wrapping the StringAnswer with java-llm-toolkit's templating capabilities, adding output instructions
        ProgrammingLanguagesService service =
                new TemplatedLLMServiceFactory(stringAnswer, TEMPLATES_ROOT).create(ProgrammingLanguagesService.class);

        ProgrammingLanguages languages = service.getBestLanguagesAsObject(2, 2, ChooseFrom.CHOOSE_FROM);
        System.out.println(
                "languages as Object = \n" + SerObject.from(languages).toYaml());
    }

    private static void demoLangchainAiServices_returningList() {

        OpenAiChatModel model = OpenAiChatModel.builder()
                .apiKey(Env.getRequired("OPENAI_API_KEY"))
                .modelName(CommonLLMs.GPT_4o.get().getModel())
                .maxTokens(2000)
                .build();

        StringAnswer stringAnswer =
                AiServices.builder(StringAnswer.class).chatLanguageModel(model).build();

        ProgrammingLanguagesService service =
                new TemplatedLLMServiceFactory(stringAnswer, TEMPLATES_ROOT).create(ProgrammingLanguagesService.class);

        List<ProgrammingLanguages.Language> languages = service.getBestLanguagesAsList(2, 2, ChooseFrom.CHOOSE_FROM);
        System.out.println("languages as List = \n"
                + SerArray.from(languages, ProgrammingLanguages.Language.class).toYaml());
    }
}
