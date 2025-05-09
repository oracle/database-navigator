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

package com.dbn.sync.java.upload.ui;

import com.dbn.common.ui.dialog.DBNDialog;
import com.dbn.sync.java.upload.JavaUploadBatch;
import com.dbn.sync.java.upload.JavaUploadManager;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.swing.Action;

@Getter
public class JavaUploaderInputDialog extends DBNDialog<JavaUploadInputForm> {
    private final JavaUploadBatch batch;

    public JavaUploaderInputDialog(JavaUploadBatch batch) {
        super(batch.getProject(), "Upload Java Content", false);
        this.batch = batch;
        renameAction(getOKAction(), "Upload");
        init();
    }

    @NotNull
    @Override
    protected JavaUploadInputForm createForm() {
        return new JavaUploadInputForm(this);
    }

    private void startUpload() {
        // apply the form field values to the input
        JavaUploadInputForm inputForm = getForm();
        inputForm.applyUserInput();

        JavaUploadManager manager = getJavaUploadManager();
        manager.startUpload(batch);
    }

    @NotNull
    private JavaUploadManager getJavaUploadManager() {
        return JavaUploadManager.getInstance(getProject());
    }


    @Override
    protected final Action @NotNull [] createActions() {
        return new Action[]{
                getOKAction(),
                getCancelAction()};
    }

    @Override
    protected void doOKAction() {
        startUpload();
        super.doOKAction();
    }
}