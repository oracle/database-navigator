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

package com.dbn.assistant;

import com.dbn.assistant.provider.AIProvider;
import com.dbn.assistant.provider.AIProviderData;
import com.dbn.assistant.provider.ProviderUrlType;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@UtilityClass
public class AssistantErrorMessages {
    private static final Pattern URL_PATTERN = Pattern.compile("(?i)\\bhttps?://[^\\s<>'\"`]+");
    @NonNls
    private static final String REDACTED_URL = "[redacted URL]";
    private static final List<TrustedApiPath> TRUSTED_API_PATHS = loadTrustedApiPaths();

    public static @Nullable String sanitizeUrls(@Nullable String message) {
        if (message == null) return null;
        return URL_PATTERN.matcher(message).replaceAll(r -> sanitizeUrl(r.group()));
    }

    public static String sanitizeUrl(String url) {
        try {
            URI uri = new URI(url);
            String scheme = uri.getScheme();
            String host = uri.getHost();
            if (scheme == null || host == null) return REDACTED_URL;

            StringBuilder builder = new StringBuilder();
            builder.append(scheme.toLowerCase(Locale.ROOT)).append("://");
            appendHost(host, builder);

            int port = uri.getPort();
            if (port != -1) {
                builder.append(":").append(port);
            }

            if (isTrustedApiPath(uri)) {
                builder.append(uri.getRawPath());
            }

            return builder.toString();
        } catch (URISyntaxException e) {
            return REDACTED_URL;
        }
    }

    private static void appendHost(String host, StringBuilder builder) {
        if (host.contains(":") && !host.startsWith("[")) {
            builder.append("[").append(host).append("]");
        } else {
            builder.append(host);
        }
    }

    private static boolean isTrustedApiPath(URI uri) {
        return TRUSTED_API_PATHS.stream().anyMatch(w -> w.matches(uri));
    }

    private static List<TrustedApiPath> loadTrustedApiPaths() {
        try {
            return AIProviderData.getAllProviders().stream()
                    .map(AssistantErrorMessages::createTrustedApiPath)
                    .filter(w -> w != null)
                    .distinct()
                    .toList();
        } catch (Throwable t) {
            // Keep error rendering fail-safe if provider metadata is unavailable.
            return Collections.emptyList();
        }
    }

    private static @Nullable TrustedApiPath createTrustedApiPath(AIProvider provider) {
        String url = provider.getUrl(ProviderUrlType.API);
        if (url == null) return null;

        try {
            URI uri = new URI(url);
            if (!"https".equalsIgnoreCase(uri.getScheme())) return null;

            String host = uri.getHost();
            if (host == null) return null;

            String pathPrefix = normalizePathPrefix(uri.getRawPath());
            if (pathPrefix == null) return null;

            return new TrustedApiPath(host, uri.getPort(), pathPrefix);
        } catch (URISyntaxException e) {
            return null;
        }
    }

    private static @Nullable String normalizePathPrefix(String path) {
        if (path == null || path.isBlank() || path.equals("/")) return null;
        while (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        return path.isEmpty() ? null : path;
    }

    private record TrustedApiPath(String host, int port, String pathPrefix) {
        private boolean matches(URI uri) {
            if (!"https".equalsIgnoreCase(uri.getScheme())) return false;

            String uriHost = uri.getHost();
            if (!host.equalsIgnoreCase(uriHost)) return false;

            int uriPort = uri.getPort();
            if (port == -1) {
                if (uriPort != -1 && uriPort != 443) return false;
            } else if (uriPort != port) {
                return false;
            }

            String path = uri.getRawPath();
            return path != null && (path.equals(pathPrefix) || path.startsWith(pathPrefix + "/"));
        }
    }
}
