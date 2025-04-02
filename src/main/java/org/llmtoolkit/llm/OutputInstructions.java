package org.llmtoolkit.llm;

import org.llmtoolkit.util.ClassToString;

public class OutputInstructions {

    private static final String INSTRUCTIONS_FOOTER =
            """
            Provide only json starting with ```. Stop after json.
            You MUST only produce valid JSON that can be parsed without errors.
            Properly escape " in strings.
            Pay attention to the instructions in @%s annotations.
            """
                    .formatted(Cue.class.getSimpleName());

    public static String singleObjectInstructions(Class<?> clazz) {
        return "\nOutput results in the single json object that corresponds to the jackson java bean '"
                + clazz.getSimpleName() + "':\n\n"
                + ClassToString.onlyRecords(clazz)
                + INSTRUCTIONS_FOOTER;
    }

    public static String arrayInstructions(Class<?> clazz) {
        return "\n"
                + "Output results in the json array of elements [{element1}, ...{elementN}], where each element is a json"
                + " object that corresponds to the jackson java bean '"
                + clazz.getSimpleName() + "':\n\n"
                + ClassToString.onlyRecords(clazz)
                + INSTRUCTIONS_FOOTER;
    }
}
