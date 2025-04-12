package org.llmtoolkit.core;

import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * The annotations force bypassing variable resolution with default templating mechanism in langchain4j.
 * Without it, if the prompt contains {{some_text}}, the attempt to resolve that variable will fail.
 */
public interface StringAnswer {
    @UserMessage("{{raw}}")
    String answer(@V("raw") String input);
}
