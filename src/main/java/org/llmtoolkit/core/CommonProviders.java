package org.llmtoolkit.core;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import org.llmtoolkit.providers.AltOpenAiLLMProvider;
import org.llmtoolkit.providers.CachingLLMProvider;
import org.llmtoolkit.providers.LLMProvider;
import org.llmtoolkit.providers.chatmodel.AnthropicChatModelProvider;
import org.llmtoolkit.providers.chatmodel.GoogleChatModelProvider;
import org.llmtoolkit.providers.chatmodel.OpenAiChatModelProvider;
import org.llmtoolkit.util.Env;

/**
 * Collection of commonly used LLM providers.
 * -
 * Note: This class violates Open-Closed Principle by coupling multiple provider implementations.
 * However, this is an intentional trade-off to enable quick experimentation and easy switching
 * between providers during development and testing.
 */
@UtilityClass
public class CommonProviders {
    public static final Supplier<@NonNull LLMProvider> OPENAI = Suppliers.memoize(
            () -> new CachingLLMProvider(new OpenAiChatModelProvider(null, Env.getRequired("OPENAI_API_KEY"))));

    public static final Supplier<@NonNull LLMProvider> GROQ = Suppliers.memoize(() -> new CachingLLMProvider(
            new OpenAiChatModelProvider("https://api.groq.com/openai/v1/", Env.getRequired("GROQ_API_KEY"))));

    public static final Supplier<@NonNull LLMProvider> ANTHROPIC = Suppliers.memoize(
            () -> new CachingLLMProvider(new AnthropicChatModelProvider(Env.getRequired("ANTHROPIC_API_KEY"))));

    public static final Supplier<@NonNull LLMProvider> GOOGLE = Suppliers.memoize(
            () -> new CachingLLMProvider(new GoogleChatModelProvider(Env.getRequired("GEMINI_API_KEY"))));

    public static final Supplier<@NonNull LLMProvider> DEEPSEEK = Suppliers.memoize(
            () -> new AltOpenAiLLMProvider("https://api.deepseek.com/v1", Env.getRequired("DEEPSEEK_API_KEY")));

    public static final Supplier<@NonNull LLMProvider> INFERENCE = Suppliers.memoize(
            () -> new AltOpenAiLLMProvider("https://api.inference.net/v1", Env.getRequired("INFERENCE_API_KEY")));
}
