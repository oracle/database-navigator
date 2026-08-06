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

package com.dbn.mcp.ui;

import com.dbn.mcp.model.McpServerImplementation;
import com.dbn.mcp.registry.McpServerStatus;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.JBUI;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import java.awt.Color;
import java.awt.Font;
import java.awt.datatransfer.StringSelection;

import static com.dbn.common.icon.Icons.MCP_SERVER_CONTAINER;
import static com.dbn.common.icon.Icons.MCP_SERVER_JAR;
import static com.dbn.common.icon.Icons.MCP_SERVER_NATIVE;
import static com.dbn.nls.NlsResources.txt;

/**
 * Presentation of the MCP server model enums, shared by the dashboard list and the details form.
 * Kept out of the model classes so those stay free of UI dependencies.
 */
@UtilityClass
final class McpServerPresentation {

    static Icon icon(@NotNull McpServerImplementation implementation) {
        if (implementation.isContainer()) return MCP_SERVER_CONTAINER;
        if (implementation.isNative()) return MCP_SERVER_NATIVE;
        return MCP_SERVER_JAR;
    }

    static String implementationName(@NotNull McpServerImplementation implementation) {
        if (implementation.isContainer()) return txt("msg.mcp.text.ImplementationContainer");
        if (implementation.isNative()) return txt("msg.mcp.text.ImplementationNative");
        return txt("msg.mcp.text.ImplementationJar");
    }

    static Icon statusIcon(@NotNull McpServerStatus status) {
        return switch (status) {
            case BUILT -> AllIcons.General.InspectionsOK;
            case DEPLOYED -> AllIcons.Actions.Commit;
            case STALE -> AllIcons.General.Warning;
        };
    }

    static String statusName(@NotNull McpServerStatus status) {
        return switch (status) {
            case BUILT -> txt("msg.mcp.text.StatusBuilt");
            case DEPLOYED -> txt("msg.mcp.text.StatusDeployed");
            case STALE -> txt("msg.mcp.text.StatusStale");
        };
    }

    static Color statusColor(@NotNull McpServerStatus status) {
        return switch (status) {
            case BUILT -> new JBColor(new Color(110, 110, 110), new Color(160, 160, 160));
            case DEPLOYED -> new JBColor(new Color(45, 125, 55), new Color(105, 190, 115));
            case STALE -> new JBColor(new Color(180, 105, 0), new Color(230, 155, 55));
        };
    }

    static Color implementationColor() {
        return new JBColor(new Color(70, 110, 165), new Color(110, 150, 210));
    }

    /** Borderless icon button - a secondary action must not compete with the value it belongs to. */
    static void initIconButton(JButton button, Icon icon, String tooltip, Runnable action) {
        button.setIcon(icon);
        button.setToolTipText(tooltip);
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setBorder(JBUI.Borders.empty(1, 4));
        button.addActionListener(e -> action.run());
    }

    static void copyToClipboard(String content) {
        CopyPasteManager.getInstance().setContents(new StringSelection(content));
    }

    static Font monospaced(JComponent component) {
        return new Font(Font.MONOSPACED, Font.PLAIN, component.getFont().getSize());
    }
}
