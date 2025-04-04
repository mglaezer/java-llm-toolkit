package org.llmtoolkit.examples.lowlevel;

import org.llmtoolkit.core.CommonLLMs;
import org.llmtoolkit.core.OutputInstructions;
import org.llmtoolkit.examples.ProgrammingLanguages;
import org.llmtoolkit.util.json.SerObject;

public class NoTemplateExample {

    /*
     * This example demonstrates how to use the java-llm-toolkit's API to interact with an LLM without using JTE templates.
     */
    public static void main(String[] args) {

        String outputInstructions = OutputInstructions.singleObjectInstructions(ProgrammingLanguages.class);
        System.out.println("OUTPUT INSTRUCTIONS:\n" + outputInstructions);

        String jsonAnswer = CommonLLMs.GPT_4o.get()
                .answer(
                        "Name 3 best programming languages, and explain why they are the best. Consider all alternatives. "
                                + "Provide 2 valid and 2 lame reasons for each. \n"
                                + outputInstructions);

        SerObject<ProgrammingLanguages> serLanguages = SerObject.from(jsonAnswer, ProgrammingLanguages.class);

        System.out.print("ANSWER:\n" + serLanguages.toYaml());
    }
}
