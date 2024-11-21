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
import com.dbn.ddl.DDLFileAttachmentManager;
import com.dbn.object.common.DBSchemaObject;
import com.dbn.object.lookup.DBObjectRef;
import org.jetbrains.annotations.NotNull;

import javax.swing.AbstractAction;
import javax.swing.Action;
import java.awt.event.ActionEvent;
import java.util.List;

import static com.dbn.common.text.TextContent.plain;

public class DetachDDLFileDialog extends DBNDialog<SelectDDLFileForm> {
    private final List<VirtualFileInfo> fileInfos;
    private final DBObjectRef<DBSchemaObject> objectRef;
    public DetachDDLFileDialog(@NotNull List<VirtualFileInfo> fileInfos, @NotNull DBSchemaObject object) {
        super(object.getProject(), "Detach DDL files", true);
        this.fileInfos = fileInfos;
        this.objectRef = DBObjectRef.of(object);
        renameAction(getOKAction(), "Detach selected");
        setDefaultSize(700, 400);
        init();
    }

    @NotNull
    @Override
    protected SelectDDLFileForm createForm() {
        DBSchemaObject object = objectRef.ensure();
        TextContent hintText = plain(
                "Following DDL files are currently attached the " + object.getQualifiedNameWithType() + ".\n\n" +
                "Select the files to detach from this " + object.getTypeName() + ".");
        return new SelectDDLFileForm(this, object, fileInfos, hintText, false);
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

    private class SelectAllAction extends AbstractAction {
        private SelectAllAction() {
            super("Detach all");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            getForm().selectAll();
            doOKAction();
        }
    }

    private class SelectNoneAction extends AbstractAction {
        private SelectNoneAction() {
            super("Detach none");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            getForm().selectNone();
            doOKAction();
        }
    }

    @Override
    protected void doOKAction() {
        DDLFileAttachmentManager fileAttachmentManager = DDLFileAttachmentManager.getInstance(getProject());
        List<VirtualFileInfo> fileInfos = getForm().getSelection();
        for (VirtualFileInfo fileInfo : fileInfos) {
            fileAttachmentManager.detachDDLFile(fileInfo.getFile());
        }
        super.doOKAction();
    }
}
