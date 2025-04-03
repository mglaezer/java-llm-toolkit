package org.llmtoolkit.providers;

import dev.langchain4j.model.chat.ChatLanguageModel;
import java.util.concurrent.ConcurrentHashMap;
import org.llmtoolkit.core.BasicLLM;
import org.llmtoolkit.providers.chatmodel.ChatModelProvider;

public class CachingLLMProvider implements LLMProvider {

    private final ChatModelProvider chatModelProvider;

    private final ConcurrentHashMap<BasicLLM, ChatLanguageModel> cache = new ConcurrentHashMap<>();

    public CachingLLMProvider(ChatModelProvider chatModelProvider) {
        this.chatModelProvider = chatModelProvider;
    }

    @Override
    public String answer(String input, BasicLLM llm) {
        ChatLanguageModel chatModel = cache.computeIfAbsent(llm, chatModelProvider::createChatModel);
        return chatModel.chat(input);
    }
}
