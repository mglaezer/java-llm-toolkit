package org.llmtoolkit.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import lombok.extern.log4j.Log4j2;
import org.springframework.util.StringUtils;

@Log4j2
public class Env {

    private static final Properties properties = new Properties();

    static {
        try (InputStream input = Env.class.getResourceAsStream("/env.overrides")) {
            if (input == null) log.warn("Unable to find env.overrides file, will use environment variables only");
            else properties.load(input);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getRequired(String name) {

        if (properties.containsKey(name)) {
            return properties.getProperty(name);
        }

        String value = System.getenv(name);

        if (!StringUtils.hasText(value)) {
            throw new RuntimeException("Missing required environment variable: " + name);
        }

        return value.strip();
    }
}
