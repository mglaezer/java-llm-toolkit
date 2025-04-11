package org.llmtoolkit.basicllm;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import org.llmtoolkit.util.Env;

/**
 * Collection of commonly used LLM providers.
 * -
 * Note: This class violates Open-Closed Principle by coupling multiple provider implementations.
 * However, this is an intentional trade-off to enable quick experimentation and easy switching
 * between providers during development and testing.
 */
@SuppressWarnings("unused")
@UtilityClass
public class CommonProviders {
    public static final Supplier<@NonNull ChatModelProvider> OPENAI =
            Suppliers.memoize(() -> new OpenAiChatModelProvider(null, Env.getRequired("OPENAI_API_KEY")));

    public static final Supplier<@NonNull ChatModelProvider> ANTHROPIC =
            Suppliers.memoize(() -> new AnthropicChatModelProvider(Env.getRequired("ANTHROPIC_API_KEY")));

    public static final Supplier<@NonNull ChatModelProvider> GOOGLE =
            Suppliers.memoize(() -> new GoogleChatModelProvider(Env.getRequired("GEMINI_API_KEY")));
}
