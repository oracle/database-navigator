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

import com.dbn.assistant.tool.approval.AssistantToolApprovalException;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NonNls;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Verifies synthetic IDE MCP tool arguments before they are forwarded to the IDE MCP server.
 * <p>
 * The verifier preserves the current project boundary by checking the explicit {@code projectPath}
 * argument and by recursively inspecting path-like argument fields for local filesystem paths or
 * {@code file:} URIs that resolve outside the current project root.
 */
@UtilityClass
public class AssistantMcpServerToolVerifier {
    static void validateIdeMcpArguments(Map<String, Object> arguments, String projectPath) {
        if (projectPath == null) return;

        Object projectPathArgument = arguments.get("projectPath");
        if (projectPathArgument != null && !Objects.equals(projectPathArgument.toString(), projectPath)) {
            throw new AssistantToolApprovalException(
                    "IDE MCP request projectPath does not match the current project path");
        }

        Path projectRoot = canonicalPath(projectPath);
        validateIdeMcpArgumentScope(arguments, projectRoot, null);
    }

    private static void validateIdeMcpArgumentScope(Object value, Path projectRoot, String argumentName) {
        if (value instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                Object key = entry.getKey();
                validateIdeMcpArgumentScope(entry.getValue(), projectRoot, key == null ? null : key.toString());
            }
            return;
        }

        if (value instanceof Collection<?> collection) {
            for (Object item : collection) {
                validateIdeMcpArgumentScope(item, projectRoot, argumentName);
            }
            return;
        }

        if (value instanceof CharSequence text) {
            validateIdeMcpPathArgument(argumentName, text.toString(), projectRoot);
        }
    }

    private static void validateIdeMcpPathArgument(String argumentName, String value, Path projectRoot) {
        if (!isPathBearingArgument(argumentName)) return;
        if (!shouldValidatePathValue(argumentName, value)) return;

        Path path;
        try {
            path = resolvePath(value, projectRoot);
        } catch (RuntimeException e) {
            throw new AssistantToolApprovalException(
                    "IDE MCP request path argument '" + argumentName + "' is invalid", e);
        }
        if (path.startsWith(projectRoot)) return;

        throw new AssistantToolApprovalException(
                "IDE MCP request path argument '" + argumentName + "' is outside the current project path");
    }

    private static boolean isPathBearingArgument(String argumentName) {
        if (argumentName == null) return false;

        @NonNls String name = normalizeArgumentName(argumentName);
        // Generic selectors need exact matches; concrete path fields are matched by suffix
        // to cover names such as filePath, sourceFile, and outputDirectory.
        return name.equals("target") ||
                name.equals("source") ||
                name.equals("url") ||
                name.equals("pathinproject") ||
                name.equals("repositorypathrelativetoproject") ||
                name.equals("filestorebuild") ||
                name.endsWith("path") ||
                name.endsWith("filepath") ||
                name.endsWith("file") ||
                name.endsWith("files") ||
                name.endsWith("directory") ||
                name.endsWith("directories") ||
                name.endsWith("dir") ||
                name.endsWith("dirs") ||
                name.endsWith("uri");
    }

    private static boolean shouldValidatePathValue(String argumentName, String value) {
        if (value.isBlank()) return false;

        @NonNls String name = normalizeArgumentName(argumentName);
        // Generic selectors and URLs may be non-filesystem values, so only validate them
        // when their value looks local; URI-suffixed fields follow the same rule.
        if (name.equals("target") || name.equals("source") || name.endsWith("uri") || name.equals("url")) {
            return looksLikeLocalPath(value);
        }
        return true;
    }

    private static boolean looksLikeLocalPath(String value) {
        if (isWindowsAbsolutePath(value)) return true;
        if (hasNonFileUriScheme(value)) return false;

        return isFileUri(value) ||
                value.startsWith("/") ||
                value.startsWith("\\") ||
                value.startsWith(".") ||
                value.contains("/") ||
                value.contains("\\");
    }

    private static Path resolvePath(String value, Path projectRoot) {
        Path path = resolveFileUri(value);
        if (path == null) path = Path.of(value);
        if (isWindowsAbsolutePath(value) && !path.isAbsolute()) {
            return Path.of(value.replace('\\', '/')).normalize();
        }
        if (!path.isAbsolute()) path = projectRoot.resolve(path);
        return canonicalPath(path);
    }

    private static Path resolveFileUri(String value) {
        if (!isFileUri(value)) return null;

        URI uri = URI.create(value);
        if (!"file".equalsIgnoreCase(uri.getScheme())) return null;
        return Path.of(uri);
    }

    private static Path canonicalPath(String path) {
        return canonicalPath(Path.of(path));
    }

    private static Path canonicalPath(Path path) {
        try {
            return path.toFile().getCanonicalFile().toPath();
        } catch (IOException e) {
            return path.toAbsolutePath().normalize();
        }
    }

    private static boolean isWindowsAbsolutePath(String value) {
        return value.matches("^[a-zA-Z]:[\\\\/].*");
    }

    private static boolean hasNonFileUriScheme(String value) {
        return value.matches("^[a-zA-Z][a-zA-Z0-9+.-]*:.*") && !isFileUri(value);
    }

    private static boolean isFileUri(String value) {
        return value.toLowerCase(Locale.ENGLISH).startsWith("file:");
    }

    private static @NonNls String normalizeArgumentName(@NonNls String argumentName) {
        return argumentName.toLowerCase(Locale.ENGLISH);
    }
}
