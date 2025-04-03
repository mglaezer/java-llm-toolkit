package org.llmtoolkit.core;

import static org.llmtoolkit.core.CommonProviders.*;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import lombok.NonNull;

@SuppressWarnings("unused")
public class CommonLLMs {
    public static final Supplier<@NonNull BasicLLM> GPT_o1 = Suppliers.memoize(() ->
            BasicLLM.builder().model("o1").provider(OPENAI.get()).thinking(true).build());

    public static final Supplier<@NonNull BasicLLM> GPT_o3_MINI = Suppliers.memoize(() -> BasicLLM.builder()
            .model("o3-mini")
            .provider(OPENAI.get())
            .thinking(true)
            .build());

    public static final Supplier<@NonNull BasicLLM> GPT_4_5_PREVIEW = Suppliers.memoize(() -> BasicLLM.builder()
            .model("gpt-4.5-preview")
            .provider(OPENAI.get())
            .thinking(true)
            .build());

    public static final Supplier<@NonNull BasicLLM> GPT_4o = Suppliers.memoize(() -> BasicLLM.builder()
            .model("chatgpt-4o-latest")
            .provider(OPENAI.get())
            .thinking(false)
            .build());

    public static final Supplier<@NonNull BasicLLM> GPT_4o_MINI = Suppliers.memoize(() -> BasicLLM.builder()
            .model("gpt-4o-mini")
            .provider(OPENAI.get())
            .thinking(false)
            .build());

    public static final Supplier<@NonNull BasicLLM> CLAUDE_3_7_SONNET = Suppliers.memoize(() -> BasicLLM.builder()
            .model("claude-3-7-sonnet-20250219")
            .provider(ANTHROPIC.get())
            .thinking(false)
            .build());

    public static final Supplier<@NonNull BasicLLM> CLAUDE_3_7_SONNET_THINKING =
            Suppliers.memoize(() -> BasicLLM.builder()
                    .model("claude-3-7-sonnet-20250219")
                    .provider(ANTHROPIC.get())
                    .thinking(true)
                    .build());

    public static final Supplier<@NonNull BasicLLM> DEEPSEEK_R1 = Suppliers.memoize(() -> BasicLLM.builder()
            .model("deepseek-reasoner")
            .provider(DEEPSEEK.get())
            .thinking(true)
            .build());

    public static final Supplier<@NonNull BasicLLM> DEEPSEEK_V3 = Suppliers.memoize(() -> BasicLLM.builder()
            .model("deepseek-chat")
            .provider(DEEPSEEK.get())
            .thinking(false)
            .maxTokens(8 * 1024)
            .build());

    public static final Supplier<@NonNull BasicLLM> DEEPSEEK_R1_INF = Suppliers.memoize(() -> BasicLLM.builder()
            .model("deepseek/deepseek-r1/fp-8")
            .provider(INFERENCE.get())
            .thinking(false)
            .build());

    public static final Supplier<@NonNull BasicLLM> DEEPSEEK_V3_INF = Suppliers.memoize(() -> BasicLLM.builder()
            .model("deepseek/deepseek-v3-0324/fp-8")
            .provider(INFERENCE.get())
            .thinking(false)
            .build());

    public static final Supplier<@NonNull BasicLLM> LLAMA_3_3 = Suppliers.memoize(() -> BasicLLM.builder()
            .model("llama-3.3-70b-versatile")
            .provider(GROQ.get())
            .thinking(false)
            .build());

    public static final Supplier<@NonNull BasicLLM> DEEPSEEK_R1_DISTILL = Suppliers.memoize(() -> BasicLLM.builder()
            .model("deepseek-r1-distill-llama-70b")
            .provider(GROQ.get())
            .thinking(true)
            .build());

    public static final Supplier<@NonNull BasicLLM> QWEN_2_5_CODER = Suppliers.memoize(() -> BasicLLM.builder()
            .model("qwen-2.5-coder-32b")
            .provider(GROQ.get())
            .thinking(false)
            .build());

    public static final Supplier<@NonNull BasicLLM> QWEN_QWQ = Suppliers.memoize(() -> BasicLLM.builder()
            .model("qwen-qwq-32b")
            .provider(GROQ.get())
            .thinking(true)
            .temperature(0.5)
            .topP(0.95)
            .build());

    public static final Supplier<@NonNull BasicLLM> GEMINI_2_5_PRO = Suppliers.memoize(() -> BasicLLM.builder()
            .model("gemini-2.5-pro-exp-03-25")
            .provider(GOOGLE.get())
            .thinking(true)
            .build());

    public static final Supplier<@NonNull BasicLLM> GEMINI_2_0_FLASH = Suppliers.memoize(() -> BasicLLM.builder()
            .model("gemini-2.0-flash")
            .provider(GOOGLE.get())
            .thinking(false)
            .build());

    private CommonLLMs() {
        // Prevent instantiation
    }
}
