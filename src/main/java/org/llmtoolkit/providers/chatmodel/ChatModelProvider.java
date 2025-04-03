package org.llmtoolkit.providers.chatmodel;

import dev.langchain4j.model.chat.ChatLanguageModel;
import org.llmtoolkit.core.BasicLLM;

public interface ChatModelProvider {
    ChatLanguageModel createChatModel(BasicLLM llm);
}
