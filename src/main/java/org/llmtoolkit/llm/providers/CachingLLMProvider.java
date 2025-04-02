package org.llmtoolkit.llm.providers;

import dev.langchain4j.model.chat.ChatLanguageModel;
import java.util.concurrent.ConcurrentHashMap;
import org.llmtoolkit.llm.LLM;
import org.llmtoolkit.llm.providers.chatmodel.ChatModelProvider;

public class CachingLLMProvider implements LLMProvider {

    private final ChatModelProvider chatModelProvider;

    private final ConcurrentHashMap<LLM, ChatLanguageModel> cache = new ConcurrentHashMap<>();

    public CachingLLMProvider(ChatModelProvider chatModelProvider) {
        this.chatModelProvider = chatModelProvider;
    }

    @Override
    public String answer(String input, LLM llm) {
        ChatLanguageModel chatModel = cache.computeIfAbsent(llm, chatModelProvider::createChatModel);
        return chatModel.chat(input);
    }
}
