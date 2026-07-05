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

package com.dbn.mcp.build;

import com.dbn.common.database.DatabaseInfo;
import com.dbn.common.util.Strings;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.DatabaseUrlType;
import com.dbn.connection.config.ConnectionDatabaseSettings;
import com.dbn.connection.config.tns.TnsNamesParser;
import com.dbn.connection.config.tns.TnsProfile;
import com.dbn.mcp.model.McpServerDefinition;
import com.dbn.mcp.model.McpToolDefinition;
import com.dbn.mcp.model.McpToolParam;
import com.dbn.mcp.util.SqlParameterParser;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NonNls;

import java.io.File;
import java.nio.file.Paths;
import java.util.List;

import static com.dbn.common.util.JdbcUrls.redactSensitiveParameters;
import static com.dbn.nls.NlsResources.txt;

@RequiredArgsConstructor
final class McpServerConfigBuilder {
    private final ConnectionHandler connection;
    private final McpServerDefinition definition;

    String build() {
        return build(definition, getRedactedConnectionUrl());
    }

    String getRedactedConnectionUrl() {
        ConnectionDatabaseSettings databaseSettings = connection.getSettings().getDatabaseSettings();
        DatabaseInfo info = connection.getDatabaseInfo();
        DatabaseUrlType urlType = info.getUrlType();

        if (urlType == DatabaseUrlType.TNS) {
            return redactSensitiveParameters(resolveTnsDescriptorUrl(info));
        }

        return redactSensitiveParameters(databaseSettings.getConnectionUrl());
    }

    private String resolveTnsDescriptorUrl(DatabaseInfo info) {
        String tnsFolder = safe(info.ensureTnsFolder());
        String tnsProfile = safe(info.getTnsProfile());

        if (Strings.isEmptyOrSpaces(tnsFolder)) {
            throw new UnsupportedOperationException(txt("msg.mcp.exception.TnsFolderNotConfigured"));
        }
        if (Strings.isEmptyOrSpaces(tnsProfile)) {
            throw new UnsupportedOperationException(txt("msg.mcp.exception.TnsProfileNotConfigured"));
        }

        File tnsFile = Paths.get(tnsFolder, "tnsnames.ora").toFile();
        if (!tnsFile.isFile()) {
            throw new UnsupportedOperationException(txt("msg.mcp.exception.TnsFileNotFound", tnsFile.getAbsolutePath()));
        }

        try {
            TnsProfile profile = TnsNamesParser.get(tnsFile).getProfiles().stream()
                    .filter(p -> p.getProfile().equalsIgnoreCase(tnsProfile))
                    .findFirst()
                    .orElseThrow(() -> new UnsupportedOperationException(
                            txt("msg.mcp.exception.TnsProfileNotFound", tnsProfile, tnsFile.getAbsolutePath())));

            String descriptor = safe(profile.getDescriptor()).trim();
            if (descriptor.isEmpty()) {
                throw new UnsupportedOperationException(txt("msg.mcp.exception.TnsProfileDescriptorEmpty", tnsProfile));
            }
            return "jdbc:oracle:thin:@" + descriptor;
        } catch (UnsupportedOperationException e) {
            throw e;
        } catch (Exception e) {
            throw new UnsupportedOperationException(txt("msg.mcp.exception.TnsFileParseFailed", tnsFile.getAbsolutePath()), e);
        }
    }

    private static String build(McpServerDefinition definition, String connectionUrl) {
        @NonNls StringBuilder sb = new StringBuilder();

        appendYamlField(sb, "", "transport", definition.getTransportType().isHttp() ? "http" : "stdio");
        sb.append("httpPort: ").append(definition.getHttpPort()).append("  # used when transport is http").append('\n');
        sb.append('\n');

        sb.append("dataSource:\n");
        appendYamlField(sb, "  ", "url", redactSensitiveParameters(connectionUrl));
        sb.append("  # username: YOUR_USER  # uncomment to override wallet credentials\n");
        sb.append("  # password: YOUR_PASS  # uncomment to override wallet credentials\n");
        sb.append('\n');

        sb.append("tools:\n");
        for (McpToolDefinition t : definition.getTools()) {
            String toolName = t.getName();
            String description = t.getDescription();
            sb.append("  ").append(toolName).append(":\n");
            appendYamlField(sb, "    ", "description", safe(description, "SQL tool"));
            appendYamlField(sb, "    ", "statement", safe(t.getStatement(), "SELECT 1 FROM dual"));

            List<McpToolParam> params = t.getParameters() != null ? t.getParameters() : List.of();
            if (!params.isEmpty()) {
                sb.append("    parameters:\n");
                for (McpToolParam row : params) {
                    sb.append("      - name: ").append(SqlParameterParser.stripColon(row.getName())).append('\n');
                    sb.append("        type: ").append(row.getType().getSchemaType()).append('\n');
                    if (Strings.isNotEmpty(row.getType().getSchemaFormat())) {
                        sb.append("        format: ").append(row.getType().getSchemaFormat()).append('\n');
                    }
                    if (Strings.isNotEmpty(row.getDescription())) {
                        appendYamlField(sb, "        ", "description", row.getDescription());
                    }
                    sb.append("        required: ").append(row.isRequired()).append('\n');
                }
            }
        }

        return sb.toString();
    }

    private static void appendYamlField(StringBuilder sb, String indent, @NonNls String key, @NonNls String value) {
        String normalized = value == null ? "" : value;
        if (normalized.contains("\n")) {
            sb.append(indent).append(key).append(": |").append('\n');
            String[] lines = normalized.split("\\R", -1);
            for (String line : lines) {
                sb.append(indent).append("  ").append(line).append('\n');
            }
        } else {
            sb.append(indent).append(key).append(": ").append(yamlValue(normalized)).append('\n');
        }
    }

    private static String yamlValue(String v) {
        if (v == null || v.isEmpty()) return "\"\"";
        boolean needsQuotes = v.contains(":") || v.contains("#") || v.contains("\"")
                || v.contains("'") || v.contains("{") || v.contains("}")
                || v.contains("[") || v.contains("]") || v.startsWith(" ") || v.endsWith(" ");
        if (!needsQuotes) return v;
        return "\"" + v.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private static String safe(String value) {
        return value != null ? value : "";
    }

    private static String safe(String value, String defaultValue) {
        return value != null && !value.isEmpty() ? value : defaultValue;
    }
}
