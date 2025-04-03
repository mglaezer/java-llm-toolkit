package org.llmtoolkit.providers;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import lombok.NonNull;
import org.llmtoolkit.core.BasicLLM;

public class AltOpenAiLLMProvider implements LLMProvider {
    private final Supplier<@NonNull AltClient> client;

    public AltOpenAiLLMProvider(String url, String apiKey) {
        this.client = Suppliers.memoize(() -> {
            if (apiKey == null) {
                throw new IllegalArgumentException("API key cannot be null");
            }
            return AltClient.createClient(url, apiKey);
        });
    }

    @Override
    public String answer(String input, BasicLLM llm) {
        return client.get()
                .answer(
                        input,
                        llm.getModel(),
                        llm.getTemperature(),
                        llm.getTopP(),
                        llm.getMaxTokens(),
                        llm.getThinkingTokens(),
                        llm.getTimeout());
    }
}
