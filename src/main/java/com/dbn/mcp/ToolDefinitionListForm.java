package com.dbn.mcp;

import lombok.Getter;
import com.dbn.common.dispose.DisposableContainers;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.util.UserInterface;
import com.dbn.common.util.Dialogs;
import com.dbn.connection.ConnectionHandler;
import com.dbn.mcp.model.ToolDefinitionModel;
import com.dbn.mcp.ui.ToolDefinitionCreateDialog;
import com.dbn.mcp.ui.ToolDefinitionListItemForm;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.ui.DialogWrapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import com.intellij.util.ui.UIUtil;

public class ToolDefinitionListForm extends DBNFormBase {
    private JPanel mainPanel;
    private JPanel listPanel;
    private JPanel actionPanel;
    private JButton addButton;

    private final List<ToolDefinitionListItemForm> toolDefinitionListItemFormList = DisposableContainers.list(this);
    @Getter private final ConnectionHandler connection;
    private JLabel emptyLabel;

    public ToolDefinitionListForm(@Nullable Disposable parent, @NotNull ConnectionHandler connection) {
        super(parent);
        this.connection = connection;
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        initEmptyLabel();
        initAddButton();
        actionPanel.add(addButton);
    }

    private void initEmptyLabel() {
        emptyLabel = new JLabel("No tools defined", SwingConstants.CENTER);
        emptyLabel.setForeground(UIUtil.getLabelForeground());
        listPanel.add(emptyLabel, BorderLayout.CENTER);
    }

    private void initAddButton() {
        addButton = new JButton("Add Tool");
        addButton.addActionListener(e -> {
            Dialogs.show(() -> new ToolDefinitionCreateDialog(getProject(), connection, null, getToolNamesExcluding(null)),
                    (dialog, exitCode) -> {
                        if (exitCode != DialogWrapper.OK_EXIT_CODE) return;
                        ToolDefinitionModel toolDefinitionModel = dialog.getForm().getToolDefinitionModel();
                        createObjectPanel(toolDefinitionModel);
                        validateInput();
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

        for (int i = 0; i < toolDefinitionListItemFormList.size(); i++) {
            toolDefinitionListItemFormList.get(i).setIndex(i);
        }

        emptyLabel.setVisible(toolDefinitionListItemFormList.isEmpty());
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
        emptyLabel.setVisible(false);

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

    public List<String> getToolNamesExcluding(@Nullable ToolDefinitionListItemForm itemToExclude) {
        return toolDefinitionListItemFormList.stream()
                .filter(item -> item != itemToExclude)
                .map(ToolDefinitionListItemForm::getToolDefinitionModel)
                .filter(Objects::nonNull)
                .map(ToolDefinitionModel::getName)
                .collect(Collectors.toList());
    }
}
