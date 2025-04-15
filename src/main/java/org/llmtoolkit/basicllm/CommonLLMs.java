package org.llmtoolkit.basicllm;

import lombok.experimental.UtilityClass;

/**
 * Consider this a living reference that captures a snapshot of available models at a given time.
 */
@SuppressWarnings("unused")
@UtilityClass
public class CommonLLMs {
    // OpenAI Models
    public static final String GPT_O1 = "o1";
    public static final String GPT_O3_MINI = "o3-mini";
    public static final String GPT_4_1 = "gpt-4.1";
    public static final String GPT_4O = "gpt-4o";
    public static final String GPT_4O_LATEST = "chatgpt-4o-latest";
    public static final String GPT_4O_MINI = "gpt-4o-mini";

    // Anthropic Models
    public static final String CLAUDE_3_7_SONNET = "claude-3-7-sonnet-20250219";

    // Groq Models
    public static final String LLAMA_3_3 = "llama-3.3-70b-versatile";
    public static final String DEEPSEEK_R1_DISTILL = "deepseek-r1-distill-llama-70b";
    public static final String QWEN_2_5_CODER = "qwen-2.5-coder-32b";
    public static final String QWEN_QWQ = "qwen-qwq-32b";

    // Google Models
    public static final String GEMINI_2_5_PRO = "gemini-2.5-pro-exp-03-25";
    public static final String GEMINI_2_0_FLASH = "gemini-2.0-flash";
}
