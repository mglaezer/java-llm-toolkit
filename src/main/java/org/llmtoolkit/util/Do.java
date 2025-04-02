package org.llmtoolkit.util;

public class Do {
    private boolean done;
    private final Runnable action;

    private Do(Runnable action, boolean executeNow) {
        this.action = action;
        if (executeNow) {
            once();
        }
    }

    public static Do once(Runnable action, boolean executeNow) {
        return new Do(action, executeNow);
    }

    public void once() {
        if (!done) {
            action.run();
            done = true;
        }
    }
}
