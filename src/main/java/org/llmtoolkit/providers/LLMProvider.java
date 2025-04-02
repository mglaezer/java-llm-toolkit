package org.llmtoolkit.providers;

import org.llmtoolkit.core.LLM;

public interface LLMProvider {
    String answer(String input, LLM llm);
}
