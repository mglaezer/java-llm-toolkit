package org.llmtoolkit.llm.example.lowlevel;

import org.llmtoolkit.core.BasicLLM;
import org.llmtoolkit.core.OutputInstructions;
import org.llmtoolkit.llm.example.template.ProgrammingLanguagesPrompt.Languages;
import org.llmtoolkit.util.json.SerObject;

public class ProgrammingLanguagesLowLevelExample {

    public static void main(String[] args) {

        String outputInstructions = OutputInstructions.singleObjectInstructions(Languages.class);
        System.out.println(outputInstructions);

        String jsonAnswer = BasicLLM.GPT_4o.get()
                .answer(
                        "name 3 best programming languages, and explain why they are the best. Consider all alternatives. "
                                + "Provide 3 valid and 5 lame reasons for each. \n"
                                + outputInstructions);

        SerObject<Languages> serLanguages = SerObject.from(jsonAnswer, Languages.class);

        System.out.print(serLanguages.toYaml());
    }
}
