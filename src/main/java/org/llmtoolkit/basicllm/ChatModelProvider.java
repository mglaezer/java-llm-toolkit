package org.llmtoolkit.basicllm;

import dev.langchain4j.model.chat.ChatModel;

public interface ChatModelProvider {
    ChatModel createChatModel(BasicLLM llm);
}
