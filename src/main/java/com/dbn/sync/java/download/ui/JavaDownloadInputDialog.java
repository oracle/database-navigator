/*
 * Copyright 2025 Oracle and/or its affiliates
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
import com.dbn.sync.java.download.JavaDownloadBatch;
import com.dbn.sync.java.download.JavaDownloadManager;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.swing.Action;

import static com.dbn.nls.NlsResources.txt;

@Getter
public class JavaDownloadInputDialog extends DBNDialog<JavaDownloadInputForm> {
    private final JavaDownloadBatch batch;

    public JavaDownloadInputDialog(JavaDownloadBatch batch) {
        super(batch.getProject(), txt("msg.java.title.DownloadJavaContent"), false);
        this.batch = batch;
        setDefaultSize(600, 600);
        init();
    }

    @NotNull
    @Override
    protected JavaDownloadInputForm createForm() {
        return new JavaDownloadInputForm(this);
    }

    private void startDownload() {
        // apply the form field values to the input
        JavaDownloadInputForm inputForm = getForm();
        inputForm.applyUserInput();

        JavaDownloadManager manager = getJavaDownloadManager();
        manager.startDownload(batch);
    }

    @NotNull
    private JavaDownloadManager getJavaDownloadManager() {
        return JavaDownloadManager.getInstance(getProject());
    }


    @Override
    protected final Action[] initializeActions() {
        renameAction(getOKAction(), "Download");
        return actions(
                getOKAction(),
                getCancelAction());
    }

    @Override
    protected void doOKAction() {
        startDownload();
        super.doOKAction();
    }
}
