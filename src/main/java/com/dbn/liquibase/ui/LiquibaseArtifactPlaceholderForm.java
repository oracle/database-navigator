package com.dbn.liquibase.ui;

import com.dbn.common.text.TextContent;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.form.DBNHeaderForm;
import com.dbn.common.ui.form.DBNHintForm;
import com.dbn.common.ui.link.HyperLinkForm;
import com.dbn.connection.ConnectionHandler;
import org.jetbrains.annotations.NotNull;

import javax.swing.JButton;
import javax.swing.JPanel;
import java.awt.BorderLayout;

import static com.dbn.nls.NlsResources.txt;

/** Placeholder card shown when a connection has no mapped Liquibase artifact. */
public class LiquibaseArtifactPlaceholderForm extends DBNFormBase {
    private JPanel mainPanel;
    private JPanel headerPanel;
    private JPanel hintPanel;
    private JPanel documentationPanel;
    private JButton attachButton;

    LiquibaseArtifactPlaceholderForm(DBNFormBase parent, ConnectionHandler connection, Runnable attachAction) {
        super(parent);
        headerPanel.add(new DBNHeaderForm(this, connection).getComponent());
        hintPanel.add(new DBNHintForm(this, TextContent.plain(txt("cfg.liquibase.hint.ArtifactPlaceholder")), null, true).getComponent());
        documentationPanel.add(HyperLinkForm.create(
                txt("cfg.liquibase.label.Documentation"),
                txt("cfg.liquibase.link.Documentation"),
                "https://docs.liquibase.com/oss/reference-guide-4-33").getComponent(), BorderLayout.EAST);
        attachButton.addActionListener(e -> attachAction.run());
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }
}
