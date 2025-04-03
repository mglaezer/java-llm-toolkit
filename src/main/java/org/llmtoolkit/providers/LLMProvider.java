package org.llmtoolkit.providers;

import org.llmtoolkit.core.BasicLLM;

public interface LLMProvider {
    String answer(String input, BasicLLM llm);
}
