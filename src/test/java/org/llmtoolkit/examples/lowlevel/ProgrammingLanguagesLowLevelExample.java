package org.llmtoolkit.examples.lowlevel;

import org.llmtoolkit.basicllm.BasicLLM;
import org.llmtoolkit.basicllm.CommonLLMs;
import org.llmtoolkit.basicllm.CommonProviders;
import org.llmtoolkit.core.OutputInstructions;
import org.llmtoolkit.examples.ProgrammingLanguages;
import org.llmtoolkit.util.json.SerObject;

public class ProgrammingLanguagesLowLevelExample {

    /*
     * This example demonstrates how to use the low-level API to interact with an LLM.
     */
    public static void main(String[] args) {

        String outputInstructions = OutputInstructions.singleObjectInstructions(ProgrammingLanguages.class);
        System.out.println("OUTPUT INSTRUCTIONS:\n" + outputInstructions);

        String jsonAnswer = BasicLLM.builder()
                .model(CommonLLMs.GPT_4O)
                .provider(CommonProviders.OPENAI.get())
                .build()
                .get()
                .chat(
                        "Name 3 best programming languages, and explain why they are the best. Consider all alternatives. "
                                + "Provide 2 valid and 2 lame reasons for each. \n"
                                + outputInstructions);

        SerObject<ProgrammingLanguages> serLanguages = SerObject.from(jsonAnswer, ProgrammingLanguages.class);

        System.out.print("ANSWER:\n" + serLanguages.toYaml());
    }
}
