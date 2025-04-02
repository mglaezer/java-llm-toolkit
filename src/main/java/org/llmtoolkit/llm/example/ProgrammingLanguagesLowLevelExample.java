package org.llmtoolkit.llm.example;

import org.llmtoolkit.llm.LLM;
import org.llmtoolkit.llm.OutputInstructions;
import org.llmtoolkit.llm.example.ProgrammingLanguagesPrompt.Languages;
import org.llmtoolkit.util.PMP;
import org.llmtoolkit.util.json.SerObject;

public class ProgrammingLanguagesLowLevelExample {

    public static void main(String[] args) {
        PMP.profile(ProgrammingLanguagesLowLevelExample::demoSingleObjectResult);
    }

    private static void demoSingleObjectResult() {
        String outputInstructions = OutputInstructions.singleObjectInstructions(Languages.class);
        System.out.println(outputInstructions);

        String jsonAnswer = LLM.GPT_4o.answer(
                "name 3 best programming languages, and explain why they are the best. Consider all alternatives. "
                        + "Provide 3 valid and 5 lame reasons for each. \n"
                        + outputInstructions);

        SerObject<Languages> serLanguages = SerObject.from(jsonAnswer, Languages.class);

        System.out.print(serLanguages.toYaml());
    }
}
