package com.dbn.mcp.ui;

import com.dbn.common.action.BasicAction;
import com.dbn.common.color.Colors;
import com.dbn.common.icon.Icons;
import com.dbn.common.ui.component.DBNComponent;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.util.Fonts;
import com.dbn.common.util.Actions;
import com.dbn.common.util.Dialogs;
import com.dbn.mcp.model.McpToolDefinition;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.components.fields.ExpandableTextField;
import com.intellij.util.ui.UIUtil;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import java.awt.BorderLayout;

import static com.dbn.common.util.Conditional.when;

@Getter
@Setter
public class McpToolDefinitionListItemForm extends DBNFormBase {
    private JPanel mainPanel;
    private ExpandableTextField toolSql;
    private JPanel removeActionPanel;
    private JLabel nameLabel;
    private JTextPane descriptionTextPane;

    private McpToolDefinition toolDefinition;

    public McpToolDefinitionListItemForm(DBNComponent parent, @NotNull McpToolDefinition toolDefinition) {
        super(parent);
        this.toolDefinition = toolDefinition;

        ActionToolbar actionToolbar = Actions.createActionToolbar(removeActionPanel, true, new EditObjectAction(), new RemoveObjectAction());
        removeActionPanel.add(actionToolbar.getComponent(), BorderLayout.NORTH);

        toolSql.setEditable(false);
        nameLabel.setFont(Fonts.regular(2));
        descriptionTextPane.setForeground(Colors.faded(UIUtil.getLabelForeground()));
        toolSql.setVisible(false);

        updateToolFields();

    }

    private void updateToolFields() {
        nameLabel.setText(toolDefinition.getName());
        descriptionTextPane.setText(toolDefinition.getDescription());
        toolSql.setText(toolDefinition.getStatement());
    }

    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }

    public class EditObjectAction extends BasicAction {
        EditObjectAction() {
            super("Edit Tool", null, Icons.ACTION_EDIT);
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            McpToolDefinitionListForm parent = getParentForm();
            Dialogs.show(() -> new McpToolDefinitionDialog(
                            getProject(),
                            parent.getConnection(),
                            parent.getServerDefinition(),
                            toolDefinition),
                    (dialog, exitCode) ->
                            when(exitCode == DialogWrapper.OK_EXIT_CODE,
                                    () -> updateToolFields()));
        }
    }

    public class RemoveObjectAction extends BasicAction {
        RemoveObjectAction() {
            super(txt("app.objects.action.RemoveObject", "Tool"), null, Icons.ACTION_DELETE);
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            getParentForm().removeToolDefinitionForm(McpToolDefinitionListItemForm.this);
        }
    }

    @NotNull
    public McpToolDefinitionListForm getParentForm() {
        return ensureParentComponent();
    }
}
