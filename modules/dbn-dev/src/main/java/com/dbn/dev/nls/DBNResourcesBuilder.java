/*
 * Copyright 2026 Oracle and/or its affiliates
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dbn.dev.nls;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Rebuilds all DBN resource bundles from the checked-in template.
 * Template blocks use placeholders such as {@code [CFG_KEYS]}.
 */
public final class DBNResourcesBuilder {
    private static final Path RESOURCE_DIRECTORY =
            Path.of("src/main/resources/messages");
    private static final String RESOURCE_PREFIX = "DBNResources";
    private static final Path TEMPLATE_PATH =
            Path.of("modules/dbn-dev/src/main/resources/messages/DBNResources.template.properties");
    private static final Pattern KEYS_PLACEHOLDER =
            Pattern.compile("^\\s*\\[([A-Za-z][A-Za-z0-9]*)_KEYS\\]\\s*$");

    private DBNResourcesBuilder() {
    }

    public static void main(String[] args) throws IOException {
        if (args.length > 1) {
            throw new IllegalArgumentException(
                    "Usage: DBNResourcesBuilder [template.properties]");
        }

        Path projectRoot = findProjectRoot();
        Path templatePath = args.length == 0
                ? projectRoot.resolve(TEMPLATE_PATH)
                : Path.of(args[0]).toAbsolutePath();
        String template = Files.readString(templatePath, StandardCharsets.UTF_8);

        try (Stream<Path> files = Files.list(projectRoot.resolve(RESOURCE_DIRECTORY))) {
            List<Path> resourceFiles = files
                    .filter(Files::isRegularFile)
                    .filter(DBNResourcesBuilder::isResourceFile)
                    .sorted()
                    .toList();
            if (resourceFiles.isEmpty()) {
                throw new IllegalStateException("No NLS resource files found in " + RESOURCE_DIRECTORY);
            }

            for (Path resourceFile : resourceFiles) {
                String source = Files.readString(resourceFile, StandardCharsets.UTF_8);
                String rebuilt = rebuild(source, template);
                Files.writeString(resourceFile, rebuilt, StandardCharsets.UTF_8);
                System.out.println("Rebuilt " + resourceFile);
            }
        }
    }

    static String rebuild(String source, String template) {
        Map<String, List<PropertyValue>> categories = readSourceCategories(source);
        List<String> templateLines = splitLines(template);
        List<String> rebuiltLines = new ArrayList<>();
        Set<String> templateCategories = new LinkedHashSet<>();

        for (int i = 0; i < templateLines.size(); i++) {
            String line = templateLines.get(i);
            Matcher placeholder = KEYS_PLACEHOLDER.matcher(line);
            if (placeholder.matches()) {
                String category = placeholder.group(1).toLowerCase(Locale.ROOT);
                if (!templateCategories.add(category)) {
                    throw new IllegalArgumentException(
                            "Duplicate category placeholder in template: " + placeholder.group(1));
                }

                List<PropertyValue> properties = categories.get(category);
                if (properties == null) {
                    throw new IllegalArgumentException(
                            "Category placeholder has no matching source keys: " + placeholder.group(1));
                }
                properties.sort(Comparator.comparing(PropertyValue::key));
                for (PropertyValue property : properties) {
                    rebuiltLines.add(toPropertyLine(property.key(), property.value()));
                }
            } else {
                if (!line.isBlank() && !isComment(line)) {
                    throw new IllegalArgumentException(
                            "Template contains a resource entry instead of a category placeholder at line " + (i + 1));
                }
                rebuiltLines.add(line);
            }
        }

        Set<String> missingCategories = new LinkedHashSet<>(categories.keySet());
        missingCategories.removeAll(templateCategories);
        if (!missingCategories.isEmpty()) {
            throw new IllegalArgumentException(
                    "Source categories missing from template: " + String.join(", ", missingCategories));
        }

        return String.join("\n", rebuiltLines) + "\n";
    }

    private static Map<String, List<PropertyValue>> readSourceCategories(String source) {
        Properties properties = new Properties();
        try {
            properties.load(new StringReader(source));
        } catch (IOException exception) {
            throw new IllegalArgumentException("Could not load resource properties", exception);
        }

        Map<String, List<PropertyValue>> categories = new LinkedHashMap<>();
        for (String key : properties.stringPropertyNames()) {
            categories.computeIfAbsent(categoryOf(key), ignored -> new ArrayList<>())
                    .add(new PropertyValue(key, properties.getProperty(key)));
        }
        return categories;
    }

    private static String toPropertyLine(String key, String value) {
        return escape(key, true) + "=" + escape(value, false);
    }

    private static String escape(String value, boolean key) {
        StringBuilder result = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char character = value.charAt(i);
            switch (character) {
                case '\\' -> result.append("\\\\");
                case '\t' -> result.append("\\t");
                case '\n' -> result.append("\\n");
                case '\f' -> result.append("\\f");
                case '\r' -> result.append("\\r");
                case ' ', '=', ':' -> {
                    if (key || (character == ' ' && i == 0)) {
                        result.append('\\');
                    }
                    result.append(character);
                }
                case '#', '!' -> {
                    if (i == 0) {
                        result.append('\\');
                    }
                    result.append(character);
                }
                default -> result.append(character);
            }
        }
        return result.toString();
    }

    private static String categoryOf(String key) {
        int dot = key.indexOf('.');
        if (dot <= 0) {
            throw new IllegalArgumentException("Resource key has no category: " + key);
        }
        return key.substring(0, dot).toLowerCase(Locale.ROOT);
    }

    private static boolean isComment(String line) {
        String trimmed = line.stripLeading();
        return trimmed.startsWith("#") || trimmed.startsWith("!");
    }

    private static Path findProjectRoot() {
        Path directory = Path.of("").toAbsolutePath();
        while (directory != null) {
            if (Files.isDirectory(directory.resolve(RESOURCE_DIRECTORY))) {
                return directory;
            }
            directory = directory.getParent();
        }
        throw new IllegalStateException("Could not locate " + RESOURCE_DIRECTORY);
    }

    private static boolean isResourceFile(Path path) {
        String fileName = path.getFileName().toString();
        return fileName.startsWith(RESOURCE_PREFIX) && fileName.endsWith(".properties");
    }

    private static List<String> splitLines(String source) {
        String normalized = source.replace("\r\n", "\n").replace('\r', '\n');
        return normalized.lines().toList();
    }

    private record PropertyValue(String key, String value) {
    }
}
