package org.llmtoolkit.core;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import org.llmtoolkit.providers.AltOpenAiLLMProvider;
import org.llmtoolkit.providers.CachingLLMProvider;
import org.llmtoolkit.providers.LLMProvider;
import org.llmtoolkit.providers.chatmodel.AnthropicChatModelProvider;
import org.llmtoolkit.providers.chatmodel.GoogleChatModelProvider;
import org.llmtoolkit.providers.chatmodel.OpenAiChatModelProvider;
import org.llmtoolkit.util.Env;

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

    private static final Supplier<@NonNull LLMProvider> OPENAI = Suppliers.memoize(
            () -> new CachingLLMProvider(new OpenAiChatModelProvider(null, Env.get("OPENAI_API_KEY"))));

    private static final Supplier<@NonNull LLMProvider> GROQ = Suppliers.memoize(() -> new CachingLLMProvider(
            new OpenAiChatModelProvider(Env.get("GROQ_API_KEY"), "https://api.groq.com/openai/v1/")));

    private static final Supplier<@NonNull LLMProvider> ANTHROPIC =
            Suppliers.memoize(() -> new CachingLLMProvider(new AnthropicChatModelProvider(null)));

    private static final Supplier<@NonNull LLMProvider> GOOGLE =
            Suppliers.memoize(() -> new CachingLLMProvider(new GoogleChatModelProvider(null)));

    private static final Supplier<@NonNull LLMProvider> DEEPSEEK = Suppliers.memoize(
            () -> new AltOpenAiLLMProvider("https://api.deepseek.com/v1", Env.get("DEEPSEEK_API_KEY")));

    private static final Supplier<@NonNull LLMProvider> INFERENCE = Suppliers.memoize(
            () -> new AltOpenAiLLMProvider("https://api.inference.net/v1", Env.get("INFERENCE_API_KEY")));

    public static final Supplier<@NonNull BasicLLM> GPT_o1 = Suppliers.memoize(
            () -> builder().model("o1").provider(OPENAI.get()).thinking(true).build());

    public static final Supplier<@NonNull BasicLLM> GPT_o3_MINI = Suppliers.memoize(() ->
            builder().model("o3-mini").provider(OPENAI.get()).thinking(true).build());

    public static final Supplier<@NonNull BasicLLM> GPT_4_5_PREVIEW = Suppliers.memoize(() -> builder()
            .model("gpt-4.5-preview")
            .provider(OPENAI.get())
            .thinking(true)
            .build());

    public static final Supplier<@NonNull BasicLLM> GPT_4o = Suppliers.memoize(() -> builder()
            .model("chatgpt-4o-latest")
            .provider(OPENAI.get())
            .thinking(false)
            .build());

    public static final Supplier<@NonNull BasicLLM> GPT_4o_MINI = Suppliers.memoize(() -> builder()
            .model("gpt-4o-mini")
            .provider(OPENAI.get())
            .thinking(false)
            .build());

    public static final Supplier<@NonNull BasicLLM> CLAUDE_3_7_SONNET = Suppliers.memoize(() -> builder()
            .model("claude-3-7-sonnet-20250219")
            .provider(ANTHROPIC.get())
            .thinking(false)
            .build());

    public static final Supplier<@NonNull BasicLLM> CLAUDE_3_7_SONNET_THINKING = Suppliers.memoize(() -> builder()
            .model("claude-3-7-sonnet-20250219")
            .provider(ANTHROPIC.get())
            .thinking(true)
            .build());

    public static final Supplier<@NonNull BasicLLM> DEEPSEEK_R1 = Suppliers.memoize(() -> builder()
            .model("deepseek-reasoner")
            .provider(DEEPSEEK.get())
            .thinking(true)
            .build());

    public static final Supplier<@NonNull BasicLLM> DEEPSEEK_V3 = Suppliers.memoize(() -> builder()
            .model("deepseek-chat")
            .provider(DEEPSEEK.get())
            .thinking(false)
            .maxTokens(8 * 1024)
            .build());

    public static final Supplier<@NonNull BasicLLM> DEEPSEEK_R1_INF = Suppliers.memoize(() -> builder()
            .model("deepseek/deepseek-r1/fp-8")
            .provider(INFERENCE.get())
            .thinking(false)
            .build());

    public static final Supplier<@NonNull BasicLLM> DEEPSEEK_V3_INF = Suppliers.memoize(() -> builder()
            .model("deepseek/deepseek-v3-0324/fp-8")
            .provider(INFERENCE.get())
            .thinking(false)
            .build());

    public static final Supplier<@NonNull BasicLLM> LLAMA_3_3 = Suppliers.memoize(() -> builder()
            .model("llama-3.3-70b-versatile")
            .provider(GROQ.get())
            .thinking(false)
            .build());

    public static final Supplier<@NonNull BasicLLM> DEEPSEEK_R1_DISTILL = Suppliers.memoize(() -> builder()
            .model("deepseek-r1-distill-llama-70b")
            .provider(GROQ.get())
            .thinking(true)
            .build());

    public static final Supplier<@NonNull BasicLLM> QWEN_2_5_CODER = Suppliers.memoize(() -> builder()
            .model("qwen-2.5-coder-32b")
            .provider(GROQ.get())
            .thinking(false)
            .build());

    public static final Supplier<@NonNull BasicLLM> QWEN_QWQ = Suppliers.memoize(() -> builder()
            .model("qwen-qwq-32b")
            .provider(GROQ.get())
            .thinking(true)
            .temperature(0.5)
            .topP(0.95)
            .build());

    public static final Supplier<@NonNull BasicLLM> GEMINI_2_5_PRO = Suppliers.memoize(() -> builder()
            .model("gemini-2.5-pro-exp-03-25")
            .provider(GOOGLE.get())
            .thinking(true)
            .build());

    public static final Supplier<@NonNull BasicLLM> GEMINI_2_0_FLASH = Suppliers.memoize(() -> builder()
            .model("gemini-2.0-flash")
            .provider(GOOGLE.get())
            .thinking(false)
            .build());

    @Override
    public String answer(String input) {
        return provider.answer(input, this);
    }
}
