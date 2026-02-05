package com.dbn.mcp;

import com.dbn.common.dispose.DisposableContainers;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.util.UserInterface;
import com.dbn.common.util.Dialogs;
import com.dbn.connection.ConnectionHandler;
import com.dbn.mcp.models.ToolDefinitionModel;
import com.dbn.mcp.ui.ToolDefinitionCreateDialog;
import com.dbn.mcp.ui.ToolDefinitionListItemForm;
import com.intellij.openapi.Disposable;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class ToolDefinitionListForm extends DBNFormBase {
    private JPanel mainPanel;
    private JPanel listPanel;
    private JPanel actionPanel;
    private JButton addButton;

    private final List<ToolDefinitionListItemForm> toolDefinitionListItemFormList = DisposableContainers.list(this);
    
    /**
     * Supplier to get the current connection from parent form.
     * We use a Supplier instead of storing the ConnectionHandler directly because
     * the user might change the connection selection after this form is created.
     */
    private final Supplier<ConnectionHandler> connectionSupplier;

    public ToolDefinitionListForm(@Nullable Disposable parent, Supplier<ConnectionHandler> connectionSupplier) {
        super(parent);
        this.connectionSupplier = connectionSupplier;
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        initAddButton();
        actionPanel.add(addButton);
    }

    private void initAddButton() {
        addButton = new JButton("Add Tool");
        addButton.addActionListener(e -> {
            ConnectionHandler connection = connectionSupplier.get();
            Dialogs.show(() -> new ToolDefinitionCreateDialog(getProject(), connection),
                    (dialog, exitCode) -> {
                        ToolDefinitionModel toolDefinitionModel = dialog.getForm().getToolDefinitionModel();
                        createObjectPanel(toolDefinitionModel);
                    });
        });
    }

    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }

    public void removeObjectPanel(ToolDefinitionListItemForm toolDefinitionListItemForm) {
        toolDefinitionListItemFormList.remove(toolDefinitionListItemForm);
        listPanel.remove(toolDefinitionListItemForm.getComponent());

        // rebuild index
        for (int i = 0; i < toolDefinitionListItemFormList.size(); i++) {
            toolDefinitionListItemFormList.get(i).setIndex(i);
        }

        UserInterface.repaint(mainPanel);
        validateInput();
    }

    public ToolDefinitionListItemForm createObjectPanel(@Nullable ToolDefinitionModel toolDefinitionModel) {
        ToolDefinitionListItemForm toolDefinitionListItemForm = new ToolDefinitionListItemForm(
                this, 
                toolDefinitionListItemFormList.size(), 
                toolDefinitionModel
        );
        toolDefinitionListItemFormList.add(toolDefinitionListItemForm);
        listPanel.add(toolDefinitionListItemForm.getComponent());

        if (isInitialized()) {
            UserInterface.repaint(mainPanel);
            toolDefinitionListItemForm.focus();
        }

        return toolDefinitionListItemForm;
    }

    public List<ToolDefinitionModel> getToolDefinitionModelList() {
        return toolDefinitionListItemFormList.stream()
                .map(ToolDefinitionListItemForm::getToolDefinitionModel)
                .collect(Collectors.toList());
    }
}
