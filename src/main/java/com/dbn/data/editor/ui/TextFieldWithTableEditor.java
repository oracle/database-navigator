/*
 * Copyright 2026 Oracle and/or its affiliates
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

package com.dbn.data.editor.ui;

import com.dbn.common.ui.table.DBNTable;
import com.dbn.data.value.NestedTableValue;
import com.dbn.editor.data.model.DatasetEditorModelCell;
import com.dbn.editor.data.nested.DBNestedTableViewerDialog;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.dbn.nls.NlsResources.txt;

public final class TextFieldWithTableEditor extends TextFieldWithEditor {
    public TextFieldWithTableEditor(@NotNull Project project, @Nullable DBNTable parentTable) {
        super(project, parentTable,
                txt("app.objects.action.ViewData"),
                txt("app.objects.action.ViewData"));
    }

    @Override
    public void openEditor() {
        if (getUserValueHolder() instanceof DatasetEditorModelCell cell &&
                cell.getUserValue() instanceof NestedTableValue nestedTableValue) {
            DBNestedTableViewerDialog.show(nestedTableValue, cell);
        }
    }
}
