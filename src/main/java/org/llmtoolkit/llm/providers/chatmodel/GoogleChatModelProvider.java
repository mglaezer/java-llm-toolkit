package org.llmtoolkit.llm.providers.chatmodel;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import java.time.Duration;
import org.llmtoolkit.llm.LLM;
import org.llmtoolkit.util.Env;

public class GoogleChatModelProvider implements ChatModelProvider {
    @Override
    public ChatLanguageModel createChatModel(LLM llm) {
        if (llm.thinkingTokens() != null) {
            throw new UnsupportedOperationException("Google does not support specifying thinking tokens");
        }

        if (llm.reasoningEffort() != null) {
            throw new UnsupportedOperationException("Google does not support specifying reasoning effort");
        }

        GoogleAiGeminiChatModel.GoogleAiGeminiChatModelBuilder builder =
                GoogleAiGeminiChatModel.builder().modelName(llm.model()).apiKey(Env.getRequired("GEMINI_API_KEY"));

        if (llm.timeout() != null) builder.timeout(Duration.ofSeconds(llm.timeout()));
        if (llm.temperature() != null) builder.temperature(llm.temperature());
        if (llm.topP() != null) builder.topP(llm.topP());
        if (llm.maxTokens() != null) builder.maxOutputTokens(llm.maxTokens());

        return builder.build();
    }
}
