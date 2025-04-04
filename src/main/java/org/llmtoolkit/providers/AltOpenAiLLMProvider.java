package org.llmtoolkit.providers;

import org.llmtoolkit.core.BasicLLM;

public class AltOpenAiLLMProvider implements LLMProvider {
    private final AltClient client;

    public AltOpenAiLLMProvider(String url, String apiKey) {
        this.client = AltClient.createClient(url, apiKey);
    }

    @Override
    public String answer(String input, BasicLLM llm) {

        if (llm.getReasoningEffort() != null) {
            throw new RuntimeException("Reasoning effort support is not implemented for this provider");
        }

        return client.answer(
                input,
                llm.getModel(),
                llm.getTemperature(),
                llm.getTopP(),
                llm.getMaxTokens(),
                llm.getThinkingTokens(),
                llm.getTimeout());
    }
}
