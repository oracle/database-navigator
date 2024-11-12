package com.dbn.common.editor;

import com.dbn.common.color.Colors;
import com.dbn.common.file.VirtualFileRef;
import com.dbn.common.icon.Icons;
import com.dbn.common.message.MessageType;
import com.dbn.common.project.ProjectRef;
import com.dbn.common.ui.form.DBNForm;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.mapping.FileConnectionContextManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.HyperlinkAdapter;
import com.intellij.ui.HyperlinkLabel;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.PlatformColors;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import java.awt.*;

public class EditorNotificationPanel extends JPanel{
    private JLabel label;
    private final JPanel actionsPanel;
    private final VirtualFileRef file;
    private final ProjectRef project;

    public EditorNotificationPanel(Project project, VirtualFile file, MessageType messageType) {
        super(new BorderLayout());
        this.file = VirtualFileRef.of(file);
        this.project = ProjectRef.of(project);

        setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 4));

        Dimension dimension = getPreferredSize();
        setPreferredSize(new Dimension((int) dimension.getWidth(), 28));

        //setPreferredSize(new Dimension(-1, 32));

        Icon icon = null;
        Color background;

        switch (messageType) {
            case INFO: {
                icon = Icons.COMMON_INFO;
                background = Colors.getInfoHintColor();
                break;
            }
            case WARNING:{
                icon = Icons.COMMON_WARNING;
                background = Colors.getWarningHintColor();
                break;
            }
            case ERROR:{
                //icon = AllIcons.General.Error;
                background = Colors.getErrorHintColor();
                break;
            }
            default:{
                //icon = AllIcons.General.Information;
                background = Colors.getLightPanelBackground();
                break;
            }
        }

        setIcon(icon);
        setBackground(background);

        actionsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        actionsPanel.setOpaque(false);
        add(actionsPanel, BorderLayout.EAST);
    }

    public void setContent(DBNForm form) {
        setContent(form.getComponent());
    }

    public void setContent(JComponent content) {
        add(content, BorderLayout.CENTER);
        content.setOpaque(false);
    }

    public void setText(String text) {
        if (label == null && text == null) return;

        initLabel();
        label.setText(text);
    }

    public void setIcon(Icon icon) {
        if (label == null && icon == null) return;

        initLabel();
        label.setIcon(icon);
    }

    private void initLabel() {
        if (label == null) {
            label = new JLabel();
            label.setBorder(JBUI.Borders.emptyRight(8));
            add(label, BorderLayout.WEST);
        }
    }

    protected void createActionLabel(String text, final Runnable action) {
        HyperlinkLabel label = new HyperlinkLabel(text, PlatformColors.BLUE, getBackground(), PlatformColors.BLUE);
        label.addHyperlinkListener(new HyperlinkAdapter() {
            @Override
            protected void hyperlinkActivated(HyperlinkEvent e) {
                action.run();
            }
        });
        actionsPanel.add(label);
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(0, 0);
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
}
