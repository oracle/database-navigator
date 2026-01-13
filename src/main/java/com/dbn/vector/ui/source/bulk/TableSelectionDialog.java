package com.dbn.vector.ui.source.bulk;

import com.dbn.common.ui.dialog.DBNDialog;
import com.dbn.common.util.Messages;
import com.dbn.connection.ConnectionHandler;
import com.dbn.vector.model.request.EmbeddingSourceTable;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import javax.swing.Action;
import java.util.ArrayList;
import java.util.List;

public class TableSelectionDialog extends DBNDialog<TableSelectionForm> {
    private final ConnectionHandler connection;
    
    // Store selected tables BEFORE dialog closes (components get disposed after close)
    private List<EmbeddingSourceTable> selectedTableSources = new ArrayList<>();

    public TableSelectionDialog(@NotNull Project project, @NotNull ConnectionHandler connection) {
        super(project, "Add Tables", true);
        this.connection = connection;
        setDefaultSize(650, 550);
        init();
    }

    @Override
    @NotNull
    protected TableSelectionForm createForm() {
        return new TableSelectionForm(this, connection);
    }

    @Override
    protected Action[] createActions() {
        renameAction(getOKAction(), "Add Selected");
        return super.createActions();
    }

    @Override
    protected void doOKAction() {
        TableSelectionForm form = getForm();

        // Validate - all selected tables must have column config
        if (!form.isValid()) {
            List<String> missing = form.getTablesWithoutConfig();
            Messages.showWarningDialog(
                    getProject(),
                    "Missing Configuration",
                    "Please configure ID and Data columns for: " + String.join(", ", missing)
            );
            return;
        }

        // Capture selected tables BEFORE closing (components will be disposed after close)
        this.selectedTableSources = form.getSelectedTableSources();
        System.out.println("TableSelectionDialog.doOKAction: captured " + selectedTableSources.size() + " tables");

        super.doOKAction();
    }

    /**
     * Get the configured table sources after dialog closes.
     * This returns the cached list that was captured before the dialog closed.
     */
    public List<EmbeddingSourceTable> getSelectedTableSources() {
        return selectedTableSources;
    }
}
