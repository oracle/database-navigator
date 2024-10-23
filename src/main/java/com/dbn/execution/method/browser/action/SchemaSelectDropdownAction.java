package com.dbn.execution.method.browser.action;

import com.dbn.common.action.ComboBoxAction;
import com.dbn.connection.ConnectionHandler;
import com.dbn.execution.method.browser.ui.MethodExecutionBrowserForm;
import com.dbn.object.DBSchema;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.Presentation;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class SchemaSelectDropdownAction extends ComboBoxAction {
    MethodExecutionBrowserForm browserComponent;

    public SchemaSelectDropdownAction(MethodExecutionBrowserForm browserComponent) {
        this.browserComponent = browserComponent;
    }

    @Override
    @NotNull
    protected DefaultActionGroup createPopupActionGroup(@NotNull JComponent component, @NotNull DataContext dataContext) {
        DefaultActionGroup actionGroup = new DefaultActionGroup();
        ConnectionHandler connection = browserComponent.getSettings().getConnection();
        if (connection != null) {
            for (DBSchema schema : connection.getObjectBundle().getSchemas()) {
                SchemaSelectAction schemaSelectAction = new SchemaSelectAction(browserComponent, schema);
                actionGroup.add(schemaSelectAction);
            }
        }
        return actionGroup;
    }

    @Override
    public void update(AnActionEvent e) {
        Presentation presentation = e.getPresentation();
        String text = "Schema";
        Icon icon = null;

        DBSchema schema = browserComponent.getSettings().getSelectedSchema();
        if (schema != null) {
            text = schema.getName();
            icon = schema.getIcon();
        }

        presentation.setText(text, false);
        presentation.setIcon(icon);
    }
 }