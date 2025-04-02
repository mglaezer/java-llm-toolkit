package org.llmtoolkit.util;

public class PMP {
    public static void profile(Runnable lambda) {
        long startTime = System.nanoTime();
        try {
            lambda.run();
        } finally {
            long endTime = System.nanoTime();
            double duration = (endTime - startTime) / 1_000_000_000.0;
            System.out.format("Time: %.1f seconds\n", duration);
        }
    }
}
