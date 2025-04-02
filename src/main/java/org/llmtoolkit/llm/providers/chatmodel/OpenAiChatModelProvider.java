package org.llmtoolkit.llm.providers.chatmodel;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiChatRequestParameters;
import java.time.Duration;
import org.llmtoolkit.llm.LLM;

public class OpenAiChatModelProvider implements ChatModelProvider {
    private final String baseUrl;
    private final String apiKey;

    public OpenAiChatModelProvider(String baseUrl, String apiKey) {
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
    }

    @Override
    public ChatLanguageModel createChatModel(LLM llm) {
        OpenAiChatModel.OpenAiChatModelBuilder builder =
                OpenAiChatModel.builder().modelName(llm.model()).apiKey(apiKey);

        if (baseUrl != null) {
            builder.baseUrl(baseUrl);
        }

        if (llm.timeout() != null) builder.timeout(Duration.ofSeconds(llm.timeout()));
        if (llm.reasoningEffort() != null) {
            if (!llm.thinking()) {
                throw new RuntimeException("Reasoning effort is not supported for non-thinking mode");
            }
            OpenAiChatRequestParameters requestParameters = OpenAiChatRequestParameters.builder()
                    .reasoningEffort(llm.reasoningEffort().getValue())
                    .build();
            builder.defaultRequestParameters(requestParameters);
        }

        if (llm.maxTokens() != null) builder.maxTokens(llm.maxTokens());
        if (llm.thinkingTokens() != null) builder.maxCompletionTokens(llm.thinkingTokens());
        if (llm.temperature() != null) builder.temperature(llm.temperature());
        if (llm.topP() != null) builder.topP(llm.topP());

        return builder.build();
    }
}
