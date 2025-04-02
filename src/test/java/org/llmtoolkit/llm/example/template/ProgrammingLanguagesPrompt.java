package org.llmtoolkit.llm.example.template;

import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import gg.jte.CodeResolver;
import gg.jte.resolve.DirectoryCodeResolver;
import java.nio.file.Path;
import java.util.List;
import org.llmtoolkit.core.LLM;
import org.llmtoolkit.core.StringAnswer;
import org.llmtoolkit.core.TemplatedLLMServiceFactory;
import org.llmtoolkit.core.annotations.Cue;
import org.llmtoolkit.core.annotations.PP;
import org.llmtoolkit.core.annotations.PT;
import org.llmtoolkit.util.Env;
import org.llmtoolkit.util.json.SerArray;
import org.llmtoolkit.util.json.SerObject;

public interface ProgrammingLanguagesPrompt {

    CodeResolver templatesRoot = new DirectoryCodeResolver(Path.of("src/test/java"));

    ChooseFrom CHOOSE_FROM = new ChooseFrom(
            "glorious", List.of("Java", "JavaScript", "Python", "Ruby", "Go", "Rust", "C++", "C#", "Scala", "Fortran"));

    record ChooseFrom(String adjective, List<String> options) {}

    record Languages(List<Language> languages) {
        @Cue("Programming language")
        public record Language(
                @Cue("name with latest version") String name, String description, List<Reason> reasons) {}

        @Cue("A reason for choosing a programming language")
        public record Reason(
                @Cue("Ids are prime numbers strictly in descending order, so that the last one is 2") int id,
                @Cue("One phrase.") String reason,
                @Cue("Describe why the reason is lame or valid") String description,
                @Cue("Valid vs lame flag") boolean isValidReason,
                @Cue("This field is not required") String someField) {}
    }

    @PT(templatePath = "programming_languages_prompt.jte")
    List<Languages.Language> getBestLanguagesAsList(
            @PP("count") int count, @PP("examplesCount") int examplesCount1, @PP("chooseFrom") ChooseFrom chooseFrom);

    @PT(templatePath = "programming_languages_prompt.jte")
    Languages getBestLanguagesAsObject(
            @PP("count") int count, @PP("examplesCount") int examplesCount1, @PP("chooseFrom") ChooseFrom chooseFrom);

    @PT(templatePath = "programming_languages_prompt.jte")
    String getBestLanguagesAsString(
            @PP("count") int count, @PP("examplesCount") int examplesCount, @PP("chooseFrom") ChooseFrom chooseFrom);

    static void main(String[] args) {
        demoLLMString();
        demoLLMList();
        demoLLMObject();
        demoAiServicesList();
        demoAiServicesObject();
    }

    private static void demoLLMString() {

        ProgrammingLanguagesPrompt service = new TemplatedLLMServiceFactory(LLM.GEMINI_2_0_FLASH, templatesRoot)
                .create(ProgrammingLanguagesPrompt.class);
        var languages = service.getBestLanguagesAsString(2, 2, CHOOSE_FROM);
        System.out.println("languages as String = \n" + languages);
    }

    private static void demoAiServicesObject() {
        GoogleAiGeminiChatModel model = GoogleAiGeminiChatModel.builder()
                .apiKey(Env.getRequired("GEMINI_API_KEY"))
                .modelName(LLM.GEMINI_2_0_FLASH.model())
                .temperature(0.5)
                .build();

        StringAnswer stringAnswer =
                AiServices.builder(StringAnswer.class).chatLanguageModel(model).build();

        ProgrammingLanguagesPrompt service =
                new TemplatedLLMServiceFactory(stringAnswer, templatesRoot).create(ProgrammingLanguagesPrompt.class);

        Languages languages = service.getBestLanguagesAsObject(2, 2, CHOOSE_FROM);
        System.out.println(
                "languages as Object = \n" + SerObject.from(languages).toYaml());
    }

    private static void demoAiServicesList() {
        OpenAiChatModel model = OpenAiChatModel.builder()
                .apiKey(Env.getRequired("OPENAI_API_KEY"))
                .modelName(LLM.GPT_4o.model())
                .maxTokens(2000)
                .build();

        StringAnswer stringAnswer =
                AiServices.builder(StringAnswer.class).chatLanguageModel(model).build();

        ProgrammingLanguagesPrompt service =
                new TemplatedLLMServiceFactory(stringAnswer, templatesRoot).create(ProgrammingLanguagesPrompt.class);

        List<Languages.Language> languages = service.getBestLanguagesAsList(2, 2, CHOOSE_FROM);
        System.out.println("languages as List = \n"
                + SerArray.from(languages, Languages.Language.class).toYaml());
    }

    private static void demoLLMObject() {
        ProgrammingLanguagesPrompt service = new TemplatedLLMServiceFactory(LLM.GEMINI_2_0_FLASH, templatesRoot)
                .create(ProgrammingLanguagesPrompt.class);
        Languages languages = service.getBestLanguagesAsObject(2, 2, CHOOSE_FROM);
        System.out.println(
                "languages as Object = \n" + SerObject.from(languages).toYaml());
    }

    private static void demoLLMList() {
        ProgrammingLanguagesPrompt service = new TemplatedLLMServiceFactory(LLM.GEMINI_2_0_FLASH, templatesRoot)
                .create(ProgrammingLanguagesPrompt.class);
        List<Languages.Language> languages = service.getBestLanguagesAsList(2, 2, CHOOSE_FROM);
        System.out.println("languages as List = \n"
                + SerArray.from(languages, Languages.Language.class).toYaml());
    }
}
