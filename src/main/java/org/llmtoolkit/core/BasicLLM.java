package org.llmtoolkit.core;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import org.llmtoolkit.providers.LLMProvider;

/**
 * A simplified abstraction for making LLM calls with commonly used parameters.
 *
 * This class provides a unified interface for different LLM providers, delegating to either:
 * - langchain4j for most providers
 * - AltOpenAiLLMProvider for OpenAI-compatible APIs (see that class for specific use cases)
 *
 * Users can implement custom providers using either langchain4j integration
 * or direct HTTP calls via LLMProvider interface.
 */
@Getter
@Builder(toBuilder = true)
public class BasicLLM implements StringAnswer {
    private final String model;
    private final LLMProvider provider;
    private final Double temperature;
    private final Double topP;
    private final boolean thinking;
    private final ReasoningEffort reasoningEffort;
    private final Integer maxTokens;
    private final Integer thinkingTokens;
    private final Integer timeout;

    public static Supplier<@NonNull BasicLLM> of(String model, LLMProvider provider) {
        return Suppliers.memoize(() -> BasicLLM.builder()
                .model(model)
                .provider(provider)
                .thinking(false)
                .build());
    }

    public static Supplier<@NonNull BasicLLM> of(String model, LLMProvider provider, boolean thinking) {
        return Suppliers.memoize(() -> BasicLLM.builder()
                .model(model)
                .provider(provider)
                .thinking(thinking)
                .build());
    }

    @Getter
    public enum ReasoningEffort {
        LOW("low"),
        MEDIUM("medium"),
        HIGH("high");

        private final String value;

        ReasoningEffort(String value) {
            this.value = value;
        }
    }

    @Override
    public String answer(String input) {
        return provider.answer(input, this);
    }
}
