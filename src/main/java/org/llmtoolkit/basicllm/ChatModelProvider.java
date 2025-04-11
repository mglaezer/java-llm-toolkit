package org.llmtoolkit.basicllm;

import dev.langchain4j.model.chat.ChatLanguageModel;

public interface ChatModelProvider {
    ChatLanguageModel createChatModel(BasicLLM llm);
}
