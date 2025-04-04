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

public class ProgrammingLanguagesPrompt {

    private static final CodeResolver TEMPLATES_ROOT = new DirectoryCodeResolver(Path.of("src/test/java"));

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
        demoLLMString();
        demoLangchainAiServices_returningObject();
        demoLangchainAiServices_returningList();
    }

    private static void demoBasicLLM_returningObject() {
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

    private static void demoLLMString() {

        ProgrammingLanguagesService service = new TemplatedLLMServiceFactory(
                        CommonLLMs.GEMINI_2_0_FLASH.get(), TEMPLATES_ROOT)
                .create(ProgrammingLanguagesService.class);

        String languages = service.getBestLanguagesAsString(2, 2, ChooseFrom.CHOOSE_FROM);
        System.out.println("languages as String = \n" + languages);
    }

    private static void demoLangchainAiServices_returningObject() {
        GoogleAiGeminiChatModel model = GoogleAiGeminiChatModel.builder()
                .apiKey(Env.getRequired("GEMINI_API_KEY"))
                .modelName(CommonLLMs.GEMINI_2_0_FLASH.get().getModel())
                .temperature(0.5)
                .build();

        StringAnswer stringAnswer =
                AiServices.builder(StringAnswer.class).chatLanguageModel(model).build();

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
