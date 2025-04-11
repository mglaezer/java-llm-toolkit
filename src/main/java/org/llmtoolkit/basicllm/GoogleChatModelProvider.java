package org.llmtoolkit.basicllm;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import java.time.Duration;

public class GoogleChatModelProvider implements ChatModelProvider {
    private final String apiKey;

    public GoogleChatModelProvider(String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    public ChatLanguageModel createChatModel(BasicLLM llm) {
        if (llm.getThinkingTokens() != null) {
            throw new UnsupportedOperationException("Google does not support specifying thinking tokens");
        }

        if (llm.getReasoningEffort() != null) {
            throw new UnsupportedOperationException("Google does not support specifying reasoning effort");
        }

        GoogleAiGeminiChatModel.GoogleAiGeminiChatModelBuilder builder =
                GoogleAiGeminiChatModel.builder().modelName(llm.getModel()).apiKey(apiKey);

        if (llm.getTimeout() != null) builder.timeout(Duration.ofSeconds(llm.getTimeout()));
        if (llm.getTemperature() != null) builder.temperature(llm.getTemperature());
        if (llm.getTopP() != null) builder.topP(llm.getTopP());
        if (llm.getMaxTokens() != null) builder.maxOutputTokens(llm.getMaxTokens());

        return builder.build();
    }
}
