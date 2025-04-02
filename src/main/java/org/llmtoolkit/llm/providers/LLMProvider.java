package org.llmtoolkit.llm.providers;

import org.llmtoolkit.llm.LLM;

public interface LLMProvider {
    String answer(String input, LLM llm);
}
