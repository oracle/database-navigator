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

package com.dbn.sync.java.download.ui;

import com.dbn.common.ui.dialog.DBNDialog;
import com.dbn.sync.java.download.JavaDownloadContext;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.swing.AbstractAction;
import javax.swing.Action;

/**
 * Wrapper factory result dialog
 * Lists all the database objects that were created as part of execution wrapper factory activity
 *
 * @author Dan Cioca (Oracle)
 */
@Getter
public class JavaDownloadResultDialog extends DBNDialog<JavaDownloadResultForm> {

    private final JavaDownloadContext context;
    private final AbstractAction openAllAction = createAction("Open All", () -> openJavaEditors(false));
    private final AbstractAction openSelectedAction = createAction("Open Selected", () -> openJavaEditors(true));

    public JavaDownloadResultDialog(Project project, JavaDownloadContext context) {
        super(project, "Java Download Result", false);
        //this.setDefaultSize(380, 420);
        this.setModal(false);  // non-modal: to allow opening the editors from the dialog
        this.setAutoSize(true);
        this.context = context;
        renameAction(getCancelAction(), "Close");
        openSelectedAction.setEnabled(false);
        openAllAction.setEnabled(context.getDownloadedFiles().size() < 15);
        init();
    }

    @NotNull
    @Override
    protected Action[] createActions() {
        return new Action[]{
                openAllAction,
                openSelectedAction,
                getCancelAction()
        };
    }

    @Override
    protected @NotNull JavaDownloadResultForm createForm() {
        return new JavaDownloadResultForm(this, context);
    }

    private void openJavaEditors(boolean selected) {
        getForm().openJavaEditors(selected);
        close(OK_EXIT_CODE);
    }

}
