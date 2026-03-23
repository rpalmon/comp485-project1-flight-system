package com.example;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public final class EnvConfig {
    private static final Path DEFAULT_ENV_PATH = Path.of(".env");

    private final Path sourcePath;
    private final Map<String, String> values;

    private EnvConfig(Path sourcePath, Map<String, String> values) {
        this.sourcePath = sourcePath.toAbsolutePath().normalize();
        this.values = Map.copyOf(values);
    }

    public static EnvConfig loadDefault() {
        return load(DEFAULT_ENV_PATH);
    }

    public static EnvConfig load(Path path) {
        Map<String, String> values = new LinkedHashMap<>();

        if (Files.exists(path)) {
            try {
                for (String rawLine : Files.readAllLines(path)) {
                    String line = rawLine.trim();
                    if (line.isEmpty() || line.startsWith("#")) {
                        continue;
                    }

                    int separatorIndex = line.indexOf('=');
                    if (separatorIndex <= 0) {
                        continue;
                    }

                    String key = line.substring(0, separatorIndex).trim();
                    String value = line.substring(separatorIndex + 1).trim();
                    values.put(key, stripQuotes(value));
                }
            } catch (IOException exception) {
                throw new IllegalStateException("Unable to read " + path.toAbsolutePath(), exception);
            }
        }

        return new EnvConfig(path, values);
    }

    public String get(String key) {
        return values.getOrDefault(key, "");
    }

    public String getOrDefault(String key, String fallback) {
        return values.getOrDefault(key, fallback);
    }

    public Path sourcePath() {
        return sourcePath;
    }

    private static String stripQuotes(String value) {
        if (value.length() >= 2) {
            boolean doubleQuoted = value.startsWith("\"") && value.endsWith("\"");
            boolean singleQuoted = value.startsWith("'") && value.endsWith("'");
            if (doubleQuoted || singleQuoted) {
                return value.substring(1, value.length() - 1);
            }
        }
        return value;
    }
}
