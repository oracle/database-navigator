package com.dbn.mcp.ui;

import com.dbn.common.action.BasicAction;
import com.dbn.common.icon.Icons;
import com.dbn.common.ui.component.DBNComponent;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.util.Actions;
import com.dbn.mcp.ToolDefinitionListForm;
import com.dbn.mcp.model.ToolDefinitionModel;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.ui.components.JBTextField;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
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
        ActionToolbar actionToolbar = Actions.createActionToolbar(removeActionPanel, true, new RemoveObjectAction());
        removeActionPanel.add(actionToolbar.getComponent(), BorderLayout.NORTH);

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
