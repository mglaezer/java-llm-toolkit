package org.llmtoolkit.examples;

import java.util.List;
import org.llmtoolkit.core.annotations.Cue;
import org.springframework.lang.Nullable;

/*
 * This record is used in several examples to demonstrate java-llm-toolkit approach to structured LLM outputs.
 * The toolkit passes Java bean definitions directly to the LLM as output format instructions.
 * All annotations (including @Cue) are visible to the LLM and provide additional context.
 * While any annotation can be used, @Cue is recommended for consistency.
 *
 * Note: Inner records are used here only for example convenience rather than being in separate files.
 */
public record ProgrammingLanguages(List<Language> languages) {

    @Cue("Programming language")
    public record Language(@Cue("name with latest version") String name, String description, List<Reason> reasons) {}

    @Cue("A reason for choosing a programming language")
    public record Reason(
            @Cue("Ids are prime numbers strictly in descending order, so that the last one is 2") int id,
            @Cue("One phrase.") String reason,
            @Cue("Describe why the reason is lame or valid") String description,
            @Cue("Valid vs lame flag") boolean isValidReason,
            @Cue("This field is not required, but please feel free to share some thoughts") @Nullable
                    String someField) {}
}
