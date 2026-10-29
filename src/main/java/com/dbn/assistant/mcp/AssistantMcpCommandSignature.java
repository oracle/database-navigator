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

package com.dbn.assistant.mcp;

import com.dbn.common.checksum.file.FileContentSignature;
import com.dbn.common.checksum.file.FileFingerprint;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static com.dbn.common.checksum.Checksum.fromStringContent;
import static com.dbn.common.checksum.ChecksumType.SHA_256;
import static com.dbn.common.util.Executables.resolveExecutableFile;

/**
 * Captures an executable and referenced command files for STDIO MCP servers,
 * binding approval to their canonical paths and contents rather than file attributes.
 */
@UtilityClass
final class AssistantMcpCommandSignature {

    static AssistantMcpCommand capture(List<String> commandTokens) {
        if (commandTokens == null || commandTokens.isEmpty()) {
            throw new IllegalArgumentException("MCP server command executable is required");
        }

        String command = commandTokens.get(0);
        File executable = resolveExecutableFile(command);
        if (executable == null) {
            throw new IllegalArgumentException("MCP server command executable could not be resolved: " + command);
        }

        Map<String, FileFingerprint> fingerprints = new LinkedHashMap<>();
        addFingerprint(executable, fingerprints);
        for (int i = 1; i < commandTokens.size(); i++) {
            String argument = commandTokens.get(i);

            String option = getOptionName(argument);
            if (isPathListOption(option)) {
                int separator = argument.indexOf('=');
                if (separator >= 0) {
                    addPathList(argument.substring(separator + 1), fingerprints);
                } else if (i + 1 < commandTokens.size()) {
                    addPathList(commandTokens.get(++i), fingerprints);
                }
                continue;
            }

            addFingerprint(new File(argument), fingerprints);
            addFileOptionValue(argument, fingerprints);
        }

        StringBuilder content = new StringBuilder();
        fingerprints.values().forEach(fingerprint -> appendFingerprint(content, fingerprint));
        String signature = fromStringContent(content.toString(), SHA_256);
        return new AssistantMcpCommand(signature, commandTokens);
    }

    static void verify(@Nullable AssistantMcpCommand command) {
        if (command == null) return;
        List<String> tokens = command.tokens();
        String signature = command.signature();

        AssistantMcpCommand actual = capture(tokens);
        if (!signature.equals(actual.signature())) {
            throw new IllegalStateException("MCP server command files changed after approval: " + tokens);
        }
    }

    private static void addFileOptionValue(String argument, Map<String, FileFingerprint> fingerprints) {
        int separator = argument.indexOf('=');
        if (separator < 0 || separator == argument.length() - 1) return;
        addFingerprint(new File(argument.substring(separator + 1)), fingerprints);
    }

    private static String getOptionName(String argument) {
        int separator = argument.indexOf('=');
        return separator < 0 ? argument : argument.substring(0, separator);
    }

    private static boolean isPathListOption(String option) {
        return "-cp".equals(option) ||
                "-classpath".equals(option) ||
                "--class-path".equals(option) ||
                "-p".equals(option) ||
                "--module-path".equals(option);
    }

    private static void addPathList(String value, Map<String, FileFingerprint> fingerprints) {
        for (String path : value.split(Pattern.quote(File.pathSeparator), -1)) {
            if (path.isEmpty()) path = ".";
            if (path.endsWith("*")) {
                File parent = new File(path).getParentFile();
                addFingerprint(parent == null ? new File(".") : parent, fingerprints);
                continue;
            }
            addFingerprint(new File(path), fingerprints);
        }
    }

    private static void addFingerprint(File file, Map<String, FileFingerprint> fingerprints) {
        if (file.isDirectory()) {
            FileFingerprint fingerprint = captureDirectory(file);
            fingerprints.putIfAbsent(fingerprint.canonicalPath(), fingerprint);
            return;
        }
        if (!file.isFile()) return;

        FileFingerprint fingerprint = FileContentSignature.capture(file);
        fingerprints.putIfAbsent(fingerprint.canonicalPath(), fingerprint);
    }

    private static FileFingerprint captureDirectory(File directory) {
        Path root = getCanonicalPath(directory);
        StringBuilder content = new StringBuilder();
        try (Stream<Path> paths = Files.walk(root, FileVisitOption.FOLLOW_LINKS)) {
            paths.filter(Files::isRegularFile)
                    .sorted(Comparator.comparing(Path::toString))
                    .forEach(path -> {
                        appendToken(content, root.relativize(path).toString());
                        appendFingerprint(content, FileContentSignature.capture(path.toFile()));
                    });
        } catch (IOException | UncheckedIOException e) {
            throw new IllegalStateException("Unable to fingerprint MCP command directory: " + directory, e);
        }

        return new FileFingerprint(root.toString(), fromStringContent(content.toString(), SHA_256));
    }

    private static Path getCanonicalPath(File file) {
        try {
            return file.getCanonicalFile().toPath();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void appendFingerprint(StringBuilder content, FileFingerprint fingerprint) {
        String path = fingerprint.canonicalPath();
        String checksum = fingerprint.checksum();
        content.append(path.length()).append(':').append(path)
                .append(checksum.length()).append(':').append(checksum);
    }

    private static void appendToken(StringBuilder content, String token) {
        content.append(token.length()).append(':').append(token);
    }

}
