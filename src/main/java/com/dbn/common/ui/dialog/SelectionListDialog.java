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
package com.dbn.common.ui.dialog;

import com.intellij.openapi.project.Project;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import java.util.List;

@Getter
public class SelectionListDialog<T> extends DBNDialog<SelectionListForm<T>> {
    private final List<T> elements;
    private final T initialSelection;
    private final Object contextObject;


    public SelectionListDialog(Project project,
                               String title,
                               @NotNull List<T> elements,
                               @Nullable T initialSelection,
                               @Nullable Object contextObject) {
        super(project, title, false);
        this.elements = elements;
        this.initialSelection = initialSelection;
        this.contextObject = contextObject;
        init();
    }

    public void setSelectButtonText(String text) {
        super.setOKButtonText(text);
    }

    @NotNull
    @Override
    protected SelectionListForm<T> createForm() {
        return new SelectionListForm<>(this, contextObject);
    }

    public List<T> getSelection() {
        if (!isOK()) return null;
        return getForm().getSelectionList().getSelectedValuesList();
    }

    @Override
    public JComponent getPreferredFocusedComponent() {
        return getForm().getSelectionList();
    }
}
