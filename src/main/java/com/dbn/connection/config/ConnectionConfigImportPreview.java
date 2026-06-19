/*
 * Copyright 2024 Oracle and/or its affiliates
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

package com.dbn.connection.config;

import com.dbn.common.database.AuthenticationInfo;
import com.dbn.common.database.DatabaseInfo;
import com.dbn.common.text.TextContent;
import com.dbn.common.util.Messages;
import com.intellij.openapi.project.Project;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static com.dbn.common.util.Strings.isNotEmpty;
import static com.dbn.common.util.Strings.truncateWithMiddleEllipsis;
import static com.dbn.nls.NlsResources.txt;

@UtilityClass
public class ConnectionConfigImportPreview {
    private static final int PREVIEW_SIZE = 5;
    private static final int PREVIEW_NAME_LENGTH = 32;
    private static final int PREVIEW_TARGET_LENGTH = 84;

    public static boolean confirm(@Nullable Project project, List<ConnectionSettings> connections) {
        int option = Messages.showConfirmationDialog(
                project,
                txt("msg.connection.title.ImportConnections"),
                TextContent.htmlMessage(txt("msg.connection.question.ImportClipboardConnections", connections.size(), createPreview(connections))).getText(),
                Messages.OPTIONS_CONTINUE_CANCEL,
                0);
        return option == 0;
    }

    private static String createPreview(List<ConnectionSettings> connections) {
        StringBuilder preview = new StringBuilder("<br>");
        int previewSize = Math.min(connections.size(), PREVIEW_SIZE);
        for (int i = 0; i < previewSize; i++) {
            ConnectionSettings connection = connections.get(i);
            ConnectionDatabaseSettings databaseSettings = connection.getDatabaseSettings();
            DatabaseInfo databaseInfo = databaseSettings.getDatabaseInfo();
            preview.append("<b>");
            preview.append(escapeHtml(truncateWithMiddleEllipsis(databaseSettings.getName(), PREVIEW_NAME_LENGTH)));
            preview.append("</b>");
            preview.append(" (");
            preview.append(escapeHtml(databaseSettings.getDatabaseType().getName()));
            preview.append(") ");

            List<String> securitySettings = getSecuritySettings(connection, databaseInfo);
            if (!securitySettings.isEmpty()) {
                preview.append(" - ");
                preview.append(escapeHtml(String.join(", ", securitySettings)));
            }

            String target = isNotEmpty(databaseInfo.getUrl()) ? databaseInfo.getUrl() : databaseInfo.getHost();
            if (isNotEmpty(target)) {
                preview.append("<br><span style='${MONOSPACE_FONT_STYLE}'>");
                preview.append(escapeHtml(truncateWithMiddleEllipsis(target, PREVIEW_TARGET_LENGTH)));
                preview.append("</span>");
            }
            preview.append("<br><br>");
        }

        int remainingCount = connections.size() - previewSize;
        if (remainingCount > 0) {
            preview.append(escapeHtml(txt("msg.connection.text.AdditionalClipboardConnections", remainingCount)));
        }
        return preview.toString();
    }

    private static List<String> getSecuritySettings(ConnectionSettings connection, DatabaseInfo databaseInfo) {
        List<String> settings = new ArrayList<>();
        if (connection.getSshTunnelSettings().isActive()) {
            settings.add(txt("msg.connection.token.SshTunnel"));
        }
        if (connection.getSslSettings().isActive()) {
            settings.add(txt("msg.connection.token.Ssl"));
        }
        if (isNotEmpty(databaseInfo.getTnsFolder())) {
            settings.add(txt("msg.connection.token.WalletTnsFolder"));
        }

        AuthenticationInfo authenticationInfo = connection.getDatabaseSettings().getAuthenticationInfo();
        if (isNotEmpty(authenticationInfo.getTokenConfigFile())) {
            settings.add(txt("msg.connection.token.TokenConfig"));
        }
        return settings;
    }

    private static String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
