package org.llmtoolkit.basicllm;

import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.model.chat.ChatLanguageModel;
import java.time.Duration;

public class AnthropicChatModelProvider implements ChatModelProvider {
    private final String apiKey;

    public AnthropicChatModelProvider(String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    public ChatLanguageModel createChatModel(BasicLLM llm) {
        if (llm.getReasoningEffort() != null) {
            throw new RuntimeException(
                    "Reasoning effort is not supported for Anthropic thinking mode, use thinking tokens instead");
        }

        AnthropicChatModel.AnthropicChatModelBuilder builder =
                AnthropicChatModel.builder().modelName(llm.getModel()).apiKey(apiKey);

        if (llm.isThinking() && (llm.getThinkingTokens() == null || llm.getMaxTokens() == null)) {
            throw new RuntimeException("Max tokens and thinking tokens must be set for thinking mode");
        }

        if (llm.getThinkingTokens() != null && !llm.isThinking()) {
            throw new RuntimeException("Thinking tokens can only be set for thinking mode");
        }

        if (llm.isThinking() && llm.getMaxTokens() <= llm.getThinkingTokens()) {
            throw new RuntimeException("Max tokens must be greater than thinking tokens");
        }

        if (llm.isThinking()) builder.thinkingType("enabled");
        if (llm.getMaxTokens() != null) builder.maxTokens(llm.getMaxTokens());
        if (llm.getThinkingTokens() != null) builder.thinkingBudgetTokens(llm.getThinkingTokens());
        if (llm.getTimeout() != null) builder.timeout(Duration.ofSeconds(llm.getTimeout()));
        if (llm.getTemperature() != null) builder.temperature(llm.getTemperature());
        if (llm.getTopP() != null) builder.topP(llm.getTopP());

        return builder.build();
    }
}
