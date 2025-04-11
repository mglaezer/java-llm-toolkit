package org.llmtoolkit.core;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import org.llmtoolkit.providers.chatmodel.ChatModelProvider;

/**
 * A simplified factory for creating ChatLanguageModel instances with commonly used parameters.
 */
@Getter
@Builder(toBuilder = true)
public class BasicLLM implements Supplier<ChatLanguageModel> {
    @NonNull
    private final String model;

    @NonNull
    private final ChatModelProvider provider;

    private final Double temperature;
    private final Double topP;
    private final boolean thinking;
    private final ReasoningEffort reasoningEffort;
    private final Integer maxTokens;
    private final Integer thinkingTokens;
    private final Integer timeout;

    public static Supplier<ChatLanguageModel> of(String model, ChatModelProvider provider) {
        return Suppliers.memoize(() -> BasicLLM.builder()
                .model(model)
                .provider(provider)
                .thinking(false)
                .build()
                .get());
    }

    public static Supplier<ChatLanguageModel> of(String model, ChatModelProvider provider, boolean thinking) {
        return Suppliers.memoize(() -> BasicLLM.builder()
                .model(model)
                .provider(provider)
                .thinking(thinking)
                .build()
                .get());
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
    @NonNull
    public ChatLanguageModel get() {
        return provider.createChatModel(this);
    }
}
