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

import com.dbn.common.ui.form.DBNForm;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.link.Hyperlinks;
import com.dbn.mcp.build.McpClientConfiguration;
import com.dbn.mcp.registry.McpServerRecord;
import com.dbn.mcp.registry.McpServerStatus;
import com.intellij.icons.AllIcons;
import com.intellij.ide.actions.RevealFileAction;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.HyperlinkLabel;
import org.jetbrains.annotations.NotNull;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DateFormat;
import java.util.Date;

import static com.dbn.mcp.ui.McpServerPresentation.initIconButton;
import static com.dbn.mcp.ui.McpServerPresentation.monospaced;
import static com.dbn.nls.NlsResources.txt;

/** The property rows of a generated server: what it is, where it is and how to reach it. */
public class McpServerOverviewForm extends DBNFormBase {
    private JPanel mainPanel;
    private JPanel imageRowPanel;
    private JPanel endpointRowPanel;
    private JLabel statusLabel;
    private JLabel statusValueLabel;
    private JLabel imageLabel;
    private JLabel imageValueLabel;
    private JLabel folderLabel;
    private JLabel folderValueLabel;
    private JLabel endpointLabel;
    private JLabel builtLabel;
    private JLabel builtValueLabel;
    private JLabel documentationLabel;
    private JButton imageCopyButton;
    private JButton endpointCopyButton;
    private JButton revealButton;
    private HyperlinkLabel endpointLink;
    private HyperlinkLabel readmeLink;

    private final McpServerRecord record;

    public McpServerOverviewForm(@NotNull DBNForm parent, @NotNull McpServerRecord record) {
        super(parent);
        this.record = record;

        initStatusRow();
        initImageRow();
        initEndpointRow();
        initFolderRow();
        initBuiltRow();
        initDocumentationRow();
    }

    private void initStatusRow() {
        McpServerStatus status = record.getStatus();
        statusLabel.setText(txt("msg.mcp.text.Status"));
        statusValueLabel.setText(McpServerPresentation.statusName(status));
        statusValueLabel.setIcon(McpServerPresentation.statusIcon(status));
        statusValueLabel.setIconTextGap(5);
        if (status != McpServerStatus.BUILT) {
            statusValueLabel.setForeground(McpServerPresentation.statusColor(status));
        }
    }

    private void initImageRow() {
        String imageName = record.getImageName();
        if (!record.getImplementation().isContainer() || imageName == null) {
            setRowVisible(false, imageLabel, imageRowPanel);
            return;
        }

        imageLabel.setText(txt("msg.mcp.text.ContainerImage"));
        imageValueLabel.setText(imageName);
        imageValueLabel.setFont(monospaced(imageValueLabel));
        initIconButton(imageCopyButton, AllIcons.Actions.Copy,
                txt("app.mcp.button.CopyToClipboard"), () -> McpServerPresentation.copyToClipboard(imageName));
    }

    /**
     * The deployed endpoint when the server runs on Graal, otherwise the local address an HTTP
     * server serves on - for an HTTP server that is the most operationally useful value here.
     */
    private void initEndpointRow() {
        String endpoint =
                record.isDeployed() ? record.getDeployment().getEndpoint() :
                record.getTransportType().isHttp() ? McpClientConfiguration.localEndpoint(record.getDefinition()) :
                null;

        if (endpoint == null) {
            setRowVisible(false, endpointLabel, endpointRowPanel);
            return;
        }

        endpointLabel.setText(txt("msg.mcp.text.Endpoint"));
        Hyperlinks.initHyperlink(endpointLink, endpoint, endpoint);
        initIconButton(endpointCopyButton, AllIcons.Actions.Copy,
                txt("app.mcp.button.CopyToClipboard"), () -> McpServerPresentation.copyToClipboard(endpoint));
    }

    private void initFolderRow() {
        Path outputPath = record.getOutputPath();
        folderLabel.setText(txt("msg.mcp.text.OutputFolder"));
        // home-relative keeps the value column narrow, so the row actions stay near their values
        folderValueLabel.setText(FileUtil.getLocationRelativeToUserHome(outputPath.toString()));
        folderValueLabel.setToolTipText(outputPath.toString());
        initIconButton(revealButton, AllIcons.Actions.MenuOpen,
                txt("msg.mcp.button.RevealFolder"), () -> RevealFileAction.openFile(outputPath.toFile()));
    }

    private void initBuiltRow() {
        if (record.getBuildTimestamp() <= 0) {
            setRowVisible(false, builtLabel, builtValueLabel);
            return;
        }
        builtLabel.setText(txt("msg.mcp.text.BuiltOn"));
        builtValueLabel.setText(DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                .format(new Date(record.getBuildTimestamp())));
    }

    private void initDocumentationRow() {
        documentationLabel.setText(txt("msg.mcp.text.Documentation"));
        Hyperlinks.initHyperlink(readmeLink, txt("msg.mcp.text.ReadmeFile"), this::openReadme);
    }

    private void openReadme() {
        Path readmePath = record.getReadmePath();
        if (!Files.isRegularFile(readmePath)) return;

        VirtualFile file = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(readmePath);
        if (file != null) FileEditorManager.getInstance(ensureProject()).openFile(file, true);
    }

    private static void setRowVisible(boolean visible, JComponent... components) {
        for (JComponent component : components) component.setVisible(visible);
    }

    @Override
    protected @NotNull JComponent getMainComponent() {
        return mainPanel;
    }
}
