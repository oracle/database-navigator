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

package com.dbn.vector.ui.request;

import com.dbn.common.ui.file.VirtualFileListForm;
import com.dbn.vector.model.request.EmbeddingSourceFiles;
import com.dbn.vector.ui.VectorToolboxFormBase;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;
import javax.swing.JPanel;

import static com.dbn.nls.NlsResources.txt;

public class EmbeddingSourceFilesForm extends VectorToolboxFormBase {
    private JPanel mainPanel;
    private JPanel fileListPanel;
    private final VirtualFileListForm fileListForm;

    public EmbeddingSourceFilesForm(@NotNull VectorToolboxFormBase parent) {
        super(parent);
        fileListForm = new VirtualFileListForm(this, txt("app.vector.title.SourceFiles"));
        fileListPanel.add(fileListForm.getComponent());
    }

    @Override
    protected void initValidation() {
        addValidation(fileListForm.getFileList(), l -> l.getModel().getSize() > 0, txt("msg.vector.error.SelectAtLeastOneFile"));
    }

    @Override
    public void resetFormChanges() {
        fileListForm.initFileData(() -> getConfig().getFileSources());
    }

    @Override
    public void applyFormChanges() {
        EmbeddingSourceFiles config = getConfig();
        config.setFilePaths(fileListForm.getFilePaths());
    }

    private EmbeddingSourceFiles getConfig() {
        return getEmbeddingRequest().getSourceConfig().getSourceFiles();
    }

    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }

    public int getFileCount() {
        return fileListForm.getFileList().getModel().getSize();
    }
}
