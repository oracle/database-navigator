/*
 * Copyright 2024 Oracle and/or its affiliates
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dbn.ddl.ui;

import com.dbn.common.file.VirtualFileInfo;
import com.dbn.common.text.TextContent;
import com.dbn.common.ui.dialog.DBNDialog;
import com.dbn.connection.ConnectionHandler;
import com.dbn.ddl.DDLFileAttachmentManager;
import com.dbn.object.common.DBSchemaObject;
import com.dbn.object.lookup.DBObjectRef;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import javax.swing.AbstractAction;
import javax.swing.Action;
import java.awt.event.ActionEvent;
import java.util.List;

import static com.dbn.common.text.TextContent.plain;

public class AttachDDLFileDialog extends DBNDialog<SelectDDLFileForm> {
    private final DBObjectRef<DBSchemaObject> object;
    private final boolean showLookupOption;
    private final List<VirtualFileInfo> fileInfos;

    public AttachDDLFileDialog(List<VirtualFileInfo> fileInfos, @NotNull DBSchemaObject object, boolean showLookupOption) {
        super(object.getProject(), "Attach DDL file", true);
        this.fileInfos = fileInfos;
        this.object = DBObjectRef.of(object);
        this.showLookupOption = showLookupOption;
        renameAction(getOKAction(), "Attach selected");
        setDefaultSize(700, 400);
        init();
    }

    @NotNull
    @Override
    protected SelectDDLFileForm createForm() {
        DBSchemaObject object = getObject();
        String typeName = object.getTypeName();
        TextContent hintText = plain(
                "Following DDL files were found matching the name of the " + object.getQualifiedNameWithType() + ".\n" +
                        "NOTE: Attached DDL files will become readonly and their content will change automatically when the " + typeName + " is edited.\n\n" +
                        "Select the files to attach to this " + typeName + ".");
        return new SelectDDLFileForm(this, object, fileInfos, hintText, showLookupOption);
    }

    @Override
    @NotNull
    protected final Action[] createActions() {
        return new Action[]{
                getOKAction(),
                new SelectAllAction(),
                new SelectNoneAction(),
                getCancelAction()
        };
    }

    @NotNull
    public DBSchemaObject getObject() {
        return DBObjectRef.ensure(object);
    }

    private class SelectAllAction extends AbstractAction {
        private SelectAllAction() {
            super("Attach all");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            getForm().selectAll();
            doOKAction();
        }
    }

    private class SelectNoneAction extends AbstractAction {
        private SelectNoneAction() {
            super("Attach none");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            SelectDDLFileForm component = getForm();
            component.selectNone();
            if (showLookupOption && component.isDoNotPromptSelected()) {
                ConnectionHandler connection = getObject().getConnection();
                connection.getSettings().getDetailSettings().setEnableDdlFileBinding(false);
            }
            close(2);
        }
    }

    @Override
    protected void doOKAction() {
        SelectDDLFileForm component = getForm();
        DBSchemaObject object = getObject();
        Project project = object.getProject();
        DDLFileAttachmentManager fileAttachmentManager = DDLFileAttachmentManager.getInstance(project);
        List<VirtualFileInfo> fileInfos = component.getSelection();
        for (VirtualFileInfo fileInfo : fileInfos) {
            fileAttachmentManager.attachDDLFile(object.ref(), fileInfo.getFile());
        }
        if (showLookupOption && component.isDoNotPromptSelected()) {
            ConnectionHandler connection = object.getConnection();
            connection.getSettings().getDetailSettings().setEnableDdlFileBinding(false);
        }

        super.doOKAction();
    }
}
