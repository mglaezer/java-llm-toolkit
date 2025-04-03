package org.llmtoolkit.core;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import lombok.NonNull;
import org.llmtoolkit.providers.AltOpenAiLLMProvider;
import org.llmtoolkit.providers.CachingLLMProvider;
import org.llmtoolkit.providers.LLMProvider;
import org.llmtoolkit.providers.chatmodel.AnthropicChatModelProvider;
import org.llmtoolkit.providers.chatmodel.GoogleChatModelProvider;
import org.llmtoolkit.providers.chatmodel.OpenAiChatModelProvider;
import org.llmtoolkit.util.Env;

public class CommonProviders {
    public static final Supplier<@NonNull LLMProvider> OPENAI = Suppliers.memoize(
            () -> new CachingLLMProvider(new OpenAiChatModelProvider(null, Env.get("OPENAI_API_KEY"))));

    public static final Supplier<@NonNull LLMProvider> GROQ = Suppliers.memoize(() -> new CachingLLMProvider(
            new OpenAiChatModelProvider(Env.get("GROQ_API_KEY"), "https://api.groq.com/openai/v1/")));

    public static final Supplier<@NonNull LLMProvider> ANTHROPIC =
            Suppliers.memoize(() -> new CachingLLMProvider(new AnthropicChatModelProvider(null)));

    public static final Supplier<@NonNull LLMProvider> GOOGLE =
            Suppliers.memoize(() -> new CachingLLMProvider(new GoogleChatModelProvider(null)));

    public static final Supplier<@NonNull LLMProvider> DEEPSEEK = Suppliers.memoize(
            () -> new AltOpenAiLLMProvider("https://api.deepseek.com/v1", Env.get("DEEPSEEK_API_KEY")));

    public static final Supplier<@NonNull LLMProvider> INFERENCE = Suppliers.memoize(
            () -> new AltOpenAiLLMProvider("https://api.inference.net/v1", Env.get("INFERENCE_API_KEY")));

    private CommonProviders() {
        // Prevent instantiation
    }
}
