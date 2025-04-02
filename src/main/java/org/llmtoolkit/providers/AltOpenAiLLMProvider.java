package org.llmtoolkit.providers;

import org.llmtoolkit.core.LLM;

public class AltOpenAiLLMProvider implements LLMProvider {
    private final AltClient client;

    public AltOpenAiLLMProvider(String url, String apiKey) {
        client = AltClient.createClient(url, apiKey);
    }

    @Override
    public String answer(String input, LLM llm) {
        return client.answer(
                input,
                llm.model(),
                llm.temperature(),
                llm.topP(),
                llm.maxTokens(),
                llm.thinkingTokens(),
                llm.timeout());
    }
}
