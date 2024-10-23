package com.dbn.navigation.action;

import com.dbn.common.action.BasicActionGroup;
import com.dbn.common.clipboard.Clipboard;
import com.dbn.common.dispose.Disposer;
import com.dbn.common.ui.util.TextFields;
import com.dbn.common.util.Editors;
import com.dbn.common.util.Strings;
import com.dbn.connection.*;
import com.dbn.editor.DatabaseFileEditorManager;
import com.dbn.navigation.object.DBObjectLookupModel;
import com.dbn.navigation.options.ObjectsLookupSettings;
import com.dbn.object.DBSchema;
import com.dbn.object.action.AnObjectAction;
import com.dbn.object.common.DBObject;
import com.dbn.object.common.property.DBObjectProperty;
import com.dbn.options.ProjectSettings;
import com.intellij.ide.actions.GotoActionBase;
import com.intellij.ide.util.gotoByName.ChooseByNamePopup;
import com.intellij.ide.util.gotoByName.ChooseByNamePopupComponent;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.ListPopup;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.dbn.common.dispose.Checks.isNotValid;
import static com.dbn.common.ui.util.TextFields.onTextChange;
import static com.dbn.common.util.Conditional.whenNotEmpty;

public class GoToDatabaseObjectAction extends GotoActionBase implements DumbAware {
    private ConnectionId latestConnectionId;
    private String latestSchemaName = "";
    private String latestUsedText;
    private String latestPredefinedText;
    private String latestClipboardText;
    private ChooseByNamePopup popup;
    @Override
    public void gotoActionPerformed(AnActionEvent event) {

        //FeatureUsageTracker.getInstance().triggerFeatureUsed("navigation.popup.file");
        Project project = event.getData(PlatformDataKeys.PROJECT);
        if (isNotValid(project)) return;

        ProjectSettings projectSettings = ProjectSettings.get(project);
        ObjectsLookupSettings objectsLookupSettings = projectSettings.getNavigationSettings().getObjectsLookupSettings();
        boolean promptConnectionSelection = objectsLookupSettings.getPromptConnectionSelection().value();

        if (promptConnectionSelection) {
            ConnectionHandler singleConnection = null;
            DefaultActionGroup actionGroup = new DefaultActionGroup();

            ConnectionManager connectionManager = ConnectionManager.getInstance(project);
            ConnectionBundle connectionBundle = connectionManager.getConnectionBundle();
            if (connectionBundle.getConnections().size() > 0) {
                if ((actionGroup.getChildrenCount() > 1)) {
                    actionGroup.addSeparator();
                }

                for (ConnectionHandler connection : connectionBundle.getConnections()) {
                    SelectConnectionAction connectionAction = new SelectConnectionAction(connection);
                    actionGroup.add(connectionAction);
                    singleConnection = connection;
                }
            }

            if (actionGroup.getChildrenCount() > 1) {
                removeActionLock();
                ListPopup popupBuilder = JBPopupFactory.getInstance().createActionGroupPopup(
                        "Select Connection / Schema for Lookup",
                        actionGroup,
                        event.getDataContext(),
                        //JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
                        false,
                        true,
                        true,
                        null,
                        actionGroup.getChildrenCount(),
                        preselect -> {
                            if (preselect instanceof SelectConnectionAction) {
                                SelectConnectionAction selectConnectionAction = (SelectConnectionAction) preselect;
                                return latestConnectionId == selectConnectionAction.getConnection().getConnectionId();
                            } else if (preselect instanceof SelectSchemaAction) {
                                SelectSchemaAction selectSchemaAction = (SelectSchemaAction) preselect;
                                DBSchema object = selectSchemaAction.getTarget();
                                return object != null && Objects.equals(latestSchemaName, object.getName());
                            }
                            return false;
                        });

/*                    if (popupBuilder instanceof ListPopupImpl) {
                    ListPopupImpl listPopup = (ListPopupImpl) popupBuilder;
                    listPopup.getList().setCellRenderer(new DefaultListCellRenderer(){
                        @Override
                        public Component getListCellRendererComponent(JList<?> actions, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                            PopupFactoryImpl.ActionItem actionItem  = (PopupFactoryImpl.ActionItem) value;
                            Component component = super.getListCellRendererComponent(actions, value, index, isSelected, cellHasFocus);
                            if (component instanceof JLabel) {
                                JLabel label = (JLabel) component;
                                label.setIcon(actionItem.getIcon());
                                label.setText(actionItem.getText().replace("&", ""));
                                AnAction action = actionItem.getAction();
                                if (!isSelected && action instanceof SelectConnectionAction) {
                                    SelectConnectionAction selectConnectionAction = (SelectConnectionAction) action;
                                    label.setBackground(selectConnectionAction.connection.getEnvironmentType().getColor());
                                }
                            }
                            return component;
                        }
                    });
                }*/
                popupBuilder.showCenteredInCurrentWindow(project);
            } else {
                showLookupPopup(event, project, singleConnection, null);
            }
        } else {
            ConnectionManager connectionManager = ConnectionManager.getInstance(project);
            ConnectionHandler connection = connectionManager.getActiveConnection(project);
            showLookupPopup(event, project, connection, null);
        }
    }


