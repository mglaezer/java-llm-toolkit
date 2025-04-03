package org.llmtoolkit.core;

import lombok.Builder;
import lombok.Getter;
import org.llmtoolkit.providers.LLMProvider;

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
