package org.llmtoolkit.core;

import static org.llmtoolkit.core.CommonProviders.*;

import com.google.common.base.Supplier;
import lombok.NonNull;
import lombok.experimental.UtilityClass;

@SuppressWarnings("unused")
@UtilityClass
public class CommonLLMs {
    public static final Supplier<@NonNull BasicLLM> GPT_o1 = BasicLLM.of("o1", OPENAI.get(), true);

    public static final Supplier<@NonNull BasicLLM> GPT_o3_MINI = BasicLLM.of("o3-mini", OPENAI.get(), true);

    public static final Supplier<@NonNull BasicLLM> GPT_4_5_PREVIEW =
            BasicLLM.of("gpt-4.5-preview", OPENAI.get(), true);

    public static final Supplier<@NonNull BasicLLM> GPT_4o = BasicLLM.of("gpt-4o", OPENAI.get());

    public static final Supplier<@NonNull BasicLLM> GPT_4o_LATEST = BasicLLM.of("chatgpt-4o-latest", OPENAI.get());

    public static final Supplier<@NonNull BasicLLM> GPT_4o_MINI = BasicLLM.of("gpt-4o-mini", OPENAI.get());

    public static final Supplier<@NonNull BasicLLM> CLAUDE_3_7_SONNET =
            BasicLLM.of("claude-3-7-sonnet-20250219", ANTHROPIC.get());

    public static final Supplier<@NonNull BasicLLM> CLAUDE_3_7_SONNET_THINKING =
            BasicLLM.of("claude-3-7-sonnet-20250219", ANTHROPIC.get(), true);

    public static final Supplier<@NonNull BasicLLM> DEEPSEEK_R1 =
            BasicLLM.of("deepseek-reasoner", DEEPSEEK.get(), true);

    public static final Supplier<@NonNull BasicLLM> DEEPSEEK_V3 = BasicLLM.of("deepseek-chat", DEEPSEEK.get());

    public static final Supplier<@NonNull BasicLLM> DEEPSEEK_R1_INF =
            BasicLLM.of("deepseek/deepseek-r1/fp-8", INFERENCE.get());

    public static final Supplier<@NonNull BasicLLM> DEEPSEEK_V3_INF =
            BasicLLM.of("deepseek/deepseek-v3-0324/fp-8", INFERENCE.get());

    public static final Supplier<@NonNull BasicLLM> LLAMA_3_3 = BasicLLM.of("llama-3.3-70b-versatile", GROQ.get());

    public static final Supplier<@NonNull BasicLLM> DEEPSEEK_R1_DISTILL =
            BasicLLM.of("deepseek-r1-distill-llama-70b", GROQ.get(), true);

    public static final Supplier<@NonNull BasicLLM> QWEN_2_5_CODER = BasicLLM.of("qwen-2.5-coder-32b", GROQ.get());

    public static final Supplier<@NonNull BasicLLM> QWEN_QWQ = BasicLLM.of("qwen-qwq-32b", GROQ.get(), true);

    public static final Supplier<@NonNull BasicLLM> GEMINI_2_5_PRO =
            BasicLLM.of("gemini-2.5-pro-exp-03-25", GOOGLE.get(), true);

    public static final Supplier<@NonNull BasicLLM> GEMINI_2_0_FLASH = BasicLLM.of("gemini-2.0-flash", GOOGLE.get());
}
