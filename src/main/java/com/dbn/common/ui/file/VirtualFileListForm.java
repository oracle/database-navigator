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

package com.dbn.common.ui.file;

import com.dbn.common.thread.Dispatch;
import com.dbn.common.ui.component.DBNComponent;
import com.dbn.common.ui.form.DBNFormBase;
import com.intellij.openapi.util.NlsContexts.Label;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.ToolbarDecorator;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.swing.JLabel;
import javax.swing.JPanel;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static com.dbn.common.ui.util.Decorators.createToolbarDecorator;
import static com.dbn.common.ui.util.Decorators.createToolbarDecoratorComponent;

public class VirtualFileListForm extends DBNFormBase {
    private JPanel component;
    private JLabel titleLabel;
    private JPanel listPanel;

    @Getter
    private final VirtualFileList fileList;

    public VirtualFileListForm(DBNComponent parent, @Label String title) {
        this(parent, title, new ArrayList<>());
    }

    public VirtualFileListForm(DBNComponent parent, @Label String title, List<VirtualFile> elements) {
        super(parent);
        titleLabel.setText(title);
        fileList = new VirtualFileList(elements);
        listPanel.add(initListComponent());
    }

    private JPanel initListComponent() {
        ToolbarDecorator decorator = createToolbarDecorator(fileList);
        decorator.setAddAction(b -> fileList.insertRows());
        decorator.setRemoveAction(b -> fileList.removeRows());
        decorator.setMoveUpAction(b -> fileList.moveRowsUp());
        decorator.setMoveDownAction(b -> fileList.moveRowsDown());

        return createToolbarDecoratorComponent(decorator, fileList);
    }

    public void initFileData(Supplier<List<VirtualFile>> supplier) {
        Dispatch.async(fileList, () -> {
            boolean enabled = fileList.isEnabled();
            try {
                return supplier.get();
            } finally {
                fileList.setEnabled(enabled);
            }

        }, l -> setFiles(l));
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return component;
    }

    public List<String> getFilePaths() {
        return fileList.getModel().getFilePaths();
    }

    public List<VirtualFile> getFiles() {
        return fileList.getFiles();
    }

    public void setFiles(List<VirtualFile> files) {
        VirtualFileListModel model = fileList.getModel();
        model.reset(files);
    }
}
