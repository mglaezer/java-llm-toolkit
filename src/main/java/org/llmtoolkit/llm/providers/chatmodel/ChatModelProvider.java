package org.llmtoolkit.llm.providers.chatmodel;

import dev.langchain4j.model.chat.ChatLanguageModel;
import org.llmtoolkit.llm.LLM;

public interface ChatModelProvider {
    ChatLanguageModel createChatModel(LLM llm);
}
