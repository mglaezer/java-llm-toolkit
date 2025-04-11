package org.llmtoolkit.basicllm;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiChatRequestParameters;
import java.time.Duration;

public class OpenAiChatModelProvider implements ChatModelProvider {
    private final String baseUrl;
    private final String apiKey;

    public OpenAiChatModelProvider(String baseUrl, String apiKey) {
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
    }

    @Override
    public ChatLanguageModel createChatModel(BasicLLM llm) {
        OpenAiChatModel.OpenAiChatModelBuilder builder =
                OpenAiChatModel.builder().modelName(llm.getModel()).apiKey(apiKey);

        if (baseUrl != null) {
            builder.baseUrl(baseUrl);
        }

        if (llm.getTimeout() != null) builder.timeout(Duration.ofSeconds(llm.getTimeout()));
        if (llm.getReasoningEffort() != null) {
            if (!llm.isThinking()) {
                throw new RuntimeException("Reasoning effort is not supported for non-thinking mode");
            }
            OpenAiChatRequestParameters requestParameters = OpenAiChatRequestParameters.builder()
                    .reasoningEffort(llm.getReasoningEffort().getValue())
                    .build();
            builder.defaultRequestParameters(requestParameters);
        }

        if (llm.getMaxTokens() != null) builder.maxTokens(llm.getMaxTokens());
        if (llm.getThinkingTokens() != null) builder.maxCompletionTokens(llm.getThinkingTokens());
        if (llm.getTemperature() != null) builder.temperature(llm.getTemperature());
        if (llm.getTopP() != null) builder.topP(llm.getTopP());

        return builder.build();
    }
}
