package org.llmtoolkit.examples.lowlevel;

import dev.langchain4j.model.openai.OpenAiChatModel;
import org.llmtoolkit.basicllm.CommonLLMs;
import org.llmtoolkit.core.OutputInstructions;
import org.llmtoolkit.examples.ProgrammingLanguages;
import org.llmtoolkit.util.Env;
import org.llmtoolkit.util.json.SerObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LowLevelExample {
    private static final Logger LOG = LoggerFactory.getLogger(LowLevelExample.class);

    /*
     * This example demonstrates how to use the low-level API to get structured output from LLM.
     */
    public static void main(String[] args) {
        String outputInstructions = OutputInstructions.singleObjectInstructions(ProgrammingLanguages.class);
        LOG.info("\nOUTPUT INSTRUCTIONS:\n{}", outputInstructions);

        String jsonAnswer = OpenAiChatModel.builder()
                .modelName(CommonLLMs.GPT_4O)
                .apiKey(Env.getRequired("OPENAI_API_KEY"))
                .build()
                .chat(
                        "Name 3 best programming languages, and explain why they are the best. Consider all alternatives. "
                                + "Provide 2 valid and 2 lame reasons for each. \n"
                                + outputInstructions);

        SerObject<ProgrammingLanguages> serLanguages = SerObject.from(jsonAnswer, ProgrammingLanguages.class);

        LOG.info("\nANSWER:\n{}", serLanguages.toYaml());
    }
}
