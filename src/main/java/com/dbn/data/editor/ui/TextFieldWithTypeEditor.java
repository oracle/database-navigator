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
import com.intellij.openapi.project.Project;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.dbn.nls.NlsResources.txt;

@Setter
public class TextFieldWithTypeEditor extends TextFieldWithEditor {
    private Runnable openAction;

    public TextFieldWithTypeEditor(@NotNull Project project, @Nullable DBNTable parentTable) {
        super(project, parentTable,
                txt("msg.dataEditor.title.EditRecord"),
                txt("msg.dataEditor.title.EditRecord"));
    }

    @Override
    public void openEditor() {
        if (openAction != null) openAction.run();
    }
}
