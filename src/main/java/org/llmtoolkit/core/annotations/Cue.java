package org.llmtoolkit.core.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Cue for the LLM to follow regarding the annotated element.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({
    ElementType.ANNOTATION_TYPE,
    ElementType.CONSTRUCTOR,
    ElementType.FIELD,
    ElementType.LOCAL_VARIABLE,
    ElementType.METHOD,
    ElementType.MODULE,
    ElementType.PACKAGE,
    ElementType.PARAMETER,
    ElementType.RECORD_COMPONENT,
    ElementType.TYPE,
    ElementType.TYPE_PARAMETER,
    ElementType.TYPE_USE
})
public @interface Cue {
    String value();
}
