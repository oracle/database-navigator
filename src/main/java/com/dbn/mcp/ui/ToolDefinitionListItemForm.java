package com.dbn.mcp.ui;

import com.dbn.common.action.BasicAction;
import com.dbn.common.icon.Icons;
import com.dbn.common.ui.component.DBNComponent;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.util.Actions;
import com.dbn.common.util.Dialogs;
import com.dbn.mcp.ToolDefinitionListForm;
import com.dbn.mcp.model.ToolDefinitionModel;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.components.JBTextField;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.BorderLayout;

@Getter
@Setter
public class ToolDefinitionListItemForm extends DBNFormBase {
    private JPanel mainPanel;
    private JBTextField toolName;
    private JBTextField toolDescription;
    private com.intellij.ui.components.fields.ExpandableTextField toolSql;
    private JPanel removeActionPanel;

    private ToolDefinitionModel toolDefinitionModel;

    private int index;

    public ToolDefinitionListItemForm(DBNComponent parent, int index, @Nullable ToolDefinitionModel toolDefinitionModel) {
        super(parent);
        this.index = index;
        this.toolDefinitionModel = toolDefinitionModel;
        ActionToolbar actionToolbar = Actions.createActionToolbar(removeActionPanel, true, new EditObjectAction(), new RemoveObjectAction());
        removeActionPanel.add(actionToolbar.getComponent(), BorderLayout.NORTH);

        toolName.setEditable(false);
        toolDescription.setEditable(false);
        toolSql.setEditable(false);

        if (toolDefinitionModel != null) {
            toolName.setText(toolDefinitionModel.getName());
            toolDescription.setText(toolDefinitionModel.getDescription());
            toolSql.setText(toolDefinitionModel.getStatement());
        }
    }

    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }

    public void focus() {
        toolName.requestFocus();
    }

    public class EditObjectAction extends BasicAction {
        EditObjectAction() {
            super("Edit Tool", null, Icons.ACTION_EDIT);
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            ToolDefinitionListForm parent = getParentForm();
            Dialogs.show(() -> new ToolDefinitionCreateDialog(getProject(), parent.getConnection(), toolDefinitionModel),
                    (dialog, exitCode) -> {
                        if (exitCode != DialogWrapper.OK_EXIT_CODE) return;
                        toolDefinitionModel = dialog.getForm().getToolDefinitionModel();
                        toolName.setText(toolDefinitionModel.getName());
                        toolDescription.setText(toolDefinitionModel.getDescription());
                        toolSql.setText(toolDefinitionModel.getStatement());
                    });
        }
    }

    public class RemoveObjectAction extends BasicAction {
        RemoveObjectAction() {
            super(txt("app.objects.action.RemoveObject", "Tool"), null, Icons.ACTION_DELETE);
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            getParentForm().removeObjectPanel(ToolDefinitionListItemForm.this);
        }
    }

    @NotNull
    public ToolDefinitionListForm getParentForm() {
        return ensureParentComponent();
    }
}
