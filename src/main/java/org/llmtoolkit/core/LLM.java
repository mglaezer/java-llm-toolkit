package org.llmtoolkit.core;

import lombok.Getter;
import org.llmtoolkit.providers.AltOpenAiLLMProvider;
import org.llmtoolkit.providers.CachingLLMProvider;
import org.llmtoolkit.providers.LLMProvider;
import org.llmtoolkit.providers.chatmodel.AnthropicChatModelProvider;
import org.llmtoolkit.providers.chatmodel.GoogleChatModelProvider;
import org.llmtoolkit.providers.chatmodel.OpenAiChatModelProvider;
import org.llmtoolkit.util.Env;

@SuppressWarnings({"unused", "LombokGetterMayBeUsed"})
public record LLM(
        String model,
        LLMProvider provider,
        Double temperature,
        Double topP,
        boolean thinking,
        ReasoningEffort reasoningEffort,
        Integer maxTokens,
        Integer thinkingTokens,
        Integer timeout)
        implements StringAnswer {

    public String getModel() {
        return model;
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

    private static final LLMProvider OPENAI =
            new CachingLLMProvider(new OpenAiChatModelProvider(null, Env.getRequired("OPENAI_API_KEY")));

    private static final LLMProvider GROQ = new CachingLLMProvider(
            new OpenAiChatModelProvider(Env.getRequired("GROQ_API_KEY"), "https://api.groq.com/openai/v1/"));

    private static final LLMProvider ANTHROPIC = new CachingLLMProvider(new AnthropicChatModelProvider());
    private static final LLMProvider GOOGLE = new CachingLLMProvider(new GoogleChatModelProvider());

    private static final LLMProvider DEEPSEEK =
            new AltOpenAiLLMProvider("https://api.deepseek.com/v1", Env.getRequired("DEEPSEEK_API_KEY"));

    private static final LLMProvider INFERENCE =
            new AltOpenAiLLMProvider("https://api.inference.net/v1", Env.getRequired("INFERENCE_API_KEY"));

    public static final LLM GPT_o1 = of("o1", OPENAI, true);
    public static final LLM GPT_o3_MINI = of("o3-mini", OPENAI, true);
    public static final LLM GPT_4_5_PREVIEW = of("gpt-4.5-preview", OPENAI, true);
    public static final LLM GPT_4o = of("chatgpt-4o-latest", OPENAI, false);
    public static final LLM GPT_4o_MINI = of("gpt-4o-mini", OPENAI, false);

    public static final LLM CLAUDE_3_7_SONNET = of("claude-3-7-sonnet-20250219", ANTHROPIC, false);
    public static final LLM CLAUDE_3_7_SONNET_THINKING = of("claude-3-7-sonnet-20250219", ANTHROPIC, true);

    public static final LLM DEEPSEEK_R1 = of("deepseek-reasoner", DEEPSEEK, true);
    public static final LLM DEEPSEEK_V3 = of("deepseek-chat", DEEPSEEK, false).maxTokens(8 * 1024);

    public static final LLM DEEPSEEK_R1_INF = of("deepseek/deepseek-r1/fp-8", INFERENCE, false);
    public static final LLM DEEPSEEK_V3_INF = of("deepseek/deepseek-v3-0324/fp-8", INFERENCE, false);

    public static final LLM LLAMA_3_3 = of("llama-3.3-70b-versatile", GROQ, false);
    public static final LLM DEEPSEEK_R1_DISTILL = of("deepseek-r1-distill-llama-70b", GROQ, true);
    public static final LLM QWEN_2_5_CODER = of("qwen-2.5-coder-32b", GROQ, false);
    public static final LLM QWEN_QWQ = of("qwen-qwq-32b", GROQ, true).t(0.5).topP(0.95);

    public static final LLM GEMINI_2_5_PRO = of("gemini-2.5-pro-exp-03-25", GOOGLE, true);
    public static final LLM GEMINI_2_0_FLASH = of("gemini-2.0-flash", GOOGLE, false);

    // Factory method to replace constructor
    public static LLM of(String model, LLMProvider provider, boolean thinking) {
        return new LLM(model, provider, null, null, thinking, null, null, null, null);
    }

    // Methods to create new instances with modified values
    public LLM t(Double temp) {
        return new LLM(model, provider, temp, topP, thinking, reasoningEffort, maxTokens, thinkingTokens, timeout);
    }

    public LLM topP(Double topP) {
        return new LLM(
                model, provider, temperature, topP, thinking, reasoningEffort, maxTokens, thinkingTokens, timeout);
    }

    public LLM maxTokens(Integer maxTokens) {
        return new LLM(
                model, provider, temperature, topP, thinking, reasoningEffort, maxTokens, thinkingTokens, timeout);
    }

    public LLM thinkingTokens(Integer thinkingTokens) {
        return new LLM(
                model, provider, temperature, topP, thinking, reasoningEffort, maxTokens, thinkingTokens, timeout);
    }

    public LLM reasoningEffort(ReasoningEffort effort) {
        return new LLM(model, provider, temperature, topP, thinking, effort, maxTokens, thinkingTokens, timeout);
    }

    public LLM timeout(Integer seconds) {
        return new LLM(
                model, provider, temperature, topP, thinking, reasoningEffort, maxTokens, thinkingTokens, seconds);
    }

    @Override
    public String answer(String input) {
        return provider.answer(input, this);
    }
}
