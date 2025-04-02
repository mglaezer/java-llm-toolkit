package org.llmtoolkit.providers.chatmodel;

import dev.langchain4j.model.chat.ChatLanguageModel;
import org.llmtoolkit.core.LLM;

public interface ChatModelProvider {
    ChatLanguageModel createChatModel(LLM llm);
}
