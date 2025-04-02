package org.llmtoolkit.util;

import io.github.cdimascio.dotenv.Dotenv;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

@Slf4j
public class Env {

    private static final Dotenv dotenv =
            Dotenv.configure().ignoreIfMissing().systemProperties().load();

    public static String getRequired(String name) {

        String value = dotenv.get(name);

        if (!StringUtils.hasText(value)) {
            throw new RuntimeException("Missing required environment variable: " + name);
        }

        return value.strip();
    }
}