    private class SelectConnectionAction extends BasicActionGroup {
        private final ConnectionRef connection;

        private SelectConnectionAction(ConnectionHandler connection) {
            this.connection = ConnectionRef.of(connection);
            Presentation presentation = getTemplatePresentation();
            presentation.setText(connection.getName(), false);
            presentation.setIcon(connection.getIcon());
            presentation.setPerformGroup(true);
            setPopup(true);
        }

        public ConnectionHandler getConnection() {
            return connection.ensure();
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            ConnectionHandler connection = getConnection();
            Project project = connection.getProject();
            showLookupPopup(e, project, connection, null);
            latestConnectionId = connection.getConnectionId();
        }

        @NotNull
        @Override
        public AnAction[] loadChildren(AnActionEvent e) {
            List<SelectSchemaAction> schemaActions = new ArrayList<>();
            ConnectionHandler connection = getConnection();
            for (DBSchema schema : connection.getObjectBundle().getSchemas()) {
                schemaActions.add(new SelectSchemaAction(schema));
            }
            return schemaActions.toArray(new AnAction[0]);
        }
    }

    private class SelectSchemaAction extends AnObjectAction<DBSchema> {
        private SelectSchemaAction(DBSchema schema) {
            super(schema);
        }

        @Override
        protected void actionPerformed(
                @NotNull AnActionEvent e,
                @NotNull Project project,
                @NotNull DBSchema object) {

            showLookupPopup(e, project, object.getConnection(), object);
            latestSchemaName = object.getName();
        }
    }


    private void showLookupPopup(AnActionEvent e, Project project, ConnectionHandler connection, DBSchema selectedSchema) {
        if (connection == null) {
            // remove action lock here since the pop-up will not be fired to remove it onClose()
            removeActionLock();
        } else {
            DBObjectLookupModel model = new DBObjectLookupModel(project, connection, selectedSchema);
            String predefinedText = getPredefinedText(project);

            popup = ChooseByNamePopup.createPopup(project, model, getPsiContext(e), predefinedText);
            JTextField textField = popup.getTextField();
            onTextChange(textField, event -> whenNotEmpty(TextFields.getText(textField), text -> latestUsedText = text));
            popup.invoke(new Callback(model), ModalityState.current(), false);
        }
    }

    private String getPredefinedText(Project project) {
        String predefinedText = null;
        FileEditor[] selectedEditors = FileEditorManager.getInstance(project).getSelectedEditors();
        for (FileEditor fileEditor : selectedEditors) {
            Editor editor = Editors.getEditor(fileEditor);
            if (editor != null) {
                predefinedText = editor.getSelectionModel().getSelectedText();
            }
            if (isValidPredefinedText(predefinedText)) {
                break;
            } else {
                predefinedText = null;
            }
        }

        String clipboardText = Strings.trim(Clipboard.getStringContent());
        if (predefinedText == null) {
            if (isValidPredefinedText(clipboardText)) {
                if (Strings.isNotEmpty(latestUsedText) &&
                        Objects.equals(clipboardText, latestClipboardText) &&
                        !Objects.equals(latestUsedText, clipboardText)) {

                    predefinedText = latestUsedText;
                } else {
                    predefinedText = clipboardText;
                }
            } else {
                predefinedText = latestPredefinedText;

            }
        }

        latestClipboardText = clipboardText;
        latestPredefinedText = Strings.trim(predefinedText);
        return latestPredefinedText;
    }

    private static boolean isValidPredefinedText(String predefinedText) {
        return predefinedText != null && predefinedText.length() < 40 && predefinedText.matches("^[a-zA-Z0-9 _\\-$#]*$");
    }

    private static void removeActionLock() {
        if (GoToDatabaseObjectAction.class.equals(myInAction)) {
            myInAction = null;
        }
    }

    private class Callback extends ChooseByNamePopupComponent.Callback {
        private final DBObjectLookupModel model;

        private Callback(DBObjectLookupModel model) {
            this.model = model;
        }

        @Override
        public void elementChosen(Object element) {
            if (element instanceof DBObject) {
                DBObject object = (DBObject) element;
                if (object.is(DBObjectProperty.EDITABLE)) {
                    Project project = object.getProject();
                    DatabaseFileEditorManager editorManager = DatabaseFileEditorManager.getInstance(project);
                    editorManager.connectAndOpenEditor(object, null, false, true);
                } else {
                    object.navigate(true);
                }
            }
        }

        @Override
        public void onClose() {
            removeActionLock();
            Disposer.dispose(model);
            popup = null;
        }
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        super.update(e);
        e.getPresentation().setText("Database Object...");
    }
}
