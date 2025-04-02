package org.llmtoolkit.providers.chatmodel;

import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.model.chat.ChatLanguageModel;
import java.time.Duration;
import org.llmtoolkit.core.LLM;
import org.llmtoolkit.util.Env;

public class AnthropicChatModelProvider implements ChatModelProvider {

    private String apiKey;

    public AnthropicChatModelProvider(String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    public ChatLanguageModel createChatModel(LLM llm) {
        if (llm.reasoningEffort() != null) {
            throw new RuntimeException(
                    "Reasoning effort is not supported for Anthropic thinking mode, use thinking tokens instead");
        }

        if (apiKey == null) apiKey = Env.getRequired("ANTHROPIC_API_KEY");

        AnthropicChatModel.AnthropicChatModelBuilder builder =
                AnthropicChatModel.builder().modelName(llm.model()).apiKey(apiKey);

        if (llm.thinking() && (llm.thinkingTokens() == null || llm.maxTokens() == null)) {
            throw new RuntimeException("Max tokens and thinking tokens must be set for thinking mode");
        }

        if (llm.thinkingTokens() != null && !llm.thinking()) {
            throw new RuntimeException("Thinking tokens can only be set for thinking mode");
        }

        if (llm.thinking() && llm.maxTokens() <= llm.thinkingTokens()) {
            throw new RuntimeException("Max tokens must be greater than thinking tokens");
        }

        if (llm.thinking()) builder.thinkingType("enabled");
        if (llm.maxTokens() != null) builder.maxTokens(llm.maxTokens());
        if (llm.thinkingTokens() != null) builder.thinkingBudgetTokens(llm.thinkingTokens());
        if (llm.timeout() != null) builder.timeout(Duration.ofSeconds(llm.timeout()));
        if (llm.temperature() != null) builder.temperature(llm.temperature());
        if (llm.topP() != null) builder.topP(llm.topP());

        return builder.build();
    }
}
