package com.dbn.liquibase.ui;

import com.dbn.common.text.TextContent;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.form.DBNHeaderForm;
import com.dbn.common.ui.form.DBNHintForm;
import com.dbn.common.ui.link.DBNHyperlinkLabel;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.dbn.liquibase.model.LiquibaseWorkspace;
import org.jetbrains.annotations.NotNull;

import javax.swing.JButton;
import javax.swing.JPanel;

import static com.dbn.nls.NlsResources.txt;

/** Placeholder card shown when a connection has no mapped Liquibase artifact. */
public class LiquibaseArtifactPlaceholderForm extends DBNFormBase {
    private JPanel mainPanel;
    private JPanel headerPanel;
    private JPanel hintPanel;
    private DBNHyperlinkLabel documentationLink;
    private JButton attachButton;

    private final LiquibaseWorkspace workspace;
    private final ConnectionId connectionId;

    LiquibaseArtifactPlaceholderForm(DBNFormBase parent, LiquibaseWorkspace workspace, ConnectionHandler connection) {
        super(parent);
        this.workspace = workspace;
        connectionId = connection.getConnectionId();
        headerPanel.add(new DBNHeaderForm(this, connection).getComponent());
        hintPanel.add(new DBNHintForm(this, TextContent.plain(txt("cfg.liquibase.hint.ArtifactPlaceholder")), null, true).getComponent());
        documentationLink.setHyperlinkText(txt("cfg.liquibase.link.LiquibaseDocumentation"));
        documentationLink.setHyperlinkTarget("https://docs.liquibase.com/oss/reference-guide-4-33");
        attachButton.addActionListener(e -> attachWorkspace());
    }

    private void attachWorkspace() {
        workspace.ensureArtifact(connectionId);
        LiquibaseWorkspaceSettingsForm parentComponent = ensureParentComponent();
        parentComponent.artifactAttached(connectionId);
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }
}
