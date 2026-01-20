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

package com.dbn.common.editor;

import com.dbn.common.color.Colors;
import com.dbn.common.compatibility.Compatibility;
import com.dbn.common.compatibility.Workaround;
import com.dbn.common.dispose.ComponentDisposer;
import com.dbn.common.dispose.StatefulDisposable;
import com.dbn.common.file.VirtualFileRef;
import com.dbn.common.icon.Icons;
import com.dbn.common.message.MessageType;
import com.dbn.common.project.ProjectRef;
import com.dbn.common.ref.WeakRef;
import com.dbn.common.ui.form.DBNForm;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.mapping.FileConnectionContextManager;
import com.intellij.codeInsight.intention.IntentionActionWithOptions;
import com.intellij.openapi.editor.colors.ColorKey;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.panels.NonOpaquePanel;
import com.intellij.util.ui.JBUI;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.util.Objects;

import static com.dbn.common.ui.util.UserInterface.findChildComponent;
import static javax.swing.SwingConstants.RIGHT;

@Getter
@Setter
public class EditorNotificationPanel extends com.intellij.ui.EditorNotificationPanel implements StatefulDisposable {
    private boolean disposed;

    private final VirtualFileRef file;
    private final ProjectRef project;
    private final WeakRef<FileEditor> fileEditor;
    private final JPanel contentPanel;

    public EditorNotificationPanel(Project project, VirtualFile file, FileEditor fileEditor, MessageType messageType) {
        super(fileEditor, getBackground(messageType), getBackgroundKey(messageType));
        this.file = VirtualFileRef.of(file);
        this.fileEditor = WeakRef.of(fileEditor);
        this.project = ProjectRef.of(project);

        int hgap = JBUI.scale(8);

        remove(this.myLabel);
        this.contentPanel = new JPanel(new BorderLayout(hgap, 0));
        this.contentPanel.setOpaque(false);
        this.contentPanel.add(this.myLabel, BorderLayout.WEST);
        add(this.contentPanel, BorderLayout.WEST);

        this.myLabel.setForeground(Banner.FOREGROUND);
        this.myLabel.setIcon(getIcon(messageType));

        this.myLabel.setHorizontalAlignment(RIGHT);
        this.myLabel.setIconTextGap(hgap);
        this.myLabel.setBorder(null);
    }

    @Override
    public @Nullable IntentionActionWithOptions getIntentionAction() {
        // do not expose links as intentions (e.g. db-assistant "Configure")
        return null;
    }

    @Override
    @Workaround // duplicate notification panels - last resort
    public void addNotify() {
        super.addNotify();

        Container parent = getParent();
        if (parent instanceof NonOpaquePanel) {
            parent = parent.getParent();
        }

        if (parent == null) return;

        for (Component component : parent.getComponents()) {
            boolean duplicate = findChildComponent(component, c -> isDuplicate(c)) != null;
            if (duplicate) {
                parent.remove(component);
            }
        }
    }

    private boolean isDuplicate(JComponent c) {
        return c.getClass() == this.getClass() && c != this;
    }

    protected Icon getIcon(MessageType messageType) {
        return switch (messageType) {
            case INFO -> Icons.COMMON_INFO;
            case SUCCESS -> null;
            case WARNING -> Icons.COMMON_WARNING;
            case ERROR -> Icons.COMMON_ERROR;
            default -> null;
        };
    }

    private static Color getBackground(MessageType messageType) {
        return switch (messageType) {
            case INFO -> Banner.INFO_BACKGROUND;
            case SUCCESS -> Banner.SUCCESS_BACKGROUND;
            case WARNING -> Banner.WARNING_BACKGROUND;
            case ERROR -> Banner.ERROR_BACKGROUND;
            default -> Colors.getLightPanelBackground();
        };
    }

    private static ColorKey getBackgroundKey(MessageType messageType) {
        return switch (messageType) {
            case INFO -> ColorKey.createColorKey("Banner.infoBackground");
            case SUCCESS -> ColorKey.createColorKey("Banner.successBackground");
            case WARNING -> ColorKey.createColorKey("Banner.warningBackground");
            case ERROR -> ColorKey.createColorKey("Banner.errorBackground");
            default -> null;
        };
    }


    public void setContent(DBNForm form) {
        setContent(form.getComponent());
    }

    public void setContent(JComponent content) {
        contentPanel.add(content, BorderLayout.CENTER);
        content.setOpaque(false);
    }

    public void setIcon(Icon icon) {
        this.myLabel.setIcon(icon);
    }

    protected VirtualFile getFile() {
        return VirtualFileRef.ensure(file);
    }

    protected Project getProject() {
        return ProjectRef.ensure(project);
    }

    protected ConnectionHandler getConnection() {
        VirtualFile file = getFile();
        Project project = getProject();
        FileConnectionContextManager contextManager = FileConnectionContextManager.getInstance(project);

        return contextManager.getConnection(file);
    }

    protected ConnectionId getConnectionId() {
        ConnectionHandler connection = getConnection();
        return connection.getConnectionId();
    }

    @Override
    public void disposeInner() {
        ComponentDisposer.dispose(this);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        EditorNotificationPanel that = (EditorNotificationPanel) o;
        return Objects.equals(file, that.file) && Objects.equals(fileEditor, that.fileEditor);
    }

    @Override
    public int hashCode() {
        return Objects.hash(file, fileEditor);
    }

    /**
     * Copy of JBUI.CurrentTheme.Banner
     */
    @Compatibility
    @UtilityClass
    public static final class Banner {
        public static final Color INFO_BACKGROUND = JBColor.namedColor("Banner.infoBackground", 0xF5F8FE, 0x25324D);
        public static final Color SUCCESS_BACKGROUND = JBColor.namedColor("Banner.successBackground", 0xF2FCF3, 0x253627);
        public static final Color WARNING_BACKGROUND = JBColor.namedColor("Banner.warningBackground", 0xFFFAEB, 0x3d3223);
        public static final Color ERROR_BACKGROUND = JBColor.namedColor("Banner.errorBackground", 0xFFF7F7, 0x402929);
        public static final Color FOREGROUND = JBColor.namedColor("Banner.foreground", 0x0, 0xDFE1E5);
    }
}
