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

import com.dbn.batch.BatchManager;
import com.dbn.common.ui.dialog.DBNDialog;
import com.dbn.sync.java.download.JavaDownloadBatch;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Action;

/**
 * Wrapper factory result dialog
 * Lists all the database objects that were created as part of execution wrapper factory activity
 *
 * @author Dan Cioca (Oracle)
 */
@Getter
public class JavaDownloadResultDialog extends DBNDialog<JavaDownloadResultForm> {

    private final JavaDownloadBatch batch;
    private final Action openAllAction = createAction("Open All", () -> openJavaEditors(false));
    private final Action openSelectedAction = createAction("Open Selected", () -> openJavaEditors(true));

    public JavaDownloadResultDialog(JavaDownloadBatch batch) {
        super(batch.getProject(), "Java Download Result", false);
        //this.setDefaultSize(380, 420);
        this.setModal(false);  // non-modal: to allow opening the editors from the dialog
        this.setAutoSize(true);
        this.batch = batch;
        renameAction(getCancelAction(), "Close");
        openSelectedAction.setEnabled(false);
        openAllAction.setEnabled(batch.getDownloadedFiles().size() < 15);
        init();
    }

    @NotNull
    @Override
    protected Action[] createActions() {
        return createActions(
                createErrorAction(),
                openAllAction,
                openSelectedAction,
                getCancelAction()
        );
    }

    @Nullable
    private Action createErrorAction() {
        if (!batch.getMessages().hasErrors()) return null;

        return createAction("Show Errors", () -> {
            BatchManager batchManager = BatchManager.getInstance(getProject());
            batchManager.showErrorDialog(batch);
        });
    }

    @Override
    protected @NotNull JavaDownloadResultForm createForm() {
        return new JavaDownloadResultForm(this, batch);
    }

    private void openJavaEditors(boolean selected) {
        getForm().openJavaEditors(selected);
        close(OK_EXIT_CODE);
    }

}
